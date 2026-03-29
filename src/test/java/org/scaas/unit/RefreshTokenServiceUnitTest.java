package org.scaas.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scaas.domain.entites.RefreshToken;
import org.scaas.domain.repositories.RefreshTokenRepository;
import org.scaas.domain.repositories.UserRepository;
import org.scaas.security.JwtService;
import org.scaas.security.RefreshTokenService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceUnitTest {

    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @BeforeEach
    void setup(){
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository, jwtService);
    }

    @Test
    void should_throw_if_token_expired(){
        RefreshToken token = RefreshToken.builder()
                .expiryDate(Instant.now().minusSeconds(10))
                .build();

        assertThrows(RuntimeException.class,() -> refreshTokenService.verifyRefreshToken(token));
    }
}
