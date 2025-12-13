package com.pingpad.modules.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;
import java.io.IOException;

@Component
@Profile("!test")  // Exclude from test profile
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        // Redirect to frontend after successful authentication
        response.sendRedirect("http://localhost:5173");
    }
}
