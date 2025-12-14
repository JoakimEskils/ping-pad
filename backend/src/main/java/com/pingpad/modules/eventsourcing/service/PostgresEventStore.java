package com.pingpad.modules.eventsourcing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.eventsourcing.core.*;
import com.pingpad.modules.eventsourcing.persistence.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostgreSQL-based implementation of the EventStore interface.
 * Uses PostgreSQL's transaction ID for reliable event processing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresEventStore implements EventStore {
    private final EventRepository eventRepository;
    private final AggregateRepository aggregateRepository;
    private final AggregateSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void appendEvents(UUID aggregateId, String aggregateType, int expectedVersion, List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        // Check and update version for optimistic concurrency control
        int newVersion = expectedVersion + events.size();
        int updated = aggregateRepository.updateVersion(aggregateId, expectedVersion, newVersion);
        
        if (updated == 0) {
            // Aggregate doesn't exist, create it
            AggregateEntity aggregate = AggregateEntity.builder()
                .id(aggregateId)
                .aggregateType(aggregateType)
                .version(newVersion)
                .build();
            aggregateRepository.save(aggregate);
        } else if (updated != 1) {
            throw new ConcurrencyException(
                String.format("Concurrency conflict for aggregate %s. Expected version %d but actual version differs.",
                    aggregateId, expectedVersion)
            );
        }

        // Get current transaction ID
        String transactionId = jdbcTemplate.queryForObject("SELECT pg_current_xact_id()::text", String.class);

        // Persist events using native SQL to properly handle JSONB casting
        int version = expectedVersion;
        for (Event event : events) {
            version++;
            try {
                String jsonData = objectMapper.writeValueAsString(event);
                String eventType = event.getClass().getName();

                // Use native SQL to properly cast String to JSONB
                // Note: transaction_id is stored as TEXT, so we don't need xid8 casting here
                String sql = "INSERT INTO es_event (transaction_id, aggregate_id, version, event_type, json_data, created_at) " +
                           "VALUES (?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP) " +
                           "RETURNING id";
                
                Long eventId = jdbcTemplate.queryForObject(
                    sql,
                    Long.class,
                    transactionId,
                    aggregateId,
                    version,
                    eventType,
                    jsonData
                );
                
                log.debug("Persisted event {} for aggregate {} at version {}", eventId, aggregateId, version);
            } catch (Exception e) {
                log.error("Failed to persist event for aggregate {}: {}", aggregateId, e.getMessage(), e);
                throw new RuntimeException("Failed to persist event: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public <T extends Aggregate> T loadAggregate(UUID aggregateId, String aggregateType, AggregateFactory<T> aggregateFactory) {
        return loadAggregate(aggregateId, aggregateType, -1, aggregateFactory);
    }

    @Override
    public <T extends Aggregate> T loadAggregate(UUID aggregateId, String aggregateType, int version, AggregateFactory<T> aggregateFactory) {
        // Try to load from snapshot first
        int fromVersion = 0;
        boolean loadAllVersions = (version < 0);

        Optional<AggregateSnapshotEntity> snapshotOpt = loadAllVersions
            ? snapshotRepository.findLatestSnapshot(aggregateId, null)
            : snapshotRepository.findLatestSnapshot(aggregateId, version);

        if (snapshotOpt.isPresent()) {
            AggregateSnapshotEntity snapshot = snapshotOpt.get();
            fromVersion = snapshot.getVersion();
            // For now, we'll replay from snapshot version
            // In a full implementation, we'd deserialize the snapshot state
        }

        // Load events from the event stream
        List<EventEntity> eventEntities;
        if (loadAllVersions) {
            eventEntities = eventRepository.findByAggregateIdAndVersionRange(aggregateId, fromVersion, null);
        } else {
            eventEntities = eventRepository.findByAggregateIdAndVersionRange(aggregateId, fromVersion, version);
        }

        // Create aggregate instance
        T aggregate = aggregateFactory.create(aggregateId, fromVersion);

        // Replay events
        for (EventEntity eventEntity : eventEntities) {
            try {
                @SuppressWarnings("unchecked")
                Class<Event> eventClass = (Class<Event>) Class.forName(eventEntity.getEventType());
                Event event = objectMapper.readValue(eventEntity.getJsonData(), eventClass);
                aggregate.apply(event);
            } catch (Exception e) {
                log.error("Failed to deserialize event", e);
                throw new RuntimeException("Failed to load aggregate", e);
            }
        }

        // Set the final version after replaying events
        if (aggregate instanceof BaseAggregate) {
            int finalVersion = loadAllVersions 
                ? (fromVersion + eventEntities.size())
                : version;
            ((BaseAggregate) aggregate).setVersionAfterReplay(finalVersion);
        }

        return aggregate;
    }

    @Override
    public List<StoredEvent> getEvents(UUID aggregateId) {
        return getEvents(aggregateId, null, null);
    }

    @Override
    public List<StoredEvent> getEvents(UUID aggregateId, Integer fromVersion, Integer toVersion) {
        List<EventEntity> eventEntities = eventRepository.findByAggregateIdAndVersionRange(
            aggregateId, fromVersion, toVersion
        );

        return eventEntities.stream()
            .map(entity -> StoredEvent.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .aggregateId(entity.getAggregateId())
                .aggregateType(null) // Would need to join with aggregate table
                .version(entity.getVersion())
                .eventType(entity.getEventType())
                .jsonData(entity.getJsonData())
                .createdAt(entity.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }
}
