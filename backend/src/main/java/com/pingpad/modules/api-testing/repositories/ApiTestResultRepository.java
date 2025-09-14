package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.user_management.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiTestResultRepository extends JpaRepository<ApiTestResult, Long> {
    List<ApiTestResult> findByUserOrderByTimestampDesc(User user);
    List<ApiTestResult> findByUserIdOrderByTimestampDesc(Long userId);
    List<ApiTestResult> findByApiEndpointIdOrderByTimestampDesc(Long endpointId);
}
