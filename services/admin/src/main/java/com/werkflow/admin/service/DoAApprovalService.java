package com.werkflow.admin.service;

import com.werkflow.admin.security.DoaValidator;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Degree of Authority (DOA) approval logic.
 * Implements approval escalation based on request amounts and user DOA levels.
 *
 * DOA Levels:
 * - Level 1: $0 - $999 (Department Manager)
 * - Level 2: $1,000 - $9,999 (Department Head)
 * - Level 3: $10,000 - $99,999 (Finance Manager)
 * - Level 4: $100,000+ (Executive/CFO)
 */
@Service
public class DoAApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(DoAApprovalService.class);

    private final DoaValidator doaValidator;

    public DoAApprovalService(DoaValidator doaValidator) {
        this.doaValidator = doaValidator;
    }

    /**
     * Get the required DOA level for a request amount.
     *
     * @param amount The request amount
     * @return DOA level (1-4)
     */
    public int getRequiredApproverLevel(BigDecimal amount) {
        if (amount == null) {
            return 1;
        }

        if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return 1;
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return 2;
        } else if (amount.compareTo(BigDecimal.valueOf(100000)) < 0) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * Get the title/role for a DOA level.
     *
     * @param doaLevel The DOA level (1-4)
     * @return Role title
     */
    public String getDoALevelTitle(int doaLevel) {
        Map<Integer, String> titles = new HashMap<>();
        titles.put(1, "Department Manager");
        titles.put(2, "Department Head");
        titles.put(3, "Finance Manager");
        titles.put(4, "Executive/CFO");

        return titles.getOrDefault(doaLevel, "Unknown");
    }

    /**
     * Get the amount limit for a DOA level.
     *
     * @param doaLevel The DOA level (1-4)
     * @return Maximum approvable amount for this level
     */
    public BigDecimal getDoALevelLimit(int doaLevel) {
        Map<Integer, BigDecimal> limits = new HashMap<>();
        limits.put(1, BigDecimal.valueOf(999));
        limits.put(2, BigDecimal.valueOf(9999));
        limits.put(3, BigDecimal.valueOf(99999));
        limits.put(4, BigDecimal.valueOf(Double.MAX_VALUE));

        return limits.getOrDefault(doaLevel, BigDecimal.ZERO);
    }

    /**
     * Validate if an approver can approve a request amount.
     *
     * @param approverDoaLevel The approver's DOA level (1-4)
     * @param requestAmount The request amount
     * @return true if approver can approve
     */
    public boolean canApprove(int approverDoaLevel, BigDecimal requestAmount) {
        int requiredLevel = getRequiredApproverLevel(requestAmount);
        boolean canApprove = approverDoaLevel >= requiredLevel;

        logger.info(
            "Approval check: Approver DOA {} for amount ${} (required level {}): {}",
            approverDoaLevel, requestAmount, requiredLevel, canApprove ? "APPROVED" : "DENIED"
        );

        return canApprove;
    }

    /**
     * Check if escalation is needed.
     *
     * @param requestAmount The request amount
     * @param assignedApproverDoaLevel The current assigned approver's DOA level
     * @return true if escalation is needed
     */
    public boolean isEscalationNeeded(BigDecimal requestAmount, Integer assignedApproverDoaLevel) {
        if (assignedApproverDoaLevel == null) {
            return true;
        }

        int requiredLevel = getRequiredApproverLevel(requestAmount);
        boolean needsEscalation = assignedApproverDoaLevel < requiredLevel;

        if (needsEscalation) {
            logger.warn(
                "Escalation needed: Approver DOA {} insufficient for amount ${} (required level {})",
                assignedApproverDoaLevel, requestAmount, requiredLevel
            );
        }

        return needsEscalation;
    }

    /**
     * Get the next escalation level.
     *
     * @param currentLevel The current DOA level
     * @return Next escalation level (capped at 4)
     */
    public int getNextEscalationLevel(int currentLevel) {
        return Math.min(currentLevel + 1, 4);
    }

    /**
     * Get all DOA levels as a map with descriptions.
     *
     * @return Map of DOA levels with descriptions
     */
    public Map<Integer, DoALevelInfo> getAllDoALevels() {
        Map<Integer, DoALevelInfo> levels = new HashMap<>();

        for (int i = 1; i <= 4; i++) {
            levels.put(i, new DoALevelInfo(
                i,
                getDoALevelTitle(i),
                getDoALevelLimit(i),
                getAmountRangeForLevel(i)
            ));
        }

        return levels;
    }

    /**
     * Get the human-readable amount range for a DOA level.
     *
     * @param doaLevel The DOA level
     * @return Amount range description
     */
    private String getAmountRangeForLevel(int doaLevel) {
        switch (doaLevel) {
            case 1:
                return "$0 - $999";
            case 2:
                return "$1,000 - $9,999";
            case 3:
                return "$10,000 - $99,999";
            case 4:
                return "$100,000+";
            default:
                return "Unknown";
        }
    }

    /**
     * DoA Level information DTO.
     */
    public static class DoALevelInfo {
        private int level;
        private String title;
        private BigDecimal limit;
        private String amountRange;

        public DoALevelInfo(int level, String title, BigDecimal limit, String amountRange) {
            this.level = level;
            this.title = title;
            this.limit = limit;
            this.amountRange = amountRange;
        }

        public int getLevel() {
            return level;
        }

        public String getTitle() {
            return title;
        }

        public BigDecimal getLimit() {
            return limit;
        }

        public String getAmountRange() {
            return amountRange;
        }
    }
}
