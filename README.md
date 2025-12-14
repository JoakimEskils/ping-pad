# PingPad

A SaaS tool for testing REST API endpoints and logging webhooks — built with Spring Boot (backend) and React + Vite (frontend), using modular monolith architecture.

## Testing Structure

PingPad uses a comprehensive testing strategy with clear separation between **unit tests** and **system tests** to ensure both isolated component testing and end-to-end feature validation.

### Unit Tests

Unit tests focus on testing individual components in isolation with all dependencies mocked. They are fast, deterministic, and verify that each component works correctly in isolation.

**Java Backend** (`backend/src/test/java/com/pingpad/modules/api_testing/unit/`):
- **Controller Tests**: Test REST endpoints with mocked services
- **Service Tests**: Test business logic with mocked repositories and external dependencies
- **gRPC Client Tests**: Test gRPC communication with mocked gRPC stubs

**Go API Testing Engine**:
- **Engine Tests** (`pkg/testing/engine_test.go`): Test HTTP request execution, retry logic, and metrics
- **gRPC Server Tests** (`internal/grpc/server_test.go`): Test gRPC protocol handling and request/response conversion

### System Tests

System tests verify complete flows through multiple services, testing real integrations including databases, gRPC communication, and event sourcing. They validate that entire features work end-to-end.

**Java Backend** (`backend/src/test/java/com/pingpad/modules/api_testing/system/`):
- **Flow Tests**: Test complete CRUD operations (create, read, update, delete) through the full stack
- **Integration Tests**: Test database persistence, event sourcing, and cache interactions
- **gRPC Integration**: Test real gRPC communication between Java backend and Go service

**Go API Testing Engine** (`api-testing-engine/system/`):
- **gRPC Flow Tests**: Test complete gRPC request/response cycles with real HTTP requests
- **End-to-End Tests**: Verify the entire testing engine workflow from gRPC call to HTTP execution

### Running Tests

```bash
# Java Backend - Unit Tests
cd backend && mvn test -Dtest="**/unit/**"

# Java Backend - System Tests
cd backend && mvn test -Dtest="**/system/**"

# Go - All Tests
cd api-testing-engine && go test ./...

# Go - Unit Tests Only
cd api-testing-engine && go test ./pkg/testing/... ./internal/grpc/...

# Go - System Tests Only
cd api-testing-engine && go test ./system/...
```

For more details, see [TEST_STRUCTURE.md](TEST_STRUCTURE.md).

## Table of Contents

