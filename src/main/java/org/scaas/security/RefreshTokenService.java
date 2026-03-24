package org.scaas.security;

import lombok.RequiredArgsConstructor;
import org.scaas.domain.entites.RefreshToken;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.RefreshTokenRepository;
import org.scaas.domain.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiration")
    private long REFRESH_TOKEN_DURATION;

    public RefreshToken createRefreshToken(String email) {

        User user = userRepository.findByEmail(email).orElseThrow();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_DURATION))
                .token(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

}
