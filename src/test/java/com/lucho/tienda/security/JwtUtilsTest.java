package com.lucho.tienda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        String testSecretKey = "c3VwZXItc2VjcmV0LWtleS1mb3Itand0LXRlc3RpbmctYmFzZTY0LWVuY29kZWQ=";
        ReflectionTestUtils.setField(jwtUtils, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtUtils, "expirationTimeMs", 3600000L);
    }

    @Test
    @DisplayName("Should generate, validate once and expose all required authentication claims")
    void generateAndParseToken_Success() {
        String expectedUsername = "admin";
        Long expectedUserId = 99L;
        String expectedRole = "ADMIN";

        String token = jwtUtils.generateToken(expectedUsername, expectedUserId, expectedRole);
        Claims claims = jwtUtils.parseAndValidateToken(token);

        assertNotNull(token);
        assertEquals(expectedUsername, claims.getSubject());
        assertEquals(expectedUserId, jwtUtils.getUserId(claims));
        assertEquals(expectedRole, jwtUtils.getRole(claims));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Should reject malformed or null tokens")
    void parseAndValidateToken_Throws_WhenTokenIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> jwtUtils.parseAndValidateToken(null));
        assertThrows(JwtException.class, () -> jwtUtils.parseAndValidateToken("invalid.token.here"));
    }
}
