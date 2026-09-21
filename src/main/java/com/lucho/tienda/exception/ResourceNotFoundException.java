package com.lucho.tienda.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}