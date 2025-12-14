package com.pingpad.modules.cache;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Module - Redis-based caching infrastructure
 * 
 * Responsibilities:
 * - Redis connection and configuration
 * - Cache service for managing cache operations
 * - Cache strategy implementation (Cache-Aside + Write-Through)
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.cache")
public class CacheModule {
}
