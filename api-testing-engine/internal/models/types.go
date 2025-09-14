package models

import (
	"time"
)

// TestRequest represents a request to test an API endpoint
type TestRequest struct {
	ID              string            `json:"id"`
	EndpointID      string            `json:"endpointId"`
	Method          string            `json:"method"`
	URL             string            `json:"url"`
	Headers         map[string]string `json:"headers"`
	Body            []byte            `json:"body,omitempty"`
	Timeout         time.Duration     `json:"timeout"`
	FollowRedirects bool              `json:"followRedirects"`
	MaxRetries      int               `json:"maxRetries"`
	UserID          string            `json:"userId"`
	CreatedAt       time.Time         `json:"createdAt"`
}

// TestResult represents the result of an API test
type TestResult struct {
	ID              string            `json:"id"`
	TestRequestID   string            `json:"testRequestId"`
	EndpointID      string            `json:"endpointId"`
	StatusCode      int               `json:"statusCode"`
	ResponseTime    time.Duration     `json:"responseTime"`
	ResponseBody    []byte            `json:"responseBody,omitempty"`
	ResponseHeaders map[string]string `json:"responseHeaders"`
	Error           string            `json:"error,omitempty"`
	Success         bool              `json:"success"`
	Timestamp       time.Time         `json:"timestamp"`
	RetryCount      int               `json:"retryCount"`
}

// BatchTestRequest represents a request to test multiple endpoints
type BatchTestRequest struct {
	ID        string        `json:"id"`
	Requests  []TestRequest `json:"requests"`
	UserID    string        `json:"userId"`
	CreatedAt time.Time     `json:"createdAt"`
}

// BatchTestResult represents the result of a batch test
type BatchTestResult struct {
	ID       string       `json:"id"`
	Results  []TestResult `json:"results"`
	Summary  TestSummary  `json:"summary"`
	Duration time.Duration `json:"duration"`
}

// TestSummary provides statistics about test results
type TestSummary struct {
	TotalTests    int           `json:"totalTests"`
	Successful    int           `json:"successful"`
	Failed        int           `json:"failed"`
	AverageTime   time.Duration `json:"averageTime"`
	MinTime       time.Duration `json:"minTime"`
	MaxTime       time.Duration `json:"maxTime"`
	SuccessRate   float64       `json:"successRate"`
	ErrorRate     float64       `json:"errorRate"`
}

// TestStatus represents the current status of a test
type TestStatus struct {
	ID        string    `json:"id"`
	Status    string    `json:"status"` // pending, running, completed, failed
	Progress  int       `json:"progress"` // 0-100
	Message   string    `json:"message,omitempty"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// HealthCheck represents the health status of the testing engine
type HealthCheck struct {
	Status    string            `json:"status"`
	Version   string            `json:"version"`
	Uptime    time.Duration     `json:"uptime"`
	Metrics   map[string]int64  `json:"metrics"`
	Timestamp time.Time         `json:"timestamp"`
}

// Config represents the configuration for the testing engine
type Config struct {
	Server   ServerConfig   `json:"server"`
	Database DatabaseConfig `json:"database"`
	Testing  TestingConfig  `json:"testing"`
	Logging  LoggingConfig  `json:"logging"`
}

type ServerConfig struct {
	Host         string        `json:"host"`
	Port         int           `json:"port"`
	ReadTimeout  time.Duration `json:"readTimeout"`
	WriteTimeout time.Duration `json:"writeTimeout"`
}

type DatabaseConfig struct {
	Host     string `json:"host"`
	Port     int    `json:"port"`
	User     string `json:"user"`
	Password string `json:"password"`
	Name     string `json:"name"`
	SSLMode  string `json:"sslMode"`
}

type TestingConfig struct {
	DefaultTimeout      time.Duration `json:"defaultTimeout"`
	MaxConcurrency      int           `json:"maxConcurrency"`
	RateLimitPerSecond  int           `json:"rateLimitPerSecond"`
	MaxRetries          int           `json:"maxRetries"`
	RetryDelay          time.Duration `json:"retryDelay"`
	MaxResponseSize     int64         `json:"maxResponseSize"`
	FollowRedirects     bool          `json:"followRedirects"`
}

type LoggingConfig struct {
	Level  string `json:"level"`
	Format string `json:"format"`
	Output string `json:"output"`
}
