package com.whrkflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 *
 * Access Swagger UI at: http://localhost:8080/api/swagger-ui.html
 * Access API Docs at: http://localhost:8080/api/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("whrkflow API")
                .version("1.0.0")
                .description("HR Management Platform with Workflow Capabilities\n\n" +
                    "This API provides comprehensive HR management features including:\n" +
                    "- Employee Management\n" +
                    "- Department Management\n" +
                    "- Leave Management\n" +
                    "- Attendance Tracking\n" +
                    "- Performance Reviews\n" +
                    "- Payroll Management\n" +
                    "- BPM Workflow Automation")
                .contact(new Contact()
                    .name("whrkflow Team")
                    .email("support@whrkflow.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.whrkflow.com")
                    .description("Production Server")
            ));
    }
}
