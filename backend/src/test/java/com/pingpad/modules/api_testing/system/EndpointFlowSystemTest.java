package com.pingpad.modules.api_testing.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import com.pingpad.modules.api_testing.repositories.ApiTestResultRepository;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System tests that test complete flows through multiple services.
 * These tests verify the integration between controllers, services, repositories, and the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndpointFlowSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiEndpointProjectionRepository projectionRepository;

    @Autowired
    private ApiTestResultRepository testResultRepository;

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Create a test user
        testUser = new User();
        testUser.setName("System Test User");
        testUser.setEmail("systemtest@example.com");
        testUser.setGithubLogin("systemtest");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testCreateReadUpdateDeleteFlow() throws Exception {
        // Step 1: Create endpoint
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("name", "System Test Endpoint");
        createRequest.put("url", "https://httpbin.org/get");
        createRequest.put("method", "GET");
        createRequest.put("headers", Map.of("Authorization", "Bearer token"));
        createRequest.put("body", null);

        String createResponse = mockMvc.perform(post("/api/endpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("System Test Endpoint"))
                .andExpect(jsonPath("$.url").value("https://httpbin.org/get"))
                .andExpect(jsonPath("$.method").value("GET"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> createdEndpoint = objectMapper.readValue(createResponse, Map.class);
        String endpointId = createdEndpoint.get("id").toString();

        // Verify endpoint was saved to database
        UUID uuid = UUID.fromString(endpointId);
        assertTrue(projectionRepository.findById(uuid).isPresent());
        ApiEndpointProjection savedEndpoint = projectionRepository.findById(uuid).get();
        assertEquals("System Test Endpoint", savedEndpoint.getName());
        assertEquals(testUser.getId(), savedEndpoint.getUserId());

        // Step 2: Read endpoint
        mockMvc.perform(get("/api/endpoints/" + endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(endpointId))
                .andExpect(jsonPath("$.name").value("System Test Endpoint"));

        // Step 3: Update endpoint
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated System Test Endpoint");
        updateRequest.put("url", "https://httpbin.org/post");
        updateRequest.put("method", "POST");
        updateRequest.put("headers", Map.of("Content-Type", "application/json"));
        updateRequest.put("body", "{\"key\":\"value\"}");

        mockMvc.perform(put("/api/endpoints/" + endpointId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated System Test Endpoint"))
                .andExpect(jsonPath("$.method").value("POST"));

        // Verify update was persisted
        ApiEndpointProjection updatedEndpoint = projectionRepository.findById(uuid).get();
        assertEquals("Updated System Test Endpoint", updatedEndpoint.getName());
        assertEquals("POST", updatedEndpoint.getMethod());

        // Step 4: Delete endpoint
        mockMvc.perform(delete("/api/endpoints/" + endpointId))
                .andExpect(status().isNoContent());

        // Verify endpoint was deleted
        assertFalse(projectionRepository.findById(uuid).isPresent());
    }

    @Test
    void testCreateAndListEndpointsFlow() throws Exception {
        // Create multiple endpoints
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> createRequest = new HashMap<>();
            createRequest.put("name", "Endpoint " + i);
            createRequest.put("url", "https://httpbin.org/get");
            createRequest.put("method", "GET");
            createRequest.put("headers", null);
            createRequest.put("body", null);

            mockMvc.perform(post("/api/endpoints")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated());
        }

        // List all endpoints
        String listResponse = mockMvc.perform(get("/api/endpoints"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify we can retrieve the endpoints
        // Note: The actual count may include endpoints from other tests, so we just verify the structure
        assertNotNull(listResponse);
        assertTrue(listResponse.contains("Endpoint 1") || listResponse.contains("Endpoint 2") || listResponse.contains("Endpoint 3"));
    }

    @Test
    void testValidationFlow() throws Exception {
        // Test missing name
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("name", "");
        invalidRequest.put("url", "https://httpbin.org/get");
        invalidRequest.put("method", "GET");

        mockMvc.perform(post("/api/endpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required"));

        // Test missing URL
        invalidRequest = new HashMap<>();
        invalidRequest.put("name", "Test Endpoint");
        invalidRequest.put("url", "");
        invalidRequest.put("method", "GET");

        mockMvc.perform(post("/api/endpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL is required"));

        // Test missing method
        invalidRequest = new HashMap<>();
        invalidRequest.put("name", "Test Endpoint");
        invalidRequest.put("url", "https://httpbin.org/get");
        invalidRequest.put("method", "");

        mockMvc.perform(post("/api/endpoints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Method is required"));
    }

    @Test
    void testGetNonExistentEndpoint() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/endpoints/" + nonExistentId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateNonExistentEndpoint() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Endpoint");
        updateRequest.put("url", "https://httpbin.org/get");
        updateRequest.put("method", "GET");

        mockMvc.perform(put("/api/endpoints/" + nonExistentId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDeleteNonExistentEndpoint() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(delete("/api/endpoints/" + nonExistentId))
                .andExpect(status().isInternalServerError());
    }
}
