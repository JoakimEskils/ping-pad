package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.ApiTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ApiTestResultRepository extends JpaRepository<ApiTestResult, Long> {
    List<ApiTestResult> findByEndpointIdOrderByTimestampDesc(UUID endpointId);
    
    @Query("SELECT r FROM ApiTestResult r WHERE r.endpointId = :endpointId AND r.timestamp >= :startTime ORDER BY r.timestamp DESC")
    List<ApiTestResult> findByEndpointIdAndTimestampAfterOrderByTimestampDesc(
        @Param("endpointId") UUID endpointId,
        @Param("startTime") LocalDateTime startTime
    );
    
    @Query("SELECT r FROM ApiTestResult r WHERE r.endpointId = :endpointId AND r.timestamp >= :startTime AND r.timestamp <= :endTime ORDER BY r.timestamp DESC")
    List<ApiTestResult> findByEndpointIdAndTimestampBetweenOrderByTimestampDesc(
        @Param("endpointId") UUID endpointId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
