#!/bin/bash

# Generate Go code from proto files
# This script generates gRPC code from the proto definition

set -e

echo "Generating Go code from proto files..."

# Check if protoc is installed
if ! command -v protoc &> /dev/null; then
    echo "Error: protoc is not installed. Please install Protocol Buffers compiler."
    echo "On macOS: brew install protobuf"
    echo "On Ubuntu: sudo apt-get install protobuf-compiler"
    exit 1
fi

# Check if protoc-gen-go is installed
if ! command -v protoc-gen-go &> /dev/null; then
    echo "Installing protoc-gen-go..."
    go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
    go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
fi

# Create output directory
mkdir -p proto/testing

# Generate Go code
protoc --go_out=. --go_opt=paths=source_relative \
       --go-grpc_out=. --go-grpc_opt=paths=source_relative \
       proto/testing.proto

echo "Proto code generation complete!"
echo "Generated files are in proto/testing/"
