package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

const baseURL = "http://localhost:8081"

type TestRequest struct {
	EndpointID string            `json:"endpointId"`
	Method     string            `json:"method"`
	URL        string            `json:"url"`
	Headers    map[string]string `json:"headers"`
	Timeout    string            `json:"timeout"`
	MaxRetries int               `json:"maxRetries"`
	UserID     string            `json:"userId"`
}

type TestResult struct {
	ID              string            `json:"id"`
	TestRequestID   string            `json:"testRequestId"`
	EndpointID      string            `json:"endpointId"`
	StatusCode      int               `json:"statusCode"`
	ResponseTime    string            `json:"responseTime"`
	ResponseHeaders map[string]string `json:"responseHeaders"`
	Error           string            `json:"error,omitempty"`
	Success         bool              `json:"success"`
	Timestamp       string            `json:"timestamp"`
	RetryCount      int               `json:"retryCount"`
}

func main() {
	fmt.Println("PingPad API Testing Engine - Example Client")
	fmt.Println("==========================================")

	// Test health endpoint
	fmt.Println("\n1. Testing health endpoint...")
	testHealth()

	// Test single endpoint
	fmt.Println("\n2. Testing single endpoint...")
	testSingleEndpoint()

	// Test batch endpoints
	fmt.Println("\n3. Testing batch endpoints...")
	testBatchEndpoints()

	// Test metrics
	fmt.Println("\n4. Testing metrics endpoint...")
	testMetrics()
}

func testHealth() {
	resp, err := http.Get(baseURL + "/api/v1/health")
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		return
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	fmt.Printf("Health Status: %s\n", string(body))
}

func testSingleEndpoint() {
	req := TestRequest{
		EndpointID: "test_endpoint_1",
		Method:     "GET",
		URL:        "https://httpbin.org/get",
		Headers: map[string]string{
			"User-Agent": "PingPad-Test-Client/1.0",
		},
		Timeout:    "30s",
		MaxRetries: 3,
		UserID:     "test_user_123",
	}

	jsonData, _ := json.Marshal(req)
	resp, err := http.Post(baseURL+"/api/v1/test/endpoint", "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		return
	}
	defer resp.Body.Close()

	var result TestResult
	json.NewDecoder(resp.Body).Decode(&result)
	
	fmt.Printf("Test Result:\n")
	fmt.Printf("  Status Code: %d\n", result.StatusCode)
	fmt.Printf("  Success: %t\n", result.Success)
	fmt.Printf("  Response Time: %s\n", result.ResponseTime)
	if result.Error != "" {
		fmt.Printf("  Error: %s\n", result.Error)
	}
}

func testBatchEndpoints() {
	batchReq := map[string]interface{}{
		"id":      "batch_test_1",
		"userId":  "test_user_123",
		"requests": []TestRequest{
			{
				EndpointID: "batch_endpoint_1",
				Method:     "GET",
				URL:        "https://httpbin.org/get",
				UserID:     "test_user_123",
			},
			{
				EndpointID: "batch_endpoint_2",
				Method:     "GET",
				URL:        "https://httpbin.org/status/200",
				UserID:     "test_user_123",
			},
			{
				EndpointID: "batch_endpoint_3",
				Method:     "GET",
				URL:        "https://httpbin.org/status/404",
				UserID:     "test_user_123",
			},
		},
	}

	jsonData, _ := json.Marshal(batchReq)
	resp, err := http.Post(baseURL+"/api/v1/test/batch", "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		return
	}
	defer resp.Body.Close()

	var result map[string]interface{}
	json.NewDecoder(resp.Body).Decode(&result)
	
	fmt.Printf("Batch Test Results:\n")
	if summary, ok := result["summary"].(map[string]interface{}); ok {
		fmt.Printf("  Total Tests: %.0f\n", summary["totalTests"])
		fmt.Printf("  Successful: %.0f\n", summary["successful"])
		fmt.Printf("  Failed: %.0f\n", summary["failed"])
		fmt.Printf("  Success Rate: %.2f%%\n", summary["successRate"])
	}
}

func testMetrics() {
	resp, err := http.Get(baseURL + "/api/v1/metrics")
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		return
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	fmt.Printf("Metrics: %s\n", string(body))
}
