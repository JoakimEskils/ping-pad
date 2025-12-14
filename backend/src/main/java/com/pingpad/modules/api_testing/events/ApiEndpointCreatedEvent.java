package com.pingpad.modules.api_testing.events;

import com.pingpad.modules.eventsourcing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Domain event fired when an API endpoint is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointCreatedEvent implements Event {
    private UUID endpointId;
    private String name;
    private String url;
    private String method;
    private String headers;
    private String body;
    private Long userId;
    private Boolean recurringEnabled;
    private String recurringInterval;
}
