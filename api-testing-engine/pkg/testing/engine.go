package testing

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"net/http"
	"sync"
	"time"

	"pingpad-api-testing-engine/internal/models"
)

// Engine represents the API testing engine
type Engine struct {
	client      *http.Client
	config      *models.TestingConfig
	rateLimiter *RateLimiter
	metrics     *Metrics
	mu          sync.RWMutex
}

// NewEngine creates a new API testing engine
func NewEngine(config *models.TestingConfig) *Engine {
	client := &http.Client{
		Timeout: config.DefaultTimeout,
		Transport: &http.Transport{
			MaxIdleConns:        100,
			MaxIdleConnsPerHost: 10,
			IdleConnTimeout:     90 * time.Second,
		},
	}

	return &Engine{
		client:      client,
		config:      config,
		rateLimiter: NewRateLimiter(config.RateLimitPerSecond),
		metrics:     NewMetrics(),
	}
}

// ExecuteTest executes a single API test
func (e *Engine) ExecuteTest(ctx context.Context, req *models.TestRequest) *models.TestResult {
	// Apply rate limiting
	e.rateLimiter.Wait()
	
	// Create HTTP request
	httpReq, err := e.createHTTPRequest(ctx, req)
	if err != nil {
		return e.createErrorResult(req, err, 0)
	}

	// Execute request with retries
	result := e.executeWithRetries(ctx, httpReq, req)
	
	// Update metrics
	e.metrics.RecordTest(result)
	
	return result
}

// ExecuteBatchTests executes multiple API tests concurrently
func (e *Engine) ExecuteBatchTests(ctx context.Context, requests []*models.TestRequest) []*models.TestResult {
	results := make([]*models.TestResult, len(requests))
	
	// Use worker pool for controlled concurrency
	semaphore := make(chan struct{}, e.config.MaxConcurrency)
	var wg sync.WaitGroup
	
	for i, req := range requests {
		wg.Add(1)
		go func(index int, request *models.TestRequest) {
			defer wg.Done()
			
			// Acquire semaphore
			semaphore <- struct{}{}
			defer func() { <-semaphore }()
			
			results[index] = e.ExecuteTest(ctx, request)
		}(i, req)
	}
	
	wg.Wait()
	return results
}

// createHTTPRequest creates an HTTP request from a TestRequest
func (e *Engine) createHTTPRequest(ctx context.Context, req *models.TestRequest) (*http.Request, error) {
	var body io.Reader
	if len(req.Body) > 0 {
		body = bytes.NewReader(req.Body)
	}

	httpReq, err := http.NewRequestWithContext(ctx, req.Method, req.URL, body)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	// Set headers
	for key, value := range req.Headers {
		httpReq.Header.Set(key, value)
	}

	// Set default headers if not provided
	if httpReq.Header.Get("User-Agent") == "" {
		httpReq.Header.Set("User-Agent", "PingPad-API-Testing-Engine/1.0")
	}

	return httpReq, nil
}

// executeWithRetries executes the HTTP request with retry logic
func (e *Engine) executeWithRetries(ctx context.Context, httpReq *http.Request, req *models.TestRequest) *models.TestResult {
	var lastResult *models.TestResult
	maxRetries := req.MaxRetries
	if maxRetries <= 0 {
		maxRetries = e.config.MaxRetries
	}

	for attempt := 0; attempt <= maxRetries; attempt++ {
		result := e.executeSingleRequest(ctx, httpReq, req, attempt)
		lastResult = result

		// If successful or non-retryable error, return immediately
		if result.Success || !e.isRetryableError(result) {
			break
		}

		// Wait before retry (exponential backoff)
		if attempt < maxRetries {
			delay := e.calculateRetryDelay(attempt)
			select {
			case <-ctx.Done():
				return result
			case <-time.After(delay):
			}
		}
	}

	return lastResult
}

