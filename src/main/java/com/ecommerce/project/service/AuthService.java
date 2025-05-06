package com.ecommerce.project.service;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface AuthService {
    ResponseEntity<UserInfoResponse> authenticateUser(LoginRequest loginRequest);
    ResponseEntity<MessageResponse> registerUser(SignupRequest signUpRequest);
    String getCurrentUsername(Authentication authentication);
    ResponseEntity<UserInfoResponse> getUserDetails(Authentication authentication);
    ResponseEntity<MessageResponse> signOutUser();
}