package com.pingpad.modules.api_testing.events;

import com.pingpad.modules.eventsourcing.core.Event;
import lombok.NoArgsConstructor;

/**
 * Domain event fired when an API endpoint is deleted.
 * This is a marker event with no data fields.
 */
@NoArgsConstructor
public class ApiEndpointDeletedEvent implements Event {
    // Marker event for deletion - no fields needed
}
