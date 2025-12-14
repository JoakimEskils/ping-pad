package com.pingpad.modules.api_testing.config;

import com.pingpad.modules.shared.interceptors.GrpcCorrelationIdInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for gRPC client to communicate with Go API Testing Engine
 */
@Configuration
@Slf4j
public class GrpcClientConfig {

    private final GrpcCorrelationIdInterceptor correlationIdInterceptor;

    @Value("${api.testing.engine.grpc.url:api-testing-engine}")
    private String grpcHost;

    @Value("${api.testing.engine.grpc.port:9090}")
    private int grpcPort;

    private ManagedChannel channel;

    public GrpcClientConfig(GrpcCorrelationIdInterceptor correlationIdInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
    }

    @Bean
    public ManagedChannel grpcChannel() {
        String target = String.format("%s:%d", grpcHost, grpcPort);
        log.info("Creating gRPC channel to {}", target);
        channel = ManagedChannelBuilder.forTarget(target)
                .intercept(correlationIdInterceptor)
                .usePlaintext() // Use plaintext for now (can be changed to TLS in production)
                .build();
        return channel;
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
        }
    }
}
