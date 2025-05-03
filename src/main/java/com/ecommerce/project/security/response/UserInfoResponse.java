package com.ecommerce.project.security.response;

import lombok.*;

import java.util.List;

/**
 * Response object that contains user info returned from authentication or user-related endpoints.
 */
@NoArgsConstructor
@Getter
@Setter
public class UserInfoResponse {
    private Long id;
    private String username;
    private List<String> roles;

    // Optional field: jwtToken (can be null)
    private String jwtToken;

    /**
     * Constructor used when JWT token is also required.
     */
    public UserInfoResponse(Long id, String username, List<String> roles, String jwtToken) {
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.jwtToken = jwtToken;
    }

    /**
     * Constructor used when JWT token should not be included.
     */
    public UserInfoResponse(Long id, String username, List<String> roles) {
        this(id, username, roles, null);
    }
}