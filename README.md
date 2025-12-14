# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks — built with Spring Boot (backend) and React + Vite (frontend), using modular monolith architecture.

## Table of Contents

- [Local Development Setup](#local-development-setup)
  - [Prerequisites](#prerequisites)
  - [Clone the repository](#clone-the-repository)
  - [Run the project with Docker Compose](#run-the-project-with-docker-compose)
- [Architecture & Data Flow](#architecture--data-flow)
  - [How it works](#how-it-works)
- [Nginx](#nginx)
- [Modular Monolith](#modular-monolith)
  - [What is a Modular Monolith?](#what-is-a-modular-monolith)
  - [Benefits](#benefits)
  - [Module Structure](#module-structure)
- [Event Sourcing](#event-sourcing)
  - [What is Event Sourcing?](#what-is-event-sourcing)
  - [Implementation Details](#implementation-details)
  - [Database Schema](#database-schema)
  - [Usage Example](#usage-example)
  - [Configuration](#configuration)
- [Correlation ID (Trace ID)](#correlation-id-trace-id)
- [Database Models](#database-models)
  - [Core Application Tables](#core-application-tables)
  - [Event Sourcing Tables](#event-sourcing-tables)
  - [Migration Management](#migration-management)

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

## Nginx

PingPad uses **Nginx** as a reverse proxy and API gateway to provide a unified entry point for all services. Nginx sits in front of the application stack and handles several critical responsibilities:

### Why Nginx?

1. **Single Entry Point**: Nginx provides a single port (80) that routes requests to the appropriate backend service (frontend, Spring Boot backend, or Go testing engine) based on URL patterns. This simplifies client-side configuration and provides a clean API surface.

2. **Rate Limiting**: Nginx implements rate limiting to protect backend services from abuse and ensure fair resource usage:
   - **Backend API** (`/api/`): Limited to 10 requests per second with burst capacity of 20
   - **Go Testing Engine** (`/go-api/`): Limited to 50 requests per second with burst capacity of 100 (higher limit due to testing workload characteristics)

3. **Load Balancing**: Nginx uses the `least_conn` load balancing algorithm for upstream services, distributing requests based on the number of active connections. This helps balance load across multiple service instances if scaled horizontally.

4. **Request Routing**: Nginx intelligently routes requests:
   - `/` → Frontend (React application)
   - `/api/` → Spring Boot backend
   - `/go-api/` → Go API Testing Engine (with URL rewriting to `/api/v1/`)

5. **Header Management**: Nginx forwards important headers like `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto` to backend services, ensuring they have accurate client information for logging, security, and protocol handling.

6. **Health Checks**: Nginx provides a lightweight health check endpoint (`/health`) that can be used by orchestration systems and monitoring tools.

7. **CORS and Security**: While CORS is also handled at the application level, Nginx can provide an additional layer of security headers and CORS configuration if needed.

This architecture pattern is common in microservices and modular monolith deployments, where a reverse proxy provides infrastructure concerns (routing, rate limiting, SSL termination) while allowing application services to focus on business logic.

## Modular Monolith

PingPad is built using a **modular monolith** architecture pattern. This design organizes the application into well-defined modules within a single deployable unit, providing clear domain boundaries while maintaining the simplicity of a monolithic deployment.

### What is a Modular Monolith?

A modular monolith is an architectural pattern that structures an application as a collection of loosely coupled modules within a single codebase and deployment. Unlike a traditional monolith where code is organized by technical layers (controllers, services, repositories), a modular monolith organizes code by business domains or features, with each module encapsulating its own domain logic, data models, and interfaces.

### Benefits

- **Clear Boundaries**: Each module has well-defined responsibilities and interfaces
- **Easier Maintenance**: Changes to one module have minimal impact on others
- **Simplified Deployment**: Single deployable unit reduces operational complexity
- **Future Flexibility**: Modules can be extracted into microservices later if needed
- **Better Organization**: Code is organized by business domain rather than technical layers

### Module Structure

The backend is organized into the following modules:

- **Auth Module**: Handles authentication, authorization, JWT token management, and security configurations
- **User Management Module**: Manages user data, profiles, and user-related operations
- **API Testing Module**: Core functionality for creating, managing, and testing API endpoints
- **Event Sourcing Module**: Provides event sourcing infrastructure for audit trails and state management
- **Shared Module**: Common utilities, configurations, and cross-cutting concerns used across modules

Each module is self-contained with its own controllers, services, models, and repositories, communicating through well-defined interfaces. This structure enables independent development and testing of each module while maintaining the operational simplicity of a single application.

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

## Correlation ID (Trace ID)

PingPad implements **correlation IDs** (also known as trace IDs) to enable end-to-end request tracing across the entire application stack. This is a best practice for distributed systems and modular monoliths, allowing you to trace a single request as it flows through multiple services and components.

### What is a Correlation ID?

A correlation ID is a unique identifier (UUID) that is generated at the entry point of a request and propagated through all service calls, database operations, and log entries. This allows you to:

- **Trace Requests**: Follow a single request from frontend → backend → Go service → database
- **Debug Issues**: Quickly find all logs related to a specific request using the correlation ID
- **Monitor Performance**: Track request latency across service boundaries
- **Audit Trails**: Correlate events across different services for the same user action

### Implementation

The correlation ID implementation spans all layers of the application:

#### Frontend (React)
- Generates a UUID v4 correlation ID on first request
- Stores correlation ID in `sessionStorage` (persists for browser session)
- Automatically includes `X-Correlation-ID` header in all API requests
- Uses the same correlation ID for all requests within a browser session

#### Backend (Spring Boot)
- **CorrelationIdFilter**: Extracts correlation ID from `X-Correlation-ID` or `X-Trace-ID` headers, or generates a new UUID if not present
- **MDC Integration**: Stores correlation ID in Mapped Diagnostic Context (MDC) for automatic inclusion in all log statements
- **Response Headers**: Adds correlation ID to response headers so clients can track their requests
- **RestTemplate Interceptor**: Automatically propagates correlation ID to downstream services (Go testing engine)

#### Go Service
- **Middleware**: Extracts correlation ID from request headers and stores it in request context
- **Logging**: All log statements include correlation ID prefix: `[correlation-id] message`
- **Response Headers**: Echoes correlation ID back in response headers

#### Logging
- All application logs include the correlation ID in the format: `[correlationId] log message`
- Logback configuration includes `%X{correlationId:-}` in the log pattern
- This makes it easy to filter logs by correlation ID: `grep "abc-123-def" application.log`

### Flow Example

```
1. Frontend generates correlation ID: "550e8400-e29b-41d4-a716-446655440000"
2. Frontend sends request with header: X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
3. Spring Boot extracts correlation ID, adds to MDC
4. Spring Boot logs: [550e8400-e29b-41d4-a716-446655440000] Processing request...
5. Spring Boot calls Go service with X-Correlation-ID header
6. Go service logs: [550e8400-e29b-41d4-a716-446655440000] Executing test...
7. All logs for this request share the same correlation ID
```

### Benefits

- **Easier Debugging**: Find all logs for a specific request with a single search
- **Performance Analysis**: Track request latency across service boundaries
- **Error Tracking**: Correlate errors across services to understand failure chains
- **Production Support**: Quickly investigate user-reported issues using correlation IDs from error messages
- **Best Practice**: Follows industry standards for observability in distributed systems

### Usage

Correlation IDs are automatically handled by the framework. No manual code is required in business logic - the correlation ID flows through automatically via filters, interceptors, and MDC.

To view correlation IDs:
- Check response headers: `X-Correlation-ID`
- Search application logs: `grep "correlation-id" application.log`
- Frontend can access correlation ID from response headers if needed

## Database Models

PingPad uses PostgreSQL as its primary database. The database schema consists of the following main tables:

### Core Application Tables

#### `users`
Stores user account information for authentication and authorization. Contains user credentials, profile information, and timestamps for account management. This table is the foundation for user-specific data isolation throughout the application.

#### `api_endpoints`
Read model (projection) for API endpoints that users create and manage. This table is automatically updated by event handlers when endpoint events occur through the event sourcing system. It provides a denormalized, query-optimized view of endpoints for fast reads, while the actual state changes are stored as events in the event store.

#### `api_test_results`
Stores the results of API endpoint tests executed by the Go testing engine. Each test execution records the HTTP response details including status code, response time, response body, headers, and any errors encountered. This historical data enables users to track endpoint performance and debug issues over time.

#### `api_keys`
Stores API keys that users can save and reuse when creating endpoints. This allows users to manage their API credentials centrally and quickly apply them to multiple endpoints without manually entering the keys each time. Keys are stored per user with unique names to prevent duplicates.

### Event Sourcing Tables

#### `es_aggregate`
Tracks aggregate versions for optimistic concurrency control in the event sourcing system. Each aggregate (like an API endpoint) has an entry here with its current version number, which prevents concurrent modification conflicts when appending new events.

#### `es_event`
Append-only event store containing all domain events. This is the source of truth for all state changes in the system. Every action (create, update, delete) is stored as an immutable event with a transaction ID, allowing for complete audit trails, time-travel queries, and event replay. Events are linked to aggregates and stored in JSONB format for flexibility.

#### `es_aggregate_snapshot`
Optional snapshots of aggregate state at specific versions for performance optimization. Instead of replaying all events from the beginning, snapshots allow the system to start from a recent version and only replay events after that point, significantly improving load times for aggregates with long event histories.

#### `es_event_subscription`
Tracks the progress of asynchronous event handlers processing events from the event store. This enables reliable event processing by recording which events have been processed by each subscription, allowing handlers to resume from the last processed event after restarts or failures.

### Migration Management

#### `flyway_schema_history`
Automatically created and managed by Flyway to track database migrations. This table records all migration scripts that have been executed, their checksums, execution times, and success status. It ensures that migrations are applied only once and in the correct order, preventing duplicate executions and maintaining database schema consistency across environments.
