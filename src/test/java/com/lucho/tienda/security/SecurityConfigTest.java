package com.lucho.tienda.security;

import com.lucho.tienda.exception.CustomAccessDeniedHandler;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.lucho.tienda.constant.ApiEndpointConstants.*;
import static com.lucho.tienda.constant.ApiFieldConstants.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CartService cartService;

    @MockBean
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void publicEndpoints_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_AccessibleWhenAuthenticated() throws Exception {
        Long userId = 1L;
        Long cartId = 1L;

        String requestUri = (ENDPOINT_CARTS + SUB_ENDPOINT_GET_CART)
                .replace("{" + PARAM_CART_ID + "}", cartId.toString());

        UserDetailsImpl principal = new UserDetailsImpl(
                userId,
                "test@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(cartService.getCartById(userId, cartId)).thenReturn(new Cart());

        mockMvc.perform(get(requestUri).with(user(principal)))
                .andExpect(status().isOk());
    }
}
