package org.scaas.protocol.requests;

import lombok.Builder;
import org.scaas.domain.enumerations.Runtime;

@Builder
public record CreateFunctionRequest(String name, Runtime runtime, String entryPoint) {

}
