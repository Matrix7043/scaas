package org.scaas.protocol.mappers;

import org.scaas.domain.entites.Function;
import org.scaas.protocol.responses.DeploymentResponse;
import org.springframework.stereotype.Component;

@Component
public class ToDeploymentResponse {

    public DeploymentResponse toDeploymentResponse(Function function) {
        return DeploymentResponse.builder()
                .id(function.getId())
                .name(function.getName())
                .invocationURL(function.getInvocationURL())
                .status(function.getDeploymentStatus())
                .deployedAt(function.getDeployedAt()).build();
    }
}
