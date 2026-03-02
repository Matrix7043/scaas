package org.scaas.protocol.requests;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

@Builder
public record CreateFunctionRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 100, message = "Name must be at most {max} chars")
        String name,
        @NotNull(message = "Runtime must be specified")
        Runtime runtime,
        @Size(max = 100, message = "Entry point must be at most {max} chars")
        String entryPoint,
        @DecimalMin(value = "0.5", message = "CPU cores must at least be {min}")
        @DecimalMax(value = "4", message = "CPU cores must at most be {max}")
        Double cpuCores,
        @DecimalMin(value = "100", message = "mem must at least be {min}")
        @DecimalMax(value = "1024", message = "mem must at most be {max}")
        Integer mem,
        @DecimalMin(value = "10", message = "PIDs cores must at least be {min}")
        @DecimalMax(value = "64", message = "PIDs cores must at most be {max}")
        Integer pids) {

        public CreateFunctionRequest {
                if(cpuCores == null) cpuCores = 0.5;
                if(mem == null) mem = 256;
                if(pids == null) pids = 50;
                if(entryPoint == null || entryPoint.isBlank()) entryPoint = "handler";
        }

}
