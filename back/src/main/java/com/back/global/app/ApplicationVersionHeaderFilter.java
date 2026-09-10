package com.back.global.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApplicationVersionHeaderFilter extends OncePerRequestFilter {
    static final String HEADER_NAME = "X-App-Version";

    private final String applicationVersion;

    public ApplicationVersionHeaderFilter(@Value("${app.version:local}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader(HEADER_NAME, applicationVersion);
        filterChain.doFilter(request, response);
    }
}
