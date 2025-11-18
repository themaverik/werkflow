package com.werkflow.dto;

import com.werkflow.entity.Priority;
import com.werkflow.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Purchase Request response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestResponseDto {

    private Long id;

    private String requestNumber;

    private String title;

    private String description;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal estimatedUnitPrice;

    private BigDecimal totalAmount;

    private Priority priority;

    private RequestStatus status;

    private String requestedBy;

    private LocalDate requestDate;

    private LocalDate requiredByDate;

    private String departmentName;

    private String businessJustification;

    private String approvedBy;

    private LocalDate approvedAt;

    private String rejectionReason;

    private String rejectedBy;

    private LocalDate rejectedAt;

    private LocalDate rfqSentAt;

    private Long preferredVendorId;

    private Long selectedVendorId;

    private Long purchaseOrderId;

    private String workflowInstanceId;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
