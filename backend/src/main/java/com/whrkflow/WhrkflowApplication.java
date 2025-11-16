package com.whrkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main Application class for whrkflow - HR Management Platform with Workflow Capabilities
 *
 * This application provides:
 * - Employee management
 * - Department management
 * - Leave management
 * - Attendance tracking
 * - Performance reviews
 * - Payroll management
 * - BPM workflow automation using Flowable
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
public class WhrkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhrkflowApplication.class, args);
    }
}
