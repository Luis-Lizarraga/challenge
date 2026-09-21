package com.lucho.tienda.service.impl;

import com.lucho.tienda.dto.AuthRequest;
import com.lucho.tienda.dto.AuthResponse;
import com.lucho.tienda.model.enums.Role;
import com.lucho.tienda.security.JwtUtils;
import com.lucho.tienda.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_ReturnsAuthResponse_WithJwtToken() {
        // Arrange
        AuthRequest request = new AuthRequest("lucho", "password123");

        // Se instancia UserDetailsImpl pasando sus 4 argumentos requeridos: (id, username, password, authorities)
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "lucho",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateToken("lucho", 1L, "USER")).thenReturn("mocked-jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateToken("lucho", 1L, "USER");
    }
}