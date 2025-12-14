package com.pingpad.modules.eventsourcing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AggregateSnapshotRepository extends JpaRepository<AggregateSnapshotEntity, Long> {
    @Query("SELECT s FROM AggregateSnapshotEntity s " +
           "WHERE s.aggregateId = :aggregateId " +
           "AND (:version IS NULL OR s.version <= :version) " +
           "ORDER BY s.version DESC")
    Optional<AggregateSnapshotEntity> findLatestSnapshot(
        @Param("aggregateId") UUID aggregateId,
        @Param("version") Integer version
    );
}
