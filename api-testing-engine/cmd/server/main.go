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

	// Add CORS middleware
	handlerWithCORS := addCORS(mux)

	// Create server
	server := &http.Server{
		Addr:         fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port),
		Handler:      handlerWithCORS,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
	}

	// Start server in a goroutine
	go func() {
		log.Printf("Starting PingPad API Testing Engine v%s on %s", version, server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server failed to start: %v", err)
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
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}
