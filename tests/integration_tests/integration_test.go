package integration_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
	"github.com/stretchr/testify/require"
)

type AuthResponse struct {
	Token   string `json:"token"`
	Profile struct {
		ProfileID int    `json:"profileId"`
		Nickname  string `json:"nickname"`
	} `json:"profile"`
}

type GroupDetail struct {
	GroupID        int    `json:"groupId"`
	Name           string `json:"name"`
	InviteCode     string `json:"inviteCode"`
	AgeRestriction *int   `json:"ageRestriction"`
}

type DBGroup struct {
	GroupID        int    `db:"group_id"`
	Name           string `db:"name"`
	InviteCode     string `db:"invite_code"`
	AgeRestriction *int16 `db:"age_restriction"`
}

func TestUserJourney_CreateGroupAndVerifyInDB(t *testing.T) {
	dbHost := os.Getenv("DB_HOST")
	if dbHost == "" {
		dbHost = "localhost"
	}
	dbPort := os.Getenv("DB_PORT")
	if dbPort == "" {
		dbPort = "5433"
	}

	apiHost := os.Getenv("API_HOST")
	if apiHost == "" {
		apiHost = "localhost"
	}
	apiPort := os.Getenv("API_PORT")
	if apiPort == "" {
		apiPort = "8080"
	}

	dbURL := fmt.Sprintf("postgres://postgres:postgres@%s:%s/copick_db?sslmode=disable", dbHost, dbPort)
	baseURL := fmt.Sprintf("http://%s:%s/api/v1", apiHost, apiPort)

	db, err := sqlx.Connect("postgres", dbURL)
	require.NoError(t, err)

	nickname := fmt.Sprintf("testuser_%d", time.Now().Unix())
	password := "StrongPass123!"

	regBody := map[string]string{
		"nickname": nickname,
		"password": password,
	}
	bodyBytes, _ := json.Marshal(regBody)

	client := &http.Client{}
	req, err := http.NewRequest("POST", baseURL+"/auth/register", bytes.NewBuffer(bodyBytes))
	require.NoError(t, err)

	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)
	require.NoError(t, err)

	respBody, _ := io.ReadAll(resp.Body)
	resp.Body.Close()

	require.Contains(t, []int{http.StatusOK, http.StatusCreated}, resp.StatusCode,
		"Unexpected status code during registration. Response: %s", string(respBody))

	var authResp AuthResponse
	require.NoError(t, json.Unmarshal(respBody, &authResp))
	require.NotEmpty(t, authResp.Token)

	groupBody := map[string]any{
		"name":           "Тестовая группа sqlx",
		"ageRestriction": 18,
	}
	groupBytes, _ := json.Marshal(groupBody)

	groupReq, err := http.NewRequest("POST", baseURL+"/groups", bytes.NewBuffer(groupBytes))
	require.NoError(t, err)
	groupReq.Header.Set("Content-Type", "application/json")
	groupReq.Header.Set("Authorization", "Bearer "+authResp.Token)

	resp, err = client.Do(groupReq)
	require.NoError(t, err)

	respBody, _ = io.ReadAll(resp.Body)
	resp.Body.Close()

	require.Equal(t, http.StatusOK, resp.StatusCode, "Failed to create group. Response: %s", string(respBody))

	var createdGroup GroupDetail
	require.NoError(t, json.Unmarshal(respBody, &createdGroup))
	require.NotZero(t, createdGroup.GroupID)

	t.Cleanup(func() {
		ctx := context.Background()
		tx, err := db.BeginTx(ctx, nil)
		if err != nil {
			t.Logf("Failed to begin cleanup transaction: %v", err)
			return
		}

		_, err = tx.ExecContext(ctx, "DELETE FROM groups WHERE group_id = $1", createdGroup.GroupID)
		if err != nil {
			t.Logf("Failed to delete group: %v", err)
			tx.Rollback()
			return
		}

		_, err = tx.ExecContext(ctx, "DELETE FROM profiles WHERE nickname = $1", nickname)
		if err != nil {
			t.Logf("Failed to delete profile: %v", err)
			tx.Rollback()
			return
		}

		err = tx.Commit()
		if err != nil {
			t.Logf("Failed to commit cleanup: %v", err)
		}

		db.Close()
	})

	var dbGroup DBGroup
	err = db.Get(&dbGroup, `SELECT group_id, name, invite_code, age_restriction FROM groups WHERE group_id = $1`, createdGroup.GroupID)
	require.NoError(t, err)

	require.Equal(t, createdGroup.GroupID, dbGroup.GroupID)
	require.Equal(t, "Тестовая группа sqlx", dbGroup.Name)
	require.NotEmpty(t, dbGroup.InviteCode)
	require.NotNil(t, dbGroup.AgeRestriction)
	require.Equal(t, int16(18), *dbGroup.AgeRestriction)
}
