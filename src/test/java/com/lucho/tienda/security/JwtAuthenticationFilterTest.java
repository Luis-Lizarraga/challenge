package com.lucho.tienda.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_SetsAuthentication_WhenTokenIsCompleteAndValid() throws Exception {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtils.parseAndValidateToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("admin");
        when(jwtUtils.getUserId(claims)).thenReturn(1L);
        when(jwtUtils.getRole(claims)).thenReturn("ADMIN");

        jwtFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("admin", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        assertEquals(1L, principal.getId());

        verify(jwtUtils, times(1)).parseAndValidateToken(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotSetAuthentication_WhenNoHeader() throws Exception {
        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotSetAuthentication_WhenHeaderDoesNotStartWithBearer() throws Exception {
        request.addHeader("Authorization", "Basic some-base64-string");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotSetAuthentication_WhenTokenIsInvalid() throws Exception {
        String token = "invalid-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtils.parseAndValidateToken(token)).thenThrow(new IllegalArgumentException("invalid JWT"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotSetAuthentication_WhenUsernameIsMissing() throws Exception {
        String token = "token-without-username";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtils.parseAndValidateToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(null);
        when(jwtUtils.getUserId(claims)).thenReturn(1L);
        when(jwtUtils.getRole(claims)).thenReturn("USER");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotSetAuthentication_WhenRoleIsMissing() throws Exception {
        String token = "token-without-role";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtils.parseAndValidateToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("lucho");
        when(jwtUtils.getUserId(claims)).thenReturn(1L);
        when(jwtUtils.getRole(claims)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNotParseToken_WhenAlreadyAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existingUser", null, new ArrayList<>())
        );

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals("existingUser", SecurityContextHolder.getContext().getAuthentication().getName());
        verifyNoInteractions(jwtUtils);
        verify(filterChain).doFilter(request, response);
    }
}
