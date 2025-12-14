# Event Sourcing Implementation

This module implements PostgreSQL-based event sourcing following the pattern described in [postgresql-event-sourcing](https://github.com/eugene-khyst/postgresql-event-sourcing).

## Overview

Event sourcing stores all changes to an application state as a sequence of events. Instead of updating records, we append events to an immutable event stream. The current state can be reconstructed by replaying all events.

## Architecture

### Core Components

1. **Event Store** (`PostgresEventStore`): Persists and retrieves events from PostgreSQL
2. **Aggregates**: Domain objects that manage their state through events
3. **Domain Events**: Immutable events representing state changes
4. **Projections**: Read models (denormalized views) for efficient querying
5. **Event Handlers**: Process events synchronously (projections) or asynchronously (integration events)

### Database Schema

- `es_aggregate`: Tracks aggregate versions for optimistic concurrency control
- `es_event`: Append-only event store
- `es_aggregate_snapshot`: Optional snapshots for performance optimization
- `es_event_subscription`: Tracks processed events for async handlers

## Usage Example

### 1. Define Domain Events

```java
public class ApiEndpointCreatedEvent implements Event {
    private UUID endpointId;
    private String name;
    private String url;
    // ... other fields
}
```

### 2. Create an Aggregate

```java
public class ApiEndpointAggregate extends BaseAggregate {
    private String name;
    private String url;
    
    public void create(String name, String url) {
        recordEvent(new ApiEndpointCreatedEvent(getId(), name, url));
    }
    
    @Override
    public void apply(Event event) {
        if (event instanceof ApiEndpointCreatedEvent) {
            this.name = ((ApiEndpointCreatedEvent) event).getName();
            this.url = ((ApiEndpointCreatedEvent) event).getUrl();
        }
    }
}
```

### 3. Use the Service

```java
@Autowired
private ApiEndpointService endpointService;

// Create endpoint
UUID endpointId = endpointService.createEndpoint("My API", "https://api.example.com", "GET", null, null, userId);

// Update endpoint
endpointService.updateEndpoint(endpointId, "Updated API", "https://api.example.com/v2", "GET", null, null);

// Query from read model
ApiEndpointProjection endpoint = endpointService.getEndpoint(endpointId);
```

## Configuration

In `application.yml`:

```yaml
event-sourcing:
  snapshotting:
    ApiEndpoint:
      enabled: false
      nth-event: 10
  subscriptions:
    type: polling  # or postgres-channel
  polling-subscriptions:
    polling-initial-delay: PT1S
    polling-interval: PT1S
```

## Features

- ✅ Optimistic concurrency control
- ✅ Event replay and state reconstruction
- ✅ Synchronous projections (read models)
- ✅ Asynchronous event processing (transactional outbox)
- ✅ PostgreSQL transaction ID for reliable event processing
- ✅ Snapshotting support (optional)

## Benefits

1. **Complete Audit Trail**: Every change is recorded as an event
2. **Time Travel**: Reconstruct state at any point in time
3. **Event Replay**: Rebuild projections from events
4. **Scalability**: Separate read and write models (CQRS)

## Migration Notes

The `api_endpoints` table has been migrated to use UUID IDs for event sourcing. The old `id` column (Long) is kept for backward compatibility during migration.
