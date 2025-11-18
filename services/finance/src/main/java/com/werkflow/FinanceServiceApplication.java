package com.werkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main Application class for werkflow Finance Service
 *
 * This application provides:
 * - Capital Expenditure (CapEx) request management
 * - CapEx approval workflow tracking
 * - Budget management and allocation
 * - Financial accounting data
 * - Invoice approval tracking
 * - BPM workflow automation using Flowable
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
