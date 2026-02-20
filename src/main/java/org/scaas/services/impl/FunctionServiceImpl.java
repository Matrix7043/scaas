package org.scaas.services.impl;

import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.exceptions.ResourceNotFoundException;
import org.scaas.protocol.mappers.ToFunctionResponse;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.security.CurrentUserService;
import org.scaas.services.FunctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final CurrentUserService currentUserService;
    private final FunctionRepository functionRepository;
    private final ToFunctionResponse mapper;

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
                .build();

        functionRepository.save(function);

        return mapper.toFunctionResponse(function);

    }

    @Override
    public Page<FunctionResponse> getFunctions(Pageable pageable) {

        User owner = currentUserService.getCurrentUser();
        return functionRepository.findByOwner(owner,pageable)
                .map(mapper::toFunctionResponse);

    }
    @Override
    public FunctionResponse updateFunctionById(UUID id, UpdateFunctionRequest request) {

        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwner(id, owner).orElseThrow(
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
    public FunctionResponse deleteFunctionById(UUID id) {

        User owner = currentUserService.getCurrentUser();

        Function function = functionRepository.findByIdAndOwner(id, owner).orElseThrow(
                () -> new ResourceNotFoundException("Function not found")
        );

        functionRepository.delete(function);

        return mapper.toFunctionResponse(function);
    }

    @Override
    public FunctionResponse getFunctionById(UUID id) {
        
        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found"));

        return mapper.toFunctionResponse(function);

    }


}
