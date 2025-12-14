package com.pingpad.modules.eventsourcing.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all domain events in the event sourcing system.
 * All domain events must implement this interface.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface Event {
    // Marker interface for domain events
}
