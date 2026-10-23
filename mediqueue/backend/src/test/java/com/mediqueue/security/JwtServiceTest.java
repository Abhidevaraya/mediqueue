package com.mediqueue.security;

import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String VALID_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", VALID_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour
        jwtService.init();
    }

    private User createSampleUser() {
        return User.builder()
                .id(100L)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should generate valid token with correct claims")
    void testGenerateTokenAndExtractClaims() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("john.doe@example.com", jwtService.extractEmail(token));
        assertEquals(100L, jwtService.extractUserId(token));
        assertEquals(UserRole.PATIENT, jwtService.extractRole(token));
        assertNotNull(jwtService.extractExpiration(token));
        assertTrue(jwtService.extractExpiration(token).isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should reject malformed JWT token")
    void testMalformedTokenRejected() {
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testInvalidSignatureRejected() {
        User user = createSampleUser();
        String token = jwtService.generateToken(user);

        // Tamper with payload
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "tampered." + parts[2];

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredTokenRejected() {
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtSecret", VALID_SECRET);
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtExpirationMs", -1000L); // Already expired
        shortLivedJwtService.init();

        User user = createSampleUser();
        String expiredToken = shortLivedJwtService.generateToken(user);

        assertFalse(shortLivedJwtService.isTokenValid(expiredToken));
    }

    @Test
    @DisplayName("Should correctly generate SHA-256 hash for token revocation tracking")
    void testHashToken() {
        String token = "sample.jwt.token";
        String hash1 = JwtService.hashToken(token);
        String hash2 = JwtService.hashToken(token);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }
}
