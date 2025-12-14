package com.pingpad.modules.eventsourcing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscriptionEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EventSubscriptionEntity s WHERE s.subscriptionName = :subscriptionName")
    Optional<EventSubscriptionEntity> findByIdForUpdate(@Param("subscriptionName") String subscriptionName);

    @Modifying
    @Query("UPDATE EventSubscriptionEntity s " +
           "SET s.lastTransactionId = :transactionId, s.lastEventId = :eventId " +
           "WHERE s.subscriptionName = :subscriptionName")
    void updateLastProcessed(@Param("subscriptionName") String subscriptionName,
                              @Param("transactionId") String transactionId,
                              @Param("eventId") Long eventId);
}
