package com.pingpad.modules.eventsourcing.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a subscription for asynchronous event processing.
 * Tracks the last processed event for each subscription.
 */
@Entity
@Table(name = "es_event_subscription")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSubscriptionEntity {
    @Id
    @Column(name = "subscription_name", length = 255)
    private String subscriptionName;

    @Column(name = "last_transaction_id")
    private String lastTransactionId;

    @Column(name = "last_event_id")
    private Long lastEventId;
}
