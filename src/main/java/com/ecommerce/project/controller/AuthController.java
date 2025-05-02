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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller responsible for user authentication and registration.
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
     * Authenticates the user and returns a JWT token along with user details.
     *
     * @param loginRequest the login request containing username and password
     * @return the authenticated user's info with JWT token
     */
    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                jwtToken
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user with the provided signup request data.
     *
     * @param signUpRequest the registration request containing username, email, password, and roles
     * @return response indicating success or failure
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@RequestBody SignupRequest signUpRequest) {

        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().trim().isEmpty()) {
            throw new InvalidLengthException("The username cannot be empty !");
        }

        if (signUpRequest.getUsername().length() < 3) {
            throw new InvalidLengthException("The username must be at least 3 characters long !");
        }

        if (signUpRequest.getUsername().length() > 20) {
            throw new InvalidLengthException("The username must have a maximum of 20 characters long !");
        }

        if (signUpRequest.getEmail() == null || signUpRequest.getEmail().trim().isEmpty()) {
            throw new InvalidLengthException("The email cannot be empty !");
        }

        if (signUpRequest.getEmail().length() < 6) {
            throw new InvalidLengthException("The email must be at least 6 characters long !");
        }

        if (signUpRequest.getEmail().length() > 50) {
            throw new InvalidLengthException("The email must have a maximum of 50 characters long !");
        }

        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        if (!signUpRequest.getEmail().matches(emailRegex)) {
            throw new InvalidFormatException("User", "email", signUpRequest.getEmail());
        }

        if (signUpRequest.getPassword() == null || signUpRequest.getPassword().trim().isEmpty()) {
            throw new InvalidLengthException("The password cannot be empty !");
        }

        if (signUpRequest.getPassword().length() < 6) {
            throw new InvalidLengthException("The password must be at least 6 characters long !");
        }

        if (signUpRequest.getPassword().length() > 40) {
            throw new InvalidLengthException("The password must have a maximum of 40 characters long !");
        }

        if (Boolean.TRUE.equals(userRepository.existsByUserName(signUpRequest.getUsername()))) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if(signUpRequest.getRole().isEmpty()){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: The rol cannot be empty !"));
        }

        // Create a new user account
        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        Set<Role> roles = getRolesFromStrings(signUpRequest.getRole());
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * Resolves a set of AppRole names (as Strings) into Role entities.
     * If no roles are provided, assigns ROLE_USER by default.
     *
     * @param strRoles the role names from the signup request
     * @return a set of resolved Role entities
     */
    private Set<Role> getRolesFromStrings(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role defaultRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role USER not found."));
            roles.add(defaultRole);
            return roles;
        }

        for (String roleName : strRoles) {
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
