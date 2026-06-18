package com.loanpro.ecommerce.application.exception;

public class EmptyOrderException extends RuntimeException {
    public EmptyOrderException() {
        super("Order must contain at least one item");
    }
}
