package models

import (
	"net/http"
)

type ErrorResponce struct {
	Status  int    `json:"-"`
	Code    string `json:"code"`
	Message string `json:"message"`
}

func (e *ErrorResponce) Error() string {
	return e.Message
}

var (
	ErrInternal     = &ErrorResponce{http.StatusInternalServerError, "INTERNAL_ERROR", "internal server error"}
	ErrInvalidLimit = &ErrorResponce{http.StatusBadRequest, "INVALID_LIMIT", "invalid limit parameter: must be a positive integer"}
	ErrInvalidPage  = &ErrorResponce{http.StatusBadRequest, "INVALID_PAGE", "invalid page parameter: must be a positive integer"}
)
