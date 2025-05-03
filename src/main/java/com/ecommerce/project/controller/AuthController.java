package com.ecommerce.project.controller;

import com.ecommerce.project.exception.InvalidFormatException;
import com.ecommerce.project.exception.InvalidLengthException;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller responsible for user authentication and registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user and returns user details with JWT in a cookie.
     */
    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(createUserInfoResponse(userDetails, jwtCookie.toString()));
    }

    /**
     * Registers a new user with validated input and assigned roles.
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@RequestBody SignupRequest signUpRequest) {
        validateSignUpRequest(signUpRequest);

        if (Boolean.TRUE.equals(userRepository.existsByUserName(signUpRequest.getUsername()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        Set<Role> roles = resolveRoles(signUpRequest.getRole());
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * Returns the current authenticated user's username.
     */
    @GetMapping("/username")
    public String currentUserName(Authentication authentication) {
        return authentication != null ? authentication.getName() : "";
    }

    /**
     * Returns detailed user information without the JWT token.
     */
    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(createUserInfoResponse(userDetails, null));
    }

    /**
     * Creates a UserInfoResponse based on user details and optional JWT token.
     */
    private UserInfoResponse createUserInfoResponse(UserDetailsImpl userDetails, String jwtToken) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtToken != null
                ? new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles, jwtToken)
                : new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);
    }

    /**
     * Validates input fields from the signup request.
     */
    private void validateSignUpRequest(SignupRequest request) {
        validateField("username", request.getUsername(), 3, 20);
        validateEmail(request.getEmail());
        validateField("password", request.getPassword(), 6, 40);

        if (request.getRole() == null || request.getRole().isEmpty()) {
            throw new InvalidLengthException("The role cannot be empty!");
        }
    }

    /**
     * Validates a string field for null, empty, and character length.
     */
    private void validateField(String fieldName, String value, int minLength, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidLengthException("The " + fieldName + " cannot be empty!");
        }

        if (value.length() < minLength) {
            throw new InvalidLengthException("The " + fieldName + " must be at least " + minLength + " characters long!");
        }

        if (value.length() > maxLength) {
            throw new InvalidLengthException("The " + fieldName + " must have a maximum of " + maxLength + " characters!");
        }
    }

    /**
     * Validates email format and length.
     */
    private void validateEmail(String email) {
        validateField("email", email, 6, 50);

        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new InvalidFormatException("User", "email", email);
        }
    }

    /**
     * Resolves a set of role names into actual Role entities.
     */
    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<String> rolesInput = Optional.ofNullable(roleNames).orElse(Set.of("user"));
        Set<Role> roles = new HashSet<>();

        for (String roleName : rolesInput) {
            AppRole appRole = switch (roleName.toLowerCase()) {
                case "admin" -> AppRole.ROLE_ADMIN;
                case "seller" -> AppRole.ROLE_SELLER;
                default -> AppRole.ROLE_USER;
            };

            Role role = roleRepository.findByRoleName(appRole)
                    .orElseThrow(() -> new RuntimeException("Error: Role " + appRole.name() + " not found."));
            roles.add(role);
        }

        return roles;
    }
}