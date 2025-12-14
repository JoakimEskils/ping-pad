package com.pingpad.modules.api_testing.events;

import com.pingpad.modules.eventsourcing.core.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event fired when an API endpoint is updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointUpdatedEvent implements Event {
    private String name;
    private String url;
    private String method;
    private String headers;
    private String body;
    private Boolean recurringEnabled;
    private String recurringInterval;
}
