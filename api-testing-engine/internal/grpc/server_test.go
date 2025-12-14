package grpc

import (
	"context"
	"testing"
	"time"

	"pingpad-api-testing-engine/internal/models"
	"pingpad-api-testing-engine/pkg/testing"
	pb "pingpad-api-testing-engine/proto"
)

func TestTestEndpoint_Success(t *testing.T) {
	// Create a test engine with a mock HTTP client
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	req := &pb.TestRequest{
		Id:         "test-1",
		EndpointId: "endpoint-1",
		Method:     "GET",
		Url:        "https://httpbin.org/get",
		Headers:    map[string]string{"User-Agent": "PingPad-Test"},
		Timeout:    "30s",
		MaxRetries: 0,
		UserId:     "user-1",
		CreatedAt:  time.Now().Format(time.RFC3339),
	}

	ctx := context.Background()
	result, err := server.TestEndpoint(ctx, req)

	if err != nil {
		t.Fatalf("TestEndpoint returned error: %v", err)
	}

	if result == nil {
		t.Fatal("TestEndpoint returned nil result")
	}

	if result.Id == "" {
		t.Error("Result ID should not be empty")
	}

	if result.EndpointId != req.EndpointId {
		t.Errorf("Expected endpoint ID %s, got %s", req.EndpointId, result.EndpointId)
	}

	// Note: The actual HTTP request may succeed or fail depending on network,
	// but we should get a valid result structure
	if result.StatusCode == 0 && result.Error == "" {
		t.Error("Result should have either status code or error")
	}
}

func TestTestEndpoint_InvalidTimeout(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	req := &pb.TestRequest{
		Id:         "test-1",
		EndpointId: "endpoint-1",
		Method:     "GET",
		Url:        "https://httpbin.org/get",
		Timeout:    "invalid-timeout",
		MaxRetries: 0,
		UserId:     "user-1",
	}

	ctx := context.Background()
	result, err := server.TestEndpoint(ctx, req)

	if err != nil {
		t.Fatalf("TestEndpoint should handle invalid timeout gracefully, got error: %v", err)
	}

	if result == nil {
		t.Fatal("TestEndpoint returned nil result")
	}

	// Should use default timeout
	if result.Id == "" {
		t.Error("Result ID should not be empty")
	}
}

func TestTestEndpoint_WithBody(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	req := &pb.TestRequest{
		Id:         "test-1",
		EndpointId: "endpoint-1",
		Method:     "POST",
		Url:        "https://httpbin.org/post",
		Headers:    map[string]string{"Content-Type": "application/json"},
		Body:       []byte(`{"key": "value"}`),
		Timeout:    "30s",
		MaxRetries: 0,
		UserId:     "user-1",
		CreatedAt:  time.Now().Format(time.RFC3339),
	}

	ctx := context.Background()
	result, err := server.TestEndpoint(ctx, req)

	if err != nil {
		t.Fatalf("TestEndpoint returned error: %v", err)
	}

	if result == nil {
		t.Fatal("TestEndpoint returned nil result")
	}

	// Verify the request was processed (result should have ID)
	if result.Id == "" {
		t.Error("Result ID should not be empty")
	}
}

func TestTestBatch(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	batchReq := &pb.BatchTestRequest{
		Id: "batch-1",
		Requests: []*pb.TestRequest{
			{
				Id:         "test-1",
				EndpointId: "endpoint-1",
				Method:     "GET",
				Url:        "https://httpbin.org/get",
				Timeout:    "30s",
				MaxRetries: 0,
				UserId:     "user-1",
			},
			{
				Id:         "test-2",
				EndpointId: "endpoint-2",
				Method:     "GET",
				Url:        "https://httpbin.org/get",
				Timeout:    "30s",
				MaxRetries: 0,
				UserId:     "user-1",
			},
		},
		UserId:    "user-1",
		CreatedAt: time.Now().Format(time.RFC3339),
	}

	ctx := context.Background()
	result, err := server.TestBatch(ctx, batchReq)

	if err != nil {
		t.Fatalf("TestBatch returned error: %v", err)
	}

	if result == nil {
		t.Fatal("TestBatch returned nil result")
	}

	if len(result.Results) != len(batchReq.Requests) {
		t.Errorf("Expected %d results, got %d", len(batchReq.Requests), len(result.Results))
	}

	if result.Summary == nil {
		t.Fatal("Batch result summary is nil")
	}

	if result.Summary.TotalTests != int32(len(batchReq.Requests)) {
		t.Errorf("Expected %d total tests in summary, got %d", len(batchReq.Requests), result.Summary.TotalTests)
	}
}

func TestGetHealth(t *testing.T) {
	config := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	req := &pb.HealthRequest{}

	ctx := context.Background()
	result, err := server.GetHealth(ctx, req)

	if err != nil {
		t.Fatalf("GetHealth returned error: %v", err)
	}

	if result == nil {
		t.Fatal("GetHealth returned nil result")
	}

	if result.Status != "healthy" {
		t.Errorf("Expected status 'healthy', got '%s'", result.Status)
	}

	if result.Metrics == nil {
		t.Error("Health response metrics should not be nil")
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

	engine := testing.NewEngine(config)
	server := NewServer(engine, &models.Config{Testing: *config})

	req := &pb.MetricsRequest{}

	ctx := context.Background()
	result, err := server.GetMetrics(ctx, req)

	if err != nil {
		t.Fatalf("GetMetrics returned error: %v", err)
	}

	if result == nil {
		t.Fatal("GetMetrics returned nil result")
	}

	if result.Metrics == nil {
		t.Error("Metrics response should not be nil")
	}
}
