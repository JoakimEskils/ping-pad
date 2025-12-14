package com.pingpad.modules.api_testing.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pingpad.modules.user_management.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "api_test_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_uuid_id", nullable = false)
    private UUID endpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time")
    private Long responseTime;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Helper method to set response headers from Map
    public void setResponseHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
            this.responseHeaders = sb.toString().trim();
        }
    }
}
