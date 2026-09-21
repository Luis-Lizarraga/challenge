package com.lucho.tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Category;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.security.JwtAuthenticationFilter;
import com.lucho.tienda.security.JwtUtils;
import com.lucho.tienda.security.UserDetailsImpl;
import com.lucho.tienda.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.lucho.tienda.constant.ApiEndpointConstants.*;
import static com.lucho.tienda.constant.ApiFieldConstants.FIELD_PRODUCT_CODE;
import static com.lucho.tienda.constant.ApiFieldConstants.PARAM_CART_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Cart cart;
    private UserDetailsImpl mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = new UserDetailsImpl(
                1L,
                "lucho",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 1. Manually inject the user into the ThreadLocal security context.
        // This allows the @AuthenticationPrincipal in the controller to resolve it without filters.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities())
        );

        Product product = new Product();
        product.setId(1L);
        product.setCode("P01");
        product.setName("Whey Protein");
        product.setCategory(new Category());
        product.setPrice(new BigDecimal("1000.00"));

        cart = new Cart();
        cart.setId(1L);
        cart.setStatus(CartStatus.CREATED);
        cart.setTotalAmount(new BigDecimal("2000.00"));
        cart.setItems(new HashSet<>());

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(new BigDecimal("1000.00"));
        cart.getItems().add(cartItem);
    }

    @AfterEach
    void tearDown() {
        // 2. Clear the context after each test to avoid polluting other tests
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create cart successfully using userDetails.getId()")
    void createCart_Returns201_WhenSuccessful() throws Exception {
        when(cartService.createCart(1L)).thenReturn(cart);

        mockMvc.perform(post(ENDPOINT_CARTS)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Should return user carts extracting user identity from Principal")
    void getUserCarts_ReturnsListOfCartResponses() throws Exception {
        // Fix: Use isNull() because the HTTP request does not send the 'status' parameter
        when(cartService.getUserCarts(eq(1L), isNull())).thenReturn(List.of(cart));

        mockMvc.perform(get(ENDPOINT_CARTS)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Should add product to cart successfully")
    void addProduct_Returns200_WhenSuccessful() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "productCode", "P01",
                "quantity", 1
        );

        when(cartService.addProduct(eq(1L), any(ProductOperationRequest.class))).thenReturn(cart);

        String requestUri = FULL_ENDPOINT_CART_PRODUCTS.replace("{" + PARAM_CART_ID + "}", "1");

        mockMvc.perform(post(requestUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                // Fix: JSON path matches the nested ProductResponse DTO
                .andExpect(jsonPath("$.items[0].product.code").value("P01"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void removeProduct_Returns200_WhenSuccessful() throws Exception {
        when(cartService.removeProduct(eq(1L), eq(1L), eq("P01"))).thenReturn(cart);

        String requestUri = (ENDPOINT_CARTS + SUB_ENDPOINT_REMOVE_PRODUCT)
                .replace("{" + PARAM_CART_ID + "}", "1")
                .replace("{" + FIELD_PRODUCT_CODE + "}", "P01");

        mockMvc.perform(delete(requestUri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getCartProducts_ReturnsListOfCartItemResponses() throws Exception {
        when(cartService.getCartProducts(eq(1L), eq(1L))).thenReturn(
                cart.getItems().stream()
                        .sorted(Comparator.comparing(CartItem::getId))
                        .toList());

        String requestUri = FULL_ENDPOINT_CART_PRODUCTS.replace("{" + PARAM_CART_ID + "}", "1");

        mockMvc.perform(get(requestUri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Fix: JSON path matches the nested ProductResponse DTO
                .andExpect(jsonPath("$[0].product.code").value("P01"))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void processOrder_Returns202_WhenSuccessful() throws Exception {
        String requestUri = (ENDPOINT_CARTS + SUB_ENDPOINT_PROCESS_ORDER)
                .replace("{" + PARAM_CART_ID + "}", "1");

        mockMvc.perform(post(requestUri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getCart_Returns200_WhenSuccessful() throws Exception {
        when(cartService.getCartById(eq(1L), eq(1L))).thenReturn(cart);

        String requestUri = (ENDPOINT_CARTS + SUB_ENDPOINT_GET_CART)
                .replace("{" + PARAM_CART_ID + "}", "1");

        mockMvc.perform(get(requestUri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}