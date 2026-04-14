package org.scaas.services.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.enumerations.DeploymentStatus;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.exceptions.DeploymentConflictException;
import org.scaas.exceptions.DeploymentServiceException;
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
    private final DeploymentStateService deploymentStateService;
    private final HashingUtil hashingUtil;
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
                .cpuCores(request.cpuCores())
                .memory(request.mem())
                .pidCount(request.pids())
                .build();

        Function updated = functionRepository.save(function);

        return mapper.toFunctionResponse(updated);

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

        String newHash = hashingUtil.hashFile(file);
        if(function.getCurrentHashCode() != null && Objects.equals(newHash, function.getCurrentHashCode())) {
            throw new RuntimeException("No changes detected");
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
            function.setCurrentHashCode(newHash);
        } catch (IOException e){
            throw new StorageException("File storage failed", e);
        }

        function.setUpdatedAt(LocalDateTime.now());
        functionRepository.save(function);

    }

    private static @NonNull String getExtension(MultipartFile file, Function function) {
        if(DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())){
            throw new DeploymentConflictException("Artifact cannot be updated when deployment is in progress");
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
    @Transactional
    public FunctionResponse updateFunctionById(UUID id, UpdateFunctionRequest request) {

        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );

        if (DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())) {
            throw new DeploymentConflictException("Function cannot be updated when deployment is in progress");
        }

        if(request.name() != null && !request.name().isEmpty()) {
            function.setName(request.name());
        }
        if(request.entryPoint() != null && !request.entryPoint().isEmpty()) {
            function.setEntryPoint(request.entryPoint());
        }
        if(request.cpuCores() != null) {
            function.setCpuCores(request.cpuCores());
        }
        if(request.mem() != null) {
            function.setMemory(request.mem());
        }
        if(request.pids() != null ) {
            function.setPidCount(request.pids());
        }
        function.setUpdatedAt(LocalDateTime.now());
        if(DeploymentStatus.DEPLOYED.equals(function.getDeploymentStatus())
        || DeploymentStatus.FAILED.equals(function.getDeploymentStatus())) {
            function.setDeploymentStatus(DeploymentStatus.OUTDATED);
        }
        Function updated = functionRepository.save(function);

        return mapper.toFunctionResponse(updated);
    }

    @Override
    public void deleteFunctionById(UUID id) {

        User owner = currentUserService.getCurrentUser();

        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );

        if (DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())) {
            throw new DeploymentConflictException("Function cannot be deleted when deployment is in progress");
        }

        if(function.getCurrentHashCode() != null && function.getStoragePath() != null) {
            deploymentService.deleteDeployment(id, function.getCurrentHashCode());
            deploymentService.deleteDeployment(id, function.getDeployedHashcode());
        }
        function.setDeletedAt(LocalDateTime.now());
        functionRepository.save(function);

    }

    @Override
    public String getArtifactContent(UUID id) {
        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found"));

        if (function.getStoragePath() == null) {
            return null;
        }

        File file = storageService.getFile(function.getStoragePath());
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            return java.nio.file.Files.readString(file.toPath());
        } catch (IOException e) {
            throw new StorageException("Failed to read artifact", e);
        }
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

        Function function = functionRepository
                .findByIdAndOwnerAndDeletedAtIsNull(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found"));

        if (DeploymentStatus.DEPLOYING.equals(function.getDeploymentStatus())) {
            throw new DeploymentConflictException("Deployment is already in progress");
        }

        if (function.getStoragePath() == null) {
            throw new ResourceNotFoundException("Artifact cannot be found");
        }

        File file = storageService.getFile(function.getStoragePath());

        if (file == null || !file.exists()) {
            throw new ResourceNotFoundException("File does not exist");
        }

        if (function.getCurrentHashCode() == null) {
            throw new ResourceNotFoundException("HashCode cannot be found");
        }

        boolean deployed = DeploymentStatus.DEPLOYED.equals(function.getDeploymentStatus());
        boolean hashChange = Objects.equals(function.getCurrentHashCode(), function.getDeployedHashcode());

        if(hashChange && deployed) {
            throw new DeploymentConflictException("No visible changes found for redeployment");
        }

        function = deploymentStateService.markDeploying(function);

        String url;

        try {
            url = deploymentService.deploy(
                    id,
                    function.getCurrentHashCode(),
                    file,
                    function.getCpuCores(),
                    function.getMemory(),
                    function.getPidCount()
            );
        } catch (DeploymentServiceException e) {
            deploymentStateService.markFailed(function);
            throw new RuntimeException("Deployment Failed");
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new DeploymentConflictException("Deployment is already in progress");
        }
        Function updated = deploymentStateService.markDeployed(function, url);

        return dMapper.toDeploymentResponse(updated);
    }

}
