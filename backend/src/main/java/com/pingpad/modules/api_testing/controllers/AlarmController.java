package com.pingpad.modules.api_testing.controllers;

import com.pingpad.modules.api_testing.services.AlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for alarm management.
 */
@RestController
@RequestMapping("/api/alarms")
@Slf4j
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;

    /**
     * Get all unacknowledged alarms for the current user.
     */
    @GetMapping
    public ResponseEntity<?> getUnacknowledgedAlarms(Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            List<AlarmService.AlarmInfo> alarms = alarmService.getUnacknowledgedAlarms(userId);
            return ResponseEntity.ok(alarms);
        } catch (Exception e) {
            log.error("Error fetching alarms", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    /**
     * Get count of unacknowledged alarms for the current user.
     */
    @GetMapping("/count")
    public ResponseEntity<?> getUnacknowledgedAlarmCount(Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            long count = alarmService.getUnacknowledgedAlarmCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error fetching alarm count", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }

    /**
     * Acknowledge an alarm.
     */
    @PostMapping("/{testResultId}/acknowledge")
    public ResponseEntity<?> acknowledgeAlarm(
            @PathVariable Long testResultId,
            Authentication authentication) {
        try {
            // TODO: Get userId from authentication
            Long userId = 1L; // Temporary - should get from JWT token
            
            alarmService.acknowledgeAlarm(userId, testResultId);
            return ResponseEntity.ok(Map.of("message", "Alarm acknowledged successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request to acknowledge alarm: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error acknowledging alarm", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage, "details", e.getClass().getSimpleName()));
        }
    }
}
