package grpc

import (
	"context"
	"fmt"
	"log"
	"net"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"

	"pingpad-api-testing-engine/internal/models"
	"pingpad-api-testing-engine/pkg/testing"
	pb "pingpad-api-testing-engine/proto"
)

// Server represents the gRPC server for API testing
type Server struct {
	pb.UnimplementedApiTestingServiceServer
	engine *testing.Engine
	config *models.Config
}

// NewServer creates a new gRPC server
func NewServer(engine *testing.Engine, config *models.Config) *Server {
	return &Server{
		engine: engine,
		config: config,
	}
}

// TestEndpoint executes a single API endpoint test
func (s *Server) TestEndpoint(ctx context.Context, req *pb.TestRequest) (*pb.TestResult, error) {
	// Extract correlation ID from metadata if present
	correlationID := extractCorrelationID(ctx)
	if correlationID != "" {
		log.Printf("[%s] Executing gRPC test for endpoint %s (%s %s)", correlationID, req.EndpointId, req.Method, req.Url)
	}

	// Parse timeout
	timeout, err := time.ParseDuration(req.Timeout)
	if err != nil || timeout == 0 {
		timeout = s.config.Testing.DefaultTimeout
	}

	// Convert proto request to internal model
	testReq := &models.TestRequest{
		ID:              req.Id,
		EndpointID:      req.EndpointId,
		Method:          req.Method,
		URL:             req.Url,
		Headers:         req.Headers,
		Body:            req.Body,
		Timeout:         timeout,
		FollowRedirects: req.FollowRedirects,
		MaxRetries:      int(req.MaxRetries),
		UserID:          req.UserId,
	}

	// Parse createdAt if provided
	if req.CreatedAt != "" {
		if t, err := time.Parse(time.RFC3339, req.CreatedAt); err == nil {
			testReq.CreatedAt = t
		} else {
			testReq.CreatedAt = time.Now()
		}
	} else {
		testReq.CreatedAt = time.Now()
	}

	// Set defaults
	if testReq.ID == "" {
		testReq.ID = generateID()
	}
	if testReq.MaxRetries == 0 {
		testReq.MaxRetries = s.config.Testing.MaxRetries
	}

	// Execute test
	startTime := time.Now()
	result := s.engine.ExecuteTest(ctx, testReq)
	executionTime := time.Since(startTime)

	if correlationID != "" {
		log.Printf("[%s] Test completed for endpoint %s in %v", correlationID, req.EndpointId, executionTime)
	}

	// Convert result to proto
	return &pb.TestResult{
		Id:              result.ID,
		TestRequestId:   result.TestRequestID,
		EndpointId:      result.EndpointID,
		StatusCode:      int32(result.StatusCode),
		ResponseTimeNanos: int64(result.ResponseTime),
		ResponseBody:    result.ResponseBody,
		ResponseHeaders: result.ResponseHeaders,
		Error:           result.Error,
		Success:         result.Success,
		Timestamp:       result.Timestamp.Format(time.RFC3339),
		RetryCount:      int32(result.RetryCount),
	}, nil
}

// TestBatch executes multiple API endpoint tests
func (s *Server) TestBatch(ctx context.Context, req *pb.BatchTestRequest) (*pb.BatchTestResult, error) {
	correlationID := extractCorrelationID(ctx)
	if correlationID != "" {
		log.Printf("[%s] Executing gRPC batch test with %d requests", correlationID, len(req.Requests))
	}

	// Convert proto requests to internal models
	requests := make([]*models.TestRequest, len(req.Requests))
	for i, protoReq := range req.Requests {
		timeout, err := time.ParseDuration(protoReq.Timeout)
		if err != nil || timeout == 0 {
			timeout = s.config.Testing.DefaultTimeout
		}

		testReq := &models.TestRequest{
			ID:              protoReq.Id,
			EndpointID:      protoReq.EndpointId,
			Method:          protoReq.Method,
			URL:             protoReq.Url,
			Headers:         protoReq.Headers,
			Body:            protoReq.Body,
			Timeout:         timeout,
			FollowRedirects: protoReq.FollowRedirects,
			MaxRetries:      int(protoReq.MaxRetries),
			UserID:          protoReq.UserId,
		}

		if protoReq.CreatedAt != "" {
			if t, err := time.Parse(time.RFC3339, protoReq.CreatedAt); err == nil {
				testReq.CreatedAt = t
			} else {
				testReq.CreatedAt = time.Now()
			}
		} else {
			testReq.CreatedAt = time.Now()
		}

		if testReq.ID == "" {
			testReq.ID = generateID()
		}
		if testReq.MaxRetries == 0 {
			testReq.MaxRetries = s.config.Testing.MaxRetries
		}

		requests[i] = testReq
	}

	// Execute batch test
	start := time.Now()
	results := s.engine.ExecuteBatchTests(ctx, requests)
	duration := time.Since(start)

	// Convert results to proto
	protoResults := make([]*pb.TestResult, len(results))
	for i, result := range results {
		protoResults[i] = &pb.TestResult{
			Id:              result.ID,
			TestRequestId:   result.TestRequestID,
			EndpointId:      result.EndpointID,
			StatusCode:      int32(result.StatusCode),
			ResponseTimeNanos: int64(result.ResponseTime),
			ResponseBody:    result.ResponseBody,
			ResponseHeaders: result.ResponseHeaders,
			Error:           result.Error,
			Success:         result.Success,
			Timestamp:       result.Timestamp.Format(time.RFC3339),
			RetryCount:      int32(result.RetryCount),
		}
	}

	// Calculate summary
	summary := calculateSummary(results)

	// Convert summary to proto
	protoSummary := &pb.TestSummary{
		TotalTests:      int32(summary.TotalTests),
		Successful:      int32(summary.Successful),
		Failed:          int32(summary.Failed),
		AverageTimeNanos: int64(summary.AverageTime),
		MinTimeNanos:    int64(summary.MinTime),
		MaxTimeNanos:    int64(summary.MaxTime),
		SuccessRate:     summary.SuccessRate,
		ErrorRate:       summary.ErrorRate,
	}

	return &pb.BatchTestResult{
		Id:           req.Id,
		Results:      protoResults,
		Summary:      protoSummary,
		DurationNanos: int64(duration),
	}, nil
}

