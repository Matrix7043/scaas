package org.scaas.protocol.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

@Builder
public record CreateFunctionRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 100, message = "Name must be at most {max} chars")
        String name,
        @NotNull(message = "Runtime must be specified")
        Runtime runtime,
        @NotBlank(message = "Entry point must not be blank")
        @Size(max = 100, message = "Entry point must be at most {max} chars")
        String entryPoint) {}
