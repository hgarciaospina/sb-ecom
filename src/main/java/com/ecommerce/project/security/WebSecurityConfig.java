package com.ecommerce.project.security;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.AuthEntryPointJwt;
import com.ecommerce.project.security.jwt.AuthTokenFilter;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;

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
 * Configuration class for Spring Security.
 * <p>
 * This class sets up the security context for the application, including:
 * - Password encoding
 * - JWT token filter
 * - Authentication provider
 * - Authorization rules
 * - In-memory initialization of default roles and users
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    // Public endpoints that do not require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**", "/v3/api-docs/**", "/h2-console/**",
            "/api/public/**", "/swagger-ui/**", "/api/test/**", "/images/**"
    };

    /**
     * Provides the JWT authentication token filter.
     *
     * @return a new instance of {@link AuthTokenFilter}
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * Configures and provides the authentication provider.
     * Uses a custom {@link UserDetailsServiceImpl} and a BCrypt password encoder.
     *
     * @param userDetailsService the user details service
     * @param passwordEncoder    the password encoder
     * @return a configured {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Provides the authentication manager for the application.
     *
     * @param authConfig the authentication configuration
     * @return the authentication manager
     * @throws Exception if any error occurs
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Provides the password encoder using BCrypt hashing.
     *
     * @return a new instance of {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http                  the HTTP security builder
     * @param unauthorizedHandler   handler for unauthorized access attempts
     * @param authenticationProvider the authentication provider
     * @param authTokenFilter       the JWT token filter
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if any error occurs
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthEntryPointJwt unauthorizedHandler,
                                           DaoAuthenticationProvider authenticationProvider,
                                           AuthTokenFilter authTokenFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * Excludes Swagger and static resources from Spring Security.
     *
     * @return a configured {@link WebSecurityCustomizer}
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/v2/api-docs", "/configuration/ui", "/swagger-resources/**",
                "/configuration/security", "/swagger-ui.html", "/webjars/**"
        );
    }

    /**
     * Initializes default roles and users on application startup.
     *
     * @param roleRepository    the role repository
     * @param userRepository    the user repository
     * @param passwordEncoder   the password encoder
     * @return a {@link CommandLineRunner} for initial data setup
     */
    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Role userRole = getOrCreateRole(roleRepository, AppRole.ROLE_USER);
            Role sellerRole = getOrCreateRole(roleRepository, AppRole.ROLE_SELLER);
            Role adminRole = getOrCreateRole(roleRepository, AppRole.ROLE_ADMIN);

            Set<Role> userRoles = Set.of(userRole);
            Set<Role> sellerRoles = Set.of(sellerRole);
            Set<Role> adminRoles = Set.of(userRole, sellerRole, adminRole);

            createUserIfNotExists(userRepository, "user1", "user1@example.com", passwordEncoder.encode("password1"), userRoles);
            createUserIfNotExists(userRepository, "seller1", "seller1@example.com", passwordEncoder.encode("password2"), sellerRoles);
            createUserIfNotExists(userRepository, "admin", "admin@example.com", passwordEncoder.encode("adminPass"), adminRoles);
        };
    }

    /**
     * Retrieves a role from the database or creates it if it doesn't exist.
     *
     * @param roleRepository the role repository
     * @param roleName       the name of the role
     * @return the existing or newly created {@link Role}
     */
    private Role getOrCreateRole(RoleRepository roleRepository, AppRole roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }

    /**
     * Creates a user with the given details if the username is not already registered.
     * If the user exists, their roles are updated.
     *
     * @param userRepository   the user repository
     * @param username         the username
     * @param email            the user's email
     * @param encodedPassword  the hashed password
     * @param roles            the set of roles to assign
     */
    private void createUserIfNotExists(UserRepository userRepository, String username, String email, String encodedPassword, Set<Role> roles) {
        userRepository.findByUserName(username).ifPresentOrElse(
                user -> {
                    user.setRoles(roles);
                    userRepository.save(user);
                },
                () -> {
                    User user = new User(username, email, encodedPassword);
                    user.setRoles(roles);
                    userRepository.save(user);
                }
        );
    }
}