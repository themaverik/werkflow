package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Purchase Request Entity
 * Represents a purchase request in the procurement system
 */
@Entity
@Table(name = "purchase_requests", indexes = {
    @Index(name = "idx_pr_number", columnList = "request_number"),
    @Index(name = "idx_pr_status", columnList = "status"),
    @Index(name = "idx_pr_requested_by", columnList = "requested_by"),
    @Index(name = "idx_pr_department", columnList = "department_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest extends BaseEntity {

    @NotBlank(message = "Request number is required")
    @Size(max = 50, message = "Request number cannot exceed 50 characters")
    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    @Column(name = "unit", length = 50)
    private String unit;

    @NotNull(message = "Estimated unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @Column(name = "estimated_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedUnitPrice;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.SUBMITTED;

    @NotBlank(message = "Requested by is required")
    @Size(max = 255, message = "Requested by cannot exceed 255 characters")
    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @NotNull(message = "Request date is required")
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @NotNull(message = "Required by date is required")
    @Column(name = "required_by_date", nullable = false)
    private LocalDate requiredByDate;

    @Size(max = 255, message = "Department name cannot exceed 255 characters")
    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Size(max = 500, message = "Business justification cannot exceed 500 characters")
    @Column(name = "business_justification", length = 500)
    private String businessJustification;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDate rejectedAt;

    @Column(name = "rfq_sent_at")
    private LocalDate rfqSentAt;

    @Column(name = "preferred_vendor_id")
    private Long preferredVendorId;

    @Column(name = "selected_vendor_id")
    private Long selectedVendorId;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
