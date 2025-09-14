package com.pingpad.modules.api_testing.models;

import com.pingpad.modules.user_management.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id")
    private ApiEndpoint apiEndpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
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
