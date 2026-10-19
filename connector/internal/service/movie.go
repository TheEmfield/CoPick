package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"strconv"
	"strings"

	"github.com/TheEmfield/CoPick/connector/internal/config"
	"github.com/TheEmfield/CoPick/connector/internal/dto"
)

type tmdbMovie struct {
	ID            int     `json:"id"`
	Title         string  `json:"title"`
	OriginalTitle string  `json:"original_title"`
	PosterPath    string  `json:"poster_path"`
	BackdropPath  string  `json:"backdrop_path"`
	VoteAverage   float64 `json:"vote_average"`
	ReleaseDate   string  `json:"release_date"`
	GenreIDs      []int   `json:"genre_ids"`
	Overview      string  `json:"overview"`
}

var genreToId = map[string]string{
	"action":      "28",
	"adventure":   "12",
	"animation":   "16",
	"comedy":      "35",
	"crime":       "80",
	"documentary": "99",
	"drama":       "18",
	"family":      "10751",
	"fantasy":     "14",
	"history":     "36",
	"horror":      "27",
	"music":       "10402",
	"mystery":     "9648",
	"romance":     "10749",
	"scifi":       "878",
	"tv_movie":    "10770",
	"thriller":    "53",
	"war":         "10752",
	"western":     "37",
}

var idToGenre = map[string]string{
	"28":    "action",
	"12":    "adventure",
	"16":    "animation",
	"35":    "comedy",
	"80":    "crime",
	"99":    "documentary",
	"18":    "drama",
	"10751": "family",
	"14":    "fantasy",
	"36":    "history",
	"27":    "horror",
	"10402": "music",
	"9648":  "mystery",
	"10749": "romance",
	"878":   "scifi",
	"10770": "tv_movie",
	"53":    "thriller",
	"10752": "war",
	"37":    "western",
}

var apiKey = os.Getenv("TMDB_API_KEY")

const (
	sort_by         = "popularity.desc"
	vote_average    = 7.0
	vote_count      = 5000
	TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"
)

type Client struct {
	client *http.Client
	cfg    config.TMDB
	logger *slog.Logger
}

func (c *Client) fetchMovie(ctx context.Context, genre string, excludedGenres []string, amount int) ([]dto.Movie, error) {
	exGenres := genresToIDString(excludedGenres, genreToId)

	genreID, ok := genreToId[genre]
	if !ok {
		return nil, fmt.Errorf("unknown genre: %s", genre)
	}

	var allMovies []dto.Movie
	seen := make(map[int]bool)
	page := 1

	for len(allMovies) < amount {
		url := fmt.Sprintf("%s/discover/movie?api_key=%s&language=ru-RU&with_genres=%s&without_genres=%s&sort_by=%s&vote_average.gte=%.1f&vote_count.gte=%d&page=%d",
			c.cfg.URL, apiKey, genreID, exGenres, sort_by, vote_average, vote_count, page,
		)

		var tmdbResp struct {
			Results []tmdbMovie `json:"results"`
		}

		req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
		if err != nil {
			return nil, fmt.Errorf("create request: %w", err)
		}

		resp, err := c.client.Do(req)
		if err != nil {
			return nil, fmt.Errorf("do request: %w", err)
		}

		if resp.StatusCode != http.StatusOK {
			body, _ := io.ReadAll(resp.Body)
			resp.Body.Close()
			return nil, fmt.Errorf("tmdb returned status %d: %s", resp.StatusCode, string(body))
		}

		if err := json.NewDecoder(resp.Body).Decode(&tmdbResp); err != nil {
			resp.Body.Close()
			return nil, fmt.Errorf("decode response: %w", err)
		}
		resp.Body.Close()

		if len(tmdbResp.Results) == 0 {
			break
		}

		for _, tmdbMovie := range tmdbResp.Results {
			if seen[tmdbMovie.ID] {
				continue
			}
			seen[tmdbMovie.ID] = true

			releaseYear := 0
			if len(tmdbMovie.ReleaseDate) >= 4 {
				releaseYear, _ = strconv.Atoi(tmdbMovie.ReleaseDate[:4])
			}

			genreIDStrs := make([]string, 0, len(tmdbMovie.GenreIDs))
			for _, id := range tmdbMovie.GenreIDs {
				genreIDStrs = append(genreIDStrs, strconv.Itoa(id))
			}
			genreNames := idsToGenreNames(genreIDStrs, idToGenre)

			movie := dto.Movie{
				ID:             tmdbMovie.ID,
				Title:          tmdbMovie.Title,
				OriginalTitle:  tmdbMovie.OriginalTitle,
				PosterURL:      TMDB_IMAGE_BASE + "/w500" + tmdbMovie.PosterPath,
				BackdropURL:    TMDB_IMAGE_BASE + "/original" + tmdbMovie.BackdropPath,
				Rating:         tmdbMovie.VoteAverage,
				ReleaseYear:    releaseYear,
				Genres:         genreNames,
				Overview:       tmdbMovie.Overview,
				RequestedGenre: genre,
			}
			allMovies = append(allMovies, movie)

			if len(allMovies) >= amount {
				break
			}
		}

		page++
	}

	return allMovies, nil
}

func (c *Client) FetchMovies(ctx context.Context, req dto.DiscoverRequest) (*dto.DiscoverResponse, error) {
	var allMovies []dto.Movie

	for _, pref := range req.Genres {
		movies, err := c.fetchMovie(ctx, pref.Genre, req.ExcludedGenres, pref.Count)
		if err != nil {
			return nil, fmt.Errorf("failed to fetch genre '%s': %w", pref.Genre, err)
		}
		allMovies = append(allMovies, movies...)
	}

	return &dto.DiscoverResponse{
		Movies:     allMovies,
		TotalCount: len(allMovies),
	}, nil
}

func genresToIDString(genres []string, genreToId map[string]string) string {
	ids := make([]string, 0, len(genres))
	for _, g := range genres {
		if id, ok := genreToId[g]; ok {
			ids = append(ids, id)
		}
	}
	return strings.Join(ids, ",")
}

func idsToGenreNames(ids []string, idToGenre map[string]string) []string {
	names := make([]string, 0, len(ids))
	for _, id := range ids {
		if name, ok := idToGenre[id]; ok {
			names = append(names, name)
		}
	}
	return names
}
