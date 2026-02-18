package org.scaas.auth;

import lombok.RequiredArgsConstructor;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {

        authService.register(registerRequest);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {

        String token = authService.login(loginRequest);

        return ResponseEntity.ok(token);
    }
}
