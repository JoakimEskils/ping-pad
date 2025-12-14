package api

import (
	"context"
	"log"
	"net/http"
)

const correlationIDHeader = "X-Correlation-ID"
const correlationIDKey = "correlationId"

// correlationIDMiddleware extracts and propagates correlation ID through the request context
func correlationIDMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Extract correlation ID from header
		correlationID := r.Header.Get(correlationIDHeader)
		
		// Add correlation ID to response headers
		if correlationID != "" {
			w.Header().Set(correlationIDHeader, correlationID)
		}
		
		// Add correlation ID to request context for use in handlers
		ctx := context.WithValue(r.Context(), correlationIDKey, correlationID)
		r = r.WithContext(ctx)
		
		// Log with correlation ID if present
		if correlationID != "" {
			log.Printf("[%s] %s %s", correlationID, r.Method, r.URL.Path)
		}
		
		next.ServeHTTP(w, r)
	})
}

// getCorrelationID retrieves the correlation ID from the request context
func getCorrelationID(ctx context.Context) string {
	if id, ok := ctx.Value(correlationIDKey).(string); ok {
		return id
	}
	return ""
}

// logWithCorrelationID logs a message with correlation ID prefix if available
func logWithCorrelationID(ctx context.Context, format string, args ...interface{}) {
	correlationID := getCorrelationID(ctx)
	if correlationID != "" {
		log.Printf("[%s] "+format, append([]interface{}{correlationID}, args...)...)
	} else {
		log.Printf(format, args...)
	}
}
