package com.werkflow.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeServiceAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Werkflow Finance Service API")
                .description("Financial Management, Budgets, and Expense Tracking")
                .version("1.0.0"));
    }
}
