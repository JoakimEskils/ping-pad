package com.pingpad.modules.auth;

import com.pingpad.modules.user_management.models.User;
import com.pingpad.modules.user_management.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Profile;
import java.util.Collections;
import java.util.Map;

@Service
@Profile("!test")  // Exclude from test profile
public class OAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuthUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuthUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuthUser.getAttributes();

        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");

        User user = userRepository.findByGithubLogin(login).orElseGet(() -> new User());
        user.setGithubLogin(login);
        user.setName(name != null ? name : login);
        user.setEmail(email != null ? email : login + "@github.com");
        userRepository.save(user);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "login"
        );
    }
}
