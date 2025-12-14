package com.pingpad.modules.shared.interceptors;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestTemplate interceptor to propagate correlation ID to downstream services.
 * 
 * This interceptor:
 * - Retrieves correlation ID from MDC
 * - Adds it to outgoing HTTP request headers
 * - Ensures correlation ID flows through service boundaries
 */
@Component
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        
        // Get correlation ID from MDC
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        
        // Add correlation ID to request headers if present
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            request.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return execution.execute(request, body);
    }
}
