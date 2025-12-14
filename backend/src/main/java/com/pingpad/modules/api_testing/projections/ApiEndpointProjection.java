package com.pingpad.modules.api_testing.projections;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Read model projection for API endpoints.
 * This is the denormalized view used for queries.
 */
@Entity
@Table(name = "api_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointProjection {
    @Id
    @Column(name = "uuid_id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String method;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
