package com.loanpro.ecommerce.application.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String message) {
        super(message);
    }
}