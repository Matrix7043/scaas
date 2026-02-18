package org.scaas.security;

import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }

        String email = auth.getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

    }
}
