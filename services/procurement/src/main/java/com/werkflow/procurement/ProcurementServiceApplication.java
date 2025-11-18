package com.werkflow.procurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ProcurementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementServiceApplication.class, args);
    }
}
