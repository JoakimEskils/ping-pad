package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"pingpad-api-testing-engine/internal/api"
	"pingpad-api-testing-engine/internal/config"
	grpcServer "pingpad-api-testing-engine/internal/grpc"
	"pingpad-api-testing-engine/pkg/testing"
)

const version = "1.0.0"

func main() {
	// Load configuration
	cfg, err := config.LoadConfig("")
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Create testing engine
	engine := testing.NewEngine(&cfg.Testing)

	// Create API handler
	handler := api.NewHandler(engine, cfg)

	// Setup routes
	mux := handler.SetupRoutes()

	// Add middleware (correlation ID first, then CORS)
	handlerWithMiddleware := handler.AddCorrelationIDMiddleware(mux)
	handlerWithCORS := addCORS(handlerWithMiddleware)

	// Create HTTP server
	server := &http.Server{
		Addr:         fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port),
		Handler:      handlerWithCORS,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
	}

	// Create gRPC server
	grpcSrv := grpcServer.NewServer(engine, cfg)
	grpcPort := 9090 // Default gRPC port, can be configured

	// Start HTTP server in a goroutine
	go func() {
		log.Printf("Starting PingPad API Testing Engine HTTP v%s on %s", version, server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("HTTP server failed to start: %v", err)
		}
	}()

	// Start gRPC server in a goroutine
	go func() {
		log.Printf("Starting PingPad API Testing Engine gRPC v%s on port %d", version, grpcPort)
		if err := grpcSrv.Start(grpcPort); err != nil {
			log.Fatalf("gRPC server failed to start: %v", err)
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	// Graceful shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	log.Println("Server exited")
}

// addCORS adds CORS headers to all responses
func addCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Correlation-ID")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}
