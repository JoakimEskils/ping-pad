package com.pingpad;

import com.pingpad.modules.auth.AuthModule;
import com.pingpad.modules.user_management.UserManagementModule;
import com.pingpad.modules.api_testing.ApiTestingModule;
import com.pingpad.modules.shared.SharedModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * PingPad Backend Application - Modular Monolith
 * 
 * This application is structured as a modular monolith with clear domain boundaries:
 * - Auth Module: Authentication and security
 * - User Management Module: User data and operations
 * - API Testing Module: API endpoint testing functionality
 * - Shared Module: Common utilities and configurations
 */
@SpringBootApplication
@Import({
    AuthModule.class,
    UserManagementModule.class,
    ApiTestingModule.class,
    SharedModule.class
})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
