package com.pingpad.modules.api_testing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpad.modules.shared.interceptors.CorrelationIdInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Go API Testing Engine integration
 */
@Configuration
public class GoTestingConfig {

    private final CorrelationIdInterceptor correlationIdInterceptor;

    public GoTestingConfig(CorrelationIdInterceptor correlationIdInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
    }

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Connection timeout: 5 seconds (time to establish connection)
        factory.setConnectTimeout(5000);
        // Read timeout: 35 seconds (30s for test + 5s buffer for processing)
        // The Go engine has a 30s default timeout for the actual HTTP test
        factory.setReadTimeout(35000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // Configure to use the application's ObjectMapper for proper JSON serialization
        restTemplate.getMessageConverters().removeIf(converter -> 
            converter instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper));
        // Add correlation ID interceptor to propagate trace IDs to Go service
        restTemplate.getInterceptors().add(correlationIdInterceptor);
        return restTemplate;
    }
}
