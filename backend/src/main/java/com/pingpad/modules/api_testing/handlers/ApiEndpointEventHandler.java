package com.pingpad.modules.api_testing.handlers;

import com.pingpad.modules.api_testing.events.ApiEndpointCreatedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointDeletedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointUpdatedEvent;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import com.pingpad.modules.eventsourcing.core.Event;
import com.pingpad.modules.cache.services.CacheService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Synchronous event handler that updates the read model (projection)
 * when API endpoint events occur.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiEndpointEventHandler {

    private final ApiEndpointProjectionRepository projectionRepository;
    private final CacheService cacheService;

    private static final String CACHE_KEY_PREFIX = "endpoint:";
    private static final String CACHE_KEY_USER_PREFIX = "endpoint:user:";

    @Transactional
    public void handle(Event event) {
        if (event instanceof ApiEndpointCreatedEvent) {
            handle((ApiEndpointCreatedEvent) event);
        } else if (event instanceof ApiEndpointUpdatedEvent) {
            handle((ApiEndpointUpdatedEvent) event);
        } else if (event instanceof ApiEndpointDeletedEvent) {
            handle((ApiEndpointDeletedEvent) event);
        }
    }

    private void handle(ApiEndpointCreatedEvent event) {
        log.debug("Handling ApiEndpointCreatedEvent for endpoint: {}", event.getEndpointId());
        
        ApiEndpointProjection projection = ApiEndpointProjection.builder()
            .id(event.getEndpointId())
            .name(event.getName())
            .url(event.getUrl())
            .method(event.getMethod())
            .headers(event.getHeaders())
            .body(event.getBody())
            .userId(event.getUserId())
            .recurringEnabled(event.getRecurringEnabled() != null ? event.getRecurringEnabled() : false)
            .recurringInterval(event.getRecurringInterval())
            .build();

        projectionRepository.save(projection);
        
        // Write-Through: Update cache immediately after DB write
        // This ensures subsequent reads get the data from cache without a DB hit
        cacheService.put(CACHE_KEY_PREFIX + event.getEndpointId(), projection);
        
        // Invalidate user's endpoint list cache since it's now stale
        cacheService.delete(CACHE_KEY_USER_PREFIX + event.getUserId());
    }

    private void handle(ApiEndpointUpdatedEvent event) {
        // Note: We need the aggregate ID to update the projection
        // This will be handled by the service layer that has access to the aggregate
    }

    public void handleUpdate(UUID endpointId, ApiEndpointUpdatedEvent event) {
        log.debug("Handling ApiEndpointUpdatedEvent for endpoint: {}", endpointId);
        
        projectionRepository.findById(endpointId).ifPresent(projection -> {
            if (event.getName() != null) {
                projection.setName(event.getName());
            }
            if (event.getUrl() != null) {
                projection.setUrl(event.getUrl());
            }
            if (event.getMethod() != null) {
                projection.setMethod(event.getMethod());
            }
            if (event.getHeaders() != null) {
                projection.setHeaders(event.getHeaders());
            }
            if (event.getBody() != null) {
                projection.setBody(event.getBody());
            }
            if (event.getRecurringEnabled() != null) {
                projection.setRecurringEnabled(event.getRecurringEnabled());
            }
            if (event.getRecurringInterval() != null) {
                projection.setRecurringInterval(event.getRecurringInterval());
            }
            projectionRepository.save(projection);
            
            // Write-Through: Update cache immediately after DB write
            // This ensures subsequent reads get the updated data from cache without a DB hit
            cacheService.put(CACHE_KEY_PREFIX + endpointId, projection);
            
            // Invalidate user's endpoint list cache since it's now stale
            cacheService.delete(CACHE_KEY_USER_PREFIX + projection.getUserId());
        });
    }

    private void handle(ApiEndpointDeletedEvent event) {
        // Note: We need the aggregate ID to delete the projection
        // This will be handled by the service layer
    }

    public void handleDelete(UUID endpointId) {
        log.debug("Handling ApiEndpointDeletedEvent for endpoint: {}", endpointId);
        
        // Get projection first to know the userId for cache invalidation
        projectionRepository.findById(endpointId).ifPresent(projection -> {
            Long userId = projection.getUserId();
            projectionRepository.deleteById(endpointId);
            
            // Delete from cache (Write-Through for deletes: remove from cache immediately)
            cacheService.delete(CACHE_KEY_PREFIX + endpointId);
            
            // Invalidate user's endpoint list cache since it's now stale
            cacheService.delete(CACHE_KEY_USER_PREFIX + userId);
        });
    }
}
