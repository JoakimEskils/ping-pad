package com.pingpad.modules.eventsourcing.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JPA entity representing an aggregate in the event store.
 * Used for optimistic concurrency control.
 */
@Entity
@Table(name = "es_aggregate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 255)
    private String aggregateType;

    @Column(name = "version", nullable = false)
    private Integer version;
}
