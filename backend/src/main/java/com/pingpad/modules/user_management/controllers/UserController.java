package com.pingpad.modules.user_management.controllers;

import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Profile("!test")  // Exclude from test profile to avoid OAuth2 dependency issues
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            // Use reflection to check for OAuth2User to avoid compile-time dependency in tests
            if (principal != null && principal.getClass().getName().equals("org.springframework.security.oauth2.core.user.OAuth2User")) {
                try {
                    java.lang.reflect.Method getAttribute = principal.getClass().getMethod("getAttribute", String.class);
                    String githubLogin = (String) getAttribute.invoke(principal, "login");
                    return userRepository.findByGithubLogin(githubLogin).orElse(null);
                } catch (Exception e) {
                    // Fallback if reflection fails
                    return null;
                }
            }
        }
        return null;
    }
}
