package com.werkflow.dto;

import com.werkflow.entity.ApprovalLevel;
import com.werkflow.entity.CapExCategory;
import com.werkflow.entity.Priority;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for CapEx request creation and update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapExRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private CapExCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Approval level is required")
    private ApprovalLevel approvalLevel;

    @Size(max = 500, message = "Business justification cannot exceed 500 characters")
    private String businessJustification;

    @Size(max = 500, message = "Expected benefits cannot exceed 500 characters")
    private String expectedBenefits;

    @NotNull(message = "Expected completion date is required")
    private LocalDate expectedCompletionDate;

    private Integer budgetYear;

    private String departmentName;
}
