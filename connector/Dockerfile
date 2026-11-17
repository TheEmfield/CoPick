FROM golang:1.26.0-alpine AS builder
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 go build -o connector ./cmd/main.go

FROM alpine:latest
WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata
COPY --from=builder /app/connector .
COPY --from=builder /app/config.yaml ./

EXPOSE 8080

ENTRYPOINT ["./connector"]
CMD ["-c", "config.yaml"]