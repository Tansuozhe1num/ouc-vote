package com.vote.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Resource private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("campus_token".equals(cookie.getName())) { token = cookie.getValue(); break; }
            }
        }
        Long userId = jwtService.parseUserId(token);
        if (userId != null) request.setAttribute(AuthContext.USER_ID_ATTRIBUTE, userId);
        chain.doFilter(request, response);
    }
}
