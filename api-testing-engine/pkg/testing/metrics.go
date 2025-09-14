package testing

import (
	"sync"
	"time"
	"pingpad-api-testing-engine/internal/models"
)

// Metrics tracks testing engine performance metrics
type Metrics struct {
	mu                sync.RWMutex
	totalTests        int64
	successfulTests   int64
	failedTests       int64
	totalResponseTime time.Duration
	minResponseTime   time.Duration
	maxResponseTime   time.Duration
	startTime         time.Time
}

// NewMetrics creates a new metrics tracker
func NewMetrics() *Metrics {
	return &Metrics{
		startTime: time.Now(),
	}
}

// RecordTest records a test result
func (m *Metrics) RecordTest(result *models.TestResult) {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.totalTests++
	
	if result.Success {
		m.successfulTests++
	} else {
		m.failedTests++
	}

	// Update response time statistics
	if m.totalTests == 1 {
		m.minResponseTime = result.ResponseTime
		m.maxResponseTime = result.ResponseTime
	} else {
		if result.ResponseTime < m.minResponseTime {
			m.minResponseTime = result.ResponseTime
		}
		if result.ResponseTime > m.maxResponseTime {
			m.maxResponseTime = result.ResponseTime
		}
	}

	m.totalResponseTime += result.ResponseTime
}

// GetMetrics returns current metrics
func (m *Metrics) GetMetrics() map[string]int64 {
	m.mu.RLock()
	defer m.mu.RUnlock()

	avgResponseTime := int64(0)
	if m.totalTests > 0 {
		avgResponseTime = int64(m.totalResponseTime / time.Duration(m.totalTests))
	}

	return map[string]int64{
		"total_tests":         m.totalTests,
		"successful_tests":    m.successfulTests,
		"failed_tests":        m.failedTests,
		"success_rate":        m.calculateSuccessRate(),
		"avg_response_time":   avgResponseTime,
		"min_response_time":   int64(m.minResponseTime),
		"max_response_time":   int64(m.maxResponseTime),
		"uptime_seconds":      int64(time.Since(m.startTime).Seconds()),
	}
}

// calculateSuccessRate calculates the success rate percentage
func (m *Metrics) calculateSuccessRate() int64 {
	if m.totalTests == 0 {
		return 0
	}
	return (m.successfulTests * 100) / m.totalTests
}

// Reset resets all metrics
func (m *Metrics) Reset() {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.totalTests = 0
	m.successfulTests = 0
	m.failedTests = 0
	m.totalResponseTime = 0
	m.minResponseTime = 0
	m.maxResponseTime = 0
	m.startTime = time.Now()
}
