package com.copick.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.copick.groups.exception.GroupNotFoundException;
import com.copick.groups.exception.NotGroupOwnerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("GlobalExceptionHandler: единый формат ошибок API")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("доменное исключение маппится в его статус и error-код")
    void domainExceptionMapsToItsStatusAndCode() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/groups/5");

        ResponseEntity<ApiError> response =
                handler.handleDomainException(new GroupNotFoundException(5), servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("GROUP_NOT_FOUND");
        assertThat(body.getMessage()).contains("5");
        assertThat(body.getPath()).isEqualTo("/api/v1/groups/5");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("403-исключение сохраняет свой статус")
    void forbiddenExceptionKeepsItsStatus() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/groups/5");

        ResponseEntity<ApiError> response =
                handler.handleDomainException(new NotGroupOwnerException(5, 7), servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getError()).isEqualTo("NOT_GROUP_OWNER");
    }

    @Test
    @DisplayName("неожиданное исключение -> 500 без раскрытия внутренних деталей")
    void unexpectedExceptionIsMaskedAs500() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/profiles/me/preferences");

        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new RuntimeException("NPE in database layer"), servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        Assertions.assertNotNull(body);
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getMessage()).isEqualTo("Внутренняя ошибка сервера");
    }
}
