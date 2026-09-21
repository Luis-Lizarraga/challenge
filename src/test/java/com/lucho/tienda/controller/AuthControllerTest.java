package com.lucho.tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucho.tienda.dto.AuthRequest;
import com.lucho.tienda.dto.AuthResponse;
import com.lucho.tienda.security.JwtUtils;
import com.lucho.tienda.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static com.lucho.tienda.constant.ApiEndpointConstants.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Mocks required by the security infrastructure when loading the context
    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void login_Returns200AndToken() throws Exception {
        AuthRequest request = new AuthRequest("userTest", "pass123");
        AuthResponse response = new AuthResponse("mocked-jwt-token");

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENDPOINT_AUTH + SUB_ENDPOINT_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }
}