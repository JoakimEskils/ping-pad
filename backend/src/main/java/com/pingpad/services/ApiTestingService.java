package com.pingpad.services;

import com.pingpad.models.ApiEndpoint;
import com.pingpad.models.ApiTestResult;
import com.pingpad.models.User;
import com.pingpad.repositories.ApiTestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApiTestingService {

    private static final Logger log = LoggerFactory.getLogger(ApiTestingService.class);
    
    private final ApiTestResultRepository apiTestResultRepository;
    private final RestTemplate restTemplate;

    public ApiTestingService(ApiTestResultRepository apiTestResultRepository, RestTemplate restTemplate) {
        this.apiTestResultRepository = apiTestResultRepository;
        this.restTemplate = restTemplate;
    }

    public ApiTestResult testEndpoint(ApiEndpoint endpoint, User user) {
        long startTime = System.currentTimeMillis();
        ApiTestResult result = new ApiTestResult();
        result.setApiEndpoint(endpoint);
        result.setUser(user);

        log.info("Testing endpoint: {} with method: {}", endpoint.getUrl(), endpoint.getMethod());

        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            if (endpoint.getHeaders() != null) {
                endpoint.getHeaders().forEach(headers::set);
                log.info("Headers set: {}", endpoint.getHeaders());
            }

            // Prepare request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(endpoint.getBody(), headers);

            // Make the HTTP request
            log.info("Making HTTP request to: {}", endpoint.getUrl());
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint.getUrl(),
                HttpMethod.valueOf(endpoint.getMethod().name()),
                requestEntity,
                String.class
            );

            long endTime = System.currentTimeMillis();

            // Set successful response data
            result.setStatusCode(response.getStatusCode().value());
            result.setResponseTime(endTime - startTime);
            result.setResponseBody(response.getBody());
            
            log.info("Response received - Status: {}, Response time: {}ms, Body length: {}", 
                response.getStatusCode().value(), endTime - startTime, 
                response.getBody() != null ? response.getBody().length() : 0);
            
            // Convert response headers to Map
            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaders().forEach((key, values) -> 
                responseHeaders.put(key, String.join(", ", values))
            );
            result.setResponseHeaders(responseHeaders);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long endTime = System.currentTimeMillis();
            
            log.warn("HTTP error response: {} - {}", e.getStatusCode().value(), e.getMessage());
            
            // Handle HTTP error responses (4xx, 5xx)
            result.setStatusCode(e.getStatusCode().value());
            result.setResponseTime(endTime - startTime);
            result.setResponseBody(e.getResponseBodyAsString());
            result.setError(e.getMessage());

            // Convert response headers to Map
            Map<String, String> responseHeaders = new HashMap<>();
            if (e.getResponseHeaders() != null) {
                e.getResponseHeaders().forEach((key, values) -> 
                    responseHeaders.put(key, String.join(", ", values))
                );
            }
            result.setResponseHeaders(responseHeaders);

        } catch (ResourceAccessException e) {
            long endTime = System.currentTimeMillis();
            
            log.error("Connection error: {}", e.getMessage());
            
            // Handle connection/timeout errors
            result.setResponseTime(endTime - startTime);
            result.setError("Connection error: " + e.getMessage());

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            
            log.error("Unexpected error testing endpoint {}: {}", endpoint.getUrl(), e.getMessage(), e);
            
            // Handle other errors
            result.setResponseTime(endTime - startTime);
            result.setError("Unexpected error: " + e.getMessage());
        }

        // Save and return the result
        log.info("Saving test result - Status: {}, Response time: {}ms, Error: {}", 
            result.getStatusCode(), result.getResponseTime(), result.getError());
        
        return apiTestResultRepository.save(result);
    }
}
