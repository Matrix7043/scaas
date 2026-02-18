package org.scaas.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;

public interface JwtService {
    Key getSigningKey();
    String generateToken(String email);
    String extractEmail(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
