package com.pingpad.modules.api_testing.repositories;

import com.pingpad.modules.api_testing.models.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUserId(Long userId);
    Optional<ApiKey> findByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndName(Long userId, String name);
    void deleteByUserIdAndId(Long userId, Long id);
}
