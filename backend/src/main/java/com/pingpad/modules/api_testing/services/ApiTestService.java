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

    @Value("${api.testing.engine.url:http://api-testing-engine:8081}")
    private String testingEngineUrl;

    /**
     * Create a clean ObjectMapper without type information for Go engine communication.
     * The default ObjectMapper includes @class annotations for event sourcing which Go doesn't understand.
     */
    private ObjectMapper createCleanObjectMapper() {
        return new ObjectMapper();
    }

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
        
        // Set timeout for the actual HTTP test (30 seconds should be enough for most APIs)
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
            
            // Use a clean ObjectMapper without type information for Go engine communication
            ObjectMapper cleanMapper = createCleanObjectMapper();
            
            // Serialize request to JSON string (without @class annotations)
            String requestJson = cleanMapper.writeValueAsString(testRequest);
            log.info("Sending test request to Go engine for endpoint {} ({} {})", 
                endpointId, endpoint.getMethod(), endpoint.getUrl());
            log.debug("Request JSON: {}", requestJson);
            
            long requestStartTime = System.currentTimeMillis();
            
            // Send as JSON string to avoid RestTemplate adding type information
            HttpHeaders sendHeaders = new HttpHeaders();
            sendHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, sendHeaders);
            
            // Get response as String to handle parsing manually
            ResponseEntity<String> stringResponse = restTemplate.exchange(
                testingEngineUrl + "/api/v1/test/endpoint",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            long requestDuration = System.currentTimeMillis() - requestStartTime;
            log.info("Received response from Go engine for endpoint {} in {}ms", endpointId, requestDuration);

            String responseBody = stringResponse.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("Empty response from testing engine");
            }

            log.info("Received response from Go engine (length: {}): {}", 
                responseBody != null ? responseBody.length() : 0, 
                responseBody != null && responseBody.length() < 500 ? responseBody : responseBody != null ? responseBody.substring(0, 500) + "..." : "null");

            // Parse JSON response manually using clean mapper
            Map<String, Object> result;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = cleanMapper.readValue(responseBody, Map.class);
                result = parsed;
            } catch (Exception e) {
                log.error("Failed to parse JSON response from Go engine. Response: {}", responseBody, e);
                throw new RuntimeException("Failed to parse response from testing engine. Response was: " + 
                    (responseBody != null && responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
            }

            // The Go engine returns response time as time.Duration which is serialized as nanoseconds (long integer)
            Long responseTimeMs = null;
            Object responseTimeObj = result.get("responseTime");
            if (responseTimeObj != null) {
                if (responseTimeObj instanceof Number) {
                    // Go's time.Duration is serialized as nanoseconds, convert to milliseconds
                    long nanos = ((Number) responseTimeObj).longValue();
                    responseTimeMs = nanos / 1_000_000;
                } else if (responseTimeObj instanceof String) {
                    // Fallback: Parse duration string if it's somehow a string (e.g., "123ms", "1.234s")
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
                        } else {
                            // Try to parse as number string
                            responseTimeMs = Long.parseLong(durationStr) / 1_000_000;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse response time: {}", durationStr);
                    }
                }
            }

            // Handle response body - Go returns []byte as base64 string in JSON
            String responseBodyStr = null;
            Object responseBodyObj = result.get("responseBody");
            if (responseBodyObj != null) {
                if (responseBodyObj instanceof String) {
                    // Go serializes []byte as base64 string, but we'll treat it as the actual content
                    // If it's base64, we'd need to decode it, but for now assume it's the actual string content
                    responseBodyStr = (String) responseBodyObj;
                } else if (responseBodyObj instanceof byte[]) {
                    responseBodyStr = new String((byte[]) responseBodyObj, java.nio.charset.StandardCharsets.UTF_8);
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
        } catch (org.springframework.http.converter.HttpMessageNotReadableException e) {
            log.error("Failed to parse response from Go engine for endpoint {}: {}", endpointId, e.getMessage(), e);
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error("Failed to parse response from testing engine: " + e.getMessage())
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            String errorMsg = e.getMessage();
            // Provide more helpful error messages for timeout errors
            if (errorMsg != null && (errorMsg.contains("Read timed out") || errorMsg.contains("timeout"))) {
                errorMsg = "Testing engine request timed out. The external API may be slow or unresponsive. " +
                          "Try again or check if the target URL is accessible.";
            } else {
                errorMsg = "Failed to connect to testing engine: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName());
            }
            log.error("Connection error testing endpoint {}: {}", endpointId, e.getMessage(), e);
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error(errorMsg)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("Error testing endpoint {}: {}", endpointId, e.getMessage(), e);
            // Create error result
            String errorMsg = "Failed to test endpoint";
            if (e.getMessage() != null) {
                errorMsg += ": " + e.getMessage();
            } else {
                errorMsg += ": " + e.getClass().getSimpleName();
            }
            // Check if it's a JSON parsing error
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("Json")) {
                errorMsg += " (Invalid JSON response from testing engine)";
            }
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error(errorMsg)
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
