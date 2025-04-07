package com.ecommerce.project.exception;

public class InvalidLengthException extends RuntimeException {
    public InvalidLengthException(String message) {
        super(message);
    }
}