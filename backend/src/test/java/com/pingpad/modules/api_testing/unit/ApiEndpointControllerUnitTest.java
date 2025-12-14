package com.pingpad.modules.api_testing.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.api_testing.controllers.ApiEndpointController;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.services.ApiEndpointService;
import com.pingpad.modules.api_testing.services.ApiTestService;
import com.pingpad.modules.api_testing.models.ApiTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApiEndpointControllerUnitTest {

    @Mock
    private ApiEndpointService apiEndpointService;

    @Mock
    private ApiTestService apiTestService;

    @InjectMocks
    private ApiEndpointController apiEndpointController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiEndpointController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateEndpoint_Success() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        ApiEndpointProjection projection = ApiEndpointProjection.builder()
                .id(endpointId)
                .name("Test Endpoint")
                .url("https://api.example.com/test")
                .method("GET")
                .userId(1L)
                .build();

        ApiEndpointController.CreateEndpointRequest request = new ApiEndpointController.CreateEndpointRequest();
        request.name = "Test Endpoint";
        request.url = "https://api.example.com/test";
        request.method = "GET";
        request.headers = new HashMap<>();
        request.body = null;

        when(apiEndpointService.createEndpoint(anyString(), anyString(), anyString(), any(), any(), anyLong(), anyBoolean(), any()))
                .thenReturn(endpointId);
        when(apiEndpointService.getEndpoint(endpointId)).thenReturn(projection);

        // Act & Assert
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(endpointId.toString()))
                .andExpect(jsonPath("$.name").value("Test Endpoint"))
                .andExpect(jsonPath("$.url").value("https://api.example.com/test"))
                .andExpect(jsonPath("$.method").value("GET"));

        verify(apiEndpointService).createEndpoint(eq("Test Endpoint"), eq("https://api.example.com/test"),
                eq("GET"), isNull(), isNull(), eq(1L), anyBoolean(), any());
    }

    @Test
    void testCreateEndpoint_MissingName() throws Exception {
        // Arrange
        ApiEndpointController.CreateEndpointRequest request = new ApiEndpointController.CreateEndpointRequest();
        request.name = "";
        request.url = "https://api.example.com/test";
        request.method = "GET";

        // Act & Assert
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required"));

        verify(apiEndpointService, never()).createEndpoint(any(), any(), any(), any(), any(), anyLong(), anyBoolean(), any());
    }

    @Test
    void testCreateEndpoint_MissingUrl() throws Exception {
        // Arrange
        ApiEndpointController.CreateEndpointRequest request = new ApiEndpointController.CreateEndpointRequest();
        request.name = "Test Endpoint";
        request.url = "";
        request.method = "GET";

        // Act & Assert
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL is required"));

        verify(apiEndpointService, never()).createEndpoint(any(), any(), any(), any(), any(), anyLong(), anyBoolean(), any());
    }

    @Test
    void testUpdateEndpoint_Success() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        ApiEndpointProjection projection = ApiEndpointProjection.builder()
                .id(endpointId)
                .name("Updated Endpoint")
                .url("https://api.example.com/updated")
                .method("POST")
                .userId(1L)
                .build();

        ApiEndpointController.UpdateEndpointRequest request = new ApiEndpointController.UpdateEndpointRequest();
        request.name = "Updated Endpoint";
        request.url = "https://api.example.com/updated";
        request.method = "POST";
        request.headers = new HashMap<>();
        request.body = "{\"key\":\"value\"}";

        doNothing().when(apiEndpointService).updateEndpoint(any(), anyString(), anyString(), anyString(), any(), any(), anyBoolean(), any());
        when(apiEndpointService.getEndpoint(endpointId)).thenReturn(projection);

        // Act & Assert
        mockMvc.perform(put("/api/endpoints/" + endpointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(endpointId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Endpoint"));

        verify(apiEndpointService).updateEndpoint(eq(endpointId), eq("Updated Endpoint"),
                eq("https://api.example.com/updated"), eq("POST"), isNull(), eq("{\"key\":\"value\"}"), anyBoolean(), any());
    }

    @Test
    void testUpdateEndpoint_InvalidId() throws Exception {
        // Arrange
        ApiEndpointController.UpdateEndpointRequest request = new ApiEndpointController.UpdateEndpointRequest();
        request.name = "Updated Endpoint";
        request.url = "https://api.example.com/updated";
        request.method = "POST";

        // Act & Assert
        mockMvc.perform(put("/api/endpoints/invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(apiEndpointService, never()).updateEndpoint(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void testDeleteEndpoint_Success() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        doNothing().when(apiEndpointService).deleteEndpoint(endpointId);

        // Act & Assert
        mockMvc.perform(delete("/api/endpoints/" + endpointId))
                .andExpect(status().isNoContent());

        verify(apiEndpointService).deleteEndpoint(endpointId);
    }

    @Test
    void testDeleteEndpoint_InvalidId() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/endpoints/invalid-uuid"))
                .andExpect(status().isBadRequest());

        verify(apiEndpointService, never()).deleteEndpoint(any());
    }

    @Test
    void testGetEndpoint_Success() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        ApiEndpointProjection projection = ApiEndpointProjection.builder()
                .id(endpointId)
                .name("Test Endpoint")
                .url("https://api.example.com/test")
                .method("GET")
                .userId(1L)
                .build();

        when(apiEndpointService.getEndpoint(endpointId)).thenReturn(projection);

        // Act & Assert
        mockMvc.perform(get("/api/endpoints/" + endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(endpointId.toString()))
                .andExpect(jsonPath("$.name").value("Test Endpoint"));

        verify(apiEndpointService).getEndpoint(endpointId);
    }

    @Test
    void testGetAllEndpoints_Success() throws Exception {
        // Arrange
        List<ApiEndpointProjection> endpoints = Arrays.asList(
                ApiEndpointProjection.builder()
                        .id(UUID.randomUUID())
                        .name("Endpoint 1")
                        .url("https://api.example.com/1")
                        .method("GET")
                        .userId(1L)
                        .build(),
                ApiEndpointProjection.builder()
                        .id(UUID.randomUUID())
                        .name("Endpoint 2")
                        .url("https://api.example.com/2")
                        .method("POST")
                        .userId(1L)
                        .build()
        );

        when(apiEndpointService.getEndpointsByUser(1L)).thenReturn(endpoints);

        // Act & Assert
        mockMvc.perform(get("/api/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(apiEndpointService).getEndpointsByUser(1L);
    }

    @Test
    void testTestEndpoint_Success() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        ApiTestResult testResult = ApiTestResult.builder()
                .endpointId(endpointId)
                .statusCode(200)
                .responseTime(150L)
                .success(true)
                .build();

        when(apiTestService.testEndpoint(endpointId, 1L)).thenReturn(testResult);

        // Act & Assert
        mockMvc.perform(post("/api/endpoints/" + endpointId + "/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpointId").value(endpointId.toString()))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.success").value(true));

        verify(apiTestService).testEndpoint(endpointId, 1L);
    }

    @Test
    void testTestEndpoint_InvalidId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/endpoints/invalid-uuid/test"))
                .andExpect(status().isBadRequest());

        verify(apiTestService, never()).testEndpoint(any(), anyLong());
    }

    @Test
    void testCreateEndpoint_WithHeaders() throws Exception {
        // Arrange
        UUID endpointId = UUID.randomUUID();
        ApiEndpointProjection projection = ApiEndpointProjection.builder()
                .id(endpointId)
                .name("Test Endpoint")
                .url("https://api.example.com/test")
                .method("GET")
                .headers("Authorization: Bearer token\nContent-Type: application/json")
                .userId(1L)
                .build();

        ApiEndpointController.CreateEndpointRequest request = new ApiEndpointController.CreateEndpointRequest();
        request.name = "Test Endpoint";
        request.url = "https://api.example.com/test";
        request.method = "GET";
        request.headers = new HashMap<>();
        request.headers.put("Authorization", "Bearer token");
        request.headers.put("Content-Type", "application/json");

        when(apiEndpointService.createEndpoint(anyString(), anyString(), anyString(), any(), any(), anyLong(), anyBoolean(), any()))
                .thenReturn(endpointId);
        when(apiEndpointService.getEndpoint(endpointId)).thenReturn(projection);

        // Act & Assert
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(apiEndpointService).createEndpoint(eq("Test Endpoint"), eq("https://api.example.com/test"),
                eq("GET"), contains("Authorization"), isNull(), eq(1L), anyBoolean(), any());
    }
}
