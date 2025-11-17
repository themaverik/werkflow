package com.werkflow.entity;

/**
 * Approval Level Enumeration
 * Tracks the approval authority level needed for a CapEx request
 */
public enum ApprovalLevel {
    /**
     * Department head approval needed
     */
    DEPARTMENT_HEAD,

    /**
     * Finance manager approval needed
     */
    FINANCE_MANAGER,

    /**
     * CFO (Chief Financial Officer) approval needed
     */
    CFO,

    /**
     * CEO approval needed
     */
    CEO,

    /**
     * Board/Executive committee approval needed
     */
    BOARD_EXECUTIVE
}
