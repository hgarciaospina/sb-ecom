package com.ecommerce.project.exceptions;

import com.ecommerce.project.payload.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> handleCategoryNotFoundException(ResourceNotFoundException ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<APIResponse> handleInvalidFormatException(InvalidFormatException ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidLengthException.class)
    public ResponseEntity<APIResponse> handleInvalidCategoryException(InvalidLengthException ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(DuplicateValueException.class)
    public ResponseEntity<APIResponse> handleDuplicateValueException(DuplicateValueException ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> handleGeneralException(Exception ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> handleAPIException(APIException ex) {
        String message = ex.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponse> handleBadCredentialsException(BadCredentialsException ex) {
        APIResponse apiResponse = new APIResponse("Invalid username or password", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

}