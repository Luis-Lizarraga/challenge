package com.lucho.tienda.service.impl;

import com.lucho.tienda.dto.AuthRequest;
import com.lucho.tienda.dto.AuthResponse;
import com.lucho.tienda.security.JwtUtils;
import com.lucho.tienda.security.UserDetailsImpl;
import com.lucho.tienda.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // Extract the custom object that already holds the user ID
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Zero extra database calls required here
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("USER");
        String token = jwtUtils.generateToken(userDetails.getUsername(), userDetails.getId(), role);

        return new AuthResponse(token);
    }
}