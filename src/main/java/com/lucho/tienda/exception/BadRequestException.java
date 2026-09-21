package com.lucho.tienda.exception;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(message);
    }
}
