package com.pingpad.modules.shared.interceptors;

import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * gRPC client interceptor to propagate correlation ID to downstream services.
 * 
 * This interceptor:
 * - Retrieves correlation ID from MDC
 * - Adds it to outgoing gRPC request metadata
 * - Ensures correlation ID flows through service boundaries
 */
@Component
public class GrpcCorrelationIdInterceptor implements ClientInterceptor {

    private static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Get correlation ID from MDC
                String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
                
                // Add correlation ID to gRPC metadata if present
                if (correlationId != null && !correlationId.trim().isEmpty()) {
                    headers.put(Metadata.Key.of(CORRELATION_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER), correlationId);
                }
                
                super.start(responseListener, headers);
            }
        };
    }
}
