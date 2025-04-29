package com.ecommerce.project.security;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.AuthEntryPointJwt;
import com.ecommerce.project.security.jwt.AuthTokenFilter;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

/**
 * Configuration class for Spring Security setup.
 * Defines password encoding, authentication manager, filter chain,
 * security exclusions, and initial role/user creation logic.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**", "/v3/api-docs/**", "/h2-console/**",
            "/swagger-ui/**", "/api/test/**", "/images/**"
    };

    /**
     * Bean for the JWT authentication token filter.
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * Configures the authentication provider with user details service and password encoder.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Provides the authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the password encoder using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the HTTP security settings.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * Ignores security for specific Swagger and static resource paths.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/v2/api-docs", "/configuration/ui", "/swagger-resources/**",
                "/configuration/security", "/swagger-ui.html", "/webjars/**"
        );
    }

    /**
     * Initializes default roles and users in the database if they don't already exist.
     */
    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create or retrieve application roles
            Role userRole = getOrCreateRole(roleRepository, AppRole.ROLE_USER);
            Role sellerRole = getOrCreateRole(roleRepository, AppRole.ROLE_SELLER);
            Role adminRole = getOrCreateRole(roleRepository, AppRole.ROLE_ADMIN);

            // Define sets of roles for each default user
            Set<Role> userRoles = Set.of(userRole);
            Set<Role> sellerRoles = Set.of(sellerRole);
            Set<Role> adminRoles = Set.of(userRole, sellerRole, adminRole);

            // Create default users if they do not exist
            createUserIfNotExists(userRepository, "user1", "user1@example.com", passwordEncoder.encode("password1"), userRoles);
            createUserIfNotExists(userRepository, "seller1", "seller1@example.com", passwordEncoder.encode("password2"), sellerRoles);
            createUserIfNotExists(userRepository, "admin", "admin@example.com", passwordEncoder.encode("adminPass"), adminRoles);
        };
    }

    /**
     * Retrieves a role by name or creates it if it doesn't exist.
     *
     * @param roleRepository the role repository
     * @param roleName       the application role name
     * @return the existing or newly created role
     */
    private Role getOrCreateRole(RoleRepository roleRepository, AppRole roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }

    /**
     * Creates a user with the specified encoded password and roles
     * or updates roles if the user already exists.
     *
     * @param userRepository  the user repository
     * @param username        the username
     * @param email           the email address
     * @param encodedPassword the already encoded password
     * @param roles           the roles to assign
     */
    private void createUserIfNotExists(UserRepository userRepository, String username, String email, String encodedPassword, Set<Role> roles) {
        if (!userRepository.existsByUserName(username)) {
            User user = new User(username, email, encodedPassword);
            user.setRoles(roles);
            userRepository.save(user);
        } else {
            userRepository.findByUserName(username).ifPresent(user -> {
                user.setRoles(roles);
                userRepository.save(user);
            });
        }
    }
}