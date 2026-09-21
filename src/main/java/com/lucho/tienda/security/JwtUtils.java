package com.lucho.tienda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTimeMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Long userId, String role) {
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature/expiration once and returns the trusted claims.
     * Invalid, expired or malformed tokens throw a JwtException/IllegalArgumentException.
     */
    public Claims parseAndValidateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }
}
