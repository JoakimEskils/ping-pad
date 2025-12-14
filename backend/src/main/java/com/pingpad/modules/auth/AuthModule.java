package com.pingpad.modules.auth;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auth Module - Handles authentication and authorization
 * 
 * Responsibilities:
 * - JWT token-based authentication
 * - Security configuration
 * - User authentication flow
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.auth")
public class AuthModule {
}
