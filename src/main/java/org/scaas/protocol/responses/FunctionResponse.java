package org.scaas.protocol.responses;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FunctionResponse(UUID id,
                               String name,
                               String entrypoint,
                               String runtime,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {

}
