package com.werkflow.config;

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
 * Access Swagger UI at: http://localhost:8084/api/swagger-ui.html
 * Access API Docs at: http://localhost:8084/api/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("werkflow Finance API")
                .version("1.0.0")
                .description("Finance Management Service with CapEx and Budget Capabilities\n\n" +
                    "This API provides comprehensive finance management features including:\n" +
                    "- Capital Expenditure (CapEx) Request Management\n" +
                    "- CapEx Approval Workflows\n" +
                    "- Budget Management and Allocation\n" +
                    "- Financial Accounting Data\n" +
                    "- Invoice Approvals\n" +
                    "- BPM Workflow Integration")
                .contact(new Contact()
                    .name("werkflow Finance Team")
                    .email("finance@werkflow.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8084/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.werkflow.com/finance")
                    .description("Production Server")
            ));
    }
}
