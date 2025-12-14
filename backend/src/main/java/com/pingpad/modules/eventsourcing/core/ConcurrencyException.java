package com.pingpad.modules.eventsourcing.core;

/**
 * Exception thrown when there's a concurrency conflict
 * (expected version doesn't match actual version).
 */
public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String message) {
        super(message);
    }
}
