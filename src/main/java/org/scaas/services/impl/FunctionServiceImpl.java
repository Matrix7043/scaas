package org.scaas.services.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.enumerations.DeploymentStatus;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.exceptions.ResourceNotFoundException;
import org.scaas.exceptions.StorageException;
import org.scaas.protocol.mappers.ToDeploymentResponse;
import org.scaas.protocol.mappers.ToFunctionResponse;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.DeploymentResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.security.CurrentUserService;
import org.scaas.services.DeploymentService;
import org.scaas.services.FunctionService;
import org.scaas.services.StorageService;
import org.scaas.utils.HashingUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final CurrentUserService currentUserService;
    private final FunctionRepository functionRepository;
    private final StorageService storageService;
    private final DeploymentService deploymentService;
    private final ToFunctionResponse mapper;
    private final ToDeploymentResponse dMapper;

    @Override
    public FunctionResponse createFunction(CreateFunctionRequest request) {

        User owner = currentUserService.getCurrentUser();
        Function function = Function.builder()
                .owner(owner)
                .name(request.name())
                .runtime(request.runtime())
                .entryPoint(request.entryPoint())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deploymentStatus(DeploymentStatus.NOT_DEPLOYED)
                .build();

        functionRepository.save(function);

        return mapper.toFunctionResponse(function);

    }

    @Override
    public Page<FunctionResponse> getFunctions(Pageable pageable) {

        User owner = currentUserService.getCurrentUser();
        return functionRepository.findByOwnerAndDeletedAtIsNull(owner,pageable)
                .map(mapper::toFunctionResponse);

    }

    @Override
    @Transactional
    public void replaceArtifact(UUID id, MultipartFile file) {

        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function with id " + id + " not found")
        );

        String extension = getExtension(file, function);
        if(!extension.equals(".py")) {
            throw new RuntimeException("Invalid file format");
        }

        String newHash = HashingUtil.hashFile(file);
        if(function.getHashCode() != null && Objects.equals(newHash, function.getHashCode())) {
            throw new RuntimeException("File already present");
        }
        if(DeploymentStatus.DEPLOYED.equals(function.getDeploymentStatus())
        || DeploymentStatus.NOT_DEPLOYED.equals(function.getDeploymentStatus())
        || DeploymentStatus.FAILED.equals(function.getDeploymentStatus())) {
            function.setDeploymentStatus(DeploymentStatus.OUTDATED);
        }

        try {
            if(function.getStoragePath() == null) {
                String newPath = storageService.upload(file, id);
                function.setStoragePath(newPath);
            } else {
                storageService.overwrite(function.getStoragePath(), file);
            }
            function.setHashCode(newHash);
        } catch (IOException e){
            throw new StorageException("File storage failed", e);
        }

        function.setUpdatedAt(LocalDateTime.now());
        functionRepository.save(function);

    }

    private static @NonNull String getExtension(MultipartFile file, Function function) {
        if(DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())){
            throw new RuntimeException("Artifact cannot be updated when deployment is in progress");
        }

        if(file == null || file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }

        String originalName = file.getOriginalFilename();

        if(originalName == null || !originalName.contains(".")) {
            throw new RuntimeException("Invalid file format");
        }

        return originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
    }

    @Override
    public FunctionResponse updateFunctionById(UUID id, UpdateFunctionRequest request) {

        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );

        if(request.name() != null && !request.name().isEmpty()) {
            function.setName(request.name());
        }
        if(request.runtime() != null){
            function.setRuntime(request.runtime());
        }
        if(request.entryPoint() != null && !request.entryPoint().isEmpty()) {
            function.setEntryPoint(request.entryPoint());
        }

        function.setUpdatedAt(LocalDateTime.now());

        Function updated = functionRepository.save(function);

        return mapper.toFunctionResponse(updated);
    }

    @Override
    public void deleteFunctionById(UUID id) {

        User owner = currentUserService.getCurrentUser();

        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );
        deploymentService.deleteDeployment(id);
        function.setDeletedAt(LocalDateTime.now());
        functionRepository.save(function);

    }

    @Override
    public FunctionResponse getFunctionById(UUID id) {
        
        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found"));

        return mapper.toFunctionResponse(function);

    }

    @Override
    public DeploymentResponse deployFunction(UUID id) {

        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );

        if(DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())){
            throw new RuntimeException("Deployment is already in progress");
        }

        File file = storageService.getFile(function.getStoragePath());

        if(!file.exists()) {
            throw new RuntimeException("File does not exist");
        }

        if(function.getHashCode() == null){
            throw new RuntimeException("Hash code not found");
        }

        boolean deployed = DeploymentStatus.DEPLOYED.equals(function.getDeploymentStatus());
        boolean hashChange = !Objects.equals(function.getHashCode(), function.getDeployedHashcode());
        String url;

        try {
            if (!deployed || hashChange) {
                function.setDeploymentStatus(DeploymentStatus.DEPLOYING);
                functionRepository.save(function);
                url = deploymentService.deploy(id, file);
            } else throw new RuntimeException("No visible changes for redeployment");
        } catch (ObjectOptimisticLockingFailureException e){
            throw new RuntimeException("Deployment is already in progress");
        } catch (Exception e){
            function.setDeploymentStatus(DeploymentStatus.FAILED);
            function.setUpdatedAt(LocalDateTime.now());
            functionRepository.save(function);
            return dMapper.toDeploymentResponse(function);
        }

        LocalDateTime time =  LocalDateTime.now();
        function.setUpdatedAt(time);
        function.setDeployedHashcode(function.getHashCode());
        function.setDeploymentStatus(DeploymentStatus.DEPLOYED);
        function.setDeployedAt(time);
        function.setInvocationURL(url);
        functionRepository.save(function);

        return dMapper.toDeploymentResponse(function);
    }

}
