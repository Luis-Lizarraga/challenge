package com.lucho.tienda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null
                && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authHeader.substring(7).trim();

            if (!token.isEmpty()) {
                authenticate(token, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            // Signature and expiration are verified once per request.
            Claims claims = jwtUtils.parseAndValidateToken(token);

            String username = claims.getSubject();
            Long userId = jwtUtils.getUserId(claims);
            String role = jwtUtils.getRole(claims);

            // Fail closed: an incomplete token never receives default permissions.
            if (username == null || username.isBlank() || userId == null || role == null || role.isBlank()) {
                return;
            }

            UserDetailsImpl userDetails = new UserDetailsImpl(
                    userId,
                    username,
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid JWT: leave the SecurityContext unauthenticated.
            // Spring Security will reject protected endpoints downstream.
        }
    }
}
