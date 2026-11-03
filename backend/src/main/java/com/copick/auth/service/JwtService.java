package com.copick.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final Duration tokenDuration;

  public JwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.duration}") Duration tokenDuration) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.tokenDuration = tokenDuration;
  }

  public String generateToken(Integer profileId) {
    Instant now = Instant.now();
    Instant expiryTime = now.plus(tokenDuration);

    return Jwts.builder()
        .subject(String.valueOf(profileId))
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiryTime))
        .signWith(signingKey)
        .compact();
  }

  public Integer extractProfile(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    return Integer.parseInt(claims.getSubject());
  }

  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
