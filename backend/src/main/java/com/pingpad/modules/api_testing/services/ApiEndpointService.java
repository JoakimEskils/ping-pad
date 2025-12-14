package com.pingpad.modules.api_testing.services;

import com.pingpad.modules.api_testing.aggregates.ApiEndpointAggregate;
import com.pingpad.modules.api_testing.events.ApiEndpointCreatedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointDeletedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointUpdatedEvent;
import com.pingpad.modules.api_testing.handlers.ApiEndpointEventHandler;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import com.pingpad.modules.eventsourcing.core.EventStore;
import com.pingpad.modules.eventsourcing.core.Event;
import com.pingpad.modules.cache.services.CacheService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing API endpoints using event sourcing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiEndpointService {
    private final EventStore eventStore;
    private final ApiEndpointProjectionRepository projectionRepository;
    private final ApiEndpointEventHandler eventHandler;
    private final CacheService cacheService;

    private static final String CACHE_KEY_PREFIX = "endpoint:";
    private static final String CACHE_KEY_USER_PREFIX = "endpoint:user:";

    /**
     * Create a new API endpoint.
     */
    @Transactional
    public UUID createEndpoint(String name, String url, String method, String headers, String body, Long userId) {
        ApiEndpointAggregate aggregate = new ApiEndpointAggregate();
        aggregate.create(name, url, method, headers, body, userId);

        List<Event> events = aggregate.getUncommittedEvents();
        eventStore.appendEvents(aggregate.getId(), ApiEndpointAggregate.AGGREGATE_TYPE, 0, events);
        aggregate.markEventsAsCommitted();

        // Update projection synchronously
        for (Event event : events) {
            eventHandler.handle(event);
        }

        return aggregate.getId();
    }

    /**
     * Update an existing API endpoint.
     */
    @Transactional
    public void updateEndpoint(UUID endpointId, String name, String url, String method, String headers, String body) {
        ApiEndpointAggregate aggregate = loadAggregate(endpointId);
        aggregate.update(name, url, method, headers, body);

        List<Event> events = aggregate.getUncommittedEvents();
        eventStore.appendEvents(aggregate.getId(), ApiEndpointAggregate.AGGREGATE_TYPE, aggregate.getVersion() - events.size(), events);
        aggregate.markEventsAsCommitted();

        // Update projection synchronously
        for (Event event : events) {
            if (event instanceof ApiEndpointUpdatedEvent) {
                eventHandler.handleUpdate(endpointId, (ApiEndpointUpdatedEvent) event);
            }
        }
    }

    /**
     * Delete an API endpoint.
     */
    @Transactional
    public void deleteEndpoint(UUID endpointId) {
        ApiEndpointAggregate aggregate = loadAggregate(endpointId);
        aggregate.delete();

        List<Event> events = aggregate.getUncommittedEvents();
        eventStore.appendEvents(aggregate.getId(), ApiEndpointAggregate.AGGREGATE_TYPE, aggregate.getVersion() - events.size(), events);
        aggregate.markEventsAsCommitted();

        // Update projection synchronously
        eventHandler.handleDelete(endpointId);
    }

    /**
     * Get an API endpoint by ID (from read model).
     * Uses Cache-Aside pattern: checks cache first, then database.
     */
    public ApiEndpointProjection getEndpoint(UUID endpointId) {
        String cacheKey = CACHE_KEY_PREFIX + endpointId;
        
        // Try cache first
        return cacheService.get(cacheKey, ApiEndpointProjection.class)
            .orElseGet(() -> {
                // Cache miss - load from database
                ApiEndpointProjection endpoint = projectionRepository.findById(endpointId)
                    .orElseThrow(() -> new IllegalArgumentException("API endpoint not found: " + endpointId));
                
                // Populate cache for future reads
                cacheService.put(cacheKey, endpoint);
                return endpoint;
            });
    }

    /**
     * Get all API endpoints for a user (from read model).
     * Uses Cache-Aside pattern: checks cache first, then database.
     */
    public List<ApiEndpointProjection> getEndpointsByUser(Long userId) {
        String cacheKey = CACHE_KEY_USER_PREFIX + userId;
        
        // Try cache first
        return cacheService.getList(cacheKey, ApiEndpointProjection.class)
            .orElseGet(() -> {
                // Cache miss - load from database
                List<ApiEndpointProjection> endpoints = projectionRepository.findByUserId(userId);
                
                // Populate cache for future reads
                cacheService.putList(cacheKey, endpoints);
                return endpoints;
            });
    }

    /**
     * Get the full event history for an API endpoint.
     */
    public List<com.pingpad.modules.eventsourcing.core.StoredEvent> getEventHistory(UUID endpointId) {
        return eventStore.getEvents(endpointId);
    }

    /**
     * Load an aggregate from the event store.
     */
    private ApiEndpointAggregate loadAggregate(UUID endpointId) {
        return eventStore.loadAggregate(
            endpointId,
            ApiEndpointAggregate.AGGREGATE_TYPE,
            (id, version) -> new ApiEndpointAggregate(id, version)
        );
    }
}
