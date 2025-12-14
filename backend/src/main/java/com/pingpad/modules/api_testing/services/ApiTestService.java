package com.pingpad.modules.api_testing.services;

import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.repositories.ApiTestResultRepository;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import testing.ApiTestingServiceGrpc;
import testing.Testing;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for testing API endpoints using the Go testing engine via gRPC.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiTestService {
    private final ManagedChannel grpcChannel;
    private final ApiEndpointService apiEndpointService;
    private final ApiTestResultRepository testResultRepository;
    private final UserRepository userRepository;

    /**
     * Test an API endpoint and save the result using gRPC.
     */
    public ApiTestResult testEndpoint(UUID endpointId, Long userId) {
        // Get endpoint from projection
        ApiEndpointProjection endpoint = apiEndpointService.getEndpoint(endpointId);
        
        // Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

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

        // Prepare gRPC request
        Testing.TestRequest.Builder requestBuilder = Testing.TestRequest.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEndpointId(endpointId.toString())
            .setMethod(endpoint.getMethod())
            .setUrl(endpoint.getUrl())
            .putAllHeaders(headersMap)
            .setTimeout("30s")
            .setFollowRedirects(true)
            .setMaxRetries(3)
            .setUserId(userId.toString())
            .setCreatedAt(Instant.now().toString());

        // For GET/DELETE requests, don't include body. For other methods, include body if present
        String method = endpoint.getMethod().toUpperCase();
        if (endpoint.getBody() != null && !endpoint.getBody().isEmpty() && 
            !"GET".equals(method) && !"DELETE".equals(method)) {
            requestBuilder.setBody(ByteString.copyFrom(
                endpoint.getBody().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }

        Testing.TestRequest testRequest = requestBuilder.build();

        // Call Go testing engine via gRPC
        ApiTestResult testResult;
        try {
            log.info("Sending gRPC test request to Go engine for endpoint {} ({} {})", 
                endpointId, endpoint.getMethod(), endpoint.getUrl());
            
            long requestStartTime = System.currentTimeMillis();
            
            // Create gRPC stub
            ApiTestingServiceGrpc.ApiTestingServiceBlockingStub stub = 
                ApiTestingServiceGrpc.newBlockingStub(grpcChannel);
            
            // Call gRPC service
            Testing.TestResult grpcResult = stub.testEndpoint(testRequest);
            
            long requestDuration = System.currentTimeMillis() - requestStartTime;
            log.info("Received gRPC response from Go engine for endpoint {} in {}ms", endpointId, requestDuration);

            // Convert response time from nanoseconds to milliseconds
            Long responseTimeMs = null;
            if (grpcResult.getResponseTimeNanos() > 0) {
                responseTimeMs = grpcResult.getResponseTimeNanos() / 1_000_000;
            }

            // Convert response body from bytes to string
            String responseBodyStr = null;
            if (grpcResult.getResponseBody() != null && grpcResult.getResponseBody().size() > 0) {
                responseBodyStr = new String(grpcResult.getResponseBody().toByteArray(), 
                    java.nio.charset.StandardCharsets.UTF_8);
            }

            // Create test result entity
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .statusCode(grpcResult.getStatusCode())
                .responseTime(responseTimeMs)
                .responseBody(responseBodyStr)
                .responseHeaders(formatHeaders(grpcResult.getResponseHeadersMap()))
                .error(grpcResult.getError().isEmpty() ? null : grpcResult.getError())
                .success(grpcResult.getSuccess())
                .timestamp(LocalDateTime.now())
                .build();

        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            log.error("gRPC error testing endpoint {}: Status {} - {}", 
                endpointId, status.getCode(), status.getDescription(), e);
            
            String errorMsg = String.format("Failed to test endpoint (gRPC %s)", status.getCode());
            if (status.getDescription() != null && !status.getDescription().isEmpty()) {
                errorMsg += ": " + status.getDescription();
            } else {
                errorMsg += ": " + e.getMessage();
            }
            
            // Provide more helpful error messages for specific status codes
            if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || 
                status.getCode() == Status.Code.UNAVAILABLE) {
                errorMsg = "Testing engine request timed out or service unavailable. " +
                          "The external API may be slow or unresponsive. " +
                          "Try again or check if the target URL is accessible.";
            }
            
            testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .user(user)
                .error(errorMsg)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("Error testing endpoint {}: {}", endpointId, e.getMessage(), e);
            String errorMsg = "Failed to test endpoint";
            if (e.getMessage() != null) {
                errorMsg += ": " + e.getMessage();
            } else {
                errorMsg += ": " + e.getClass().getSimpleName();
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


    /**
     * Get test results for an endpoint within a time range.
     */
    public java.util.List<ApiTestResult> getTestResults(UUID endpointId, LocalDateTime startTime, LocalDateTime endTime) {
        return testResultRepository.findByEndpointIdAndTimestampBetweenOrderByTimestampDesc(
            endpointId, startTime, endTime
        );
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
