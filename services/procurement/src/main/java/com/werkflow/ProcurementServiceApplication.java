package com.werkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main Application class for werkflow Procurement Service
 *
 * This application provides:
 * - Purchase Request management
 * - Vendor management and master data
 * - Purchase Order (PO) creation and tracking
 * - Request for Quotations (RFQs)
 * - BPM workflow automation using Flowable
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
public class ProcurementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementServiceApplication.class, args);
    }
}
