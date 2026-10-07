package config

type Config struct {
	Logger   Logger   `yaml:"logger"`
	Postgres Postgres `yaml:"postgres"`
}

type Logger struct {
	Level  string `yaml:"level"  env:"LOG_LEVEL"  env-default:"info"`
	Format string `yaml:"format" env:"LOG_FORMAT" env-default:"text"`
}

type Postgres struct {
	Host     string `yaml:"host"     env:"POSTGRES_HOST"     env-default:"localhost"`
	Port     string `yaml:"port"     env:"POSTGRES_PORT"     env-default:"5432"`
	User     string `                env:"POSTGRES_USER"     validate:"required"`
	Password string `                env:"POSTGRES_PASSWORD" validate:"required"`
	DB       string `                env:"POSTGRES_DB"       validate:"required"`
	SSLMode  string `yaml:"ssl_mode" env:"POSTGRES_SSL_MODE" env-default:"disable"`
}
