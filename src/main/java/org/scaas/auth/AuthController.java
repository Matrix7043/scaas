package org.scaas.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.RefreshToken;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.LogoutRequest;
import org.scaas.protocol.requests.RefreshTokenRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.protocol.responses.AuthResponse;
import org.scaas.security.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(refreshTokenService.handleRefreshToken(refreshTokenRequest.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest.refreshToken());
        return ResponseEntity.ok("Logout successfully");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<String> logoutAll(Authentication authentication) {
        authService.logoutAll(authentication.getName());
        return ResponseEntity.ok("Logged out all devices successfully");
    }
}
