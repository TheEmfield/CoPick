package com.copick.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

@DisplayName("JwtAuthenticationEntryPoint: 401 с JSON-телом для неаутентифицированных")
class JwtAuthenticationEntryPointTest {

    @Test
    @DisplayName("формирует ответ 401 c JSON-ошибкой и путём запроса")
    void commenceWritesUnauthorizedJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/groups/5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");

        JsonNode body = objectMapper.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("UNATHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("Требуется авторизация для доступа к этому ресурсу");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/groups/5");
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }
}
