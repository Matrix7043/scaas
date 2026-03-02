package org.scaas.services.impl;

import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.Function;
import org.scaas.domain.enumerations.DeploymentStatus;
import org.scaas.domain.repositories.FunctionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeploymentStateService {

    private final FunctionRepository functionRepository;

    @Transactional
    public Function markDeploying(Function function) {
        function.setDeploymentStatus(DeploymentStatus.DEPLOYING);
        return functionRepository.save(function);
    }

    @Transactional
    public Function markDeployed(Function function, String url) {
        function.setDeploymentStatus(DeploymentStatus.DEPLOYED);
        function.setDeployedHashcode(function.getCurrentHashCode());
        function.setInvocationURL(url);
        function.setDeployedAt(LocalDateTime.now());
        function.setUpdatedAt(LocalDateTime.now());
        return functionRepository.save(function);
    }

    @Transactional
    public void markFailed(Function function) {
        function.setDeploymentStatus(DeploymentStatus.FAILED);
        function.setUpdatedAt(LocalDateTime.now());
        functionRepository.save(function);
    }
}
