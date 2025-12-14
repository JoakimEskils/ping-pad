package testing

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"pingpad-api-testing-engine/internal/models"
)

func TestNewEngine(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 100,
		MaxRetries:         3,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	if engine == nil {
		t.Fatal("NewEngine returned nil")
	}

	if engine.client == nil {
		t.Error("Engine client is nil")
	}

	if engine.config != config {
		t.Error("Engine config not set correctly")
	}

	if engine.rateLimiter == nil {
		t.Error("Engine rate limiter is nil")
	}

	if engine.metrics == nil {
		t.Error("Engine metrics is nil")
	}
}

func TestExecuteTest_Success(t *testing.T) {
	// Create a test HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"success": true}`))
	}))
	defer server.Close()

	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000, // High rate limit for tests
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	req := &models.TestRequest{
		ID:         "test-1",
		EndpointID: "endpoint-1",
		Method:     "GET",
		URL:        server.URL,
		Headers:    make(map[string]string),
		Timeout:    30 * time.Second,
		MaxRetries: 0,
		UserID:     "user-1",
		CreatedAt:  time.Now(),
	}

	ctx := context.Background()
	result := engine.ExecuteTest(ctx, req)

	if result == nil {
		t.Fatal("ExecuteTest returned nil")
	}

	if !result.Success {
		t.Errorf("Expected success=true, got success=false. Error: %s", result.Error)
	}

	if result.StatusCode != http.StatusOK {
		t.Errorf("Expected status code 200, got %d", result.StatusCode)
	}

	if result.ResponseTime <= 0 {
		t.Error("Response time should be greater than 0")
	}

	if len(result.ResponseBody) == 0 {
		t.Error("Response body should not be empty")
	}
}

func TestExecuteTest_NotFound(t *testing.T) {
	// Create a test HTTP server that returns 404
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte(`Not Found`))
	}))
	defer server.Close()

	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	req := &models.TestRequest{
		ID:         "test-1",
		EndpointID: "endpoint-1",
		Method:     "GET",
		URL:        server.URL,
		Headers:    make(map[string]string),
		Timeout:    30 * time.Second,
		MaxRetries: 0,
		UserID:     "user-1",
		CreatedAt:  time.Now(),
	}

	ctx := context.Background()
	result := engine.ExecuteTest(ctx, req)

	if result == nil {
		t.Fatal("ExecuteTest returned nil")
	}

	if result.Success {
		t.Error("Expected success=false for 404 status, got success=true")
	}

	if result.StatusCode != http.StatusNotFound {
		t.Errorf("Expected status code 404, got %d", result.StatusCode)
	}
}

func TestExecuteTest_WithHeaders(t *testing.T) {
	// Create a test HTTP server that checks headers
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader != "Bearer test-token" {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"authenticated": true}`))
	}))
	defer server.Close()

	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	headers := map[string]string{
		"Authorization": "Bearer test-token",
		"Content-Type":  "application/json",
	}

	req := &models.TestRequest{
		ID:         "test-1",
		EndpointID: "endpoint-1",
		Method:     "GET",
		URL:        server.URL,
		Headers:    headers,
		Timeout:    30 * time.Second,
		MaxRetries: 0,
		UserID:     "user-1",
		CreatedAt:  time.Now(),
	}

	ctx := context.Background()
	result := engine.ExecuteTest(ctx, req)

	if result == nil {
		t.Fatal("ExecuteTest returned nil")
	}

	if !result.Success {
		t.Errorf("Expected success=true with valid headers, got success=false. Error: %s", result.Error)
	}

	if result.StatusCode != http.StatusOK {
		t.Errorf("Expected status code 200, got %d", result.StatusCode)
	}
}

func TestExecuteTest_POSTWithBody(t *testing.T) {
	// Create a test HTTP server that echoes the request body
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "POST" {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"received": true}`))
	}))
	defer server.Close()

	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	req := &models.TestRequest{
		ID:         "test-1",
		EndpointID: "endpoint-1",
		Method:     "POST",
		URL:        server.URL,
		Headers:    map[string]string{"Content-Type": "application/json"},
		Body:       []byte(`{"key": "value"}`),
		Timeout:    30 * time.Second,
		MaxRetries: 0,
		UserID:     "user-1",
		CreatedAt:  time.Now(),
	}

	ctx := context.Background()
	result := engine.ExecuteTest(ctx, req)

	if result == nil {
		t.Fatal("ExecuteTest returned nil")
	}

	if !result.Success {
		t.Errorf("Expected success=true for POST request, got success=false. Error: %s", result.Error)
	}

	if result.StatusCode != http.StatusOK {
		t.Errorf("Expected status code 200, got %d", result.StatusCode)
	}
}

