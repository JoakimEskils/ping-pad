package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.ApiTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTestResultRepository extends JpaRepository<ApiTestResult, Long> {
}
