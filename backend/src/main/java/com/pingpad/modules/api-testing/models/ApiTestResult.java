package com.pingpad.modules.api_testing.models;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "api_test_results")
public class ApiTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private ApiEndpoint apiEndpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer statusCode;

    private Long responseTime;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @ElementCollection
    @CollectionTable(name = "api_test_result_headers", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private Map<String, String> responseHeaders;

    @Column(columnDefinition = "TEXT")
    private String error;

    @DateTimeFormat
    private LocalDateTime timestamp;

    // Constructors
    public ApiTestResult() {}

    public ApiTestResult(ApiEndpoint apiEndpoint, User user) {
        this.apiEndpoint = apiEndpoint;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApiEndpoint getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(ApiEndpoint apiEndpoint) { this.apiEndpoint = apiEndpoint; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
