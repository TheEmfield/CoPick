package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/TheEmfield/CoPick/connector/internal/config"
	"github.com/TheEmfield/CoPick/connector/internal/logger"
	"github.com/TheEmfield/CoPick/connector/internal/server/handlers"
	"github.com/TheEmfield/CoPick/connector/internal/server/middleware"
	"github.com/TheEmfield/CoPick/connector/internal/service"
	"github.com/labstack/echo/v4"
)

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

	if err := run(cfg, logger); err != nil {
		logger.Error(
			"run server",
			"error", err,
		)

		os.Exit(1)
	}
}

func run(cfg *config.Config, logger *slog.Logger) error {
	fetcher := service.New(cfg.TMDB, logger)
	api := handlers.NewServiceAPI(logger, cfg, fetcher)
	router := setupServer(api)

	go func() {
		logger.Info("starting server", "addr", router.Server.Addr)
		if err := router.Start(router.Server.Addr); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Error("server crashed", "error", err)
		}
	}()

	ctx, cancel := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer cancel()
	<-ctx.Done()

	logger.Info("received shutdown signal, starting graceful shutdown")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), cfg.Server.ShutdownTimeout)
	defer shutdownCancel()

	if err := router.Shutdown(shutdownCtx); err != nil {
		return fmt.Errorf("graceful shutdown failed: %w", err)
	}

	logger.Info("server stopped gracefully")
	return nil
}

func setupServer(api *handlers.ServiceAPI) *echo.Echo {
	router := echo.New()

	router.Debug = false
	router.HideBanner = true
	router.Logger.SetOutput(io.Discard)

	router.Use(middleware.Recover(api.Logger))
	router.Use(middleware.Logger(api.Logger))

	router.Server.Addr = api.Config.Server.Host + ":" + api.Config.Server.Port
	router.Server.ReadTimeout = api.Config.Server.ReadTimeout
	router.Server.WriteTimeout = api.Config.Server.WriteTimeout
	router.Server.IdleTimeout = api.Config.Server.IdleTimeout

	router.POST("/discover", api.GetMovies)

	router.HTTPErrorHandler = handlers.ErrorHandler(api.Logger)

	return router
}
