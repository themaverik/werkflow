package com.werkflow.engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Engine Service API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.name:Werkflow Engine Service}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI engineServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(appName + " API")
                .description("""
                    Central Flowable BPM Engine for Werkflow Enterprise Platform

                    ## Overview
                    This service provides workflow orchestration capabilities for all departments.

                    ## Features
                    - Process Definition Management (deploy, version control)
                    - Process Instance Execution
                    - Task Management and Assignment
                    - Process Variable Management
                    - Workflow History and Monitoring

                    ## Authentication
                    All endpoints require OAuth2 JWT token from Keycloak.

                    ## Roles
                    - WORKFLOW_DESIGNER: Can deploy and manage process definitions
                    - SUPER_ADMIN: Full access to all operations
                    - Authenticated users: Can start processes and complete assigned tasks
                    """)
                .version(appVersion)
                .contact(new Contact()
                    .name("Werkflow Platform Team")
                    .email("platform@werkflow.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://werkflow.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development"),
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Docker Environment")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .schemaRequirement("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token from Keycloak OAuth2 authorization server"));
    }
}
