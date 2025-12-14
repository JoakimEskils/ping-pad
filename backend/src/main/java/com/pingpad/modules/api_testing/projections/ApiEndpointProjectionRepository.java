package com.pingpad.modules.api_testing.projections;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiEndpointProjectionRepository extends JpaRepository<ApiEndpointProjection, UUID> {
    Optional<ApiEndpointProjection> findById(UUID id);
    List<ApiEndpointProjection> findByUserId(Long userId);
    List<ApiEndpointProjection> findByRecurringEnabledTrue();
    void deleteById(UUID id);
}
