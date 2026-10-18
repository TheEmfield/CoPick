package middleware

import (
	"log/slog"
	"net/http"
	"runtime/debug"

	"github.com/TheEmfield/CoPick/connector/internal/models"
	"github.com/labstack/echo/v4"
)

func Recover(logger *slog.Logger) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(ctx echo.Context) error {
			defer func() {
				if r := recover(); r != nil {
					logger.Error(
						"panic recovered",
						"error", r,
						"path", ctx.Path(),
						"method", ctx.Request().Method,
						"stack", string(debug.Stack()),
					)

					err := ctx.JSON(http.StatusInternalServerError, models.ErrInternal)
					if err != nil {
						logger.Error("responce with error fail", "error", err)
					}
				}
			}()

			return next(ctx)
		}
	}
}
