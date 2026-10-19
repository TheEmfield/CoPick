package handlers

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"strings"

	"github.com/TheEmfield/CoPick/connector/internal/config"
	"github.com/TheEmfield/CoPick/connector/internal/dto"
	"github.com/TheEmfield/CoPick/connector/internal/models"
	"github.com/labstack/echo/v4"
)

type fetcher interface {
	FetchMovies(ctx context.Context, req dto.DiscoverRequest) (*dto.DiscoverResponse, error)
}

type ServiceAPI struct {
	Logger  *slog.Logger
	Config  *config.Config
	Fetcher fetcher
}

func NewServiceAPI(logger *slog.Logger, config *config.Config, fetcher fetcher) *ServiceAPI {
	return &ServiceAPI{
		Logger:  logger,
		Config:  config,
		Fetcher: fetcher,
	}
}

func ErrorHandler(logger *slog.Logger) echo.HTTPErrorHandler {
	return func(err error, ctx echo.Context) {
		if ctx.Response().Committed {
			return
		}

		var handled *models.ErrorResponce
		if errors.As(err, &handled) {
			err := ctx.JSON(
				handled.Status,
				&struct {
					Error *models.ErrorResponce `json:"error"`
				}{handled},
			)
			if err != nil {
				logger.Error("responce with error fail", "error", err)
			}

			return
		}

		err = ctx.JSON(http.StatusInternalServerError, models.ErrInternal)
		if err != nil {
			logger.Error("responce with error fail", "error", err)
		}
	}
}

func (api *ServiceAPI) GetMovies(ctx echo.Context) error {
	var req dto.DiscoverRequest
	if err := ctx.Bind(&req); err != nil {
		api.Logger.Warn("invalid request body", "error", err)
		return echo.NewHTTPError(http.StatusBadRequest, "Невалидный формат JSON")
	}

	if len(req.Preferences) == 0 {
		api.Logger.Warn("empty preferences in request")
		return echo.NewHTTPError(http.StatusBadRequest, "Массив preferences не может быть пустым")
	}

	for _, pref := range req.Preferences {
		if pref.Count <= 0 {
			api.Logger.Warn("invalid count in preferences", "genre", pref.Genre, "count", pref.Count)
			return echo.NewHTTPError(http.StatusBadRequest, "Количество фильмов (count) должно быть больше 0")
		}
	}

	response, err := api.Fetcher.FetchMovies(ctx.Request().Context(), req)
	if err != nil {
		api.Logger.Error("failed to fetch movies from TMDB", "error", err)

		if strings.Contains(err.Error(), "unknown genre") {
			return echo.NewHTTPError(http.StatusBadRequest, err.Error())
		}

		return echo.NewHTTPError(http.StatusInternalServerError, "Ошибка при получении данных от внешнего API")
	}

	return ctx.JSON(http.StatusOK, response)
}
