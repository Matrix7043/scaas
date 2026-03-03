package org.scaas.protocol.responses;

import lombok.Builder;
import org.scaas.domain.enumerations.DeploymentStatus;
import org.scaas.domain.enumerations.Runtime;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FunctionResponse(UUID id,
                               String name,
                               String entryPoint,
                               Runtime runtime,
                               DeploymentStatus deploymentStatus,
                               Integer pid,
                               Double cpuCores,
                               Integer memory,
                               String invocationURL,
                               LocalDateTime deployedAt,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt,
                               Boolean hasArtifact) {

}
