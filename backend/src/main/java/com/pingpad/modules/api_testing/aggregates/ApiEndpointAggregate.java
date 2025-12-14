package com.pingpad.modules.api_testing.aggregates;

import com.pingpad.modules.api_testing.events.ApiEndpointCreatedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointDeletedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointUpdatedEvent;
import com.pingpad.modules.eventsourcing.core.BaseAggregate;
import com.pingpad.modules.eventsourcing.core.Event;
import lombok.Getter;

import java.util.UUID;

/**
 * Event-sourced aggregate for API endpoints.
 */
@Getter
public class ApiEndpointAggregate extends BaseAggregate {
    public static final String AGGREGATE_TYPE = "ApiEndpoint";

    private String name;
    private String url;
    private String method;
    private String headers;
    private String body;
    private Long userId;
    private Boolean recurringEnabled;
    private String recurringInterval;
    private boolean deleted;

    // Default constructor for creating new aggregates
    public ApiEndpointAggregate() {
        super();
    }

    // Constructor for loading existing aggregates
    public ApiEndpointAggregate(UUID id, int version) {
        super(id, version);
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    /**
     * Create a new API endpoint.
     */
    public void create(String name, String url, String method, String headers, String body, Long userId, Boolean recurringEnabled, String recurringInterval) {
        if (this.name != null) {
            throw new IllegalStateException("API endpoint already exists");
        }
        recordEvent(new ApiEndpointCreatedEvent(getId(), name, url, method, headers, body, userId, recurringEnabled != null ? recurringEnabled : false, recurringInterval));
    }

    /**
     * Update an existing API endpoint.
     */
    public void update(String name, String url, String method, String headers, String body, Boolean recurringEnabled, String recurringInterval) {
        if (this.name == null) {
            throw new IllegalStateException("API endpoint does not exist");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot update deleted API endpoint");
        }
        recordEvent(new ApiEndpointUpdatedEvent(name, url, method, headers, body, recurringEnabled, recurringInterval));
    }

    /**
     * Delete an API endpoint.
     */
    public void delete() {
        if (this.name == null) {
            throw new IllegalStateException("API endpoint does not exist");
        }
        if (deleted) {
            throw new IllegalStateException("API endpoint already deleted");
        }
        recordEvent(new ApiEndpointDeletedEvent());
    }

    @Override
    public void apply(Event event) {
        if (event instanceof ApiEndpointCreatedEvent) {
            apply((ApiEndpointCreatedEvent) event);
        } else if (event instanceof ApiEndpointUpdatedEvent) {
            apply((ApiEndpointUpdatedEvent) event);
        } else if (event instanceof ApiEndpointDeletedEvent) {
            apply((ApiEndpointDeletedEvent) event);
        }
    }

    private void apply(ApiEndpointCreatedEvent event) {
        this.name = event.getName();
        this.url = event.getUrl();
        this.method = event.getMethod();
        this.headers = event.getHeaders();
        this.body = event.getBody();
        this.userId = event.getUserId();
        this.recurringEnabled = event.getRecurringEnabled() != null ? event.getRecurringEnabled() : false;
        this.recurringInterval = event.getRecurringInterval();
        this.deleted = false;
    }

    private void apply(ApiEndpointUpdatedEvent event) {
        if (event.getName() != null) {
            this.name = event.getName();
        }
        if (event.getUrl() != null) {
            this.url = event.getUrl();
        }
        if (event.getMethod() != null) {
            this.method = event.getMethod();
        }
        if (event.getHeaders() != null) {
            this.headers = event.getHeaders();
        }
        if (event.getBody() != null) {
            this.body = event.getBody();
        }
        if (event.getRecurringEnabled() != null) {
            this.recurringEnabled = event.getRecurringEnabled();
        }
        if (event.getRecurringInterval() != null) {
            this.recurringInterval = event.getRecurringInterval();
        }
    }

    private void apply(ApiEndpointDeletedEvent event) {
        this.deleted = true;
    }
}
