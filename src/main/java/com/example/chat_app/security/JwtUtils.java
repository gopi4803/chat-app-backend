package com.example.chat_app.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.security.Key;

@Slf4j
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expirationMs}")
    private int jwtExpiration;
    @Value("${jwt.refreshExpirationMs}")
    private int jwtRefreshExpiration;

    public String generateAccessToken(String email){
        return buildToken(email,jwtExpiration);
    }

    public String generateRefreshToken(String email){
        return buildToken(email,jwtRefreshExpiration);
    }

    public String buildToken(String email,int expiryMs){
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expiryMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromJwtToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Token : {}",e.getMessage(),e);
            return false;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public long getRefreshExpirationSeconds() {
        return jwtRefreshExpiration / 1000L;
    }

    public int getRefreshExpirationMs() {
        return jwtRefreshExpiration;
    }
}
