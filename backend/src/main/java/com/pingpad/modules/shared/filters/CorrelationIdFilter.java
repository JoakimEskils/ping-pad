package com.pingpad.modules.shared.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle correlation IDs (trace IDs) for request tracing across the modular monolith.
 * 
 * This filter:
 * - Extracts correlation ID from X-Correlation-ID or X-Trace-ID header
 * - Generates a new UUID if not present
 * - Adds it to MDC (Mapped Diagnostic Context) for logging
 * - Adds it to response headers
 * - Cleans up MDC after request processing
 * 
 * Note: This filter is registered via FilterConfig to ensure it runs with highest priority.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(request);
            
            // Add to MDC for logging
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
            
            // Add to response headers
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Extracts correlation ID from request headers or generates a new one.
     * Checks both X-Correlation-ID and X-Trace-ID headers for compatibility.
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // First check X-Correlation-ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        // Fallback to X-Trace-ID if X-Correlation-ID is not present
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader(TRACE_ID_HEADER);
        }
        
        // Generate new UUID if no correlation ID is present
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        return correlationId.trim();
    }
}
