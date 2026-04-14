package org.scaas.services;

import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.DeploymentResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FunctionService {
    FunctionResponse createFunction(CreateFunctionRequest request);

    FunctionResponse updateFunctionById(UUID id, UpdateFunctionRequest request);

    void deleteFunctionById(UUID id);

    Page<FunctionResponse> getFunctions(Pageable pageable);

    void replaceArtifact(UUID id, MultipartFile file);

    String getArtifactContent(UUID id);

    FunctionResponse getFunctionById(UUID id);

    DeploymentResponse deployFunction(UUID id);
}
