package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"pingpad-api-testing-engine/internal/models"
	"pingpad-api-testing-engine/pkg/testing"
)

// TestRequestJSON represents the JSON structure for test requests
type TestRequestJSON struct {
	ID              string            `json:"id"`
	EndpointID      string            `json:"endpointId"`
	Method          string            `json:"method"`
	URL             string            `json:"url"`
	Headers         map[string]string `json:"headers"`
	Body            []byte            `json:"body,omitempty"`
	Timeout         string            `json:"timeout"`
	FollowRedirects bool              `json:"followRedirects"`
	MaxRetries      int               `json:"maxRetries"`
	UserID          string            `json:"userId"`
	CreatedAt       time.Time         `json:"createdAt"`
}

// Handler handles HTTP requests for the API testing engine
type Handler struct {
	engine *testing.Engine
	config *models.Config
}

// NewHandler creates a new API handler
func NewHandler(engine *testing.Engine, config *models.Config) *Handler {
	return &Handler{
		engine: engine,
		config: config,
	}
}

// TestEndpoint handles single endpoint testing
func (h *Handler) TestEndpoint(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var reqJSON TestRequestJSON
	if err := json.NewDecoder(r.Body).Decode(&reqJSON); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Parse timeout string
	timeout, err := time.ParseDuration(reqJSON.Timeout)
	if err != nil || timeout == 0 {
		timeout = h.config.Testing.DefaultTimeout
	}

	// Set defaults
	if reqJSON.MaxRetries == 0 {
		reqJSON.MaxRetries = h.config.Testing.MaxRetries
	}
	if reqJSON.ID == "" {
		reqJSON.ID = generateID()
	}
	if reqJSON.CreatedAt.IsZero() {
		reqJSON.CreatedAt = time.Now()
	}

	// Convert to internal request
	req := &models.TestRequest{
		ID:              reqJSON.ID,
		EndpointID:      reqJSON.EndpointID,
		Method:          reqJSON.Method,
		URL:             reqJSON.URL,
		Headers:         reqJSON.Headers,
		Body:            reqJSON.Body,
		Timeout:         timeout,
		FollowRedirects: reqJSON.FollowRedirects,
		MaxRetries:      reqJSON.MaxRetries,
		UserID:          reqJSON.UserID,
		CreatedAt:       reqJSON.CreatedAt,
	}

	// Execute test
	ctx := r.Context()
	result := h.engine.ExecuteTest(ctx, req)

	// Return result
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(result)
}

// TestBatch handles batch endpoint testing
func (h *Handler) TestBatch(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var batchReq models.BatchTestRequest
	if err := json.NewDecoder(r.Body).Decode(&batchReq); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Convert to internal requests with parsed timeouts
	requests := make([]*models.TestRequest, len(batchReq.Requests))
	for i, req := range batchReq.Requests {
		// Set defaults
		if req.MaxRetries == 0 {
			req.MaxRetries = h.config.Testing.MaxRetries
		}
		if req.ID == "" {
			req.ID = generateID()
		}
		if req.CreatedAt.IsZero() {
			req.CreatedAt = time.Now()
		}
		if req.Timeout == 0 {
			req.Timeout = h.config.Testing.DefaultTimeout
		}

		requests[i] = &req
	}

	// Execute batch test
	ctx := r.Context()
	start := time.Now()
	results := h.engine.ExecuteBatchTests(ctx, requests)
	duration := time.Since(start)

	// Calculate summary
	summary := h.calculateSummary(results)

	// Create batch result
	batchResult := models.BatchTestResult{
		ID:       batchReq.ID,
		Results:  convertToResults(results),
		Summary:  summary,
		Duration: duration,
	}

	// Return result
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(batchResult)
}

// GetTestStatus handles test status requests
func (h *Handler) GetTestStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// For now, return a simple status
	// In a real implementation, this would track ongoing tests
	status := models.TestStatus{
		ID:        r.URL.Query().Get("id"),
		Status:    "completed",
		Progress:  100,
		Message:   "Test completed",
		UpdatedAt: time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(status)
}

// GetHealth handles health check requests
func (h *Handler) GetHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	metrics := h.engine.GetMetrics()
	
	health := models.HealthCheck{
		Status:    "healthy",
		Version:   "1.0.0",
		Uptime:    time.Since(time.Now().Add(-24 * time.Hour)), // Placeholder
		Metrics:   metrics,
		Timestamp: time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(health)
}

// GetMetrics handles metrics requests
func (h *Handler) GetMetrics(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	metrics := h.engine.GetMetrics()
	
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(metrics)
}

// calculateSummary calculates test summary statistics
func (h *Handler) calculateSummary(results []*models.TestResult) models.TestSummary {
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
		TotalTests:    total,
		Successful:    successful,
		Failed:        total - successful,
		AverageTime:   avgTime,
		MinTime:       minTime,
		MaxTime:       maxTime,
		SuccessRate:   successRate,
		ErrorRate:     errorRate,
	}
}

// convertToResults converts pointer slice to value slice
func convertToResults(results []*models.TestResult) []models.TestResult {
	converted := make([]models.TestResult, len(results))
	for i, result := range results {
		converted[i] = *result
	}
	return converted
}

// generateID generates a unique ID
func generateID() string {
	return fmt.Sprintf("test_%d", time.Now().UnixNano())
}

// SetupRoutes sets up HTTP routes
func (h *Handler) SetupRoutes() *http.ServeMux {
	mux := http.NewServeMux()
	
	mux.HandleFunc("/api/v1/test/endpoint", h.TestEndpoint)
	mux.HandleFunc("/api/v1/test/batch", h.TestBatch)
	mux.HandleFunc("/api/v1/test/status", h.GetTestStatus)
	mux.HandleFunc("/api/v1/health", h.GetHealth)
	mux.HandleFunc("/api/v1/metrics", h.GetMetrics)
	
	return mux
}
