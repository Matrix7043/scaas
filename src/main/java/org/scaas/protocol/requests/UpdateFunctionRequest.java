package org.scaas.protocol.requests;

import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

@Builder
public record UpdateFunctionRequest(
        String name,
        Runtime runtime,
        String entryPoint
) {
}
