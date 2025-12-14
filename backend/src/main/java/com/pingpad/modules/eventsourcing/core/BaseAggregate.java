package com.pingpad.modules.eventsourcing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base implementation of Aggregate that provides common functionality
 * for managing events and versioning.
 */
public abstract class BaseAggregate implements Aggregate {
    protected UUID id;
    protected int version;
    protected final List<Event> uncommittedEvents = new ArrayList<>();

    protected BaseAggregate() {
        this.id = UUID.randomUUID();
        this.version = 0;
    }

    protected BaseAggregate(UUID id, int version) {
        this.id = id;
        this.version = version;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    // Package-private for internal use
    void setVersion(int version) {
        this.version = version;
    }

    /**
     * Set the version after replaying events from the event store.
     * This is called by the EventStore after loading and replaying events.
     * 
     * @param version The final version after replaying events
     */
    public void setVersionAfterReplay(int version) {
        this.version = version;
    }

    /**
     * Record a new event that will be persisted.
     */
    protected void recordEvent(Event event) {
        uncommittedEvents.add(event);
        apply(event);
        version++;
    }

    @Override
    public void apply(Event event) {
        // Subclasses should override this to handle specific events
    }

    @Override
    public List<Event> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    @Override
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
