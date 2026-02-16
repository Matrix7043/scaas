package org.scaas.services;

import org.scaas.protocol.responses.FunctionResponse;

import java.util.List;

public interface FunctionService {
    FunctionResponse createFunction(String name, String runtime, String entrypoint);
    List<FunctionResponse> getFunctions();
}
