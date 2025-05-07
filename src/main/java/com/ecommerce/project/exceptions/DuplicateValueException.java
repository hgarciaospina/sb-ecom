package com.ecommerce.project.exceptions;

public class DuplicateValueException extends RuntimeException {
    public DuplicateValueException(String message) {
        super(message);
    }
}