package com.werkflow.entity;

/**
 * Vendor Status Enumeration
 * Tracks the status of a vendor in the system
 */
public enum VendorStatus {
    /**
     * Vendor is active and approved for procurement
     */
    ACTIVE,

    /**
     * Vendor is inactive but can be reactivated
     */
    INACTIVE,

    /**
     * Vendor is pending approval by procurement team
     */
    PENDING_APPROVAL,

    /**
     * Vendor has been blacklisted
     */
    BLACKLISTED,

    /**
     * Vendor account is suspended
     */
    SUSPENDED,

    /**
     * Vendor is under probation/review
     */
    PROBATION
}
