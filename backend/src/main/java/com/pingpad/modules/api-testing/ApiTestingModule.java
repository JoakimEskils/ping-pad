package com.pingpad.modules.api_testing;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * API Testing Module - Handles API endpoint testing functionality
 * 
 * Responsibilities:
 * - API endpoint management
 * - HTTP request execution
 * - Test result storage and retrieval
 * - API testing endpoints
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.api_testing")
public class ApiTestingModule {
}
