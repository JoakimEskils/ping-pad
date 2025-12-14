package com.pingpad.modules.eventsourcing.core;

import java.util.List;
import java.util.UUID;

/**
 * Base interface for aggregates in the event sourcing system.
 * Aggregates manage their state by applying events.
 */
public interface Aggregate {
    /**
     * Get the unique identifier of the aggregate.
     */
    UUID getId();

    /**
     * Get the current version of the aggregate.
     */
    int getVersion();

    /**
     * Get the type of the aggregate.
     */
    String getAggregateType();

    /**
     * Apply a domain event to the aggregate, updating its state.
     */
    void apply(Event event);

    /**
     * Get all uncommitted events (events that haven't been persisted yet).
     */
    List<Event> getUncommittedEvents();

    /**
     * Mark all events as committed (after they've been persisted).
     */
    void markEventsAsCommitted();
}
