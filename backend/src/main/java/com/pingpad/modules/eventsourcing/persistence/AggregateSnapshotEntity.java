package com.pingpad.modules.eventsourcing.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JPA entity representing a snapshot of an aggregate state.
 * Used for optimization when aggregates have many events.
 */
@Entity
@Table(name = "es_aggregate_snapshot", indexes = {
    @Index(name = "idx_es_aggregate_snapshot_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_es_aggregate_snapshot_version", columnList = "aggregate_id, version")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "json_data", nullable = false, columnDefinition = "jsonb")
    private String jsonData;
}