// executeSingleRequest executes a single HTTP request
func (e *Engine) executeSingleRequest(ctx context.Context, httpReq *http.Request, req *models.TestRequest, attempt int) *models.TestResult {
	start := time.Now()
	
	// Set timeout for this request if specified
	if req.Timeout > 0 {
		ctx, cancel := context.WithTimeout(ctx, req.Timeout)
		defer cancel()
		httpReq = httpReq.WithContext(ctx)
	}

	// Execute request
	resp, err := e.client.Do(httpReq)
	responseTime := time.Since(start)

	if err != nil {
		return &models.TestResult{
			ID:            generateID(),
			TestRequestID: req.ID,
			EndpointID:    req.EndpointID,
			StatusCode:    0,
			ResponseTime:  responseTime,
			Error:         err.Error(),
			Success:       false,
			Timestamp:     time.Now(),
			RetryCount:    attempt,
		}
	}
	defer resp.Body.Close()

	// Read response body (with size limit)
	body, err := e.readResponseBody(resp)
	if err != nil {
		return &models.TestResult{
			ID:              generateID(),
			TestRequestID:   req.ID,
			EndpointID:      req.EndpointID,
			StatusCode:      resp.StatusCode,
			ResponseTime:    responseTime,
			ResponseHeaders: convertHeaders(resp.Header),
			Error:           err.Error(),
			Success:         false,
			Timestamp:       time.Now(),
			RetryCount:      attempt,
		}
	}

	// Determine success based on status code
	success := resp.StatusCode >= 200 && resp.StatusCode < 300

	return &models.TestResult{
		ID:              generateID(),
		TestRequestID:   req.ID,
		EndpointID:      req.EndpointID,
		StatusCode:      resp.StatusCode,
		ResponseTime:    responseTime,
		ResponseBody:    body,
		ResponseHeaders: convertHeaders(resp.Header),
		Success:         success,
		Timestamp:       time.Now(),
		RetryCount:      attempt,
	}
}

// readResponseBody reads the response body with size limits
func (e *Engine) readResponseBody(resp *http.Response) ([]byte, error) {
	limit := e.config.MaxResponseSize
	if limit <= 0 {
		limit = 10 * 1024 * 1024 // 10MB default
	}

	reader := io.LimitReader(resp.Body, limit)
	return io.ReadAll(reader)
}

// isRetryableError determines if an error should trigger a retry
func (e *Engine) isRetryableError(result *models.TestResult) bool {
	if result.Success {
		return false
	}

	// Retry on network errors or 5xx status codes
	if result.StatusCode == 0 || (result.StatusCode >= 500 && result.StatusCode < 600) {
		return true
	}

	// Don't retry on 4xx client errors (except 408, 429)
	if result.StatusCode >= 400 && result.StatusCode < 500 {
		return result.StatusCode == 408 || result.StatusCode == 429
	}

	return false
}

// calculateRetryDelay calculates the delay for retry attempts
func (e *Engine) calculateRetryDelay(attempt int) time.Duration {
	baseDelay := e.config.RetryDelay
	if baseDelay <= 0 {
		baseDelay = time.Second
	}

	// Exponential backoff with jitter
	delay := baseDelay * time.Duration(1<<uint(attempt))
	if delay > 30*time.Second {
		delay = 30 * time.Second
	}

	return delay
}

// GetMetrics returns current engine metrics
func (e *Engine) GetMetrics() map[string]int64 {
	e.mu.RLock()
	defer e.mu.RUnlock()
	return e.metrics.GetMetrics()
}

// createErrorResult creates a test result for an error
func (e *Engine) createErrorResult(req *models.TestRequest, err error, retryCount int) *models.TestResult {
	return &models.TestResult{
		ID:            generateID(),
		TestRequestID: req.ID,
		EndpointID:    req.EndpointID,
		StatusCode:    0,
		ResponseTime:  0,
		Error:         err.Error(),
		Success:       false,
		Timestamp:     time.Now(),
		RetryCount:    retryCount,
	}
}

// convertHeaders converts http.Header to map[string]string
func convertHeaders(headers http.Header) map[string]string {
	result := make(map[string]string)
	for key, values := range headers {
		if len(values) > 0 {
			result[key] = values[0]
		}
	}
	return result
}

// generateID generates a unique ID for test results
func generateID() string {
	return fmt.Sprintf("test_%d", time.Now().UnixNano())
}
