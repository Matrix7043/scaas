package org.scaas.protocol.requests;

import lombok.Builder;

@Builder
public record RegisterRequest(String username, String firstName, String lastName, String email, String password) {
}
