package com.ecommerce.project.exception;

public class DuplicateValueException extends RuntimeException {
    public DuplicateValueException(String message) {
        super(message);
    }
}