package config

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"time"

	"pingpad-api-testing-engine/internal/models"
)

// LoadConfig loads configuration from file or environment variables
func LoadConfig(configPath string) (*models.Config, error) {
	config := &models.Config{
		Server: models.ServerConfig{
			Host:         getEnv("SERVER_HOST", "localhost"),
			Port:         getEnvInt("SERVER_PORT", 8081),
			ReadTimeout:  getEnvDuration("SERVER_READ_TIMEOUT", 30*time.Second),
			WriteTimeout: getEnvDuration("SERVER_WRITE_TIMEOUT", 30*time.Second),
		},
		Database: models.DatabaseConfig{
			Host:     getEnv("DB_HOST", "localhost"),
			Port:     getEnvInt("DB_PORT", 5432),
			User:     getEnv("DB_USER", "pingpad"),
			Password: getEnv("DB_PASSWORD", "password"),
			Name:     getEnv("DB_NAME", "pingpad_testing"),
			SSLMode:  getEnv("DB_SSL_MODE", "disable"),
		},
		Testing: models.TestingConfig{
			DefaultTimeout:      getEnvDuration("TESTING_DEFAULT_TIMEOUT", 30*time.Second),
			MaxConcurrency:      getEnvInt("TESTING_MAX_CONCURRENCY", 100),
			RateLimitPerSecond:  getEnvInt("TESTING_RATE_LIMIT", 100),
			MaxRetries:          getEnvInt("TESTING_MAX_RETRIES", 3),
			RetryDelay:          getEnvDuration("TESTING_RETRY_DELAY", 1*time.Second),
			MaxResponseSize:     getEnvInt64("TESTING_MAX_RESPONSE_SIZE", 10*1024*1024), // 10MB
			FollowRedirects:     getEnvBool("TESTING_FOLLOW_REDIRECTS", true),
		},
		Logging: models.LoggingConfig{
			Level:  getEnv("LOG_LEVEL", "info"),
			Format: getEnv("LOG_FORMAT", "json"),
			Output: getEnv("LOG_OUTPUT", "stdout"),
		},
	}

	// Load from config file if provided
	if configPath != "" {
		if err := loadFromFile(config, configPath); err != nil {
			return nil, fmt.Errorf("failed to load config from file: %w", err)
		}
	}

	return config, nil
}

// loadFromFile loads configuration from a JSON file
func loadFromFile(config *models.Config, path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}

	return json.Unmarshal(data, config)
}

// Helper functions for environment variable parsing
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intValue, err := strconv.Atoi(value); err == nil {
			return intValue
		}
	}
	return defaultValue
}

func getEnvInt64(key string, defaultValue int64) int64 {
	if value := os.Getenv(key); value != "" {
		if intValue, err := strconv.ParseInt(value, 10, 64); err == nil {
			return intValue
		}
	}
	return defaultValue
}

func getEnvBool(key string, defaultValue bool) bool {
	if value := os.Getenv(key); value != "" {
		if boolValue, err := strconv.ParseBool(value); err == nil {
			return boolValue
		}
	}
	return defaultValue
}

func getEnvDuration(key string, defaultValue time.Duration) time.Duration {
	if value := os.Getenv(key); value != "" {
		if duration, err := time.ParseDuration(value); err == nil {
			return duration
		}
	}
	return defaultValue
}
