# Build stage
FROM golang:1.25-alpine AS builder

# Install git and protoc (required for go mod download and proto generation)
RUN apk add --no-cache git protobuf

# Install protoc plugins and add to PATH
# Default GOPATH in golang image is /go
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@latest && \
    go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

ENV PATH=$PATH:/go/bin

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum* ./
RUN go mod download

# Copy all source code
COPY . .

# Generate proto files (must happen after copying source)
RUN protoc --go_out=. --go_opt=paths=source_relative \
           --go-grpc_out=. --go-grpc_opt=paths=source_relative \
           proto/testing.proto

# Update go.sum and build the application
RUN go mod tidy && \
    CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o main ./cmd/server

# Final stage
FROM alpine:latest

RUN apk --no-cache add ca-certificates tzdata

WORKDIR /root/

# Copy the binary from builder stage
COPY --from=builder /app/main .

# Copy config file
COPY --from=builder /app/config.json .

# Expose ports (HTTP and gRPC)
EXPOSE 8081 9090

# Run the application
CMD ["./main"]
