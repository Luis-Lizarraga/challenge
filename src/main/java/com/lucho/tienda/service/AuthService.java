package com.lucho.tienda.service;

import com.lucho.tienda.dto.AuthRequest;
import com.lucho.tienda.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
}