package com.pingpad.modules.eventsourcing.core;

import java.util.List;
import java.util.UUID;

/**
 * Interface for the event store that persists and retrieves events.
 */
public interface EventStore {
    /**
     * Append events to the event stream for an aggregate.
     * 
     * @param aggregateId The ID of the aggregate
     * @param aggregateType The type of the aggregate
     * @param expectedVersion The expected current version of the aggregate (for optimistic concurrency)
     * @param events The events to append
     * @throws ConcurrencyException if the expected version doesn't match the actual version
     */
    void appendEvents(UUID aggregateId, String aggregateType, int expectedVersion, List<Event> events);

    /**
     * Load an aggregate by replaying all events from the event stream.
     * 
     * @param aggregateId The ID of the aggregate
     * @param aggregateType The type of the aggregate
     * @param aggregateFactory Factory function to create a new aggregate instance
     * @return The reconstructed aggregate
     */
    <T extends Aggregate> T loadAggregate(UUID aggregateId, String aggregateType, AggregateFactory<T> aggregateFactory);

    /**
     * Load an aggregate at a specific version by replaying events up to that version.
     * 
     * @param aggregateId The ID of the aggregate
     * @param aggregateType The type of the aggregate
     * @param version The version to load
     * @param aggregateFactory Factory function to create a new aggregate instance
     * @return The reconstructed aggregate at the specified version
     */
    <T extends Aggregate> T loadAggregate(UUID aggregateId, String aggregateType, int version, AggregateFactory<T> aggregateFactory);

    /**
     * Get all events for an aggregate.
     * 
     * @param aggregateId The ID of the aggregate
     * @return List of stored events
     */
    List<StoredEvent> getEvents(UUID aggregateId);

    /**
     * Get events for an aggregate within a version range.
     * 
     * @param aggregateId The ID of the aggregate
     * @param fromVersion Starting version (exclusive)
     * @param toVersion Ending version (inclusive)
     * @return List of stored events
     */
    List<StoredEvent> getEvents(UUID aggregateId, Integer fromVersion, Integer toVersion);

    /**
     * Factory interface for creating aggregate instances.
     */
    @FunctionalInterface
    interface AggregateFactory<T extends Aggregate> {
        T create(UUID id, int version);
    }
}
