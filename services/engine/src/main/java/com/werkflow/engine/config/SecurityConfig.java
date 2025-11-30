package com.werkflow.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for the Engine Service
 * Configures OAuth2 resource server with JWT token validation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - use AntPathRequestMatcher for multiple servlet contexts
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()

                // Process definition endpoints - require workflow designer role
                .requestMatchers(new AntPathRequestMatcher("/api/process-definitions/**", HttpMethod.POST.name())).hasAnyRole("WORKFLOW_DESIGNER", "SUPER_ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/process-definitions/**", HttpMethod.DELETE.name())).hasAnyRole("WORKFLOW_DESIGNER", "SUPER_ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/process-definitions/**", HttpMethod.GET.name())).authenticated()

                // Process instance endpoints - authenticated users
                .requestMatchers(new AntPathRequestMatcher("/api/process-instances/**")).authenticated()

                // Task endpoints - authenticated users (task assignment handles authorization)
                .requestMatchers(new AntPathRequestMatcher("/api/tasks/**")).authenticated()

                // Workflow task endpoints - authenticated users (authorization by query filters)
                .requestMatchers(new AntPathRequestMatcher("/workflows/tasks/**")).authenticated()

                // Process monitoring endpoints - authenticated users (authorization by service layer)
                .requestMatchers(new AntPathRequestMatcher("/workflows/processes/**")).authenticated()

                // History endpoints - authenticated users
                .requestMatchers(new AntPathRequestMatcher("/api/history/**")).authenticated()

                // Werkflow custom API endpoints - authenticated users
                .requestMatchers(new AntPathRequestMatcher("/werkflow/api/**")).authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<String> realmRoles = realmAccess != null
                ? (Collection<String>) realmAccess.get("roles")
                : List.of();

            // Extract resource roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Collection<String> resourceRoles = List.of();
            if (resourceAccess != null) {
                Map<String, Object> resource = (Map<String, Object>) resourceAccess.get("werkflow-engine");
                if (resource != null) {
                    resourceRoles = (Collection<String>) resource.get("roles");
                }
            }

            // Combine all roles and add ROLE_ prefix
            return realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
        });

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:4000",  // Admin Portal
            "http://localhost:4001"   // HR Portal
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
