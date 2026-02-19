package org.scaas.protocol.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(max = 100, message = "Username size can at most be {max} chars")
        String username,
        @NotBlank(message = "FirstName cannot be blank")
        @Size(max = 50, message = "Firstname size can at most be {max} chars")
        String firstName,
        @NotBlank(message = "LastName cannot be blank")
        @Size(max = 50, message = "Lastname size can at most be {max} chars")
        String lastName,
        @Email(message = "Invalid Email format")
        @NotBlank(message = "Email cannot be blank")
        String email,
        @Size(min = 6, max = 12,
                message = "Password size at least must be {min} and at most {max}")
        String password) {}
