package org.scaas.protocol.requests;

import lombok.Builder;

@Builder
public record RefreshTokenRequest(
        String refreshToken
) {
}
