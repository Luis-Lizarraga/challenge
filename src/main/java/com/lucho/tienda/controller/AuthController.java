package com.lucho.tienda.controller;

import com.lucho.tienda.constant.ApiEndpointConstants;
import com.lucho.tienda.constant.OpenApiMessageConstants;
import com.lucho.tienda.dto.AuthRequest;
import com.lucho.tienda.dto.AuthResponse;
import com.lucho.tienda.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.lucho.tienda.constant.ApiEndpointConstants.*;

@RestController
@RequestMapping(ApiEndpointConstants.ENDPOINT_AUTH)
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = OpenApiMessageConstants.TAG_AUTH_DESC)
public class AuthController {

    private final AuthService authService;

    @PostMapping(SUB_ENDPOINT_LOGIN)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}