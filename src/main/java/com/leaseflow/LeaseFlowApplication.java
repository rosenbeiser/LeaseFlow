package com.leaseflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LeaseFlow Application Entry Point.
 *
 * <p>LeaseFlow is a Smart Property Lease &amp; Utility Billing Management System.
 * It provides a Spring Boot REST backend with a polished vanilla-JS frontend
 * served from the same server.
 *
 * <p>{@code @EnableScheduling} activates the daily invoice fine/overdue scheduler
 * defined in {@code InvoiceService}.
 */
@SpringBootApplication
@EnableScheduling
public class LeaseFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaseFlowApplication.class, args);
    }
}

