FROM golang:1.25.7 AS builder
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 go build -o migrator ./cmd/migrator

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/migrator .

COPY --from=builder /app/migrations ./migrations
COPY --from=builder /app/config.yaml ./
