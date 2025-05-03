package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

/**
 * JwtUtils is a utility class that provides functionality for:
 * - Generating JSON Web Tokens (JWT) for authenticated users.
 * - Extracting JWTs from HTTP cookies.
 * - Parsing and validating JWTs.
 *
 * This class uses HMAC SHA-based signing with a Base64-encoded secret key.
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * The Base64-encoded secret used for signing JWTs.
     */
    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    /**
     * The validity duration of the JWT in milliseconds.
     */
    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    /**
     * The name of the cookie where the JWT will be stored.
     */
    @Value("${spring.app.jwtCookieName}")
    private String jwtCookieName;

    /**
     * Retrieves the JWT from the request's cookies.
     *
     * @param request the incoming HTTP request
     * @return the JWT string if found, otherwise null
     */
    public String getJwtFromCookies(HttpServletRequest request) {
        return Optional.of(WebUtils.getCookie(request, jwtCookieName))
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * Generates a secure HTTP-only cookie that contains the JWT for a user.
     *
     * @param userPrincipal the authenticated user
     * @return a ResponseCookie object containing the JWT
     */
    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        return ResponseCookie.from(jwtCookieName, jwt)
                .path("/api")
                .maxAge(24 * 60L * 60L) // 1 day in seconds
                .httpOnly(false)
                .secure(false) // Consider setting to false if not using HTTPS during development
                .build();
    }

    /**
     * Generates a cookie that effectively removes the JWT by setting its value to null.
     * This is typically used during logout to clear the authentication cookie.
     *
     * @return a ResponseCookie with a null value and appropriate path set
     */
    public ResponseCookie generateClearedJwtCookie() {
        return ResponseCookie.from(jwtCookieName, "")
                .path("/api")
                .maxAge(0)
                .httpOnly(true)
                .build();
    }

    /**
     *
     * Generates a signed JWT token from the provided username.
     *
     * @param username the username to embed in the token
     * @return a signed JWT string
     */
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the username embedded in the token
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates the JWT token by checking its structure, signature, and expiration.
     *
     * @param token the JWT to validate
     * @return true if the token is valid; false otherwise
     */
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Decodes the Base64-encoded secret and generates the signing key.
     *
     * @return the HMAC secret key used for signing and verifying JWTs
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}