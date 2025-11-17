package com.werkflow.entity;

/**
 * Purchase Request Status Enumeration
 * Tracks the status of a purchase request through its lifecycle
 */
public enum RequestStatus {
    /**
     * Initial state - Purchase request has been submitted
     */
    SUBMITTED,

    /**
     * Request is under review by procurement team
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
     * RFQ has been sent to vendors
     */
    RFQ_SENT,

    /**
     * Quotes received from vendors
     */
    QUOTES_RECEIVED,

    /**
     * Vendor selected, PO ready to be created
     */
    VENDOR_SELECTED,

    /**
     * Purchase order has been created
     */
    PO_CREATED,

    /**
     * PO sent to vendor
     */
    PO_SENT,

    /**
     * PO acknowledged by vendor
     */
    PO_ACKNOWLEDGED,

    /**
     * Goods/services are being delivered
     */
    IN_DELIVERY,

    /**
     * Goods/services have been received
     */
    RECEIVED,

    /**
     * Invoice verified and payment processed
     */
    COMPLETED,

    /**
     * Request has been cancelled
     */
    CANCELLED
}
