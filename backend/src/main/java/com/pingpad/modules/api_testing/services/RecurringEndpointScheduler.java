package com.pingpad.modules.api_testing.services;

import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service that automatically tests endpoints with recurring enabled.
 * Runs every 30 seconds and checks if each endpoint should be tested based on its interval.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringEndpointScheduler {
    
    private final ApiEndpointService apiEndpointService;
    private final ApiTestService apiTestService;
    private final ApiEndpointProjectionRepository projectionRepository;

    /**
     * Test all endpoints with recurring enabled based on their intervals.
     * Runs every 30 seconds (30,000 milliseconds).
     */
    @Scheduled(fixedRate = 30000) // 30 seconds in milliseconds
    public void testRecurringEndpoints() {
        log.debug("Starting scheduled test for recurring endpoints");
        
        try {
            List<ApiEndpointProjection> recurringEndpoints = apiEndpointService.getRecurringEndpoints();
            
            if (recurringEndpoints.isEmpty()) {
                log.debug("No recurring endpoints found");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            int testedCount = 0;
            
            for (ApiEndpointProjection endpoint : recurringEndpoints) {
                try {
                    // Check if endpoint should be tested based on its interval
                    if (shouldTestEndpoint(endpoint, now)) {
                        log.debug("Testing recurring endpoint: {} ({}) with interval: {}", 
                            endpoint.getName(), endpoint.getId(), endpoint.getRecurringInterval());
                        
                        // Use the endpoint's userId for testing
                        apiTestService.testEndpoint(endpoint.getId(), endpoint.getUserId());
                        
                        // Update last_run_at timestamp
                        updateLastRunAt(endpoint.getId(), now);
                        
                        testedCount++;
                        log.debug("Successfully tested recurring endpoint: {}", endpoint.getId());
                    }
                } catch (Exception e) {
                    log.error("Error testing recurring endpoint {} ({}): {}", 
                        endpoint.getId(), endpoint.getName(), e.getMessage(), e);
                    // Continue with other endpoints even if one fails
                }
            }
            
            if (testedCount > 0) {
                log.info("Completed scheduled test for {} recurring endpoint(s)", testedCount);
            }
        } catch (Exception e) {
            log.error("Error in scheduled recurring endpoint test: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if an endpoint should be tested based on its interval and last run time.
     */
    private boolean shouldTestEndpoint(ApiEndpointProjection endpoint, LocalDateTime now) {
        String interval = endpoint.getRecurringInterval();
        if (interval == null || interval.isEmpty()) {
            return false;
        }

        LocalDateTime lastRun = endpoint.getLastRunAt();
        if (lastRun == null) {
            // Never run before, test it
            return true;
        }

        Duration timeSinceLastRun = Duration.between(lastRun, now);
        Duration intervalDuration = parseInterval(interval);
        
        return timeSinceLastRun.compareTo(intervalDuration) >= 0;
    }

    /**
     * Parse interval string to Duration.
     * Supported formats: "30s", "5m", "1h", "24h"
     */
    private Duration parseInterval(String interval) {
        if (interval == null || interval.isEmpty()) {
            return Duration.ofSeconds(30); // Default to 30 seconds
        }

        interval = interval.trim().toLowerCase();
        
        if (interval.endsWith("s")) {
            long seconds = Long.parseLong(interval.substring(0, interval.length() - 1));
            return Duration.ofSeconds(seconds);
        } else if (interval.endsWith("m")) {
            long minutes = Long.parseLong(interval.substring(0, interval.length() - 1));
            return Duration.ofMinutes(minutes);
        } else if (interval.endsWith("h")) {
            long hours = Long.parseLong(interval.substring(0, interval.length() - 1));
            return Duration.ofHours(hours);
        } else {
            log.warn("Unknown interval format: {}, defaulting to 30s", interval);
            return Duration.ofSeconds(30);
        }
    }

    /**
     * Update the last_run_at timestamp for an endpoint.
     */
    @Transactional
    public void updateLastRunAt(UUID endpointId, LocalDateTime lastRunAt) {
        projectionRepository.findById(endpointId).ifPresent(endpoint -> {
            endpoint.setLastRunAt(lastRunAt);
            projectionRepository.save(endpoint);
        });
    }
}
