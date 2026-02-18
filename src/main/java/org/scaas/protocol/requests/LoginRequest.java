package org.scaas.protocol.requests;

import lombok.Builder;

@Builder
public record LoginRequest(String email, String password) {
}
