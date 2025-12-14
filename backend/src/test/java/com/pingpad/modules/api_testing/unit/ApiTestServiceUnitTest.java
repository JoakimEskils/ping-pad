package com.pingpad.modules.api_testing.unit;

import com.pingpad.modules.api_testing.models.ApiTestResult;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.repositories.ApiTestResultRepository;
import com.pingpad.modules.api_testing.services.ApiEndpointService;
import com.pingpad.modules.api_testing.services.ApiTestService;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import testing.ApiTestingServiceGrpc;
import testing.Testing;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTestServiceUnitTest {

    @Mock
    private ManagedChannel grpcChannel;

    @Mock
    private ApiEndpointService apiEndpointService;

    @Mock
    private ApiTestResultRepository testResultRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApiTestService apiTestService;

    private UUID testEndpointId;
    private Long testUserId;
    private User testUser;
    private ApiEndpointProjection testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpointId = UUID.randomUUID();
        testUserId = 1L;

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testEndpoint = ApiEndpointProjection.builder()
                .id(testEndpointId)
                .name("Test Endpoint")
                .url("https://httpbin.org/get")
                .method("GET")
                .headers("Authorization: Bearer token")
                .body(null)
                .userId(testUserId)
                .build();
    }

    @Test
    void testTestEndpoint_Success() {
        // Arrange
        Testing.TestResult grpcResult = Testing.TestResult.newBuilder()
                .setId("test-result-id")
                .setEndpointId(testEndpointId.toString())
                .setStatusCode(200)
                .setResponseTimeNanos(150_000_000L) // 150ms
                .setResponseBody(com.google.protobuf.ByteString.copyFromUtf8("{\"success\":true}"))
                .putResponseHeaders("Content-Type", "application/json")
                .setSuccess(true)
                .setTimestamp(LocalDateTime.now().toString())
                .setRetryCount(0)
                .build();

        ApiTestingServiceGrpc.ApiTestingServiceBlockingStub stub = mock(ApiTestingServiceGrpc.ApiTestingServiceBlockingStub.class);
        when(grpcChannel).thenReturn(grpcChannel);
        when(ApiTestingServiceGrpc.newBlockingStub(grpcChannel)).thenReturn(stub);
        when(stub.testEndpoint(any(Testing.TestRequest.class))).thenReturn(grpcResult);

        when(apiEndpointService.getEndpoint(testEndpointId)).thenReturn(testEndpoint);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        ApiTestResult savedResult = ApiTestResult.builder()
                .endpointId(testEndpointId)
                .user(testUser)
                .statusCode(200)
                .responseTime(150L)
                .responseBody("{\"success\":true}")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();

        when(testResultRepository.save(any(ApiTestResult.class))).thenReturn(savedResult);

        // Act
        ApiTestResult result = apiTestService.testEndpoint(testEndpointId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertEquals(150L, result.getResponseTime());
        assertTrue(result.getSuccess());
        assertNull(result.getError());

        verify(apiEndpointService).getEndpoint(testEndpointId);
        verify(userRepository).findById(testUserId);
        verify(stub).testEndpoint(any(Testing.TestRequest.class));
        verify(testResultRepository).save(any(ApiTestResult.class));
    }

    @Test
    void testTestEndpoint_WithPostMethod() {
        // Arrange
        ApiEndpointProjection postEndpoint = ApiEndpointProjection.builder()
                .id(testEndpointId)
                .name("POST Endpoint")
                .url("https://httpbin.org/post")
                .method("POST")
                .headers("Content-Type: application/json")
                .body("{\"key\":\"value\"}")
                .userId(testUserId)
                .build();

        Testing.TestResult grpcResult = Testing.TestResult.newBuilder()
                .setId("test-result-id")
                .setEndpointId(testEndpointId.toString())
                .setStatusCode(200)
                .setResponseTimeNanos(200_000_000L)
                .setResponseBody(com.google.protobuf.ByteString.copyFromUtf8("{\"received\":true}"))
                .setSuccess(true)
                .setRetryCount(0)
                .build();

        ApiTestingServiceGrpc.ApiTestingServiceBlockingStub stub = mock(ApiTestingServiceGrpc.ApiTestingServiceBlockingStub.class);
        when(ApiTestingServiceGrpc.newBlockingStub(grpcChannel)).thenReturn(stub);
        when(stub.testEndpoint(any(Testing.TestRequest.class))).thenReturn(grpcResult);

        when(apiEndpointService.getEndpoint(testEndpointId)).thenReturn(postEndpoint);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(testResultRepository.save(any(ApiTestResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiTestResult result = apiTestService.testEndpoint(testEndpointId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.getSuccess());

        ArgumentCaptor<Testing.TestRequest> requestCaptor = ArgumentCaptor.forClass(Testing.TestRequest.class);
        verify(stub).testEndpoint(requestCaptor.capture());
        Testing.TestRequest capturedRequest = requestCaptor.getValue();
        assertEquals("POST", capturedRequest.getMethod());
        assertEquals("https://httpbin.org/post", capturedRequest.getUrl());
        assertTrue(capturedRequest.getBody().size() > 0);
    }

    @Test
    void testTestEndpoint_GrpcError() {
        // Arrange
        StatusRuntimeException grpcException = new StatusRuntimeException(
                Status.UNAVAILABLE.withDescription("Service unavailable")
        );

        ApiTestingServiceGrpc.ApiTestingServiceBlockingStub stub = mock(ApiTestingServiceGrpc.ApiTestingServiceBlockingStub.class);
        when(ApiTestingServiceGrpc.newBlockingStub(grpcChannel)).thenReturn(stub);
        when(stub.testEndpoint(any(Testing.TestRequest.class))).thenThrow(grpcException);

        when(apiEndpointService.getEndpoint(testEndpointId)).thenReturn(testEndpoint);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(testResultRepository.save(any(ApiTestResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiTestResult result = apiTestService.testEndpoint(testEndpointId, testUserId);

        // Assert
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("gRPC UNAVAILABLE") || result.getError().contains("Testing engine request timed out"));

        verify(testResultRepository).save(any(ApiTestResult.class));
    }

    @Test
    void testTestEndpoint_EndpointNotFound() {
        // Arrange
        when(apiEndpointService.getEndpoint(testEndpointId))
                .thenThrow(new IllegalArgumentException("API endpoint not found: " + testEndpointId));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            apiTestService.testEndpoint(testEndpointId, testUserId);
        });

        verify(apiEndpointService).getEndpoint(testEndpointId);
        verify(userRepository, never()).findById(any());
        verify(testResultRepository, never()).save(any());
    }

    @Test
    void testTestEndpoint_UserNotFound() {
        // Arrange
        when(apiEndpointService.getEndpoint(testEndpointId)).thenReturn(testEndpoint);
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            apiTestService.testEndpoint(testEndpointId, testUserId);
        });

        verify(apiEndpointService).getEndpoint(testEndpointId);
        verify(userRepository).findById(testUserId);
        verify(testResultRepository, never()).save(any());
    }

    @Test
    void testTestEndpoint_WithHeaders() {
        // Arrange
        ApiEndpointProjection endpointWithHeaders = ApiEndpointProjection.builder()
                .id(testEndpointId)
                .name("Endpoint with Headers")
                .url("https://httpbin.org/headers")
                .method("GET")
                .headers("Authorization: Bearer token\nX-Custom-Header: value")
                .body(null)
                .userId(testUserId)
                .build();

        Testing.TestResult grpcResult = Testing.TestResult.newBuilder()
                .setId("test-result-id")
                .setStatusCode(200)
                .setResponseTimeNanos(100_000_000L)
                .setSuccess(true)
                .setRetryCount(0)
                .build();

        ApiTestingServiceGrpc.ApiTestingServiceBlockingStub stub = mock(ApiTestingServiceGrpc.ApiTestingServiceBlockingStub.class);
        when(ApiTestingServiceGrpc.newBlockingStub(grpcChannel)).thenReturn(stub);
        when(stub.testEndpoint(any(Testing.TestRequest.class))).thenReturn(grpcResult);

        when(apiEndpointService.getEndpoint(testEndpointId)).thenReturn(endpointWithHeaders);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(testResultRepository.save(any(ApiTestResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiTestResult result = apiTestService.testEndpoint(testEndpointId, testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccess());

        ArgumentCaptor<Testing.TestRequest> requestCaptor = ArgumentCaptor.forClass(Testing.TestRequest.class);
        verify(stub).testEndpoint(requestCaptor.capture());
        Testing.TestRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.getHeadersMap().containsKey("Authorization"));
        assertTrue(capturedRequest.getHeadersMap().containsKey("X-Custom-Header"));
    }
}
