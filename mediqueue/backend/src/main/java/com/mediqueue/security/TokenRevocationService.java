package com.mediqueue.security;

import com.mediqueue.user.entity.RevokedToken;
import com.mediqueue.user.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        String tokenHash = JwtService.hashToken(token);
        return revokedTokenRepository.existsByTokenHash(tokenHash);
    }

    @Transactional
    public void revokeToken(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            return;
        }

        String tokenHash = JwtService.hashToken(token);
        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        String email = jwtService.extractEmail(token);
        Instant expiresAt = jwtService.extractExpiration(token);
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(86400);
        }

        RevokedToken revokedToken = RevokedToken.builder()
                .tokenHash(tokenHash)
                .userEmail(email)
                .expiresAt(expiresAt)
                .revokedAt(Instant.now())
                .build();

        revokedTokenRepository.save(revokedToken);
        log.info("Token revoked successfully for user: {}", email);
    }

    @Transactional
    public void pruneExpiredTokens() {
        int deleted = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Pruned {} expired revoked tokens from database.", deleted);
        }
    }
}
