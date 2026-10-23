package service

import (
	"context"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/TheEmfield/CoPick/connector/internal/config"
	"github.com/TheEmfield/CoPick/connector/internal/dto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func discardLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

func TestMovieClient_FetchMovies_Success(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		mockResponse := map[string]interface{}{
			"results": []map[string]interface{}{
				{
					"id": 12345, "title": "Test Movie", "original_title": "Test",
					"poster_path": "/p.jpg", "backdrop_path": "/b.jpg",
					"vote_average": 8.5, "release_date": "2023-10-19",
					"genre_ids": []int{28, 12}, "overview": "Desc",
				},
			},
		}
		json.NewEncoder(w).Encode(mockResponse)
	}))
	defer mockServer.Close()

	client := NewMovieClient(config.TMDB{URL: mockServer.URL, TMDBTimeout: 5 * time.Second}, discardLogger())
	req := dto.DiscoverRequest{
		Preferences:    []dto.GenrePreference{{Genre: "action", Count: 1}},
		ExcludedGenres: []string{"horror"},
	}

	resp, err := client.FetchMovies(context.Background(), req)

	require.NoError(t, err)
	require.NotNil(t, resp)
	assert.Equal(t, 1, resp.TotalCount)
	assert.Len(t, resp.Movies, 1)
	assert.Equal(t, 12345, resp.Movies[0].ID)
	assert.Equal(t, 2023, resp.Movies[0].ReleaseYear)
	assert.Equal(t, "https://image.tmdb.org/t/p/w500/p.jpg", resp.Movies[0].PosterURL)
}

func TestMovieClient_FetchMovies_UnknownGenre(t *testing.T) {
	client := NewMovieClient(config.TMDB{}, discardLogger())
	req := dto.DiscoverRequest{Preferences: []dto.GenrePreference{{Genre: "unknown", Count: 1}}}

	resp, err := client.FetchMovies(context.Background(), req)

	require.Error(t, err)
	require.Nil(t, resp)
	assert.Contains(t, err.Error(), "unknown genre: unknown")
}

func TestMovieClient_FetchMovies_APIError(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
		w.Write([]byte(`{"error": "unauthorized"}`))
	}))
	defer mockServer.Close()

	client := NewMovieClient(config.TMDB{URL: mockServer.URL}, discardLogger())
	req := dto.DiscoverRequest{Preferences: []dto.GenrePreference{{Genre: "action", Count: 1}}}

	resp, err := client.FetchMovies(context.Background(), req)

	require.Error(t, err)
	require.Nil(t, resp)
	assert.Contains(t, err.Error(), "tmdb returned status 401")
}

func TestMovieClient_FetchMovies_EmptyResults(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"results": []}`))
	}))
	defer mockServer.Close()

	client := NewMovieClient(config.TMDB{URL: mockServer.URL}, discardLogger())
	req := dto.DiscoverRequest{Preferences: []dto.GenrePreference{{Genre: "action", Count: 1}}}

	resp, err := client.FetchMovies(context.Background(), req)

	require.NoError(t, err)
	assert.Equal(t, 0, resp.TotalCount)
	assert.Empty(t, resp.Movies)
}

func TestMovieClient_FetchMovies_Deduplication(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		mockResponse := map[string]interface{}{
			"results": []map[string]interface{}{
				{"id": 1, "title": "M1", "original_title": "M1", "poster_path": "/1.jpg", "backdrop_path": "/1.jpg", "vote_average": 8.0, "release_date": "2020-01-01", "genre_ids": []int{28}, "overview": "D1"},
				{"id": 1, "title": "M1", "original_title": "M1", "poster_path": "/1.jpg", "backdrop_path": "/1.jpg", "vote_average": 8.0, "release_date": "2020-01-01", "genre_ids": []int{28}, "overview": "D1"},
				{"id": 2, "title": "M2", "original_title": "M2", "poster_path": "/2.jpg", "backdrop_path": "/2.jpg", "vote_average": 8.0, "release_date": "2020-01-01", "genre_ids": []int{28}, "overview": "D2"},
			},
		}
		json.NewEncoder(w).Encode(mockResponse)
	}))
	defer mockServer.Close()

	client := NewMovieClient(config.TMDB{URL: mockServer.URL}, discardLogger())
	req := dto.DiscoverRequest{Preferences: []dto.GenrePreference{{Genre: "action", Count: 2}}}

	resp, err := client.FetchMovies(context.Background(), req)

	require.NoError(t, err)
	assert.Equal(t, 2, resp.TotalCount)
	assert.Len(t, resp.Movies, 2)
	assert.Equal(t, 1, resp.Movies[0].ID)
	assert.Equal(t, 2, resp.Movies[1].ID)
}

func TestMovieClient_FetchMovies_InvalidDate(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		mockResponse := map[string]interface{}{
			"results": []map[string]interface{}{
				{"id": 1, "title": "M1", "original_title": "M1", "poster_path": "/1.jpg", "backdrop_path": "/1.jpg", "vote_average": 8.0, "release_date": "invalid", "genre_ids": []int{28}, "overview": "D1"},
			},
		}
		json.NewEncoder(w).Encode(mockResponse)
	}))
	defer mockServer.Close()

	client := NewMovieClient(config.TMDB{URL: mockServer.URL}, discardLogger())
	req := dto.DiscoverRequest{Preferences: []dto.GenrePreference{{Genre: "action", Count: 1}}}

	resp, err := client.FetchMovies(context.Background(), req)

	require.NoError(t, err)
	assert.Equal(t, 0, resp.Movies[0].ReleaseYear)
}
