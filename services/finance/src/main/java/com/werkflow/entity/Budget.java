package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Budget Entity
 * Represents budget allocations for different categories and departments
 */
@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budget_year", columnList = "budget_year"),
    @Index(name = "idx_budget_category", columnList = "category"),
    @Index(name = "idx_budget_department", columnList = "department_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @NotNull(message = "Budget year is required")
    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private CapExCategory category;

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    @Column(name = "department_name", length = 100)
    private String departmentName;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.0", message = "Allocated amount must be non-negative")
    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "utilized_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Calculates the remaining budget amount
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    public void calculateRemainingAmount() {
        if (allocatedAmount != null && utilizedAmount != null) {
            this.remainingAmount = allocatedAmount.subtract(utilizedAmount);
        }
    }
}
