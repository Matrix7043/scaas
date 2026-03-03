package org.scaas.protocol.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateFunctionRequest(
        @Size(max = 100, message = "Name point must be at most {max} chars")
        String name,
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
        Integer pids) {}