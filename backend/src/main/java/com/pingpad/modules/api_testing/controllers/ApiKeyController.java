package com.pingpad.modules.api_testing.controllers;

import com.pingpad.modules.api_testing.models.ApiKey;
import com.pingpad.modules.api_testing.services.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for API key management.
 */
@RestController
@RequestMapping("/api/api-keys")
@Slf4j
@RequiredArgsConstructor
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    /**
     * Get all API keys for the current user.
     */
    @GetMapping
    public ResponseEntity<List<ApiKey>> getUserApiKeys(Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            List<ApiKey> apiKeys = apiKeyService.getUserApiKeys(userId);
            return ResponseEntity.ok(apiKeys);
        } catch (Exception e) {
            log.error("Error fetching API keys", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific API key by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiKey> getApiKey(@PathVariable Long id, Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            return apiKeyService.getApiKey(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new API key.
     */
    @PostMapping
    public ResponseEntity<ApiKey> createApiKey(
            @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            if (request.name == null || request.name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.keyValue == null || request.keyValue.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ApiKey apiKey = apiKeyService.createApiKey(userId, request.name.trim(), request.keyValue);
            return ResponseEntity.status(HttpStatus.CREATED).body(apiKey);
        } catch (IllegalArgumentException e) {
            log.error("Error creating API key", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing API key.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiKey> updateApiKey(
            @PathVariable Long id,
            @RequestBody UpdateApiKeyRequest request,
            Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            if (request.name == null || request.name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ApiKey apiKey = apiKeyService.updateApiKey(
                userId, 
                id, 
                request.name.trim(), 
                request.keyValue
            );
            return ResponseEntity.ok(apiKey);
        } catch (IllegalArgumentException e) {
            log.error("Error updating API key", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete an API key.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id, Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            apiKeyService.deleteApiKey(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting API key", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request DTOs
    public static class CreateApiKeyRequest {
        public String name;
        public String keyValue;
    }

    public static class UpdateApiKeyRequest {
        public String name;
        public String keyValue;
    }
}
