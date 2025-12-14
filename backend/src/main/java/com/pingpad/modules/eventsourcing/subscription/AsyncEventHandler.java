package com.pingpad.modules.eventsourcing.subscription;

import com.pingpad.modules.eventsourcing.core.Event;
import com.pingpad.modules.eventsourcing.persistence.EventEntity;

/**
 * Interface for asynchronous event handlers.
 * These handlers process events after they've been committed to the event store.
 */
public interface AsyncEventHandler {
    /**
     * Get the name of this subscription.
     */
    String getSubscriptionName();

    /**
     * Get the aggregate type this handler processes events for.
     */
    String getAggregateType();

    /**
     * Handle an event asynchronously.
     * 
     * @param event The domain event
     * @param eventEntity The stored event entity with metadata
     */
    void handle(Event event, EventEntity eventEntity);
}
