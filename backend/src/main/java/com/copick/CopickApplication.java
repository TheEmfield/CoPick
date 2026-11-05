package com.copick;

import com.copick.algorithm.config.AlgorithmConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AlgorithmConfig.class)
public class CopickApplication {
    public static void main(String[] args) {
        SpringApplication.run(CopickApplication.class, args);
    }
}