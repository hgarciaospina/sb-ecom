package com.ecommerce.project.exceptions;

public class InvalidLengthException extends RuntimeException {
    public InvalidLengthException(String message) {
        super(message);
    }
}