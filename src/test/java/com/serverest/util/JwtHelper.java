package com.serverest.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class JwtHelper {
    private static final String SECRET;

    static {
        try (java.io.InputStream is = JwtHelper.class.getClassLoader()
                .getResourceAsStream("jwt.properties")) {
            Properties props = new Properties();
            props.load(is);
            SECRET = props.getProperty("serverest.jwt.secret");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load jwt.properties from classpath", e);
        }
    }

    public static String generateExpiredToken(String email, String password) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .claims(Map.of("email", email, "password", password))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }
}
