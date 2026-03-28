package org.scaas.protocol.requests;

public record LogoutRequest(
        String refreshToken
) {
}