// GetHealth returns the health status
func (s *Server) GetHealth(ctx context.Context, req *pb.HealthRequest) (*pb.HealthResponse, error) {
	metrics := s.engine.GetMetrics()

	return &pb.HealthResponse{
		Status:        "healthy",
		Version:       "1.0.0",
		UptimeSeconds: int64(time.Since(time.Now().Add(-24 * time.Hour)).Seconds()), // Placeholder
		Metrics:       metrics,
		Timestamp:     time.Now().Format(time.RFC3339),
	}, nil
}

// GetMetrics returns current metrics
func (s *Server) GetMetrics(ctx context.Context, req *pb.MetricsRequest) (*pb.MetricsResponse, error) {
	metrics := s.engine.GetMetrics()

	return &pb.MetricsResponse{
		Metrics: metrics,
	}, nil
}

// Start starts the gRPC server
func (s *Server) Start(port int) error {
	lis, err := net.Listen("tcp", fmt.Sprintf("0.0.0.0:%d", port))
	if err != nil {
		return err
	}

	// Create gRPC server with unary interceptor for correlation ID
	grpcServer := grpc.NewServer(
		grpc.UnaryInterceptor(correlationIDInterceptor),
	)

	pb.RegisterApiTestingServiceServer(grpcServer, s)

	log.Printf("Starting gRPC server on port %d", port)
	return grpcServer.Serve(lis)
}

// extractCorrelationID extracts correlation ID from gRPC metadata
func extractCorrelationID(ctx context.Context) string {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return ""
	}

	values := md.Get("x-correlation-id")
	if len(values) > 0 {
		return values[0]
	}

	// Also check for X-Trace-ID
	values = md.Get("x-trace-id")
	if len(values) > 0 {
		return values[0]
	}

	return ""
}

// correlationIDInterceptor is a gRPC unary interceptor that logs correlation IDs
func correlationIDInterceptor(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
	correlationID := extractCorrelationID(ctx)
	if correlationID != "" {
		ctx = context.WithValue(ctx, "correlationId", correlationID)
	}
	return handler(ctx, req)
}

// calculateSummary calculates test summary statistics
func calculateSummary(results []*models.TestResult) models.TestSummary {
	if len(results) == 0 {
		return models.TestSummary{}
	}

	total := len(results)
	successful := 0
	var totalTime time.Duration
	minTime := results[0].ResponseTime
	maxTime := results[0].ResponseTime

	for _, result := range results {
		if result.Success {
			successful++
		}
		totalTime += result.ResponseTime

		if result.ResponseTime < minTime {
			minTime = result.ResponseTime
		}
		if result.ResponseTime > maxTime {
			maxTime = result.ResponseTime
		}
	}

	avgTime := totalTime / time.Duration(total)
	successRate := float64(successful) / float64(total) * 100
	errorRate := 100 - successRate

	return models.TestSummary{
		TotalTests:  total,
		Successful:   successful,
		Failed:       total - successful,
		AverageTime:  avgTime,
		MinTime:      minTime,
		MaxTime:      maxTime,
		SuccessRate:  successRate,
		ErrorRate:    errorRate,
	}
}

// generateID generates a unique ID
func generateID() string {
	return "test_" + time.Now().Format(time.RFC3339Nano)
}
