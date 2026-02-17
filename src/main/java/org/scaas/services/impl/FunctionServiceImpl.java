package org.scaas.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.protocol.mappers.ToFunctionResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.security.CurrentUserService;
import org.scaas.services.FunctionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final CurrentUserService currentUserService;
    private final FunctionRepository functionRepository;
    private final ToFunctionResponse toFunctionResponse;

    @Override
    public FunctionResponse createFunction(String name, String runtime, String entrypoint) {

        User owner = currentUserService.getCurrentUser();
        Function function = Function.builder()
                .owner(owner)
                .name(name)
                .runtime(runtime)
                .entrypoint(entrypoint)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        functionRepository.save(function);

        return toFunctionResponse.toFunctionResponse(function);

    }


    @Override
    public List<FunctionResponse> getFunctions() {

        User owner = currentUserService.getCurrentUser();
        return functionRepository.findByOwner(owner)
                .stream()
                .map(toFunctionResponse::toFunctionResponse)
                .toList();

    }

    @Override
    public FunctionResponse getFunctionById(UUID id) {
        
        User owner = currentUserService.getCurrentUser();
        Function function = functionRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new RuntimeException("Function not found"));

        return toFunctionResponse.toFunctionResponse(function);

    }


}
