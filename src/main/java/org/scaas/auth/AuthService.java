package org.scaas.auth;

import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    String login(LoginRequest request);
}
