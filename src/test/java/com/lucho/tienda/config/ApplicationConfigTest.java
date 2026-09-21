package com.lucho.tienda.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Test
    void passwordEncoder_ReturnsBCryptInstance() {
        // Act
        PasswordEncoder encoder = applicationConfig.passwordEncoder();

        // Assert
        assertNotNull(encoder);
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);

        String rawPassword = "password123";
        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded));
    }

    @Test
    void authenticationManager_ReturnsManagerFromConfiguration() throws Exception {
        // Arrange
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // Act
        AuthenticationManager result = applicationConfig.authenticationManager(authenticationConfiguration);

        // Assert
        assertNotNull(result);
        assertEquals(authenticationManager, result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }
}