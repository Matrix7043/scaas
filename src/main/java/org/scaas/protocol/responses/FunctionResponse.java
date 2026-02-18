package org.scaas.protocol.responses;

import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FunctionResponse(UUID id,
                               String name,
                               String entrypoint,
                               Runtime runtime,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {

}
