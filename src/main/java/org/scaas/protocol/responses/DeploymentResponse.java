package org.scaas.protocol.responses;

import lombok.Builder;
import org.scaas.domain.enumerations.DeploymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DeploymentResponse(
        UUID id,
        String name,
        String invocationURL,
        DeploymentStatus status,
        LocalDateTime deployedAt
) {
}
