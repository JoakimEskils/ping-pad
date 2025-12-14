package com.pingpad;

import com.pingpad.modules.auth.AuthModule;
import com.pingpad.modules.user_management.UserManagementModule;
import com.pingpad.modules.shared.SharedModule;
import com.pingpad.modules.cache.CacheModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * PingPad Backend Application - Modular Monolith
 * 
 * This application is structured as a modular monolith with clear domain boundaries:
 * - Auth Module: Authentication and security
 * - User Management Module: User data and operations
 * - API Testing Module: API endpoint testing functionality (in api_testing package)
 * - Event Sourcing Module: Event sourcing infrastructure
 * - Cache Module: Redis-based caching layer
 * - Shared Module: Common utilities and configurations
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.pingpad",
    "com.pingpad.modules.auth",
    "com.pingpad.modules.user_management", 
    "com.pingpad.modules.api_testing",
    "com.pingpad.modules.shared",
    "com.pingpad.modules.eventsourcing",
    "com.pingpad.modules.cache"
})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
