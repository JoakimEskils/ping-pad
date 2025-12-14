package com.pingpad.modules.api_testing.models;

import com.pingpad.modules.user_management.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an API key stored by a user.
 * API keys are used to authenticate requests to external APIs.
 */
@Entity
@Table(name = "api_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "key_value", nullable = false, columnDefinition = "TEXT")
    private String keyValue; // Store encrypted/hashed value

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
