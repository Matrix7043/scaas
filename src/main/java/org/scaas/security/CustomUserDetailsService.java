package org.scaas.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(email)
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
