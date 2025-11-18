package com.werkflow.entity;

/**
 * CapEx Request Status Enumeration
 * Tracks the status of a CapEx (Capital Expenditure) request through its lifecycle
 */
public enum CapExStatus {
    /**
     * Initial state - CapEx request has been submitted
     */
    SUBMITTED,

    /**
     * Request is under review by finance team
     */
    UNDER_REVIEW,

    /**
     * Request awaiting approval from appropriate authority
     */
    PENDING_APPROVAL,

    /**
     * Request has been approved
     */
    APPROVED,

    /**
     * Request has been rejected
     */
    REJECTED,

    /**
     * Request has been budgeted for implementation
     */
    BUDGETED,

    /**
     * CapEx is in procurement stage
     */
    IN_PROCUREMENT,

    /**
     * CapEx has been completed/acquired
     */
    COMPLETED,

    /**
     * Request has been cancelled
     */
    CANCELLED
}
