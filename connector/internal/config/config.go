package config

import "time"

type Config struct {
	Logger   Logger   `yaml:"logger"`
	Postgres Postgres `yaml:"postgres"`
	TMDB     TMDB     `yaml:"tmdb"`
	Server   Server   `yaml:"server"`
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

type TMDB struct {
	TMDBTimeout time.Duration `yaml:"tmdb_timeout"     env:"FETCHER_TMDB_TIMEOUT"     env-default:"10s"`
	URL         string        `yaml:"url"              env:"SERVER_URL"               env-default:"https://api.themoviedb.org/3"`
}

type Server struct {
	Host            string        `yaml:"host"             env:"HTTP_HOST"             env-default:"localhost"`
	Port            string        `yaml:"port"             env:"HTTP_PORT"             env-default:"8080"`
	ReadTimeout     time.Duration `yaml:"read_timeout"     env:"HTTP_READ_TIMEOUT"     env-default:"10s"`
	WriteTimeout    time.Duration `yaml:"write_timeout"    env:"HTTP_WRITE_TIMEOUT"    env-default:"10s"`
	IdleTimeout     time.Duration `yaml:"idle_timeout"     env:"HTTP_IDLE_TIMEOUT"     env-default:"60s"`
	ShutdownTimeout time.Duration `yaml:"shutdown_timeout" env:"HTTP_SHUTDOWN_TIMEOUT" env-default:"20s"`
}
