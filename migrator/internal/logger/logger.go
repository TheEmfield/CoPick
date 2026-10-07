package logger

import (
	"errors"
	"log/slog"
	"os"

	"github.com/TheEmfield/CoPick/migrator/internal/config"
)

var (
	ErrUnknownLevel  = errors.New("unknown log level")
	ErrUnknownFormat = errors.New("unknown log format")
)

func Setup(cfg *config.Logger) (*slog.Logger, error) {
	levelMapper := map[string]slog.Level{
		"debug": slog.LevelDebug,
		"info":  slog.LevelInfo,
		"warn":  slog.LevelWarn,
		"error": slog.LevelError,
	}

	level, ok := levelMapper[cfg.Level]
	if !ok {
		return nil, ErrUnknownLevel
	}

	ops := &slog.HandlerOptions{Level: level}

	var handler slog.Handler
	switch cfg.Format {
	case "json":
		handler = slog.NewJSONHandler(os.Stdout, ops)

	case "text":
		handler = slog.NewTextHandler(os.Stdout, ops)

	default:
		return nil, ErrUnknownFormat
	}

	return slog.New(handler), nil
}
