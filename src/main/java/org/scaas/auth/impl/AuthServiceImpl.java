package org.scaas.auth.impl;

import lombok.RequiredArgsConstructor;
import org.scaas.auth.AuthService;
import org.scaas.domain.entites.User;
import org.scaas.domain.enumerations.Role;
import org.scaas.domain.repositories.RefreshTokenRepository;
import org.scaas.domain.repositories.UserRepository;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public void register(RegisterRequest request) {

        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new IllegalArgumentException("Email already exists");
        }
        if(userRepository.findByUsername(request.username()).isPresent()){
            throw new IllegalArgumentException("Username already exists");
        }

        User newUser = User.builder()
                .username(request.username())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(newUser);

    }

    @Override
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email()).
                orElseThrow(() -> new RuntimeException("Email not found"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BadCredentialsException("Wrong Email or password");
        }

        return jwtService.generateToken(user.getEmail());
    }

    @Override
    public void logout(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public void logoutAll(String email) {
        userRepository.findByEmail(email).ifPresent(refreshTokenRepository::deleteByUser);
    }
}
