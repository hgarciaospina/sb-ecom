package com.ecommerce.project.security.services;

import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of") // Creates a static factory method: UserDetailsImpl.of(...)
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Equals & hashCode only use the 'id' field
public class UserDetailsImpl implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EqualsAndHashCode.Include
    private final Long id; // User ID

    private final  String username; // Username used for authentication
    private final  String email;    // User email

    @JsonIgnore
    private final  String password; // Password is hidden from JSON serialization

    private final Collection<? extends GrantedAuthority> authorities; // User roles/permissions

    /**
     * Factory method to build a UserDetailsImpl from a User entity.
     *
     * @param user the User entity from the database
     * @return a new UserDetailsImpl instance
     */
    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::toGrantedAuthority) // Use helper method to convert role to authority
                .toList();

        return UserDetailsImpl.of(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    // The following methods indicate that the user's account is active and not expired or locked.

    @Override
    public boolean isAccountNonExpired() {
        return true; // Account is valid and not expired
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Account is not locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credentials are valid and not expired
    }

    @Override
    public boolean isEnabled() {
        return true; // Account is enabled
    }
}