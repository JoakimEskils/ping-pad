package com.pingpad.dto;

import com.pingpad.models.ApiEndpoint;
import java.time.LocalDateTime;
import java.util.Map;

public class ApiEndpointResponse {
    private Long id;
    private String name;
    private String url;
    private ApiEndpoint.HttpMethod method;
    private Map<String, String> headers;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ApiEndpointResponse() {}

    public ApiEndpointResponse(Long id, String name, String url, ApiEndpoint.HttpMethod method, Map<String, String> headers, String body, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public ApiEndpoint.HttpMethod getMethod() { return method; }
    public void setMethod(ApiEndpoint.HttpMethod method) { this.method = method; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ApiEndpointResponse fromEntity(ApiEndpoint endpoint) {
        ApiEndpointResponse response = new ApiEndpointResponse();
        response.setId(endpoint.getId());
        response.setName(endpoint.getName());
        response.setUrl(endpoint.getUrl());
        response.setMethod(endpoint.getMethod());
        response.setHeaders(endpoint.getHeaders());
        response.setBody(endpoint.getBody());
        response.setCreatedAt(endpoint.getCreatedAt());
        response.setUpdatedAt(endpoint.getUpdatedAt());
        return response;
    }
}
