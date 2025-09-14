package com.pingpad.modules.user_management;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * User Management Module - Handles user data and operations
 * 
 * Responsibilities:
 * - User entity management
 * - User repository operations
 * - User profile endpoints
 */
@Configuration
@ComponentScan(basePackages = "com.pingpad.modules.user_management")
public class UserManagementModule {
}
