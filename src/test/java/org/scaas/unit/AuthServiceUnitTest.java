package org.scaas.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scaas.auth.impl.AuthServiceImpl;
import org.scaas.domain.entites.RefreshToken;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.RefreshTokenRepository;
import org.scaas.domain.repositories.UserRepository;
import org.scaas.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void should_delete_refresh_token_if_exists() {
        RefreshToken token = RefreshToken.builder()
                    .token("abc")
                    .build();

        when(refreshTokenRepository.findByToken("abc"))
                .thenReturn(Optional.of(token));

        authService.logout("abc");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void should_do_nothing_if_token_not_found() {
        when(refreshTokenRepository.findByToken("abc"))
                .thenReturn(Optional.empty());

        authService.logout("abc");

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void should_delete_all_tokens_for_user() {
        User user = User.builder()
                .email("test@mail.com")
                .build();

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        authService.logoutAll("test@mail.com");

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
