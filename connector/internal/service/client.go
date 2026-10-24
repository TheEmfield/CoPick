package service

import (
	"context"
	"log/slog"

	"github.com/TheEmfield/CoPick/connector/internal/config"
	"github.com/TheEmfield/CoPick/connector/internal/dto"
)

type Client struct {
	movieClient *MovieClient
}

func New(cfg *config.Config, logger *slog.Logger) *Client {
	return &Client{
		movieClient: NewMovieClient(cfg.TMDB, logger),
	}
}

func (c *Client) FetchMovies(ctx context.Context, req dto.DiscoverRequest) (*dto.DiscoverResponse, error) {
	return c.movieClient.FetchMovies(ctx, req)
}
