package com.lucho.tienda.exception;

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String message) {
        super(message);
    }
}