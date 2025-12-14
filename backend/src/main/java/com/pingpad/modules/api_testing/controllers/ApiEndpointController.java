package com.pingpad.modules.api_testing.controllers;

import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.services.ApiEndpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for API endpoint management.
 */
@RestController
@RequestMapping("/api/endpoints")
@Slf4j
@RequiredArgsConstructor
public class ApiEndpointController {
    private final ApiEndpointService apiEndpointService;
    private final com.pingpad.modules.api_testing.services.ApiTestService apiTestService;

    /**
     * Get all endpoints for the current user.
     */
    @GetMapping
    public ResponseEntity<List<ApiEndpointProjection>> getAllEndpoints(Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            List<ApiEndpointProjection> endpoints = apiEndpointService.getEndpointsByUser(userId);
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            log.error("Error fetching endpoints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a single endpoint by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiEndpointProjection> getEndpoint(@PathVariable String id) {
        try {
            UUID endpointId = UUID.fromString(id);
            ApiEndpointProjection endpoint = apiEndpointService.getEndpoint(endpointId);
            return ResponseEntity.ok(endpoint);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new endpoint.
     */
    @PostMapping
    public ResponseEntity<?> createEndpoint(
            @RequestBody CreateEndpointRequest request,
            Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            // Validate request
            if (request.name == null || request.name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name is required"));
            }
            if (request.url == null || request.url.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL is required"));
            }
            if (request.method == null || request.method.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Method is required"));
            }
            
            // Convert headers map to string
            String headersStr = null;
            if (request.headers != null && !request.headers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                request.headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
                headersStr = sb.toString().trim();
            }
            
            // Set recurringEnabled based on whether interval is provided
            Boolean recurringEnabled = (request.recurringInterval != null && !request.recurringInterval.trim().isEmpty()) 
                ? true 
                : (request.recurringEnabled != null ? request.recurringEnabled : false);
            
            UUID endpointId = apiEndpointService.createEndpoint(
                request.name.trim(),
                request.url.trim(),
                request.method.trim(),
                headersStr,
                request.body,
                userId,
                recurringEnabled,
                request.recurringInterval != null && !request.recurringInterval.trim().isEmpty() ? request.recurringInterval.trim() : null
            );
            
            ApiEndpointProjection endpoint = apiEndpointService.getEndpoint(endpointId);
            return ResponseEntity.status(HttpStatus.CREATED).body(endpoint);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating endpoint: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating endpoint", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    /**
     * Update an existing endpoint.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEndpoint(
            @PathVariable String id,
            @RequestBody UpdateEndpointRequest request,
            Authentication authentication) {
        try {
            UUID endpointId = UUID.fromString(id);
            
            // Validate request
            if (request.name == null || request.name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name is required"));
            }
            if (request.url == null || request.url.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL is required"));
            }
            if (request.method == null || request.method.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Method is required"));
            }
            
            // Convert headers map to string
            String headersStr = null;
            if (request.headers != null && !request.headers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                request.headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
                headersStr = sb.toString().trim();
            }
            
            // Set recurringEnabled based on whether interval is provided
            Boolean recurringEnabled = (request.recurringInterval != null && !request.recurringInterval.trim().isEmpty()) 
                ? true 
                : (request.recurringEnabled != null ? request.recurringEnabled : false);
            
            apiEndpointService.updateEndpoint(
                endpointId,
                request.name.trim(),
                request.url.trim(),
                request.method.trim(),
                headersStr,
                request.body,
                recurringEnabled,
                request.recurringInterval != null && !request.recurringInterval.trim().isEmpty() ? request.recurringInterval.trim() : null
            );
            
            ApiEndpointProjection endpoint = apiEndpointService.getEndpoint(endpointId);
            return ResponseEntity.ok(endpoint);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating endpoint: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating endpoint", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    /**
     * Delete an endpoint.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id) {
        try {
            UUID endpointId = UUID.fromString(id);
            apiEndpointService.deleteEndpoint(endpointId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test an endpoint.
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testEndpoint(
            @PathVariable String id,
            Authentication authentication) {
        try {
            UUID endpointId = UUID.fromString(id);
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            com.pingpad.modules.api_testing.models.ApiTestResult result = 
                apiTestService.testEndpoint(endpointId, userId);
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for testing endpoint: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error testing endpoint: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    /**
     * Get analytics/test results for an endpoint.
     * Supports optional time range query parameters: hours (default 24) or days.
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<?> getEndpointAnalytics(
            @PathVariable String id,
            @RequestParam(required = false) Integer hours,
            @RequestParam(required = false) Integer days,
            Authentication authentication) {
        try {
            UUID endpointId = UUID.fromString(id);
            
            // Calculate time range
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTime;
            
            if (days != null && days > 0) {
                startTime = endTime.minusDays(days);
            } else if (hours != null && hours > 0) {
                startTime = endTime.minusHours(hours);
            } else {
                // Default to 24 hours
                startTime = endTime.minusHours(24);
            }
            
            List<com.pingpad.modules.api_testing.models.ApiTestResult> results = 
                apiTestService.getTestResults(endpointId, startTime, endTime);
            
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for endpoint analytics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching endpoint analytics: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    // Request DTOs
    public static class CreateEndpointRequest {
        public String name;
        public String url;
        public String method;
        public java.util.Map<String, String> headers;
        public String body;
        public Boolean recurringEnabled;
        public String recurringInterval;
    }

    public static class UpdateEndpointRequest {
        public String name;
        public String url;
        public String method;
        public java.util.Map<String, String> headers;
        public String body;
        public Boolean recurringEnabled;
        public String recurringInterval;
    }
}
