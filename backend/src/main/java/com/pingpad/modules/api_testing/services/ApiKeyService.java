package com.pingpad.modules.api_testing.services;

import com.pingpad.modules.api_testing.models.ApiKey;
import com.pingpad.modules.api_testing.repositories.ApiKeyRepository;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import com.pingpad.modules.cache.services.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing API keys.
 * Note: In production, API keys should be encrypted before storage.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;

    private static final String CACHE_KEY_PREFIX = "apikey:";
    private static final String CACHE_KEY_USER_PREFIX = "apikey:user:";

    /**
     * Get all API keys for a user.
     * Uses Cache-Aside pattern: checks cache first, then database.
     */
    public List<ApiKey> getUserApiKeys(Long userId) {
        String cacheKey = CACHE_KEY_USER_PREFIX + userId;
        
        // Try cache first
        return cacheService.getList(cacheKey, ApiKey.class)
            .orElseGet(() -> {
                // Cache miss - load from database
                List<ApiKey> apiKeys = apiKeyRepository.findByUserId(userId);
                
                // Populate cache for future reads
                cacheService.putList(cacheKey, apiKeys);
                return apiKeys;
            });
    }

    /**
     * Get a specific API key by ID and user ID.
     * Uses Cache-Aside pattern: checks cache first, then database.
     */
    public Optional<ApiKey> getApiKey(Long userId, Long apiKeyId) {
        String cacheKey = CACHE_KEY_PREFIX + apiKeyId;
        
        // Try cache first
        Optional<ApiKey> cached = cacheService.get(cacheKey, ApiKey.class);
        if (cached.isPresent()) {
            ApiKey key = cached.get();
            // Verify it belongs to the user
            if (key.getUser().getId().equals(userId)) {
                return cached;
            }
            // Cache contains wrong user's key, remove it
            cacheService.delete(cacheKey);
        }
        
        // Cache miss or wrong user - load from database
        Optional<ApiKey> apiKey = apiKeyRepository.findById(apiKeyId)
            .filter(key -> key.getUser().getId().equals(userId));
        
        // Populate cache if found
        apiKey.ifPresent(key -> cacheService.put(cacheKey, key));
        
        return apiKey;
    }

    /**
     * Create a new API key.
     */
    @Transactional
    public ApiKey createApiKey(Long userId, String name, String keyValue) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if name already exists for this user
        if (apiKeyRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("API key with name '" + name + "' already exists");
        }

        // TODO: Encrypt keyValue before storing in production using AES encryption
        // For now, we'll store it as-is (NOT SECURE - for development only)
        // In production, use: keyValue = encryptionService.encrypt(keyValue);
        ApiKey apiKey = ApiKey.builder()
            .user(user)
            .name(name)
            .keyValue(keyValue) // TODO: Encrypt before storing in production
            .build();

        apiKey = apiKeyRepository.save(apiKey);
        
        // Write-Through: Update cache immediately after DB write
        cacheService.put(CACHE_KEY_PREFIX + apiKey.getId(), apiKey);
        
        // Invalidate user's API key list cache since it's now stale
        cacheService.delete(CACHE_KEY_USER_PREFIX + userId);
        
        return apiKey;
    }

    /**
     * Update an existing API key.
     */
    @Transactional
    public ApiKey updateApiKey(Long userId, Long apiKeyId, String name, String keyValue) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("API key does not belong to user");
        }

        // Check if new name conflicts with existing key (excluding current key)
        if (!apiKey.getName().equals(name) && apiKeyRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("API key with name '" + name + "' already exists");
        }

        apiKey.setName(name);
        if (keyValue != null && !keyValue.isEmpty()) {
            // TODO: Encrypt keyValue before storing in production using AES encryption
            // In production, use: keyValue = encryptionService.encrypt(keyValue);
            apiKey.setKeyValue(keyValue);
        }

        apiKey = apiKeyRepository.save(apiKey);
        
        // Write-Through: Update cache immediately after DB write
        cacheService.put(CACHE_KEY_PREFIX + apiKey.getId(), apiKey);
        
        // Invalidate user's API key list cache since it's now stale
        cacheService.delete(CACHE_KEY_USER_PREFIX + userId);
        
        return apiKey;
    }

    /**
     * Delete an API key.
     */
    @Transactional
    public void deleteApiKey(Long userId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("API key does not belong to user");
        }

        apiKeyRepository.delete(apiKey);
        
        // Write-Through for deletes: remove from cache immediately
        cacheService.delete(CACHE_KEY_PREFIX + apiKeyId);
        
        // Invalidate user's API key list cache since it's now stale
        cacheService.delete(CACHE_KEY_USER_PREFIX + userId);
    }
}
