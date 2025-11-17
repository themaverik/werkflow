package com.werkflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security Configuration with Keycloak OAuth2/JWT
 *
 * Procurement Roles:
 * - PROCUREMENT_ADMIN: Full access to all procurement modules
 * - PROCUREMENT_MANAGER: Manage purchase requests, vendors, purchase orders
 * - PROCUREMENT_ANALYST: Create and review purchase requests
 * - VENDOR_MANAGER: Manage vendor master data
 * - DEPARTMENT_USER: Create purchase requests for their department
 * - EMPLOYEE: View own purchase requests
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/swagger-ui/**"),
                    new AntPathRequestMatcher("/swagger-ui.html"),
                    new AntPathRequestMatcher("/actuator/**"),
                    new AntPathRequestMatcher("/flowable-ui/**"),
                    new AntPathRequestMatcher("/flowable-rest/**"),
                    new AntPathRequestMatcher("/dmn-api/**"),
                    new AntPathRequestMatcher("/form-api/**"),
                    new AntPathRequestMatcher("/content-api/**"),
                    new AntPathRequestMatcher("/cmmn-api/**")
                ).permitAll()

                // Purchase Request endpoints
                .requestMatchers(new AntPathRequestMatcher("/procurement/requests", "POST")).hasAnyRole("PROCUREMENT_ADMIN", "PROCUREMENT_ANALYST", "DEPARTMENT_USER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/requests/**", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "PROCUREMENT_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/requests/**/approve", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "PROCUREMENT_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/requests/**/reject", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "PROCUREMENT_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/requests/**", "GET")).authenticated()

                // Vendor endpoints
                .requestMatchers(new AntPathRequestMatcher("/procurement/vendors", "POST")).hasAnyRole("PROCUREMENT_ADMIN", "VENDOR_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/vendors/**", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "VENDOR_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/vendors/**/approve", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "VENDOR_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/vendors/**/reject", "PUT")).hasAnyRole("PROCUREMENT_ADMIN", "VENDOR_MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/procurement/vendors/**", "GET")).authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * Converter to extract Keycloak roles from JWT and convert to Spring Security authorities
     */
    private static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        }
    }
}
