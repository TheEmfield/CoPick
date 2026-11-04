package com.copick.algorithm.config;

public record AlgorithmConfig(
        double lambda, // Сила штрафа за нелюбимый жанр (по умолч. 0.5)
        double alpha,  // Баланс «среднее» и «наименее довольные» (по умолч. 0.6)
        double beta,   // Бонус за попадание в контекст «сегодня» (по умолч. 0.4)
        double rho,    // Сила поправки за качество (по умолч. 0.15)
        double mu,     // Баланс релевантности и разнообразия в MMR (по умолч. 0.7)
        double kappa,
        int M,         // Размер пула кандидатов для MMR (по умолч. 50)
        int K          // Размер итоговой подборки (по умолч. 20)
) {
    public AlgorithmConfig {
        if (lambda < 0 || alpha < 0 || alpha > 1 || beta < 0 || rho < 0 || mu < 0 || mu > 1) {
            throw new IllegalArgumentException("Некорректные параметры алгоритма");
        }
        if (kappa < 0 || kappa > 1) {
            throw new IllegalArgumentException("kappa должен быть от 0 до 1");
        }
        if (M <= 0 || K <= 0 || M < K) {
            throw new IllegalArgumentException("M должно быть >= K и > 0");
        }
    }

    public AlgorithmConfig() {
        this(0.5, 0.6, 0.4, 0.15, 0.7, 0.5, 50, 20);
    }
}