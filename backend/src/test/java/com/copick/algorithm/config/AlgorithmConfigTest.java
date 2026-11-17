package com.copick.algorithm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("AlgorithmConfig: валидация параметров конвейера рекомендаций")
class AlgorithmConfigTest {

    @Test
    @DisplayName("принимает корректные пользовательские значения")
    void acceptsValidCustomParameters() {
        assertThatCode(() -> new AlgorithmConfig(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 100, 10))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("граничные значения (0, 1) считаются валидными")
    void acceptsBoundaryValues() {
        assertThatCode(() -> new AlgorithmConfig(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20, 20))
                .doesNotThrowAnyException();
        assertThatCode(() -> new AlgorithmConfig(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 20, 20))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "lambda={0}, alpha={1}, beta={2}, rho={3}, mu={4} -> ошибка")
    @CsvSource({
            "-0.1, 0.6, 0.4, 0.15, 0.7",  // отрицательный lambda
            "0.5, -0.1, 0.4, 0.15, 0.7",  // отрицательный alpha
            "0.5, 1.1, 0.4, 0.15, 0.7",   // alpha > 1
            "0.5, 0.6, -0.1, 0.15, 0.7",  // отрицательный beta
            "0.5, 0.6, 0.4, -0.1, 0.7",   // отрицательный rho
            "0.5, 0.6, 0.4, 0.15, -0.1",  // отрицательный mu
            "0.5, 0.6, 0.4, 0.15, 1.1"    // mu > 1
    })
    void rejectsInvalidCoreParameters(double lambda, double alpha, double beta, double rho, double mu) {
        assertThatThrownBy(() -> new AlgorithmConfig(lambda, alpha, beta, rho, mu, 0.5, 50, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректные параметры алгоритма");
    }

    @ParameterizedTest(name = "kappa={0} -> ошибка")
    @CsvSource({"-0.01", "1.01", "-1.0", "2.0"})
    void rejectsKappaOutOfRange(double kappa) {
        assertThatThrownBy(() -> new AlgorithmConfig(0.5, 0.6, 0.4, 0.15, 0.7, kappa, 50, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kappa должен быть от 0 до 1");
    }

    @ParameterizedTest(name = "M={0}, K={1} -> ошибка")
    @CsvSource({
            "0,  20",   // M <= 0
            "-5, 20",   // M < 0
            "50, 0",    // K <= 0
            "50, -1",   // K < 0
            "10, 20"    // M < K
    })
    void rejectsInvalidPoolSizes(int m, int k) {
        assertThatThrownBy(() -> new AlgorithmConfig(0.5, 0.6, 0.4, 0.15, 0.7, 0.5, m, k))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("M должно быть >= K и > 0");
    }
}
