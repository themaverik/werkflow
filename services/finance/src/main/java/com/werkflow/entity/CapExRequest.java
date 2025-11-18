package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CapEx Request Entity
 * Represents a Capital Expenditure request in the organization
 */
@Entity
@Table(name = "capex_requests", indexes = {
    @Index(name = "idx_capex_request_number", columnList = "request_number"),
    @Index(name = "idx_capex_status", columnList = "status"),
    @Index(name = "idx_capex_category", columnList = "category"),
    @Index(name = "idx_capex_requested_by", columnList = "requested_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapExRequest extends BaseEntity {

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

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private CapExCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CapExStatus status = CapExStatus.SUBMITTED;

    @NotNull(message = "Approval level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 30)
    private ApprovalLevel approvalLevel;

    @NotBlank(message = "Requested by is required")
    @Size(max = 255, message = "Requested by cannot exceed 255 characters")
    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @NotNull(message = "Request date is required")
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Size(max = 500, message = "Business justification cannot exceed 500 characters")
    @Column(name = "business_justification", length = 500)
    private String businessJustification;

    @Size(max = 500, message = "Expected benefits cannot exceed 500 characters")
    @Column(name = "expected_benefits", length = 500)
    private String expectedBenefits;

    @Column(name = "budget_year")
    private Integer budgetYear;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

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

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
