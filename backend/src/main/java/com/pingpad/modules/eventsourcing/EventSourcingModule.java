package com.pingpad.modules.eventsourcing;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Event Sourcing module configuration.
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.eventsourcing")
public class EventSourcingModule {
}
