package middleware

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/TheEmfield/CoPick/connector/internal/models"
	"github.com/labstack/echo/v4"
)

func Logger(logger *slog.Logger) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(ctx echo.Context) error {
			start := time.Now()

			err := next(ctx)

			duration := time.Since(start)
			status := ctx.Response().Status

			if err != nil {
				if hadledErr, ok := err.(*models.ErrorResponce); ok {
					status = hadledErr.Status
				} else {
					status = http.StatusInternalServerError
				}
			}

			attributes := []any{
				"method", ctx.Request().Method,
				"path", ctx.Path(),
				"status", status,
				"duration", duration.String(),
				"ip", ctx.RealIP(),
				"user_agent", ctx.Request().UserAgent(),
			}

			if err != nil {
				attributes = append(attributes, "error", err.Error())
			}

			if status == http.StatusInternalServerError {
				logger.Error("request", attributes...)
				return err
			}

			logger.Info("request", attributes...)
			return err
		}
	}
}
