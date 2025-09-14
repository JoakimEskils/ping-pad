package com.pingpad.modules.api_testing.models;

import com.pingpad.modules.api_testing.models.ApiEndpoint;
import java.util.Map;

public class CreateApiEndpointRequest {
    private String name;
    private String url;
    private ApiEndpoint.HttpMethod method;
    private Map<String, String> headers;
    private String body;

    // Constructors
    public CreateApiEndpointRequest() {}

    public CreateApiEndpointRequest(String name, String url, ApiEndpoint.HttpMethod method, Map<String, String> headers, String body) {
        this.name = name;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    // Getters and Setters
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
}
