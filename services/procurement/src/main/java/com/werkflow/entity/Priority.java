package com.werkflow.entity;

/**
 * Priority Enumeration
 * Indicates the urgency/importance level of a purchase request
 */
public enum Priority {
    /**
     * Low priority - can be deferred
     */
    LOW,

    /**
     * Normal priority - standard processing
     */
    NORMAL,

    /**
     * High priority - expedited handling
     */
    HIGH,

    /**
     * Critical priority - immediate attention required
     */
    CRITICAL
}
