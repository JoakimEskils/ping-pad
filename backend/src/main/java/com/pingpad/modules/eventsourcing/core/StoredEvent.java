package com.pingpad.modules.eventsourcing.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an event that has been stored in the event store.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent {
    private Long id;
    private String transactionId;
    private UUID aggregateId;
    private String aggregateType;
    private int version;
    private String eventType;
    private String jsonData;
    private LocalDateTime createdAt;
}
