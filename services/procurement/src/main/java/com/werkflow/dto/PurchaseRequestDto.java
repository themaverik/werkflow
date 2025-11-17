package com.werkflow.dto;

import com.werkflow.entity.Priority;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Purchase Request creation and update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    @NotNull(message = "Estimated unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal estimatedUnitPrice;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Required by date is required")
    private LocalDate requiredByDate;

    @Size(max = 255, message = "Department name cannot exceed 255 characters")
    private String departmentName;

    @Size(max = 500, message = "Business justification cannot exceed 500 characters")
    private String businessJustification;

    private Long preferredVendorId;
}
