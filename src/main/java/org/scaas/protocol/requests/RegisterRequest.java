package org.scaas.protocol.requests;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RegisterRequest {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
