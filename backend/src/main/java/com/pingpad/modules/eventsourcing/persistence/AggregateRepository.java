package com.pingpad.modules.eventsourcing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AggregateRepository extends JpaRepository<AggregateEntity, UUID> {
    Optional<AggregateEntity> findById(UUID id);

    @Modifying
    @Query("UPDATE AggregateEntity a SET a.version = :newVersion " +
           "WHERE a.id = :aggregateId AND a.version = :expectedVersion")
    int updateVersion(@Param("aggregateId") UUID aggregateId,
                      @Param("expectedVersion") Integer expectedVersion,
                      @Param("newVersion") Integer newVersion);
}
