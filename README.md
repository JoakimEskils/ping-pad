# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks — built with Spring Boot (backend) and React + Vite (frontend), using modular monolith architecture.

## Local Development Setup

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed and running
- Git

### Clone the repository

```bash
git clone https://github.com/yourusername/ping-pad.git
cd ping-pad
```

### Run the project with Docker Compose

run tests:
```bash
cd backend && mvn test
```

Build and run locally:

```bash
docker compose up --build
```

This will build and start two containers:

- **Backend (Spring Boot)** on http://localhost:8080
- **Frontend (React + Vite served by nginx)** on http://localhost:5173
- **API Testing Engine (Go)** on http://localhost:8081

## Architecture & Data Flow

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React         │    │   Spring Boot    │    │   Go Testing    │    │   Target APIs   │
│   Frontend      │    │   Backend        │    │   Engine        │    │   (External)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │ 1. User clicks        │                       │                       │
         │    "Test API"         │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │                       │                       │
         │                       │ 2. Get endpoint       │                       │
         │                       │    from DB            │                       │
         │                       │                       │                       │
         │                       │ 3. Convert to         │                       │
         │                       │    TestRequest        │                       │
         │                       ├──────────────────────►│                       │
         │                       │                       │                       │
         │                       │                       │ 4. Execute HTTP       │
         │                       │                       │    request            │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │ 5. Get response      │
         │                       │                       │◄──────────────────────┤
         │                       │                       │                       │
         │                       │ 6. Return TestResult  │                       │
         │                       │◄──────────────────────┤                       │
         │                       │                       │                       │
         │                       │ 7. Save to DB         │                       │
         │                       │    (PostgreSQL)       │                       │
         │                       │                       │                       │
         │ 8. Return result      │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
```

### How it works

1. **User presses "Test API"** in the React frontend
2. **Spring Boot backend** gets endpoint from db
3. **Spring Boot** converts the endpoint to a test request and sends it to Go Testing Service
4. **Go Testing Engine** executes the actual HTTP request to the target API
5. **Go Testing Engine** returns the test result to backend
6. **Spring Boot** saves the result to db
7. **Spring Boot** returns the result to frontend

The Go Testing Engine provides high-performance HTTP testing with excellent concurrency, while Spring Boot handles business logic and data persistence via Postgres DB.

## Event Sourcing

PingPad uses **PostgreSQL-based event sourcing** following the pattern described in [postgresql-event-sourcing](https://github.com/eugene-khyst/postgresql-event-sourcing). This design pattern provides a complete audit trail, time-travel capabilities, and enables CQRS (Command Query Responsibility Segregation).

### What is Event Sourcing?

Instead of storing only the current state of entities, event sourcing stores all changes as a sequence of immutable events. The current state is reconstructed by replaying events. This provides:

- **Complete Audit Trail**: Every change is recorded as an event
- **Time Travel**: Reconstruct state at any point in time
- **Event Replay**: Rebuild projections from events
- **Scalability**: Separate read and write models (CQRS)

### Implementation Details

The event sourcing infrastructure includes:

- **Event Store**: PostgreSQL-based append-only event store (`es_event` table)
- **Aggregates**: Domain objects that manage state through events (e.g., `ApiEndpointAggregate`)
- **Domain Events**: Immutable events representing state changes (e.g., `ApiEndpointCreatedEvent`)
- **Projections**: Read models for efficient querying (e.g., `ApiEndpointProjection`)
- **Event Handlers**: Synchronous handlers update projections, async handlers process integration events

### Database Schema

The event store uses the following tables:

- `es_aggregate`: Tracks aggregate versions for optimistic concurrency control
- `es_event`: Append-only event store with transaction IDs for reliable processing
- `es_aggregate_snapshot`: Optional snapshots for performance optimization
- `es_event_subscription`: Tracks processed events for asynchronous handlers

### Usage Example

```java
// Create an API endpoint using event sourcing
ApiEndpointService endpointService;
UUID endpointId = endpointService.createEndpoint(
    "My API", 
    "https://api.example.com", 
    "GET", 
    null, 
    null, 
    userId
);

// Query from read model (projection)
ApiEndpointProjection endpoint = endpointService.getEndpoint(endpointId);

// Get full event history
List<StoredEvent> history = endpointService.getEventHistory(endpointId);
```

### Configuration

Event sourcing can be configured in `application.yml`:

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

For more details, see the [Event Sourcing README](backend/src/main/java/com/pingpad/modules/eventsourcing/README.md).
