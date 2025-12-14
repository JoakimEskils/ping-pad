package com.pingpad.modules.api_testing.models;

import com.pingpad.modules.user_management.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "acknowledged_alarms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcknowledgedAlarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @Column(name = "test_result_id", nullable = false)
    private Long testResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "acknowledged_at", nullable = false, updatable = false)
    private LocalDateTime acknowledgedAt;

    @PrePersist
    protected void onCreate() {
        if (acknowledgedAt == null) {
            acknowledgedAt = LocalDateTime.now();
        }
    }
}
