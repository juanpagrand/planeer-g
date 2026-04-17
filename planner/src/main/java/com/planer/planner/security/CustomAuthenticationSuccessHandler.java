package com.planer.planner.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        boolean isSuperAdmin = false;
        
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_SUPERADMIN".equals(auth.getAuthority())) {
                isSuperAdmin = true;
                break;
            }
        }
        
        if (isSuperAdmin) {
            response.sendRedirect("/superadmin");
        } else {
            response.sendRedirect("/");
        }
    }
}
