package com.pingpad.modules.shared;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Module - Common utilities and configurations
 * 
 * Responsibilities:
 * - Common configurations (RestTemplate, etc.)
 * - Shared utilities
 * - Cross-cutting concerns
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.shared")
public class SharedModule {
}
