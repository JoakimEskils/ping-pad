package com.pingpad.modules.user_management.controllers;

import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                // Don't send password hash to frontend
                user.setPassword(null);
            }
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
            }

            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Update name if provided
            if (request.name != null && !request.name.trim().isEmpty()) {
                user.setName(request.name.trim());
            }

            userRepository.save(user);

            // Don't send password hash to frontend
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(
            @RequestBody UpdatePasswordRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
            }

            // Validate request
            if (request.currentPassword == null || request.currentPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Current password is required"));
            }
            if (request.newPassword == null || request.newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "New password must be at least 6 characters"));
            }

            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(request.currentPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Current password is incorrect"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Error updating password: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating password", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage));
        }
    }

    public static class UpdateUserRequest {
        public String name;
    }

    public static class UpdatePasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}
