package org.scaas.protocol.requests;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

@Builder
public record UpdateFunctionRequest(
        @Size(max = 100, message = "Name point must be at most {max} chars")
        String name,
        Runtime runtime,
        @Size(max = 100, message = "Entry point must be at most {max} chars")
        String entryPoint
) {
}
