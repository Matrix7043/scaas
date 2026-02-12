package org.scaas.security;

import java.security.Key;

public interface JwtService {
    Key getSigningKey();
    String generateToken(String email);
    String extractEmail(String token);
}
