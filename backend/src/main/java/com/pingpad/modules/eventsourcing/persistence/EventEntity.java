package com.pingpad.modules.eventsourcing.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an event in the event store.
 */
@Entity
@Table(name = "es_event", indexes = {
    @Index(name = "idx_es_event_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_es_event_aggregate_version", columnList = "aggregate_id, version"),
    @Index(name = "idx_es_event_transaction_id", columnList = "transaction_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "json_data", nullable = false, columnDefinition = "jsonb")
    private String jsonData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
