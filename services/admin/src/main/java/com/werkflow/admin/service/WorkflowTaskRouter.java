package com.werkflow.admin.service;

import com.werkflow.admin.security.JwtClaimsExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Routes approval tasks to appropriate users based on workflow type,
 * amount, and DOA (Delegation of Authority) levels.
 *
 * Routing priorities:
 * 1. Line Manager (DOA Level 1, <$1K)
 * 2. Department Head (DOA Level 2, <$10K)
 * 3. Finance Manager (DOA Level 3, <$100K)
 * 4. CFO/CEO (DOA Level 4, unlimited)
 */
@Service
public class WorkflowTaskRouter {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskRouter.class);

    private final JwtClaimsExtractor jwtClaimsExtractor;

    public WorkflowTaskRouter(JwtClaimsExtractor jwtClaimsExtractor) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    /**
     * Route a CapEx approval task to the appropriate approver.
     *
     * @param requestAmount The CapEx request amount
     * @param requestorDepartment The department of the requester
     * @param currentApproverDoaLevel The DOA level of the current task assignee
     * @return Task assignment info with userId/groupName
     */
    public TaskAssignment routeCapExApprovalTask(
        BigDecimal requestAmount,
        String requestorDepartment,
        Integer currentApproverDoaLevel) {

        int requiredDoaLevel = calculateRequiredDoaLevel(requestAmount);
        String taskName = "CapEx Approval";

        // If current approver has sufficient authority, assign to them
        if (currentApproverDoaLevel != null && currentApproverDoaLevel >= requiredDoaLevel) {
            logger.info(
                "CapEx request for ${} ($${}) routed to approver with DOA Level {}",
                taskName, requestAmount, currentApproverDoaLevel
            );
            return new TaskAssignment(null, "finance_approvers", requiredDoaLevel);
        }

        // Otherwise, escalate to higher authority
        String escalationRole = getEscalationRole(requiredDoaLevel, requestorDepartment);
        logger.warn(
            "CapEx request for ${} escalated from DOA Level {} to Level {} (role: {})",
            taskName, currentApproverDoaLevel, requiredDoaLevel, escalationRole
        );

        return new TaskAssignment(null, escalationRole, requiredDoaLevel);
    }

    /**
     * Route a Procurement approval task.
     *
     * @param requestAmount The purchase order amount
     * @param vendorId The vendor ID
     * @return Task assignment info
     */
    public TaskAssignment routeProcurementApprovalTask(
        BigDecimal requestAmount,
        String vendorId) {

        int requiredDoaLevel = calculateRequiredDoaLevel(requestAmount);
        String taskName = "Procurement Approval";

        logger.info(
            "Procurement request for ${} with vendor {} routed to DOA Level {} approver",
            taskName, vendorId, requiredDoaLevel
        );

        String escalationRole = getEscalationRole(requiredDoaLevel, "Procurement");
        return new TaskAssignment(null, escalationRole, requiredDoaLevel);
    }

    /**
     * Route an Asset Transfer approval task.
     *
     * @param assetValue The asset value
     * @param fromHub The source hub/warehouse
     * @param toHub The destination hub/warehouse
     * @return Task assignment info
     */
    public TaskAssignment routeAssetTransferApprovalTask(
        BigDecimal assetValue,
        String fromHub,
        String toHub) {

        int requiredDoaLevel = calculateRequiredDoaLevel(assetValue);
        String taskName = "Asset Transfer Approval";

        logger.info(
            "Asset transfer from {} to {} (value: ${}) routed to DOA Level {} approver",
            fromHub, toHub, assetValue, requiredDoaLevel
        );

        String escalationRole = getEscalationRole(requiredDoaLevel, "Inventory");
        return new TaskAssignment(null, escalationRole, requiredDoaLevel);
    }

    /**
     * Calculate required DOA level based on amount.
     * DOA Levels:
     * - Level 1: < $1,000
     * - Level 2: $1,000 - $9,999
     * - Level 3: $10,000 - $99,999
     * - Level 4: $100,000+
     *
     * @param amount The request amount
     * @return DOA level (1-4)
     */
    public int calculateRequiredDoaLevel(BigDecimal amount) {
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
     * Get the escalation role/group for a DOA level and department.
     *
     * @param doaLevel The required DOA level (1-4)
     * @param department The department name
     * @return Group name for task assignment
     */
    private String getEscalationRole(int doaLevel, String department) {
        Map<Integer, String> roleMap = new HashMap<>();
        roleMap.put(1, "department_managers");
        roleMap.put(2, "department_heads");
        roleMap.put(3, "finance_approvers");
        roleMap.put(4, "executive_approvers");

        return roleMap.getOrDefault(doaLevel, "finance_approvers");
    }

    /**
     * Task assignment DTO containing routing decision.
     */
    public static class TaskAssignment {
        private String userId;
        private String candidateGroup;
        private Integer requiredDoaLevel;
        private String routingReason;

        public TaskAssignment(String userId, String candidateGroup, Integer requiredDoaLevel) {
            this.userId = userId;
            this.candidateGroup = candidateGroup;
            this.requiredDoaLevel = requiredDoaLevel;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getCandidateGroup() {
            return candidateGroup;
        }

        public void setCandidateGroup(String candidateGroup) {
            this.candidateGroup = candidateGroup;
        }

        public Integer getRequiredDoaLevel() {
            return requiredDoaLevel;
        }

        public void setRequiredDoaLevel(Integer requiredDoaLevel) {
            this.requiredDoaLevel = requiredDoaLevel;
        }

        public String getRoutingReason() {
            return routingReason;
        }

        public void setRoutingReason(String routingReason) {
            this.routingReason = routingReason;
        }
    }
}
