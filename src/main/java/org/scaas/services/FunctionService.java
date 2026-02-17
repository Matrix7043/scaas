package org.scaas.services;

import org.scaas.protocol.responses.FunctionResponse;

import java.util.List;
import java.util.UUID;

public interface FunctionService {
    FunctionResponse createFunction(String name, String runtime, String entrypoint);
    List<FunctionResponse> getFunctions();
    FunctionResponse getFunctionById(UUID id);
}
