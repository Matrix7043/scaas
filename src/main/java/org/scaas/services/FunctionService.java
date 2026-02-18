package org.scaas.services;

import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;

import java.util.List;
import java.util.UUID;

public interface FunctionService {
    FunctionResponse createFunction(CreateFunctionRequest request);
    List<FunctionResponse> getFunctions();
    FunctionResponse getFunctionById(UUID id);
}
