package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * CapEx Approval Entity
 * Tracks approval records for CapEx requests at different levels
 */
@Entity
@Table(name = "capex_approvals", indexes = {
    @Index(name = "idx_capex_approval_request_id", columnList = "capex_request_id"),
    @Index(name = "idx_capex_approval_level", columnList = "approval_level"),
    @Index(name = "idx_capex_approval_approver", columnList = "approver")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapExApproval extends BaseEntity {

    @NotNull(message = "CapEx request ID is required")
    @Column(name = "capex_request_id", nullable = false)
    private Long capexRequestId;

    @NotNull(message = "Approval level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 30)
    private ApprovalLevel approvalLevel;

    @NotBlank(message = "Approver is required")
    @Size(max = 255, message = "Approver cannot exceed 255 characters")
    @Column(name = "approver", nullable = false, length = 255)
    private String approver;

    @NotNull(message = "Approval status is required")
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus;  // PENDING, APPROVED, REJECTED

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Column(name = "approval_order")
    private Integer approvalOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
