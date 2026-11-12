package com.copick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtService: генерация и валидация JWT-токенов")
class JwtServiceTest {

    private static final String SECRET = "copick-test-secret-key-copick-test-secret-key-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("generateToken -> extractProfile: профиль восстанавливается из токена")
    void tokenRoundTripReturnsProfileId() {
        String token = jwtService.generateToken(42);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractProfile(token)).isEqualTo(42);
    }

    @Test
    @DisplayName("свежесгенерированный токен валиден")
    void freshTokenIsValid() {
        String token = jwtService.generateToken(7);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("просроченный токен невалиден")
    void expiredTokenIsInvalid() {
        JwtService expiredService = new JwtService(SECRET, Duration.ofSeconds(-10));
        String token = expiredService.generateToken(42);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("мусорная строка не является валидным токеном")
    void garbageTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }

    @Test
    @DisplayName("токен, подписанный другим ключом, невалиден")
    void tokenSignedWithDifferentKeyIsInvalid() {
        JwtService otherService =
                new JwtService("another-secret-key-another-secret-key-another-secret-123", Duration.ofMinutes(30));
        String foreignToken = otherService.generateToken(42);

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("extractProfile бросает исключение для невалидного токена")
    void extractProfileFailsForInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractProfile("broken.token.value"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("extractProfile бросает NumberFormatException, если subject не число")
    void extractProfileFailsForNonNumericSubject() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("not-a-number")
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThatThrownBy(() -> jwtService.extractProfile(token))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("каждый вызов generateToken выдаёт разные токены")
    void tokensAreUniquePerGeneration() throws InterruptedException {
        String first = jwtService.generateToken(1);
        Thread.sleep(1000);
        String second = jwtService.generateToken(1);

        assertThat(first).isNotEqualTo(second);
    }
}
