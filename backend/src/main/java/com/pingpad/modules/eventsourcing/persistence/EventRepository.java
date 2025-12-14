package com.pingpad.modules.eventsourcing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findByAggregateIdOrderByVersionAsc(UUID aggregateId);

    @Query("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId " +
           "AND (:fromVersion IS NULL OR e.version > :fromVersion) " +
           "AND (:toVersion IS NULL OR e.version <= :toVersion) " +
           "ORDER BY e.version ASC")
    List<EventEntity> findByAggregateIdAndVersionRange(
        @Param("aggregateId") UUID aggregateId,
        @Param("fromVersion") Integer fromVersion,
        @Param("toVersion") Integer toVersion
    );
}
