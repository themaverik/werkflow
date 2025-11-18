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
 * Access Swagger UI at: http://localhost:8085/api/swagger-ui.html
 * Access API Docs at: http://localhost:8085/api/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("werkflow Procurement API")
                .version("1.0.0")
                .description("Procurement Management Service with Vendor, Purchase Request, and Purchase Order Capabilities\n\n" +
                    "This API provides comprehensive procurement management features including:\n" +
                    "- Purchase Request Management\n" +
                    "- Vendor Master Data Management\n" +
                    "- Purchase Order (PO) Creation and Tracking\n" +
                    "- Request for Quotations (RFQs)\n" +
                    "- BPM Workflow Integration")
                .contact(new Contact()
                    .name("werkflow Procurement Team")
                    .email("procurement@werkflow.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8085/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.werkflow.com/procurement")
                    .description("Production Server")
            ));
    }
}