- [Local Development Setup](#local-development-setup)
  - [Prerequisites](#prerequisites)
  - [Clone the repository](#clone-the-repository)
  - [Run the project with Docker Compose](#run-the-project-with-docker-compose)
- [Architecture & Data Flow](#architecture--data-flow)
  - [How it works](#how-it-works)
- [gRPC Communication](#grpc-communication)
  - [Why gRPC?](#why-grpc)
  - [Implementation Details](#implementation-details)
  - [Configuration](#configuration)
  - [Benefits Over JSON REST](#benefits-over-json-rest)
  - [Migration Notes](#migration-notes)
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
- [Caching Layer](#caching-layer)
  - [What is Caching?](#what-is-caching)
  - [Chosen Cache Pattern: Hybrid Cache-Aside + Write-Through](#chosen-cache-pattern-hybrid-cache-aside--write-through)
  - [Why This Pattern?](#why-this-pattern)
  - [Implementation Details](#implementation-details-1)
  - [Example Flow](#example-flow)
  - [Configuration](#configuration-1)
  - [Benefits](#benefits)
  - [Trade-offs](#trade-offs)
  - [Future Enhancements](#future-enhancements)
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

## Automatic/Recurring Endpoint Testing

PingPad supports **automatic recurring testing** of API endpoints, allowing you to monitor your APIs continuously without manual intervention. When enabled, endpoints are automatically tested every 30 seconds, and results are stored for real-time analytics.

### How It Works

1. **Enable Recurring Testing**: When creating or editing an endpoint in the frontend, check the "Enable automatic/recurring calls" checkbox. This sets the `recurring_enabled` flag in the database.

2. **Scheduled Task**: A Spring Boot scheduled task (`RecurringEndpointScheduler`) runs every 30 seconds and automatically:
   - Queries the database for all endpoints with `recurring_enabled = true`
   - Executes a test for each endpoint using the same testing infrastructure as manual tests
   - Saves test results to the `api_test_results` table

3. **Real-Time Analytics**: The analytics modal in the frontend:
   - Fetches real test results from the database
   - Automatically polls for updates every 5 seconds when open
   - Displays charts showing response time, request volume, success rate, and status code distribution
   - Supports time range selection (24 hours, 7 days, 30 days)

### Implementation Details

#### Backend Components

- **Database Schema**: The `api_endpoints` table includes a `recurring_enabled` boolean column (default: `false`)
- **Scheduled Service**: `RecurringEndpointScheduler` uses Spring's `@Scheduled` annotation with `fixedRate = 30000` (30 seconds)
- **Analytics Endpoint**: `GET /api/endpoints/{id}/analytics?hours={hours}` or `?days={days}` returns test results filtered by time range
- **Event Sourcing**: The `recurringEnabled` field is part of the event-sourced aggregate, ensuring all changes are tracked

#### Frontend Components

- **Checkbox Control**: Added to the endpoint create/edit dialog
- **Analytics Modal**: Completely rewritten to use real data with automatic polling
- **Visual Indicators**: Endpoints with recurring enabled show an "Auto-enabled" badge in the analytics modal

### Docker Configuration

When running with Docker Compose, the scheduled task runs automatically within the Spring Boot container:

```yaml
backend:
  build:
    context: .
    dockerfile: ./backend/Dockerfile
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  # ... other config
```

The scheduled task is enabled by default when Spring Boot starts. No additional configuration is required - it will automatically:
- Connect to the PostgreSQL database (via Docker networking)
- Query for recurring endpoints
- Execute tests via the Go testing engine (also running in Docker)
- Store results in the shared database

### Usage Example

1. **Create an endpoint** with recurring enabled:
   - Click "Create Endpoint"
   - Fill in endpoint details (URL, method, headers, etc.)
   - Check "Enable automatic/recurring calls"
   - Save the endpoint

2. **View real-time analytics**:
   - Click "View Analytics" on any endpoint
   - The modal opens showing real test results
   - Data automatically updates every 5 seconds
   - Select different time ranges (24h, 7d, 30d) to view historical data

3. **Monitor endpoint health**:
   - Charts show response time trends
   - Success rate indicates endpoint reliability
   - Status code distribution helps identify issues
   - Error count highlights problematic periods

### Benefits

- **Continuous Monitoring**: Endpoints are tested automatically without manual intervention
- **Historical Data**: All test results are stored, enabling trend analysis
- **Real-Time Updates**: Analytics modal refreshes automatically when open
- **Performance Tracking**: Monitor response times and success rates over time
- **Issue Detection**: Quickly identify when endpoints start failing or slowing down

### Configuration

The scheduled task interval can be modified in `RecurringEndpointScheduler.java`:

```java
@Scheduled(fixedRate = 30000) // 30 seconds in milliseconds
public void testRecurringEndpoints() {
    // ...
}
```

To change the interval, modify the `fixedRate` value (in milliseconds). For example:
- 30 seconds: `30000` (default)
- 1 minute: `60000`
- 5 minutes: `300000`
- 15 minutes: `900000`

The analytics polling interval in the frontend can be adjusted in `EndpointDetail.tsx`:

```typescript
useEffect(() => {
  const interval = setInterval(() => {
    fetchAnalytics();
  }, 5000); // Poll every 5 seconds
  return () => clearInterval(interval);
}, [endpoint.id, timeRange]);
```

For more details on the implementation, see:
- Backend scheduler: `backend/src/main/java/com/pingpad/modules/api_testing/services/RecurringEndpointScheduler.java`
- Analytics endpoint: `backend/src/main/java/com/pingpad/modules/api_testing/controllers/ApiEndpointController.java`
- Frontend analytics: `frontend/src/components/EndpointDetail.tsx`

## gRPC Communication

PingPad uses **gRPC** for communication between the Java backend service and the Go API testing engine, replacing the previous JSON-over-HTTP REST API. This provides better performance, type safety, and streaming capabilities.

### Why gRPC?

1. **Performance**: gRPC uses HTTP/2 and Protocol Buffers (protobuf) for efficient binary serialization, resulting in faster communication and lower latency compared to JSON over HTTP/1.1.

2. **Type Safety**: Protocol Buffers provide strong typing and schema validation, reducing runtime errors and ensuring data consistency between services.

3. **Code Generation**: Both Java and Go generate client/server code from the same `.proto` file, ensuring type compatibility and reducing manual serialization code.

4. **Streaming Support**: gRPC supports unary, server streaming, client streaming, and bidirectional streaming, enabling future enhancements like real-time test progress updates.

5. **Language Agnostic**: Protocol Buffers work across multiple languages, making it easy to add new services in different languages if needed.

### Implementation Details

#### Protocol Definition

The gRPC service is defined in `api-testing-engine/proto/testing.proto`:

```protobuf
service ApiTestingService {
  rpc TestEndpoint(TestRequest) returns (TestResult);
  rpc TestBatch(BatchTestRequest) returns (BatchTestResult);
  rpc GetHealth(HealthRequest) returns (HealthResponse);
  rpc GetMetrics(MetricsRequest) returns (MetricsResponse);
}
```

#### Go gRPC Server

The Go service implements the gRPC server in `api-testing-engine/internal/grpc/server.go`:

- **Server**: Runs on port 9090 (configurable)
- **Correlation ID Support**: Extracts correlation IDs from gRPC metadata headers (`x-correlation-id`) for request tracing
- **Error Handling**: Properly converts internal errors to gRPC status codes
- **Concurrent Execution**: Handles multiple concurrent test requests efficiently

The gRPC server runs alongside the HTTP server, allowing both protocols to coexist during migration or for different use cases.

#### Java gRPC Client

The Java backend uses a gRPC client to communicate with the Go service:

- **Channel Configuration**: Managed channel configured in `GrpcClientConfig` with correlation ID interceptor
- **Service Stub**: Blocking stub for synchronous calls (can be upgraded to async stub for better performance)
- **Correlation ID Propagation**: Automatically propagates correlation IDs from MDC to gRPC metadata
- **Error Handling**: Converts gRPC status codes to meaningful error messages

#### Code Generation

**Go**: Code is generated using `protoc` with the Go plugin:
```bash
protoc --go_out=. --go-grpc_out=. proto/testing.proto
```

**Java**: Code is generated automatically during Maven build using the `protobuf-maven-plugin`:
- Proto files are read from `api-testing-engine/proto/`
- Generated Java classes are placed in `target/generated-sources/protobuf/java/`
- Package structure: `testing.*`

### Configuration

gRPC connection settings can be configured in `application.properties`:

```properties
# gRPC client configuration
api.testing.engine.grpc.url=api-testing-engine
api.testing.engine.grpc.port=9090
```

### Benefits Over JSON REST

1. **Reduced Latency**: Binary protocol and HTTP/2 multiplexing reduce network overhead
2. **Smaller Payloads**: Protocol Buffers are more compact than JSON
3. **Type Safety**: Compile-time type checking prevents serialization errors
4. **Better Error Handling**: Structured gRPC status codes vs HTTP status codes
5. **Future-Proof**: Easy to add streaming, compression, and other advanced features

### Migration Notes

- The HTTP REST API is still available for backward compatibility or direct testing
- Both gRPC (port 9090) and HTTP (port 8081) servers run simultaneously
- Correlation IDs work seamlessly across both protocols
- All existing functionality is preserved with improved performance

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
- **Cache Module**: Redis-based caching layer for improved performance and reduced database load
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

## Caching Layer

PingPad implements a **Redis-based caching layer** between services and the database to improve performance and reduce database load. The caching strategy uses a **hybrid pattern** combining **Cache-Aside (Lazy Loading)** for reads and **Write-Through** for writes, optimized for the event-sourced architecture.

### What is Caching?

Caching stores frequently accessed data in fast, in-memory storage (Redis) to avoid expensive database queries. When data is requested, the system first checks the cache. If found (cache hit), it returns immediately. If not found (cache miss), it queries the database and stores the result in cache for future requests.

### Chosen Cache Pattern: Hybrid Cache-Aside + Write-Through

PingPad uses a **hybrid caching pattern** that combines the best aspects of Cache-Aside and Write-Through:

#### **Cache-Aside (Lazy Loading) for Reads**
- **How it works**: Application code explicitly manages cache
  1. Check cache first
  2. If cache miss, load from database
  3. Store result in cache for future reads
- **Used for**: All read operations (`getEndpoint`, `getEndpointsByUser`, `getUserApiKeys`, etc.)

#### **Write-Through for Writes**
- **How it works**: Write to database first, then immediately update cache
  1. Update database
  2. Update cache with new data immediately
  3. Invalidate related cache entries (e.g., user's list cache)
- **Used for**: All write operations (create, update, delete)

### Why This Pattern?

This hybrid pattern was chosen for several reasons specific to PingPad's architecture:

1. **Event Sourcing Compatibility**: 
   - Projections are updated synchronously after events are persisted
   - We have the updated data immediately available, making Write-Through natural
   - No need to wait for async cache updates

2. **Read-Heavy Workload**:
   - API endpoints and API keys are read frequently (listing, viewing details)
   - Cache-Aside ensures popular data stays in cache
   - Reduces database load for common queries

3. **Consistency**:
   - Write-Through ensures cache always has the latest data after writes
   - Subsequent reads immediately benefit from cached data
   - Avoids stale data issues that pure Cache-Aside with invalidation can have

4. **Performance**:
   - Write-Through eliminates cache misses on reads immediately after writes
   - Cache-Aside minimizes database queries for frequently accessed data
   - Default TTL of 1 hour provides automatic expiration for safety

5. **Simplicity**:
   - Application code explicitly controls cache behavior
   - Easy to understand and debug
   - No complex cache synchronization logic needed

### Implementation Details

The caching layer is implemented as a dedicated **Cache Module** (`com.pingpad.modules.cache`), following the modular monolith architecture pattern. This module is self-contained with its own configuration and services.

#### Cache Module Structure
- **`CacheModule`**: Module configuration and component scanning
- **`RedisConfig`**: Redis connection and template configuration
- **`CacheService`**: Unified interface for cache operations

- **Key Naming**: Uses prefixes for organization (`endpoint:`, `endpoint:user:`, `apikey:`, etc.)
- **Serialization**: Uses Jackson JSON serialization for complex objects
- **TTL**: Default 1-hour expiration, configurable per operation
- **Error Handling**: Gracefully degrades if Redis is unavailable (returns empty, logs warning)

#### Cached Entities

Currently cached:
- **API Endpoints**: Individual endpoints and user's endpoint lists
- **API Keys**: Individual keys and user's key lists

#### Cache Invalidation Strategy

1. **On Create/Update**: 
   - Update individual item cache (Write-Through)
   - Invalidate user's list cache (list becomes stale)

2. **On Delete**:
   - Remove individual item from cache
   - Invalidate user's list cache

3. **Automatic Expiration**: 
   - All cache entries expire after 1 hour (configurable)
   - Ensures data freshness even if invalidation fails

### Example Flow

#### Read Operation (Cache-Aside)
```
1. Request: GET /api/endpoints/{id}
2. Service checks cache: "endpoint:{id}"
3. Cache HIT → Return cached data (no DB query)
   OR
   Cache MISS → Query database → Store in cache → Return data
```

#### Write Operation (Write-Through)
```
1. Request: PUT /api/endpoints/{id}
2. Update database projection
3. Update cache: "endpoint:{id}" = new data
4. Invalidate cache: "endpoint:user:{userId}" (list is stale)
5. Return success
```

### Configuration

Redis is configured via `application.properties`:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

In Docker, Redis runs as a separate service and is automatically connected via Docker networking.

### Benefits

- **Reduced Database Load**: Frequently accessed data served from Redis (sub-millisecond latency)
- **Improved Response Times**: Cache hits are 10-100x faster than database queries
- **Better Scalability**: Database can handle more concurrent users
- **Cost Efficiency**: Fewer database queries reduce resource usage
- **Resilience**: Graceful degradation if Redis is temporarily unavailable

### Trade-offs

- **Memory Usage**: Redis requires memory to store cached data
- **Cache Invalidation Complexity**: Must carefully invalidate related cache entries
- **Eventual Consistency**: List caches may be briefly stale after updates (acceptable for this use case)
- **Cache Warming**: First request after cache expiration requires a database query

### Future Enhancements

Potential improvements:
- **Cache Warming**: Pre-populate cache on application startup
- **Adaptive TTL**: Adjust TTL based on access patterns
- **Cache Metrics**: Monitor hit/miss rates to optimize caching strategy
- **Distributed Cache**: Support Redis Cluster for high availability

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
