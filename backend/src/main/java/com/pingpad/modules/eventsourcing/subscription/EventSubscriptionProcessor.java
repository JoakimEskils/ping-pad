package com.pingpad.modules.eventsourcing.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.eventsourcing.core.Event;
import com.pingpad.modules.eventsourcing.persistence.EventEntity;
import com.pingpad.modules.eventsourcing.persistence.EventRepository;
import com.pingpad.modules.eventsourcing.persistence.EventSubscriptionEntity;
import com.pingpad.modules.eventsourcing.persistence.EventSubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Polling-based event subscription processor for asynchronous event handling.
 * Processes events from the event store and updates subscriptions.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "event-sourcing.subscriptions.type",
    havingValue = "polling",
    matchIfMissing = false
)
public class EventSubscriptionProcessor {
    private final EventSubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final List<AsyncEventHandler> asyncEventHandlers;

    /**
     * Process new events for all subscriptions.
     * Polls the database every second (configurable).
     */
    @Scheduled(initialDelayString = "${event-sourcing.polling-subscriptions.polling-initial-delay:PT1S}", 
               fixedDelayString = "${event-sourcing.polling-subscriptions.polling-interval:PT1S}")
    @Transactional
    public void processSubscriptions() {
        if (asyncEventHandlers == null || asyncEventHandlers.isEmpty()) {
            return;
        }
        for (AsyncEventHandler handler : asyncEventHandlers) {
            String subscriptionName = handler.getSubscriptionName();
            processSubscription(subscriptionName, handler);
        }
    }

    private void processSubscription(String subscriptionName, AsyncEventHandler handler) {
        try {
            // Lock the subscription row to prevent concurrent processing
            Optional<EventSubscriptionEntity> subscriptionOpt = subscriptionRepository.findByIdForUpdate(subscriptionName);
            
            if (subscriptionOpt.isEmpty()) {
                // Create subscription if it doesn't exist
                EventSubscriptionEntity subscription = EventSubscriptionEntity.builder()
                    .subscriptionName(subscriptionName)
                    .lastTransactionId(null)
                    .lastEventId(null)
                    .build();
                subscriptionRepository.save(subscription);
                return;
            }

            EventSubscriptionEntity subscription = subscriptionOpt.get();
            String lastTransactionId = subscription.getLastTransactionId();
            Long lastEventId = subscription.getLastEventId();

            // Get the current snapshot xmin to ensure we only process committed transactions
            String xmin = jdbcTemplate.queryForObject(
                "SELECT pg_snapshot_xmin(pg_current_snapshot())::text", 
                String.class
            );

            // Query for new events using transaction ID and event ID comparison
            List<EventEntity> newEvents;
            if (lastTransactionId == null || lastEventId == null) {
                // First time processing - get all events for this aggregate type
                newEvents = jdbcTemplate.query(
                    "SELECT e.* FROM es_event e " +
                    "JOIN es_aggregate a ON a.id = e.aggregate_id " +
                    "WHERE a.aggregate_type = ? " +
                    "AND e.transaction_id::xid8 < ?::xid8 " +
                    "ORDER BY e.transaction_id ASC, e.id ASC",
                    (rs, rowNum) -> {
                        EventEntity event = new EventEntity();
                        event.setId(rs.getLong("id"));
                        event.setTransactionId(rs.getString("transaction_id"));
                        event.setAggregateId(java.util.UUID.fromString(rs.getString("aggregate_id")));
                        event.setVersion(rs.getInt("version"));
                        event.setEventType(rs.getString("event_type"));
                        event.setJsonData(rs.getString("json_data"));
                        return event;
                    },
                    handler.getAggregateType(),
                    xmin
                );
            } else {
                // Get events after the last processed one
                // Using row comparison: (transaction_id, id) > (last_transaction_id, last_event_id)
                // This is equivalent to: transaction_id > last_transaction_id OR 
                //                          (transaction_id = last_transaction_id AND id > last_event_id)
                newEvents = jdbcTemplate.query(
                    "SELECT e.* FROM es_event e " +
                    "JOIN es_aggregate a ON a.id = e.aggregate_id " +
                    "WHERE a.aggregate_type = ? " +
                    "AND (e.transaction_id::xid8 > ?::xid8 OR (e.transaction_id::xid8 = ?::xid8 AND e.id > ?)) " +
                    "AND e.transaction_id::xid8 < ?::xid8 " +
                    "ORDER BY e.transaction_id ASC, e.id ASC",
                    (rs, rowNum) -> {
                        EventEntity event = new EventEntity();
                        event.setId(rs.getLong("id"));
                        event.setTransactionId(rs.getString("transaction_id"));
                        event.setAggregateId(java.util.UUID.fromString(rs.getString("aggregate_id")));
                        event.setVersion(rs.getInt("version"));
                        event.setEventType(rs.getString("event_type"));
                        event.setJsonData(rs.getString("json_data"));
                        return event;
                    },
                    handler.getAggregateType(),
                    lastTransactionId,
                    lastTransactionId,
                    lastEventId,
                    xmin
                );
            }

            // Process each event
            for (EventEntity eventEntity : newEvents) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<Event> eventClass = (Class<Event>) Class.forName(eventEntity.getEventType());
                    Event event = objectMapper.readValue(eventEntity.getJsonData(), eventClass);
                    
                    handler.handle(event, eventEntity);
                    
                    // Update last processed event
                    lastTransactionId = eventEntity.getTransactionId();
                    lastEventId = eventEntity.getId();
                } catch (Exception e) {
                    log.error("Error processing event {} for subscription {}", eventEntity.getId(), subscriptionName, e);
                    // Continue processing other events
                }
            }

            // Update subscription with last processed event
            if (!newEvents.isEmpty()) {
                subscriptionRepository.updateLastProcessed(
                    subscriptionName,
                    lastTransactionId,
                    lastEventId
                );
            }
        } catch (Exception e) {
            log.error("Error processing subscription {}", subscriptionName, e);
        }
    }
}
