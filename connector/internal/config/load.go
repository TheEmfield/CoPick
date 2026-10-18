package config

import (
	"fmt"

	"github.com/go-playground/validator/v10"
	"github.com/ilyakaznacheev/cleanenv"
)

func Load(path string) (*Config, error) {
	cfg := &Config{}

	if path != "" {
		if err := cleanenv.ReadConfig(path, cfg); err != nil {
			return nil, fmt.Errorf("parse config fail: %w", err)
		}
	} else {
		if err := cleanenv.ReadEnv(cfg); err != nil {
			return nil, fmt.Errorf("get env variables fail: %w", err)
		}
	}

	if err := validator.New().Struct(cfg); err != nil {
		return nil, fmt.Errorf("invalid config: %w", err)
	}

	return cfg, nil
}
