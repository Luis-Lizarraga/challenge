package com.lucho.tienda.dto;

import com.lucho.tienda.constant.ErrorMessageConstants;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = ErrorMessageConstants.USERNAME_REQUIRED)
        String username,

        @NotBlank(message = ErrorMessageConstants.PASSWORD_REQUIRED)
        String password
) {}