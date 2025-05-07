package com.ecommerce.project.exceptions;

/**
 * Exception thrown when an input format is invalid for a specific field.
 */
public class InvalidFormatException extends RuntimeException {
    private final String resourceName;
    private final String field;
    private final String invalidValue;

    public InvalidFormatException() {
        this.resourceName = null;
        this.field = null;
        this.invalidValue = null;
    }

    public InvalidFormatException(String resourceName, String field, String invalidValue) {
        super(String.format("Invalid format for %s in %s: '%s'", field, resourceName, invalidValue));
        this.resourceName = resourceName;
        this.field = field;
        this.invalidValue = invalidValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getField() {
        return field;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
