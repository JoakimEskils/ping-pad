package com.pingpad.modules.api_testing.services;

import com.pingpad.modules.api_testing.models.AcknowledgedAlarm;
import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.repositories.AcknowledgedAlarmRepository;
import com.pingpad.modules.api_testing.repositories.ApiTestResultRepository;
import com.pingpad.modules.api_testing.services.ApiEndpointService;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing alarms (errors from endpoint tests).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmService {
    
    private final ApiTestResultRepository testResultRepository;
    private final AcknowledgedAlarmRepository acknowledgedAlarmRepository;
    private final ApiEndpointService apiEndpointService;
    private final UserRepository userRepository;

    /**
     * Get all unacknowledged alarms (errors) for the current user.
     * Returns test results that have success=false and haven't been acknowledged.
     */
    public List<AlarmInfo> getUnacknowledgedAlarms(Long userId) {
        // Get all endpoints for the user
        List<ApiEndpointProjection> endpoints = apiEndpointService.getEndpointsByUser(userId);
        Set<UUID> endpointIds = endpoints.stream()
            .map(ApiEndpointProjection::getId)
            .collect(Collectors.toSet());

        if (endpointIds.isEmpty()) {
            return List.of();
        }

        // Get all failed test results for user's endpoints (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<ApiTestResult> failedResults = testResultRepository.findByEndpointIdInAndSuccessFalseAndTimestampAfter(
            endpointIds, sevenDaysAgo
        );

        // Get all acknowledged test result IDs for this user
        Set<Long> acknowledgedTestResultIds = acknowledgedAlarmRepository.findByUserId(userId)
            .stream()
            .map(AcknowledgedAlarm::getTestResultId)
            .collect(Collectors.toSet());

        // Filter out acknowledged alarms and map to AlarmInfo
        Map<UUID, ApiEndpointProjection> endpointMap = endpoints.stream()
            .collect(Collectors.toMap(ApiEndpointProjection::getId, e -> e));

        return failedResults.stream()
            .filter(result -> !acknowledgedTestResultIds.contains(result.getId()))
            .map(result -> {
                ApiEndpointProjection endpoint = endpointMap.get(result.getEndpointId());
                return AlarmInfo.builder()
                    .testResultId(result.getId())
                    .endpointId(result.getEndpointId())
                    .endpointName(endpoint != null ? endpoint.getName() : "Unknown")
                    .endpointUrl(endpoint != null ? endpoint.getUrl() : "")
                    .error(result.getError())
                    .statusCode(result.getStatusCode())
                    .timestamp(result.getTimestamp())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Acknowledge an alarm (mark it as addressed).
     */
    @Transactional
    public void acknowledgeAlarm(Long userId, Long testResultId) {
        // Check if already acknowledged
        if (acknowledgedAlarmRepository.existsByUserIdAndTestResultId(userId, testResultId)) {
            log.debug("Alarm {} already acknowledged by user {}", testResultId, userId);
            return;
        }

        // Get the test result to find the endpoint ID
        ApiTestResult testResult = testResultRepository.findById(testResultId)
            .orElseThrow(() -> new IllegalArgumentException("Test result not found: " + testResultId));

        // Get the user entity
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Create acknowledgment
        AcknowledgedAlarm alarm = AcknowledgedAlarm.builder()
            .endpointId(testResult.getEndpointId())
            .testResultId(testResultId)
            .user(user)
            .build();

        acknowledgedAlarmRepository.save(alarm);
        log.info("Alarm {} acknowledged by user {}", testResultId, userId);
    }

    /**
     * Get count of unacknowledged alarms for the user.
     */
    public long getUnacknowledgedAlarmCount(Long userId) {
        return getUnacknowledgedAlarms(userId).size();
    }

    /**
     * DTO for alarm information.
     */
    @lombok.Data
    @lombok.Builder
    public static class AlarmInfo {
        private Long testResultId;
        private UUID endpointId;
        private String endpointName;
        private String endpointUrl;
        private String error;
        private Integer statusCode;
        private LocalDateTime timestamp;
    }
}
