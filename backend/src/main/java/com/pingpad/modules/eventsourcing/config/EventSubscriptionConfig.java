package com.pingpad.modules.eventsourcing.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for event subscription processing.
 * Enables scheduling when polling-based subscriptions are used.
 */
@Configuration
@ConditionalOnProperty(
    name = "event-sourcing.subscriptions.type",
    havingValue = "polling",
    matchIfMissing = false
)
@EnableScheduling
public class EventSubscriptionConfig {
    // Scheduling is enabled via @EnableScheduling
}
