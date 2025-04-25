package com.ecommerce.project.controller;

import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Authenticate user with username and password, then return JWT and user info.
     *
     * @param loginRequest the login request payload
     * @return ResponseEntity with user details and JWT
     */
    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Perform authentication using the authentication manager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get authenticated user's details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generate JWT token using the user details
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        // Extract roles from user authorities
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList(); // Java 16+ alternative to Collectors.toList()

        // Build and return the response with user info and token
        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                jwtToken
        );

        return ResponseEntity.ok(response);
    }
}