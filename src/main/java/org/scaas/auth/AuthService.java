package org.scaas.auth;

import org.scaas.protocol.requests.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
}
