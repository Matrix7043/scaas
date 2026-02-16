package org.scaas.protocol.responses;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class FunctionResponse {

    private UUID id;
    private String name;
    private String entrypoint;
    private String runtime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
