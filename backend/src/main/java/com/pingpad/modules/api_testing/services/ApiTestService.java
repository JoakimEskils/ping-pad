package com.pingpad.modules.api_testing.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.repositories.ApiTestResultRepository;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for testing API endpoints using the Go testing engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiTestService {
    private final RestTemplate restTemplate;
    private final ApiEndpointService apiEndpointService;
    private final ApiTestResultRepository testResultRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${api.testing.engine.url:http://api-testing-engine:8081}")
    private String testingEngineUrl;

    /**
     * Test an API endpoint and save the result.
     */
    public ApiTestResult testEndpoint(UUID endpointId, Long userId) {
        // Get endpoint from projection
        ApiEndpointProjection endpoint = apiEndpointService.getEndpoint(endpointId);
        
        // Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Prepare request for Go testing engine
        Map<String, Object> testRequest = new HashMap<>();
        testRequest.put("id", UUID.randomUUID().toString());
        testRequest.put("endpointId", endpointId.toString());
        testRequest.put("method", endpoint.getMethod());
        testRequest.put("url", endpoint.getUrl());
        
        // Parse headers from string
        Map<String, String> headersMap = new HashMap<>();
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            String[] headerLines = endpoint.getHeaders().split("\n");
            for (String line : headerLines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    headersMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        testRequest.put("headers", headersMap);
        
        // For GET/DELETE requests, don't include body. For other methods, include body if present
        String method = endpoint.getMethod().toUpperCase();
        if (endpoint.getBody() != null && !endpoint.getBody().isEmpty() && 
            !"GET".equals(method) && !"DELETE".equals(method)) {
            // Convert body string to byte array - Jackson will serialize it as base64 in JSON
            testRequest.put("body", endpoint.getBody().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        // For GET/DELETE, omit body field entirely (don't send null or empty)
        
        testRequest.put("timeout", "30s");
        testRequest.put("followRedirects", true);
        testRequest.put("maxRetries", 3);
        testRequest.put("userId", userId.toString());
        // Go expects RFC3339 format (ISO-8601) for time.Time
        // Format: 2006-01-02T15:04:05Z07:00
        // Instant.toString() produces: 2007-12-03T10:15:30.00Z (which is valid RFC3339)
        testRequest.put("createdAt", java.time.Instant.now().toString());

        // Call Go testing engine
        ApiTestResult testResult;
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            // Serialize request to JSON string for logging (before sending)
            try {
                String requestJson = objectMapper.writeValueAsString(testRequest);
                log.info("Sending test request to Go engine for endpoint {} ({} {}): {}", 
                    endpointId, endpoint.getMethod(), endpoint.getUrl(), requestJson);
            } catch (Exception e) {
                log.warn("Could not serialize request for logging: {}", e.getMessage());
            }
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(testRequest, httpHeaders);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                testingEngineUrl + "/api/v1/test/endpoint",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            Map<String, Object> result = response.getBody();
            if (result == null) {
                throw new RuntimeException("Empty response from testing engine");
            }

            // The Go engine returns response time as a duration string (e.g., "123ms" or "1.234s")
            // or as nanoseconds. We need to parse it.
            Long responseTimeMs = null;
            Object responseTimeObj = result.get("responseTime");
            if (responseTimeObj != null) {
                if (responseTimeObj instanceof Number) {
                    // Assume nanoseconds, convert to milliseconds
                    responseTimeMs = ((Number) responseTimeObj).longValue() / 1_000_000;
                } else if (responseTimeObj instanceof String) {
                    // Parse duration string (e.g., "123ms", "1.234s")
                    String durationStr = (String) responseTimeObj;
                    try {
                        if (durationStr.endsWith("ms")) {
                            responseTimeMs = Long.parseLong(durationStr.substring(0, durationStr.length() - 2));
                        } else if (durationStr.endsWith("s")) {
                            double seconds = Double.parseDouble(durationStr.substring(0, durationStr.length() - 1));
                            responseTimeMs = (long) (seconds * 1000);
                        } else if (durationStr.endsWith("ns")) {
                            long nanos = Long.parseLong(durationStr.substring(0, durationStr.length() - 2));
                            responseTimeMs = nanos / 1_000_000;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse response time: " + durationStr);
                    }
                }
            }

            // Handle response body - could be byte array or string
            String responseBodyStr = null;
            Object responseBodyObj = result.get("responseBody");
            if (responseBodyObj != null) {
                if (responseBodyObj instanceof byte[]) {
                    responseBodyStr = new String((byte[]) responseBodyObj);
                } else {
                    responseBodyStr = responseBodyObj.toString();
                }
            }

            // Create test result entity
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .statusCode(getInteger(result, "statusCode"))
                .responseTime(responseTimeMs)
                .responseBody(responseBodyStr)
                .responseHeaders(formatHeaders(getMap(result, "responseHeaders")))
                .error(getString(result, "error"))
                .success(getBoolean(result, "success"))
                .timestamp(LocalDateTime.now())
                .build();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("HTTP error testing endpoint {}: Status {} - Response: {}", 
                endpointId, e.getStatusCode(), responseBody, e);
            String errorMsg = String.format("Failed to test endpoint (HTTP %d)", e.getStatusCode().value());
            if (responseBody != null && !responseBody.isEmpty()) {
                errorMsg += ": " + responseBody;
            } else {
                errorMsg += ": " + e.getMessage();
            }
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error(errorMsg)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error testing endpoint {}: {}", endpointId, e.getMessage(), e);
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error("Failed to connect to testing engine: " + e.getMessage())
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("Error testing endpoint {}: {}", endpointId, e.getMessage(), e);
            // Create error result
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error("Failed to test endpoint: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        return testResultRepository.save(testResult);
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, String>) value;
        }
        return null;
    }

    private String formatHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        return sb.toString().trim();
    }
}
