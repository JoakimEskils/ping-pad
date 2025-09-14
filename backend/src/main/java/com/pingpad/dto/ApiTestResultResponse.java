package com.pingpad.dto;

import com.pingpad.models.ApiTestResult;
import java.time.LocalDateTime;
import java.util.Map;

public class ApiTestResultResponse {
    private Long id;
    private Long endpointId;
    private Integer statusCode;
    private Long responseTime;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private String error;
    private LocalDateTime timestamp;

    // Constructors
    public ApiTestResultResponse() {}

    public ApiTestResultResponse(Long id, Long endpointId, Integer statusCode, Long responseTime, String responseBody, Map<String, String> responseHeaders, String error, LocalDateTime timestamp) {
        this.id = id;
        this.endpointId = endpointId;
        this.statusCode = statusCode;
        this.responseTime = responseTime;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
        this.error = error;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEndpointId() { return endpointId; }
    public void setEndpointId(Long endpointId) { this.endpointId = endpointId; }

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

    public static ApiTestResultResponse fromEntity(ApiTestResult result) {
        ApiTestResultResponse response = new ApiTestResultResponse();
        response.setId(result.getId());
        response.setEndpointId(result.getApiEndpoint().getId());
        response.setStatusCode(result.getStatusCode());
        response.setResponseTime(result.getResponseTime());
        response.setResponseBody(result.getResponseBody());
        response.setResponseHeaders(result.getResponseHeaders());
        response.setError(result.getError());
        response.setTimestamp(result.getTimestamp());
        return response;
    }
}
