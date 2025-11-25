package com.werkflow.admin.security;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validates Delegation of Authority (DOA) levels for financial approvals.
 * DOA Levels:
 *   Level 1: $0 - $1,000
 *   Level 2: $1,000 - $10,000
 *   Level 3: $10,000 - $100,000
 *   Level 4: >$100,000
 */
@Component
public class DoaValidator {

    private static final BigDecimal LEVEL_1_MAX = BigDecimal.valueOf(1000);
    private static final BigDecimal LEVEL_2_MAX = BigDecimal.valueOf(10000);
    private static final BigDecimal LEVEL_3_MAX = BigDecimal.valueOf(100000);

    /**
     * Calculate required DOA level based on approval amount
     *
     * @param amount Approval amount
     * @return Required DOA level (1, 2, 3, or 4)
     */
    public int calculateRequiredDoaLevel(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(LEVEL_1_MAX) <= 0) {
            return 1;
        } else if (amount.compareTo(LEVEL_2_MAX) <= 0) {
            return 2;
        } else if (amount.compareTo(LEVEL_3_MAX) <= 0) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * Validate if user's DOA level is sufficient for approval amount
     *
     * @param userDoaLevel User's DOA level from JWT token
     * @param approvalAmount Approval amount
     * @return true if user is authorized, false otherwise
     */
    public boolean isAuthorizedForAmount(int userDoaLevel, BigDecimal approvalAmount) {
        int requiredLevel = calculateRequiredDoaLevel(approvalAmount);
        return userDoaLevel >= requiredLevel;
    }

    /**
     * Get approval authority description for DOA level
     *
     * @param doaLevel DOA level (1-4)
     * @return Human-readable description
     */
    public String getDoaLevelDescription(int doaLevel) {
        return switch (doaLevel) {
            case 1 -> "Up to $1,000";
            case 2 -> "$1,000 to $10,000";
            case 3 -> "$10,000 to $100,000";
            case 4 -> "Over $100,000 (Unlimited)";
            default -> "No approval authority";
        };
    }

    /**
     * Get typical role title for DOA level
     *
     * @param doaLevel DOA level (1-4)
     * @return Typical role title
     */
    public String getDoaLevelRoleTitle(int doaLevel) {
        return switch (doaLevel) {
            case 1 -> "Team Manager";
            case 2 -> "Department Head";
            case 3 -> "Senior Manager / CFO";
            case 4 -> "C-Suite Executive";
            default -> "No approval authority";
        };
    }
}
