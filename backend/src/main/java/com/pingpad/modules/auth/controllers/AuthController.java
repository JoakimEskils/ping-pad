package com.pingpad.modules.auth.controllers;

import com.pingpad.modules.auth.services.CustomUserDetailsService;
import com.pingpad.modules.auth.utils.JwtTokenUtil;
import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Profile("!test")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil,
                         CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Validate input
        if (request.email == null || request.email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (request.name == null || request.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        }
        if (request.password == null || request.password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered"));
        }

        // Create new user
        User user = new User();
        user.setEmail(request.email.trim().toLowerCase());
        user.setName(request.name.trim());
        user.setPassword(passwordEncoder.encode(request.password));
        user = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "name", user.getName()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email, request.password)
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.email);
        final String token = jwtTokenUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.email).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("user", Map.of(
            "id", user != null ? user.getId() : null,
            "email", userDetails.getUsername(),
            "name", user != null ? user.getName() : ""
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-as-test")
    public ResponseEntity<?> loginAsTest() {
        try {
            // Authenticate test user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("test-user@pingpad.local", "test-password")
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername("test-user@pingpad.local");
            final String token = jwtTokenUtil.generateToken(userDetails);

            User testUser = userRepository.findByEmail("test-user@pingpad.local")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("user", Map.of(
                "email", testUser.getEmail(),
                "name", testUser.getName(),
                "id", testUser.getId()
            ));

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Test user authentication failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // With JWT, logout is handled client-side by removing the token
        // This endpoint is just for consistency
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    public static class RegisterRequest {
        public String email;
        public String name;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }
}
