package com.pingpad.modules.api_testing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // Configure to use the application's ObjectMapper for proper JSON serialization
        restTemplate.getMessageConverters().removeIf(converter -> 
            converter instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper));
        return restTemplate;
    }
}
