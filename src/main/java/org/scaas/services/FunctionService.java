package org.scaas.services;

import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FunctionService {
    FunctionResponse createFunction(CreateFunctionRequest request);
    FunctionResponse updateFunctionById(UUID id, UpdateFunctionRequest request);
    FunctionResponse deleteFunctionById(UUID id);
    Page<FunctionResponse> getFunctions(Pageable pageable);
    FunctionResponse getFunctionById(UUID id);
}
