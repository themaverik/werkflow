package com.werkflow.entity;

/**
 * Purchase Order Status Enumeration
 * Tracks the status of a purchase order through its lifecycle
 */
public enum PurchaseOrderStatus {
    /**
     * Draft - PO is being prepared
     */
    DRAFT,

    /**
     * PO has been submitted/created
     */
    SUBMITTED,

    /**
     * PO sent to vendor
     */
    SENT,

    /**
     * Vendor has acknowledged the PO
     */
    ACKNOWLEDGED,

    /**
     * PO is in fulfillment process
     */
    IN_FULFILLMENT,

    /**
     * Goods/services are being delivered
     */
    IN_DELIVERY,

    /**
     * Goods/services have been received and verified
     */
    RECEIVED,

    /**
     * Invoice received and matched with PO and receipt
     */
    INVOICED,

    /**
     * Payment has been processed
     */
    PAID,

    /**
     * PO is closed
     */
    CLOSED,

    /**
     * PO has been cancelled
     */
    CANCELLED
}
