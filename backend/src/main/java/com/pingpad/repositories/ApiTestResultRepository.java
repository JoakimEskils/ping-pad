package com.pingpad.repositories;

import com.pingpad.models.ApiTestResult;
import com.pingpad.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiTestResultRepository extends JpaRepository<ApiTestResult, Long> {
    List<ApiTestResult> findByUserOrderByTimestampDesc(User user);
    List<ApiTestResult> findByUserIdOrderByTimestampDesc(Long userId);
    List<ApiTestResult> findByApiEndpointIdOrderByTimestampDesc(Long endpointId);
}
