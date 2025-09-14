package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.ApiEndpoint;
import com.pingpad.modules.user_management.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {
    List<ApiEndpoint> findByUserOrderByCreatedAtDesc(User user);
    List<ApiEndpoint> findByUserIdOrderByCreatedAtDesc(Long userId);
}
