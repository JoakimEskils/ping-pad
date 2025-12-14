# Event Sourcing Architecture Explanation

## Overview

The event sourcing architecture in PingPad works differently from traditional CRUD operations. Instead of directly updating database records, **every change is stored as an immutable event**, and the current state is reconstructed by replaying events.

## How It Works: Step by Step

### Example: Creating an API Endpoint

When you create an endpoint, here's what happens:

#### 1. **Aggregate Creates Events** (In-Memory)
```java
ApiEndpointAggregate aggregate = new ApiEndpointAggregate();
aggregate.create(name, url, method, headers, body, userId);
```
- The aggregate object (in memory) validates the operation
- It records an `ApiEndpointCreatedEvent` with all the data
- The event is stored in the aggregate's uncommitted events list
- The aggregate applies the event to its own state (for validation)

#### 2. **Events Are Appended to Event Store** (Database)
```java
eventStore.appendEvents(aggregate.getId(), aggregateType, 0, events);
```

This does several things:

**a) Creates/Updates `es_aggregate` entry:**
- This table is **NOT** the document itself - it's just a version tracker
- It stores: `id` (UUID), `aggregate_type` (e.g., "ApiEndpoint"), `version` (integer)
- For a new aggregate, it creates: `(uuid, "ApiEndpoint", 1)`
- For updates, it increments the version: `(uuid, "ApiEndpoint", 2)`, then `3`, etc.
- This enables **optimistic concurrency control** - prevents two people from updating simultaneously

**b) Appends events to `es_event` table:**
- Each event becomes a **new row** in `es_event`
- The table is **append-only** - events are never deleted or modified
- Each row contains:
  - `aggregate_id`: Which aggregate this event belongs to
  - `version`: Event version (1, 2, 3, ...)
  - `event_type`: Class name like "ApiEndpointCreatedEvent"
  - `json_data`: The full event data as JSONB
  - `transaction_id`: PostgreSQL transaction ID for reliable processing
  - `created_at`: Timestamp

So for creating an endpoint, you get:
```
es_aggregate: (endpoint-uuid, "ApiEndpoint", 1)
es_event: (1, endpoint-uuid, 1, "ApiEndpointCreatedEvent", {...json...}, tx-id, timestamp)
```

#### 3. **Read Model (Projection) is Updated**
```java
eventHandler.handle(event);  // Updates api_endpoints table
```

- The event handler listens to events
- It updates the `api_endpoints` table (the read model/projection)
- This is a **denormalized view** optimized for queries
- For create: Inserts a new row
- For update: Updates the existing row
- For delete: Deletes the row

### Example: Updating an Endpoint

When you update an endpoint:

1. **Load the aggregate** by replaying all events:
   ```java
   aggregate = loadAggregate(endpointId);
   // This queries es_event for all events with this aggregate_id
   // Replays them in order (version 1, 2, 3...) to rebuild current state
   ```

2. **Record update event**:
   ```java
   aggregate.update(name, url, method, headers, body);
   // Creates ApiEndpointUpdatedEvent
   ```

3. **Append new event**:
   - `es_aggregate.version` increments: `1 → 2`
   - New row in `es_event`: `(2, endpoint-uuid, 2, "ApiEndpointUpdatedEvent", {...}, tx-id, timestamp)`

4. **Update projection**:
   - `api_endpoints` table row is updated with new values

## Key Concepts

### `es_aggregate` Table
- **Purpose**: Version tracking and optimistic concurrency control
- **NOT** the actual data - just metadata
- One row per aggregate (endpoint, user, etc.)
- Version number increments with each event

### `es_event` Table
- **Purpose**: The actual source of truth - all state changes
- **Append-only**: Events are never modified or deleted
- Multiple rows per aggregate (one per event)
- Contains full event data as JSONB
- Ordered by version number

### `api_endpoints` Table (Projection)
- **Purpose**: Fast read queries
- **Denormalized view** derived from events
- Updated by event handlers
- Can be rebuilt from events if needed
- This is what you query in normal operations

## Why This Architecture?

1. **Complete Audit Trail**: Every change is recorded forever
2. **Time Travel**: Can reconstruct state at any point in time
3. **Event Replay**: Can rebuild projections from scratch
4. **Scalability**: Separate read/write models (CQRS)
5. **Concurrency**: Optimistic locking via version numbers

## Data Flow Summary

```
User Action (Create Endpoint)
    ↓
Aggregate.recordEvent() → Creates event in memory
    ↓
EventStore.appendEvents() → Writes to es_aggregate + es_event
    ↓
Event Handler.handle() → Updates api_endpoints (read model)
    ↓
Query api_endpoints → Fast reads for users
```

## Important Notes

- **Events are immutable**: Once written, never changed
- **Aggregate state is rebuilt**: Loaded by replaying events
- **Projections are derived**: They can be deleted and rebuilt from events
- **Version numbers prevent conflicts**: Two updates at same time will fail (optimistic locking)
