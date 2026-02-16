package org.scaas.protocol.mappers;

import org.scaas.domain.entites.Function;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.stereotype.Component;

@Component
public class ToFunctionResponse {

    public FunctionResponse toFunctionResponse(Function function) {
        return FunctionResponse.builder()
                .id(function.getId())
                .name(function.getName())
                .runtime(function.getRuntime())
                .entrypoint(function.getEntrypoint())
                .createdAt(function.getCreatedAt())
                .updatedAt(function.getUpdatedAt())
                .build();
    }

}
