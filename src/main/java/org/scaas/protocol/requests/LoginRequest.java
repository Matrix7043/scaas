package org.scaas.protocol.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequest(
        @Email(message = "Invalid Email format")
        @NotBlank(message = "Email cannot be blank")
        String email,
        @NotBlank(message = "Password cannot be blank")
        String password) {}
