package org.scaas.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.RefreshToken;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.protocol.responses.AuthResponse;
import org.scaas.security.JwtService;
import org.scaas.security.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {

        authService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        String token = authService.login(loginRequest);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginRequest.email());
        AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .build();

        return ResponseEntity.ok(response);
    }

    //TODO: Add Logout Method and Add Index to RefreshToken

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody String refreshToken) {

        return ResponseEntity.ok(refreshTokenService.handleRefreshToken(refreshToken));
    }
}
