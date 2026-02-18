package org.scaas.protocol.requests;

import org.scaas.domain.enumerations.Runtime;

public record UpdateFunctionRequest(
        String name,
        Runtime runtime,
        String entryPoint
) {
}
