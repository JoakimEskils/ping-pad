package com.pingpad.modules.api_testing.unit;

import com.pingpad.modules.api_testing.aggregates.ApiEndpointAggregate;
import com.pingpad.modules.api_testing.events.ApiEndpointCreatedEvent;
import com.pingpad.modules.api_testing.events.ApiEndpointUpdatedEvent;
import com.pingpad.modules.api_testing.handlers.ApiEndpointEventHandler;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjection;
import com.pingpad.modules.api_testing.projections.ApiEndpointProjectionRepository;
import com.pingpad.modules.api_testing.services.ApiEndpointService;
import com.pingpad.modules.cache.services.CacheService;
import com.pingpad.modules.eventsourcing.core.Event;
import com.pingpad.modules.eventsourcing.core.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiEndpointServiceUnitTest {

    @Mock
    private EventStore eventStore;

    @Mock
    private ApiEndpointProjectionRepository projectionRepository;

    @Mock
    private ApiEndpointEventHandler eventHandler;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ApiEndpointService apiEndpointService;

    private UUID testEndpointId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testEndpointId = UUID.randomUUID();
        testUserId = 1L;
    }

    @Test
    void testCreateEndpoint_Success() {
        // Arrange
        String name = "Test Endpoint";
        String url = "https://api.example.com/test";
        String method = "GET";
        String headers = "Authorization: Bearer token";
        String body = null;

        ArgumentCaptor<UUID> aggregateIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> aggregateTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> versionCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<List<Event>> eventsCaptor = ArgumentCaptor.forClass(List.class);

        doNothing().when(eventStore).appendEvents(
                aggregateIdCaptor.capture(),
                aggregateTypeCaptor.capture(),
                versionCaptor.capture(),
                eventsCaptor.capture()
        );
        doNothing().when(eventHandler).handle(any(Event.class));

        // Act
        UUID result = apiEndpointService.createEndpoint(name, url, method, headers, body, testUserId, false, null);

        // Assert
        assertNotNull(result);
        assertEquals(ApiEndpointAggregate.AGGREGATE_TYPE, aggregateTypeCaptor.getValue());
        assertEquals(0, versionCaptor.getValue());
        assertFalse(eventsCaptor.getValue().isEmpty());
        assertTrue(eventsCaptor.getValue().get(0) instanceof ApiEndpointCreatedEvent);

        verify(eventStore).appendEvents(any(), eq(ApiEndpointAggregate.AGGREGATE_TYPE), eq(0), anyList());
        verify(eventHandler, atLeastOnce()).handle(any(Event.class));
    }

    @Test
    void testUpdateEndpoint_Success() {
        // Arrange
        // Note: getEvents() is not needed since we're mocking loadAggregate directly
        when(eventStore.loadAggregate(eq(testEndpointId), eq(ApiEndpointAggregate.AGGREGATE_TYPE), any()))
                .thenAnswer(invocation -> {
                    ApiEndpointAggregate aggregate = new ApiEndpointAggregate(testEndpointId, 1);
                    aggregate.create("Old Name", "https://api.example.com/old", "GET", null, null, testUserId, false, null);
                    aggregate.markEventsAsCommitted();
                    return aggregate;
                });

        String newName = "Updated Name";
        String newUrl = "https://api.example.com/updated";
        String newMethod = "POST";
        String newHeaders = "Content-Type: application/json";
        String newBody = "{\"key\":\"value\"}";

        ArgumentCaptor<List<Event>> eventsCaptor = ArgumentCaptor.forClass(List.class);

        doNothing().when(eventStore).appendEvents(any(), any(), anyInt(), eventsCaptor.capture());
        doNothing().when(eventHandler).handleUpdate(any(), any(ApiEndpointUpdatedEvent.class));

        // Act
        apiEndpointService.updateEndpoint(testEndpointId, newName, newUrl, newMethod, newHeaders, newBody, null, null);

        // Assert
        verify(eventStore).appendEvents(eq(testEndpointId), eq(ApiEndpointAggregate.AGGREGATE_TYPE), anyInt(), anyList());
        verify(eventHandler).handleUpdate(eq(testEndpointId), any(ApiEndpointUpdatedEvent.class));
    }

    @Test
    void testDeleteEndpoint_Success() {
        // Arrange
        // Note: getEvents() is not needed since we're mocking loadAggregate directly
        when(eventStore.loadAggregate(eq(testEndpointId), eq(ApiEndpointAggregate.AGGREGATE_TYPE), any()))
                .thenAnswer(invocation -> {
                    ApiEndpointAggregate aggregate = new ApiEndpointAggregate(testEndpointId, 1);
                    aggregate.create("Test", "https://api.example.com/test", "GET", null, null, testUserId, false, null);
                    aggregate.markEventsAsCommitted();
                    return aggregate;
                });

        doNothing().when(eventStore).appendEvents(any(), any(), anyInt(), anyList());
        doNothing().when(eventHandler).handleDelete(testEndpointId);

        // Act
        apiEndpointService.deleteEndpoint(testEndpointId);

        // Assert
        verify(eventStore).appendEvents(eq(testEndpointId), eq(ApiEndpointAggregate.AGGREGATE_TYPE), anyInt(), anyList());
        verify(eventHandler).handleDelete(testEndpointId);
    }

    @Test
    void testGetEndpoint_FromCache() {
        // Arrange
        ApiEndpointProjection cachedProjection = ApiEndpointProjection.builder()
                .id(testEndpointId)
                .name("Cached Endpoint")
                .url("https://api.example.com/cached")
                .method("GET")
                .userId(testUserId)
                .build();

        when(cacheService.get(anyString(), eq(ApiEndpointProjection.class)))
                .thenReturn(Optional.of(cachedProjection));

        // Act
        ApiEndpointProjection result = apiEndpointService.getEndpoint(testEndpointId);

        // Assert
        assertNotNull(result);
        assertEquals(cachedProjection, result);
        assertEquals("Cached Endpoint", result.getName());
        verify(cacheService).get("endpoint:" + testEndpointId, ApiEndpointProjection.class);
        verify(projectionRepository, never()).findById(any());
    }

    @Test
    void testGetEndpoint_FromDatabase() {
        // Arrange
        ApiEndpointProjection dbProjection = ApiEndpointProjection.builder()
                .id(testEndpointId)
                .name("DB Endpoint")
                .url("https://api.example.com/db")
                .method("GET")
                .userId(testUserId)
                .build();

        when(cacheService.get(anyString(), eq(ApiEndpointProjection.class)))
                .thenReturn(Optional.empty());
        when(projectionRepository.findById(testEndpointId))
                .thenReturn(Optional.of(dbProjection));
        doNothing().when(cacheService).put(anyString(), any(ApiEndpointProjection.class));

        // Act
        ApiEndpointProjection result = apiEndpointService.getEndpoint(testEndpointId);

        // Assert
        assertNotNull(result);
        assertEquals(dbProjection, result);
        assertEquals("DB Endpoint", result.getName());
        verify(projectionRepository).findById(testEndpointId);
        verify(cacheService).put("endpoint:" + testEndpointId, dbProjection);
    }

    @Test
    void testGetEndpoint_NotFound() {
        // Arrange
        when(cacheService.get(anyString(), eq(ApiEndpointProjection.class)))
                .thenReturn(Optional.empty());
        when(projectionRepository.findById(testEndpointId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            apiEndpointService.getEndpoint(testEndpointId);
        });

        verify(projectionRepository).findById(testEndpointId);
    }

    @Test
    void testGetEndpointsByUser_FromCache() {
        // Arrange
        List<ApiEndpointProjection> cachedEndpoints = Arrays.asList(
                ApiEndpointProjection.builder()
                        .id(UUID.randomUUID())
                        .name("Endpoint 1")
                        .url("https://api.example.com/1")
                        .method("GET")
                        .userId(testUserId)
                        .build(),
                ApiEndpointProjection.builder()
                        .id(UUID.randomUUID())
                        .name("Endpoint 2")
                        .url("https://api.example.com/2")
                        .method("POST")
                        .userId(testUserId)
                        .build()
        );

        when(cacheService.getList(anyString(), eq(ApiEndpointProjection.class)))
                .thenReturn(Optional.of(cachedEndpoints));

        // Act
        List<ApiEndpointProjection> result = apiEndpointService.getEndpointsByUser(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cacheService).getList("endpoint:user:" + testUserId, ApiEndpointProjection.class);
        verify(projectionRepository, never()).findByUserId(any());
    }

    @Test
    void testGetEndpointsByUser_FromDatabase() {
        // Arrange
        List<ApiEndpointProjection> dbEndpoints = Arrays.asList(
                ApiEndpointProjection.builder()
                        .id(UUID.randomUUID())
                        .name("Endpoint 1")
                        .url("https://api.example.com/1")
                        .method("GET")
                        .userId(testUserId)
                        .build()
        );

        when(cacheService.getList(anyString(), eq(ApiEndpointProjection.class)))
                .thenReturn(Optional.empty());
        when(projectionRepository.findByUserId(testUserId))
                .thenReturn(dbEndpoints);
        doNothing().when(cacheService).putList(anyString(), anyList());

        // Act
        List<ApiEndpointProjection> result = apiEndpointService.getEndpointsByUser(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectionRepository).findByUserId(testUserId);
        verify(cacheService).putList("endpoint:user:" + testUserId, dbEndpoints);
    }
}
