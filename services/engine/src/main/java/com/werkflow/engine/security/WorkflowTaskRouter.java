package com.werkflow.engine.security;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for routing workflow tasks to appropriate users/groups based on Keycloak attributes.
 * Implements task assignment logic for asset request workflow and other workflows.
 */
@Service
public class WorkflowTaskRouter {

    private final KeycloakUserService keycloakUserService;

    public WorkflowTaskRouter(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    /**
     * Route task to line manager
     *
     * @param submitterUserId Submitter's Keycloak user ID
     * @return Manager's user ID, or null if not found
     */
    public String routeToLineManager(String submitterUserId) {
        // Get manager ID from submitter's profile
        String managerId = keycloakUserService.getUserAttribute(submitterUserId, "manager_id");

        if (managerId == null || managerId.isEmpty()) {
            throw new IllegalStateException("User " + submitterUserId + " has no manager assigned");
        }

        // Verify manager has approver role
        if (!keycloakUserService.hasRole(managerId, "asset_request_approver")) {
            throw new IllegalStateException("Manager " + managerId + " does not have approver role");
        }

        return managerId;
    }

    /**
     * Route task to IT department approvers
     *
     * @return List of user IDs in IT department who can approve
     */
    public List<String> routeToItDepartment() {
        List<String> itApprovers = new ArrayList<>();

        // Get users from IT Managers group
        itApprovers.addAll(keycloakUserService.getGroupMembers("/IT Department/Managers"));

        // Get users from IT POC group
        itApprovers.addAll(keycloakUserService.getGroupMembers("/IT Department/POC"));

        if (itApprovers.isEmpty()) {
            throw new IllegalStateException("No IT approvers found");
        }

        return itApprovers;
    }

    /**
     * Route task to Procurement department approvers
     *
     * @return List of user IDs in Procurement department who can approve
     */
    public List<String> routeToProcurement() {
        List<String> procurementApprovers = new ArrayList<>();

        // Get users from Procurement Managers group
        procurementApprovers.addAll(keycloakUserService.getGroupMembers("/Procurement Department/Managers"));

        // Get users from Procurement POC group
        procurementApprovers.addAll(keycloakUserService.getGroupMembers("/Procurement Department/POC"));

        if (procurementApprovers.isEmpty()) {
            throw new IllegalStateException("No procurement approvers found");
        }

        return procurementApprovers;
    }

    /**
     * Route task to Finance department based on DOA level
     *
     * @param amount Request amount
     * @return User ID of finance approver, or null if not found
     */
    public String routeToFinanceByDoa(BigDecimal amount) {
        int requiredDoaLevel = calculateRequiredDoaLevel(amount);

        // Get all members of Finance Approvers group
        List<String> financeApprovers = keycloakUserService.getGroupMembers("/Finance Department/Approvers");

        // Find first approver with sufficient DOA level
        for (String userId : financeApprovers) {
            String doaLevelStr = keycloakUserService.getUserAttribute(userId, "doa_level");
            if (doaLevelStr != null && !doaLevelStr.isEmpty()) {
                try {
                    int userDoaLevel = Integer.parseInt(doaLevelStr);
                    if (userDoaLevel >= requiredDoaLevel) {
                        return userId;
                    }
                } catch (NumberFormatException e) {
                    // Skip this user
                }
            }
        }

        throw new IllegalStateException(
            "No finance approver found with DOA level >= " + requiredDoaLevel + " for amount $" + amount
        );
    }

    /**
     * Route task to all Finance approvers with sufficient DOA level
     *
     * @param amount Request amount
     * @return List of user IDs with sufficient DOA level
     */
    public List<String> routeToFinanceByDoaMultiple(BigDecimal amount) {
        int requiredDoaLevel = calculateRequiredDoaLevel(amount);
        List<String> eligibleApprovers = new ArrayList<>();

        // Get all members of Finance Approvers group
        List<String> financeApprovers = keycloakUserService.getGroupMembers("/Finance Department/Approvers");

        // Find all approvers with sufficient DOA level
        for (String userId : financeApprovers) {
            String doaLevelStr = keycloakUserService.getUserAttribute(userId, "doa_level");
            if (doaLevelStr != null && !doaLevelStr.isEmpty()) {
                try {
                    int userDoaLevel = Integer.parseInt(doaLevelStr);
                    if (userDoaLevel >= requiredDoaLevel) {
                        eligibleApprovers.add(userId);
                    }
                } catch (NumberFormatException e) {
                    // Skip this user
                }
            }
        }

        if (eligibleApprovers.isEmpty()) {
            throw new IllegalStateException(
                "No finance approvers found with DOA level >= " + requiredDoaLevel + " for amount $" + amount
            );
        }

        return eligibleApprovers;
    }

    /**
     * Route task to specific warehouse hub manager
     *
     * @param hubId Hub identifier (e.g., "HUB_A", "CENTRAL")
     * @return User ID of hub manager
     */
    public String routeToHubManager(String hubId) {
        String groupPath = switch (hubId) {
            case "CENTRAL" -> "/Inventory Warehouse/Central Hub";
            case "HUB_A" -> "/Inventory Warehouse/Hub A";
            case "HUB_B" -> "/Inventory Warehouse/Hub B";
            default -> throw new IllegalArgumentException("Unknown hub ID: " + hubId);
        };

        List<String> hubMembers = keycloakUserService.getGroupMembers(groupPath);

        if (hubMembers.isEmpty()) {
            throw new IllegalStateException("No manager found for hub: " + hubId);
        }

        // Return first member (in real scenario, might have more complex logic)
        return hubMembers.get(0);
    }

    /**
     * Route task to department POC
     *
     * @param department Department name (e.g., "HR", "IT")
     * @return User ID of department POC
     */
    public String routeToDepartmentPoc(String department) {
        String groupPath = "/" + department + " Department/POC";

        List<String> pocMembers = keycloakUserService.getGroupMembers(groupPath);

        if (pocMembers.isEmpty()) {
            throw new IllegalStateException("No POC found for department: " + department);
        }

        // Return first POC (in real scenario, might use is_poc attribute to select)
        return pocMembers.get(0);
    }

    /**
     * Route task to all members of a department
     *
     * @param department Department name
     * @return List of user IDs
     */
    public List<String> routeToDepartment(String department) {
        String groupPath = "/" + department + " Department";
        return keycloakUserService.getGroupMembers(groupPath);
    }

    /**
     * Route task to Transport coordinators
     *
     * @return List of user IDs
     */
    public List<String> routeToTransportCoordinators() {
        return keycloakUserService.getGroupMembers("/Transport Department/Coordinators");
    }

    /**
     * Route task to users with specific role
     *
     * @param roleName Role name
     * @return List of user IDs with the role
     */
    public List<String> routeToUsersWithRole(String roleName) {
        return keycloakUserService.getUsersWithRole(roleName);
    }

    /**
     * Calculate required DOA level based on amount
     *
     * @param amount Request amount
     * @return Required DOA level (1-4)
     */
    private int calculateRequiredDoaLevel(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) {
            return 1;
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) <= 0) {
            return 2;
        } else if (amount.compareTo(BigDecimal.valueOf(100000)) <= 0) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * Get candidates for a specific workflow task
     *
     * @param workflowKey Workflow definition key
     * @param taskKey     Task definition key
     * @param variables   Process variables (may contain amount, submitter, etc.)
     * @return List of candidate user IDs
     */
    public List<String> getTaskCandidates(String workflowKey, String taskKey, java.util.Map<String, Object> variables) {
        // This would typically query workflow_role_mappings table
        // For now, implementing basic routing logic

        if ("asset_request".equals(workflowKey)) {
            return switch (taskKey) {
                case "line_manager_approval" -> {
                    String submitterId = (String) variables.get("submitter_user_id");
                    yield List.of(routeToLineManager(submitterId));
                }
                case "it_approval" -> routeToItDepartment();
                case "procurement_approval" -> routeToProcurement();
                case "finance_doa_approval" -> {
                    BigDecimal amount = (BigDecimal) variables.get("amount");
                    yield routeToFinanceByDoaMultiple(amount);
                }
                case "hub_assignment" -> {
                    String hubId = (String) variables.get("assigned_hub_id");
                    yield List.of(routeToHubManager(hubId));
                }
                default -> List.of();
            };
        }

        return List.of();
    }
}
