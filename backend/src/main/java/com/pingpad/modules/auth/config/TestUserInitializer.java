package com.pingpad.modules.auth.config;

import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class TestUserInitializer {

    @Bean
    public CommandLineRunner initializeTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if test user already exists
            if (userRepository.findByEmail("test-user@pingpad.local").isEmpty()) {
                User testUser = new User();
                testUser.setEmail("test-user@pingpad.local");
                testUser.setName("Test User");
                // Password: "test-password"
                testUser.setPassword(passwordEncoder.encode("test-password"));
                userRepository.save(testUser);
                System.out.println("Test user created: test-user@pingpad.local / test-password");
            }
        };
    }
}