func TestExecuteTest_InvalidURL(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	req := &models.TestRequest{
		ID:         "test-1",
		EndpointID: "endpoint-1",
		Method:     "GET",
		URL:        "http://invalid-url-that-does-not-exist-12345.com",
		Headers:    make(map[string]string),
		Timeout:    5 * time.Second, // Short timeout for test
		MaxRetries: 0,
		UserID:     "user-1",
		CreatedAt:  time.Now(),
	}

	ctx := context.Background()
	result := engine.ExecuteTest(ctx, req)

	if result == nil {
		t.Fatal("ExecuteTest returned nil")
	}

	if result.Success {
		t.Error("Expected success=false for invalid URL, got success=true")
	}

	if result.Error == "" {
		t.Error("Expected error message for invalid URL")
	}
}

func TestExecuteBatchTests(t *testing.T) {
	// Create a test HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"success": true}`))
	}))
	defer server.Close()

	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	requests := []*models.TestRequest{
		{
			ID:         "test-1",
			EndpointID: "endpoint-1",
			Method:     "GET",
			URL:        server.URL,
			Headers:    make(map[string]string),
			Timeout:    30 * time.Second,
			MaxRetries: 0,
			UserID:     "user-1",
			CreatedAt:  time.Now(),
		},
		{
			ID:         "test-2",
			EndpointID: "endpoint-2",
			Method:     "GET",
			URL:        server.URL,
			Headers:    make(map[string]string),
			Timeout:    30 * time.Second,
			MaxRetries: 0,
			UserID:     "user-1",
			CreatedAt:  time.Now(),
		},
		{
			ID:         "test-3",
			EndpointID: "endpoint-3",
			Method:     "GET",
			URL:        server.URL,
			Headers:    make(map[string]string),
			Timeout:    30 * time.Second,
			MaxRetries: 0,
			UserID:     "user-1",
			CreatedAt:  time.Now(),
		},
	}

	ctx := context.Background()
	results := engine.ExecuteBatchTests(ctx, requests)

	if len(results) != len(requests) {
		t.Fatalf("Expected %d results, got %d", len(requests), len(results))
	}

	for i, result := range results {
		if result == nil {
			t.Errorf("Result %d is nil", i)
			continue
		}

		if !result.Success {
			t.Errorf("Result %d expected success=true, got success=false. Error: %s", i, result.Error)
		}

		if result.StatusCode != http.StatusOK {
			t.Errorf("Result %d expected status code 200, got %d", i, result.StatusCode)
		}
	}
}

func TestGetMetrics(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := NewEngine(config)

	metrics := engine.GetMetrics()

	if metrics == nil {
		t.Fatal("GetMetrics returned nil")
	}

	// Check that expected keys exist
	expectedKeys := []string{
		"total_tests",
		"successful_tests",
		"failed_tests",
		"success_rate",
		"avg_response_time",
		"min_response_time",
		"max_response_time",
		"uptime_seconds",
	}

	for _, key := range expectedKeys {
		if _, exists := metrics[key]; !exists {
			t.Errorf("Expected metric key '%s' not found", key)
		}
	}
}
