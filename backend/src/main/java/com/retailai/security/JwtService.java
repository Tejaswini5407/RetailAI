package com.retailai.security;

import com.retailai.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
        @Value("${jwt.secret}") String secretString,
        @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = initSecretKey(secretString);
        this.expirationMs = expirationMs;
    }

    private SecretKey initSecretKey(String secretString) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretString);
        } catch (Exception e) {
            keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            // Guarantee minimum 256-bit key length for HMAC-SHA256
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getUserId())
                .claim("organizationId", userDetails.getOrganizationId())
                .claim("organizationCode", userDetails.getOrganizationCode())
                .claim("role", userDetails.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object val = parseClaims(token).get("userId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    public Long extractOrganizationId(String token) {
        Object val = parseClaims(token).get("organizationId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    public String extractOrganizationCode(String token) {
        return parseClaims(token).get("organizationCode", String.class);
    }

    public UserRole extractRole(String token) {
        String roleStr = parseClaims(token).get("role", String.class);
        return roleStr != null ? UserRole.valueOf(roleStr) : null;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
