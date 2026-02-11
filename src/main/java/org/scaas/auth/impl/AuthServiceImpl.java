package org.scaas.auth.impl;

import lombok.RequiredArgsConstructor;
import org.scaas.auth.AuthService;
import org.scaas.domain.entites.User;
import org.scaas.domain.enumerations.Role;
import org.scaas.domain.repositories.UserRepository;
import org.scaas.protocol.requests.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest request) {

        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new IllegalArgumentException("Email already exists");
        }
        if(userRepository.findByUsername(request.getUsername()).isPresent()){
            throw new IllegalArgumentException("Username already exists");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(newUser);

    }
}
