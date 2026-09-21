package com.lucho.tienda.security;

import com.lucho.tienda.constant.ApiEndpointConstants;
import com.lucho.tienda.exception.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Standard for stateless REST APIs authenticated with JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Enable frame options (Required for H2 Console UI to render correctly)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                // 3. Configure endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Explicitly allow access to the H2 Console
                        .requestMatchers(PathRequest.toH2Console()).permitAll()

                        // Allow favicon requests to prevent unnecessary 403 server logs
                        .requestMatchers("/favicon.ico").permitAll()

                        // Allow public endpoints defined in constants (Auth, Swagger, Health, Prometheus)
                        .requestMatchers(ApiEndpointConstants.PUBLIC_PATHS).permitAll()

                        // Require ADMIN role for all other Actuator endpoints (e.g. /actuator/env, /actuator/beans)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Require authentication for any other API request
                        .anyRequest().authenticated()
                )

                // 4. Session management set to STATELESS for JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}