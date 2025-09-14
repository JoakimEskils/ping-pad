package com.pingpad.repositories;

import com.pingpad.models.ApiEndpoint;
import com.pingpad.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {
    List<ApiEndpoint> findByUserOrderByCreatedAtDesc(User user);
    List<ApiEndpoint> findByUserIdOrderByCreatedAtDesc(Long userId);
}
