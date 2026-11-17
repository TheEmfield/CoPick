package com.copick.testsupport;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Утилиты для MockMvc контроллеров, использующих
 * {@code @AuthenticationPrincipal Integer currentProfileId}.
 *
 * <p>MockMvc не поднимает Spring Security, поэтому резолвер
 * аргументов и SecurityContext настраиваются вручную.
 */
public final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    /** Кладёт в SecurityContext аутентификацию с заданным profileId в качестве principal. */
    public static void authenticateAs(Integer profileId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(profileId, "n/a", "ROLE_USER"));
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /** MockMvc с поддержкой @AuthenticationPrincipal и глобальным обработчиком ошибок. */
    public static MockMvc standaloneWithPrincipal(Object controller, Object... advice) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(advice)
                .build();
    }
}
