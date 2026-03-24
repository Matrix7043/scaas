package org.scaas.protocol.responses;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
