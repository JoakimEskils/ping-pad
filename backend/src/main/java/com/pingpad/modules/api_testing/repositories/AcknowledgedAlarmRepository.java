package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.AcknowledgedAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcknowledgedAlarmRepository extends JpaRepository<AcknowledgedAlarm, Long> {
    List<AcknowledgedAlarm> findByUserId(Long userId);
    boolean existsByUserIdAndTestResultId(Long userId, Long testResultId);
    void deleteByTestResultId(Long testResultId);
    List<AcknowledgedAlarm> findByEndpointId(UUID endpointId);
}
