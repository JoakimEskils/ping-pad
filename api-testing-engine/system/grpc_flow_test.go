package system

import (
	"context"
	"net"
	"testing"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	grpcServer "pingpad-api-testing-engine/internal/grpc"
	"pingpad-api-testing-engine/internal/models"
	testingEngine "pingpad-api-testing-engine/pkg/testing"
	pb "pingpad-api-testing-engine/proto"
)

// TestGrpcFlow_EndToEnd tests the complete gRPC flow from client to server
func TestGrpcFlow_EndToEnd(t *testing.T) {
	// Setup test server
	testingConfig := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testingEngine.NewEngine(testingConfig)
	appConfig := &models.Config{
		Testing: *testingConfig,
	}

	server := grpcServer.NewServer(engine, appConfig)

	// Start gRPC server on a random port
	listener, err := net.Listen("tcp", ":0")
	if err != nil {
		t.Fatalf("Failed to listen: %v", err)
	}

	serverAddr := listener.Addr().String()

	// Create gRPC server manually
	grpcSrv := grpc.NewServer(
		grpc.UnaryInterceptor(func(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
			return handler(ctx, req)
		}),
	)
	pb.RegisterApiTestingServiceServer(grpcSrv, server)

	go func() {
		if err := grpcSrv.Serve(listener); err != nil {
			t.Errorf("gRPC server failed: %v", err)
		}
	}()

	// Give server time to start
	time.Sleep(100 * time.Millisecond)

	// Connect to the server
	conn, err := grpc.Dial(serverAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatalf("Failed to connect to server: %v", err)
	}
	defer conn.Close()

	client := pb.NewApiTestingServiceClient(conn)

	// Test 1: Single endpoint test
	t.Run("TestEndpoint", func(t *testing.T) {
		req := &pb.TestRequest{
			Id:         "system-test-1",
			EndpointId: "endpoint-1",
			Method:     "GET",
			Url:        "https://httpbin.org/get",
			Headers:    map[string]string{"User-Agent": "PingPad-SystemTest"},
			Timeout:    "30s",
			MaxRetries: 0,
			UserId:     "user-1",
			CreatedAt:  time.Now().Format(time.RFC3339),
		}

		ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		defer cancel()

		result, err := client.TestEndpoint(ctx, req)
		if err != nil {
			t.Fatalf("TestEndpoint failed: %v", err)
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

		// Note: Actual HTTP request may succeed or fail, but we should get a valid response structure
		if result.StatusCode == 0 && result.Error == "" {
			t.Error("Result should have either status code or error")
		}
	})

	// Test 2: Batch test
	t.Run("TestBatch", func(t *testing.T) {
		batchReq := &pb.BatchTestRequest{
			Id: "system-batch-1",
			Requests: []*pb.TestRequest{
				{
					Id:         "batch-test-1",
					EndpointId: "endpoint-1",
					Method:     "GET",
					Url:        "https://httpbin.org/get",
					Timeout:    "30s",
					MaxRetries: 0,
					UserId:     "user-1",
				},
				{
					Id:         "batch-test-2",
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

		ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		defer cancel()

		result, err := client.TestBatch(ctx, batchReq)
		if err != nil {
			t.Fatalf("TestBatch failed: %v", err)
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
	})

	// Test 3: Health check
	t.Run("GetHealth", func(t *testing.T) {
		req := &pb.HealthRequest{}

		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		result, err := client.GetHealth(ctx, req)
		if err != nil {
			t.Fatalf("GetHealth failed: %v", err)
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
	})

	// Test 4: Metrics
	t.Run("GetMetrics", func(t *testing.T) {
		req := &pb.MetricsRequest{}

		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		result, err := client.GetMetrics(ctx, req)
		if err != nil {
			t.Fatalf("GetMetrics failed: %v", err)
		}

		if result == nil {
			t.Fatal("GetMetrics returned nil result")
		}

		if result.Metrics == nil {
			t.Error("Metrics response should not be nil")
		}
	})
}

// TestGrpcFlow_WithPOSTRequest tests POST requests with body
func TestGrpcFlow_WithPOSTRequest(t *testing.T) {
	testingConfig := &models.TestingConfig{
		DefaultTimeout:     30 * time.Second,
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testingEngine.NewEngine(testingConfig)
	appConfig := &models.Config{
		Testing: *testingConfig,
	}

	server := grpcServer.NewServer(engine, appConfig)

	listener, err := net.Listen("tcp", ":0")
	if err != nil {
		t.Fatalf("Failed to listen: %v", err)
	}

	go func() {
		if err := grpcServer.Start(listener.Addr().(*net.TCPAddr).Port); err != nil {
			t.Errorf("gRPC server failed: %v", err)
		}
	}()

	time.Sleep(100 * time.Millisecond)

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatalf("Failed to connect to server: %v", err)
	}
	defer conn.Close()

	client := pb.NewApiTestingServiceClient(conn)

	req := &pb.TestRequest{
		Id:         "system-post-test",
		EndpointId: "endpoint-1",
		Method:     "POST",
		Url:        "https://httpbin.org/post",
		Headers:    map[string]string{"Content-Type": "application/json"},
		Body:       []byte(`{"key": "value", "test": true}`),
		Timeout:    "30s",
		MaxRetries: 0,
		UserId:     "user-1",
		CreatedAt:  time.Now().Format(time.RFC3339),
	}

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	result, err := client.TestEndpoint(ctx, req)
	if err != nil {
		t.Fatalf("TestEndpoint with POST failed: %v", err)
	}

	if result == nil {
		t.Fatal("TestEndpoint returned nil result")
	}

	if result.Id == "" {
		t.Error("Result ID should not be empty")
	}
}

// TestGrpcFlow_ErrorHandling tests error scenarios
func TestGrpcFlow_ErrorHandling(t *testing.T) {
	testingConfig := &models.TestingConfig{
		DefaultTimeout:     5 * time.Second, // Short timeout
		MaxConcurrency:     10,
		RateLimitPerSecond: 1000,
		MaxRetries:         0,
		RetryDelay:         time.Second,
		MaxResponseSize:    10 * 1024 * 1024,
		FollowRedirects:    true,
	}

	engine := testingEngine.NewEngine(testingConfig)
	appConfig := &models.Config{
		Testing: *testingConfig,
	}

	server := grpcServer.NewServer(engine, appConfig)

	listener, err := net.Listen("tcp", ":0")
	if err != nil {
		t.Fatalf("Failed to listen: %v", err)
	}

	go func() {
		if err := grpcServer.Start(listener.Addr().(*net.TCPAddr).Port); err != nil {
			t.Errorf("gRPC server failed: %v", err)
		}
	}()

	time.Sleep(100 * time.Millisecond)

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatalf("Failed to connect to server: %v", err)
	}
	defer conn.Close()

	client := pb.NewApiTestingServiceClient(conn)

	// Test with invalid URL (should still return a result, but with error)
	req := &pb.TestRequest{
		Id:         "system-error-test",
		EndpointId: "endpoint-1",
		Method:     "GET",
		Url:        "http://invalid-url-that-does-not-exist-12345.com",
		Timeout:    "5s",
		MaxRetries: 0,
		UserId:     "user-1",
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	result, err := client.TestEndpoint(ctx, req)
	if err != nil {
		// gRPC call itself should succeed, but the HTTP request may fail
		t.Fatalf("TestEndpoint should return result even on error, got gRPC error: %v", err)
	}

	if result == nil {
		t.Fatal("TestEndpoint should return result even on error")
	}

	// Result should indicate failure
	if result.Success {
		t.Error("Expected success=false for invalid URL")
	}

	if result.Error == "" {
		t.Error("Expected error message for invalid URL")
	}
}
