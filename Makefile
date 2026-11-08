test:
	go test ./...

build-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o bin/ ./cmd/...

lint:
	golangci-lint run ./...

.PHONY: test build-linux lint
