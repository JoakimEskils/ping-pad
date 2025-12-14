package com.pingpad.modules.api_testing.handlers;

import com.pingpad.modules.api_testing.events.ApiEndpointCreatedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointDeletedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointUpdatedEvent;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import com.pingpad.modules.eventsourcing.core.Event;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Synchronous event handler that updates the read model (projection)
 * when API endpoint events occur.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiEndpointEventHandler {

    private final ApiEndpointProjectionRepository projectionRepository;

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
            .build();

        projectionRepository.save(projection);
    }

    private void handle(ApiEndpointUpdatedEvent event) {
        // Note: We need the aggregate ID to update the projection
        // This will be handled by the service layer that has access to the aggregate
    }

    public void handleUpdate(java.util.UUID endpointId, ApiEndpointUpdatedEvent event) {
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
            projectionRepository.save(projection);
        });
    }

    private void handle(ApiEndpointDeletedEvent event) {
        // Note: We need the aggregate ID to delete the projection
        // This will be handled by the service layer
    }

    public void handleDelete(java.util.UUID endpointId) {
        log.debug("Handling ApiEndpointDeletedEvent for endpoint: {}", endpointId);
        projectionRepository.deleteById(endpointId);
    }
}
