package handlers

import (
	"bytes"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/TheEmfield/CoPick/connector/internal/dto"
	"github.com/TheEmfield/CoPick/connector/internal/server/handlers/mocks"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func discardLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

func TestGetMovies_Success(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	expectedResp := &dto.DiscoverResponse{Movies: []dto.Movie{{ID: 1, Title: "Test"}}, TotalCount: 1}
	mockFetcher.On("FetchMovies", mock.Anything, mock.AnythingOfType("dto.DiscoverRequest")).Return(expectedResp, nil).Once()

	api := NewServiceAPI(discardLogger(), nil, mockFetcher)
	reqBody := []byte(`{"preferences": [{"genre": "action", "count": 1}], "excluded_genres": ["horror"]}`)

	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.NoError(t, err)
	assert.Equal(t, http.StatusOK, rec.Code)
}

func TestGetMovies_InvalidJSON(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	api := NewServiceAPI(discardLogger(), nil, mockFetcher)

	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader([]byte(`{invalid json}`)))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.Error(t, err)
	httpErr, ok := err.(*echo.HTTPError)
	require.True(t, ok)
	assert.Equal(t, http.StatusBadRequest, httpErr.Code)
	mockFetcher.AssertNotCalled(t, "FetchMovies")
}

func TestGetMovies_EmptyPreferences(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	api := NewServiceAPI(discardLogger(), nil, mockFetcher)

	reqBody := []byte(`{"preferences": [], "excluded_genres": []}`)
	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.Error(t, err)
	httpErr := err.(*echo.HTTPError)
	assert.Equal(t, http.StatusBadRequest, httpErr.Code)
}

func TestGetMovies_InvalidCount(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	api := NewServiceAPI(discardLogger(), nil, mockFetcher)

	reqBody := []byte(`{"preferences": [{"genre": "action", "count": 0}]}`)
	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.Error(t, err)
	httpErr := err.(*echo.HTTPError)
	assert.Equal(t, http.StatusBadRequest, httpErr.Code)
	mockFetcher.AssertNotCalled(t, "FetchMovies")
}

func TestGetMovies_FetcherUnknownGenreError(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	mockFetcher.On("FetchMovies", mock.Anything, mock.Anything).Return(nil, errors.New("unknown genre: fantasy")).Once()

	api := NewServiceAPI(discardLogger(), nil, mockFetcher)
	reqBody := []byte(`{"preferences": [{"genre": "fantasy", "count": 1}]}`)

	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.Error(t, err)
	httpErr := err.(*echo.HTTPError)
	assert.Equal(t, http.StatusBadRequest, httpErr.Code)
}

func TestGetMovies_FetcherInternalError(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	mockFetcher.On("FetchMovies", mock.Anything, mock.Anything).Return(nil, errors.New("connection timeout")).Once()

	api := NewServiceAPI(discardLogger(), nil, mockFetcher)
	reqBody := []byte(`{"preferences": [{"genre": "action", "count": 1}]}`)

	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.Error(t, err)
	httpErr := err.(*echo.HTTPError)
	assert.Equal(t, http.StatusInternalServerError, httpErr.Code)
}

func TestGetMovies_MultiplePreferences(t *testing.T) {
	mockFetcher := mocks.NewFetcher(t)
	expectedResp := &dto.DiscoverResponse{Movies: []dto.Movie{{ID: 1}, {ID: 2}}, TotalCount: 2}
	mockFetcher.On("FetchMovies", mock.Anything, mock.AnythingOfType("dto.DiscoverRequest")).Return(expectedResp, nil).Once()

	api := NewServiceAPI(discardLogger(), nil, mockFetcher)
	reqBody := []byte(`{"preferences": [{"genre": "action", "count": 1}, {"genre": "comedy", "count": 1}]}`)

	req := httptest.NewRequest(http.MethodPost, "/discover", bytes.NewReader(reqBody))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	echoCtx := echo.New().NewContext(req, rec)

	err := api.GetMovies(echoCtx)

	require.NoError(t, err)
	assert.Equal(t, http.StatusOK, rec.Code)
}
