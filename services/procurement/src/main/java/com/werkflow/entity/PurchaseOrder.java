package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Purchase Order Entity
 * Represents a purchase order (PO) in the procurement system
 */
@Entity
@Table(name = "purchase_orders", indexes = {
    @Index(name = "idx_po_number", columnList = "po_number"),
    @Index(name = "idx_po_status", columnList = "status"),
    @Index(name = "idx_po_vendor_id", columnList = "vendor_id"),
    @Index(name = "idx_po_request_id", columnList = "purchase_request_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends BaseEntity {

    @NotBlank(message = "PO number is required")
    @Size(max = 50, message = "PO number cannot exceed 50 characters")
    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber;

    @NotNull(message = "Purchase request ID is required")
    @Column(name = "purchase_request_id", nullable = false)
    private Long purchaseRequestId;

    @NotNull(message = "Vendor ID is required")
    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @NotBlank(message = "Vendor name is required")
    @Size(max = 255, message = "Vendor name cannot exceed 255 characters")
    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @NotBlank(message = "PO description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    @Column(name = "unit", length = 50)
    private String unit;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "PO amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "po_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal poAmount;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @NotNull(message = "PO date is required")
    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @NotNull(message = "Delivery date is required")
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Size(max = 255, message = "Delivery address cannot exceed 255 characters")
    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Size(max = 255, message = "Payment terms cannot exceed 255 characters")
    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "created_by_user")
    private String createdByUser;

    @Column(name = "sent_date")
    private LocalDate sentDate;

    @Column(name = "acknowledged_date")
    private LocalDate acknowledgedDate;

    @Column(name = "delivery_received_date")
    private LocalDate deliveryReceivedDate;

    @Column(name = "invoice_received_date")
    private LocalDate invoiceReceivedDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "po_closed_date")
    private LocalDate poClosedDate;

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
