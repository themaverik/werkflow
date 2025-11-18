package com.werkflow.dto;

import com.werkflow.entity.ApprovalLevel;
import com.werkflow.entity.CapExCategory;
import com.werkflow.entity.CapExStatus;
import com.werkflow.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for CapEx request response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapExResponseDto {

    private Long id;

    private String requestNumber;

    private String title;

    private String description;

    private CapExCategory category;

    private BigDecimal amount;

    private Priority priority;

    private CapExStatus status;

    private ApprovalLevel approvalLevel;

    private String requestedBy;

    private LocalDate requestDate;

    private LocalDate expectedCompletionDate;

    private String businessJustification;

    private String expectedBenefits;

    private Integer budgetYear;

    private String departmentName;

    private BigDecimal approvedAmount;

    private String approvedBy;

    private LocalDate approvedAt;

    private String rejectionReason;

    private String rejectedBy;

    private LocalDate rejectedAt;

    private String workflowInstanceId;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
