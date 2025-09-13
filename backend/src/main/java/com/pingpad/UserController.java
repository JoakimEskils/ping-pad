package com.pingpad;

import com.pingpad.models.User;
import com.pingpad.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String githubLogin = oAuth2User.getAttribute("login");
            return userRepository.findByGithubLogin(githubLogin).orElse(null);
        }
        return null;
    }
} 