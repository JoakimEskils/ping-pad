package com.pingpad.controllers;

import com.pingpad.dto.ApiEndpointResponse;
import com.pingpad.dto.ApiTestResultResponse;
import com.pingpad.dto.CreateApiEndpointRequest;
import com.pingpad.models.ApiEndpoint;
import com.pingpad.models.ApiTestResult;
import com.pingpad.models.User;
import com.pingpad.repositories.ApiEndpointRepository;
import com.pingpad.repositories.ApiTestResultRepository;
import com.pingpad.repositories.UserRepository;
import com.pingpad.services.ApiTestingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/endpoints")
public class ApiEndpointController {

    private final ApiEndpointRepository apiEndpointRepository;
    private final ApiTestResultRepository apiTestResultRepository;
    private final UserRepository userRepository;
    private final ApiTestingService apiTestingService;

    public ApiEndpointController(ApiEndpointRepository apiEndpointRepository, 
                                ApiTestResultRepository apiTestResultRepository, 
                                UserRepository userRepository, 
                                ApiTestingService apiTestingService) {
        this.apiEndpointRepository = apiEndpointRepository;
        this.apiTestResultRepository = apiTestResultRepository;
        this.userRepository = userRepository;
        this.apiTestingService = apiTestingService;
    }

    @GetMapping
    public ResponseEntity<List<ApiEndpointResponse>> getUserEndpoints(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByUserOrderByCreatedAtDesc(user);
        List<ApiEndpointResponse> response = endpoints.stream()
                .map(ApiEndpointResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiEndpointResponse> createEndpoint(
            @RequestBody CreateApiEndpointRequest request,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setUser(user);
        endpoint.setName(request.getName());
        endpoint.setUrl(request.getUrl());
        endpoint.setMethod(request.getMethod());
        endpoint.setHeaders(request.getHeaders());
        endpoint.setBody(request.getBody());

        ApiEndpoint savedEndpoint = apiEndpointRepository.save(endpoint);
        return ResponseEntity.ok(ApiEndpointResponse.fromEntity(savedEndpoint));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiEndpointResponse> getEndpoint(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ApiEndpoint> endpoint = apiEndpointRepository.findById(id);
        if (endpoint.isEmpty() || !endpoint.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiEndpointResponse.fromEntity(endpoint.get()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiEndpointResponse> updateEndpoint(
            @PathVariable Long id,
            @RequestBody CreateApiEndpointRequest request,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ApiEndpoint> optionalEndpoint = apiEndpointRepository.findById(id);
        if (optionalEndpoint.isEmpty() || !optionalEndpoint.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        ApiEndpoint endpoint = optionalEndpoint.get();
        endpoint.setName(request.getName());
        endpoint.setUrl(request.getUrl());
        endpoint.setMethod(request.getMethod());
        endpoint.setHeaders(request.getHeaders());
        endpoint.setBody(request.getBody());

        ApiEndpoint savedEndpoint = apiEndpointRepository.save(endpoint);
        return ResponseEntity.ok(ApiEndpointResponse.fromEntity(savedEndpoint));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ApiEndpoint> endpoint = apiEndpointRepository.findById(id);
        if (endpoint.isEmpty() || !endpoint.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        apiEndpointRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ApiTestResultResponse> testEndpoint(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ApiEndpoint> optionalEndpoint = apiEndpointRepository.findById(id);
        if (optionalEndpoint.isEmpty() || !optionalEndpoint.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        ApiEndpoint endpoint = optionalEndpoint.get();
        ApiTestResult result = apiTestingService.testEndpoint(endpoint, user);
        
        return ResponseEntity.ok(ApiTestResultResponse.fromEntity(result));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<List<ApiTestResultResponse>> getEndpointResults(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ApiEndpoint> endpoint = apiEndpointRepository.findById(id);
        if (endpoint.isEmpty() || !endpoint.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<ApiTestResult> results = apiTestResultRepository.findByApiEndpointIdOrderByTimestampDesc(id);
        List<ApiTestResultResponse> response = results.stream()
                .map(ApiTestResultResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String githubLogin = oAuth2User.getAttribute("login");
            return userRepository.findByGithubLogin(githubLogin).orElse(null);
        }
        return null;
    }
}
