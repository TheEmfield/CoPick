package main

import (
	"flag"
	"fmt"
	"log/slog"
	"os"
	"strconv"

	"github.com/TheEmfield/CoPick/migrator/internal/config"
	"github.com/TheEmfield/CoPick/migrator/internal/logger"
	"github.com/golang-migrate/migrate/v4"
)

var CommandMapper = map[string]func(*slog.Logger, *migrate.Migrate){
	"up":      up,
	"down":    down,
	"version": version,
	"force":   force,
}

func main() {
	var configPath string
	flag.StringVar(&configPath, "c", "config.yaml", "config path")
	flag.Parse()

	cfg, err := config.Load(configPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "config parse fail: %s\n", err)
		os.Exit(1)
	}

	logger, err := logger.Setup(&cfg.Logger)
	if err != nil {
		fmt.Fprintf(os.Stderr, "setup logger fail: %s\n", err)
		os.Exit(1)
	}

	if len(os.Args) < 2 {
		logger.Error("empty command")
		os.Exit(1)
	}

	if exec, ok := CommandMapper[os.Args[1]]; !ok {
		logger.Error("invalid command")
		os.Exit(1)

	} else {
		info := fmt.Sprintf(
			"postgres://%s:%s@%s:%s/%s?sslmode=%s",
			cfg.Postgres.User, cfg.Postgres.Password, cfg.Postgres.Host,
			cfg.Postgres.Port, cfg.Postgres.DB, cfg.Postgres.SSLMode,
		)

		migrator, err := migrate.New("file://migrations", info)
		if err != nil {
			logger.Error("create migrate instance fail", "error", err)
			os.Exit(1)
		}

		defer func() {
			sourceErr, dbErr := migrator.Close()
			if sourceErr != nil {
				logger.Error("migrator close source error", "error", sourceErr)
			}

			if dbErr != nil {
				logger.Error("migrator close database error", "error", dbErr)
			}
		}()

		exec(logger, migrator)
	}
}

func up(logger *slog.Logger, migrator *migrate.Migrate) {
	err := migrator.Up()
	switch err {
	case nil:
		logger.Info("migrations applied successfully")

	case migrate.ErrNoChange:
		logger.Info("no changes to migrate")

	default:
		logger.Error("migrate up fail", "error", err)
		os.Exit(1)
	}
}

func down(logger *slog.Logger, migrator *migrate.Migrate) {
	err := migrator.Steps(-1)
	switch err {
	case nil:
		logger.Info("migration rolled back successfully")

	case migrate.ErrNoChange:
		logger.Info("no migrations to rollback")

	default:
		logger.Error("migrate down fail", "error", err)
		os.Exit(1)
	}
}

func version(logger *slog.Logger, migrator *migrate.Migrate) {
	version, dirty, err := migrator.Version()
	if err != nil {
		logger.Error("get migration version fail", "error", err)
		os.Exit(1)
	}

	logger.Info("migration version", "version", version, "dirty", dirty)
}

func force(logger *slog.Logger, migrator *migrate.Migrate) {
	if len(os.Args) < 3 {
		logger.Error("force command requires version number")
		os.Exit(1)
	}

	version, err := strconv.Atoi(os.Args[2])
	if err != nil || version <= 0 {
		logger.Error("invalid version", "version", os.Args[2])
		os.Exit(1)
	}

	if err := migrator.Force(version); err != nil {
		logger.Error("force version fail", "error", err)
		os.Exit(1)
	}

	logger.Info("forced version", "version", version)
}
