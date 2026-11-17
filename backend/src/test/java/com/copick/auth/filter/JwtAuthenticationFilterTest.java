package com.copick.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.copick.auth.service.JwtService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

@DisplayName("JwtAuthenticationFilter: извлечение аутентификации из Bearer-токена")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "copick-test-secret-key-copick-test-secret-key-0123456789";

    private JwtAuthenticationFilter filter;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, Duration.ofMinutes(30));
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void doFilter(MockHttpServletRequest request) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("запрос без заголовка Authorization проходит дальше без аутентификации")
    void requestWithoutAuthHeaderContinuesWithoutAuthentication() throws Exception {
        doFilter(new MockHttpServletRequest());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("заголовок не с префиксом Bearer игнорируется")
    void nonBearerAuthHeaderIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("валидный токен -> в SecurityContext кладётся profileId и ROLE_USER")
    void validTokenSetsAuthentication() throws Exception {
        String token = jwtService.generateToken(42);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/groups");

        doFilter(request);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(42);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    @DisplayName("невалидный токен не ломает запрос: аутентификация не устанавливается")
    void invalidTokenDoesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken.jwt.token");

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("существующая аутентификация не перезаписывается фильтром")
    void existingAuthenticationIsNotOverwritten() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken(
                99, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        String token = jwtService.generateToken(42);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        doFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(99);
    }
}
