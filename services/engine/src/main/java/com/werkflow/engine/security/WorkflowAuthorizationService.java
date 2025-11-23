package com.werkflow.engine.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for workflow-specific authorization decisions.
 * Implements business rules for task assignment and approval routing.
 */
@Service
public class WorkflowAuthorizationService {

    private final KeycloakRoleExtractor roleExtractor;

    public WorkflowAuthorizationService(KeycloakRoleExtractor roleExtractor) {
        this.roleExtractor = roleExtractor;
    }

    /**
     * Check if user can submit asset requests
     *
     * @param jwt User's JWT token
     * @return true if authorized
     */
    public boolean canSubmitAssetRequest(Jwt jwt) {
        return roleExtractor.hasAnyRole(jwt, "asset_request_requester", "employee");
    }

    /**
     * Check if user can approve asset requests (general)
     *
     * @param jwt User's JWT token
     * @return true if authorized
     */
    public boolean canApproveAssetRequest(Jwt jwt) {
        return roleExtractor.hasRole(jwt, "asset_request_approver");
    }

    /**
     * Check if user is the line manager for a specific employee
     *
     * @param jwt            Manager's JWT token
     * @param submitterUserId Submitter's Keycloak user ID
     * @param submitterManagerId Submitter's manager ID from their profile
     * @return true if user is the manager
     */
    public boolean isLineManager(Jwt jwt, String submitterUserId, String submitterManagerId) {
        String currentUserId = roleExtractor.getUserId(jwt);
        return currentUserId.equals(submitterManagerId) &&
               roleExtractor.hasRole(jwt, "asset_request_approver");
    }

    /**
     * Check if user can approve IT department requests
     *
     * @param jwt User's JWT token
     * @return true if authorized
     */
    public boolean canApproveItRequest(Jwt jwt) {
        boolean hasRole = roleExtractor.hasRole(jwt, "asset_request_approver");
        boolean inItDepartment = roleExtractor.isMemberOfAnyGroup(jwt,
            "/IT Department/Managers",
            "/IT Department/POC"
        );
        String department = roleExtractor.getDepartment(jwt);

        return hasRole && inItDepartment && "IT".equalsIgnoreCase(department);
    }

    /**
     * Check if user can approve procurement requests
     *
     * @param jwt User's JWT token
     * @return true if authorized
     */
    public boolean canApproveProcurement(Jwt jwt) {
        boolean hasRole = roleExtractor.hasRole(jwt, "procurement_approver");
        boolean inProcurementDepartment = roleExtractor.isMemberOfAnyGroup(jwt,
            "/Procurement Department/Managers",
            "/Procurement Department/POC"
        );
        String department = roleExtractor.getDepartment(jwt);

        return hasRole && inProcurementDepartment && "Procurement".equalsIgnoreCase(department);
    }

    /**
     * Check if user can approve based on Delegation of Authority (DOA) level
     *
     * @param jwt    User's JWT token
     * @param amount Request amount
     * @return true if authorized
     */
    public boolean canApproveByDoaLevel(Jwt jwt, BigDecimal amount) {
        Integer userDoaLevel = roleExtractor.getDoaLevel(jwt);
        if (userDoaLevel == null) {
            return false;
        }

        int requiredLevel = calculateRequiredDoaLevel(amount);

        // User's DOA level must be >= required level
        boolean hasRequiredLevel = userDoaLevel >= requiredLevel;

        // Must also have corresponding role
        boolean hasDoaRole = switch (requiredLevel) {
            case 1 -> roleExtractor.hasAnyRole(jwt, "doa_approver_level1", "doa_approver_level2", "doa_approver_level3", "doa_approver_level4");
            case 2 -> roleExtractor.hasAnyRole(jwt, "doa_approver_level2", "doa_approver_level3", "doa_approver_level4");
            case 3 -> roleExtractor.hasAnyRole(jwt, "doa_approver_level3", "doa_approver_level4");
            case 4 -> roleExtractor.hasRole(jwt, "doa_approver_level4");
            default -> false;
        };

        // Must be in Finance department
        boolean inFinanceDepartment = roleExtractor.isMemberOfGroup(jwt, "/Finance Department/Approvers");

        return hasRequiredLevel && hasDoaRole && inFinanceDepartment;
    }

    /**
     * Calculate required DOA level based on amount
     *
     * @param amount Request amount
     * @return Required DOA level (1-4)
     */
    public int calculateRequiredDoaLevel(BigDecimal amount) {
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
     * Get required DOA role name for amount
     *
     * @param amount Request amount
     * @return Role name
     */
    public String getRequiredDoaRole(BigDecimal amount) {
        int level = calculateRequiredDoaLevel(amount);
        return "doa_approver_level" + level;
    }

    /**
     * Check if user can manage inventory hub
     *
     * @param jwt   User's JWT token
     * @param hubId Hub identifier
     * @return true if authorized
     */
    public boolean canManageHub(Jwt jwt, String hubId) {
        String userHubId = roleExtractor.getHubId(jwt);
        boolean isCentralHubManager = roleExtractor.hasRole(jwt, "central_hub_manager");
        boolean isHubManager = roleExtractor.hasRole(jwt, "hub_manager");

        // Central hub manager can manage all hubs
        if (isCentralHubManager) {
            return true;
        }

        // Regular hub manager can only manage their assigned hub
        return isHubManager && hubId != null && hubId.equals(userHubId);
    }

    /**
     * Check if user is department POC
     *
     * @param jwt        User's JWT token
     * @param department Department name
     * @return true if user is POC for the department
     */
    public boolean isDepartmentPoc(Jwt jwt, String department) {
        boolean hasPocRole = roleExtractor.hasRole(jwt, "department_poc");
        boolean isPocAttribute = roleExtractor.isPoc(jwt);
        String userDepartment = roleExtractor.getDepartment(jwt);

        return hasPocRole && isPocAttribute &&
               department != null && department.equalsIgnoreCase(userDepartment);
    }

    /**
     * Check if user can design/deploy workflows
     *
     * @param jwt User's JWT token
     * @return true if authorized
     */
    public boolean canDesignWorkflows(Jwt jwt) {
        return roleExtractor.hasAnyRole(jwt, "workflow_designer", "super_admin");
    }

    /**
     * Check if user is super admin
     *
     * @param jwt User's JWT token
     * @return true if super admin
     */
    public boolean isSuperAdmin(Jwt jwt) {
        return roleExtractor.hasRole(jwt, "super_admin");
    }

    /**
     * Check if user is system admin
     *
     * @param jwt User's JWT token
     * @return true if admin
     */
    public boolean isAdmin(Jwt jwt) {
        return roleExtractor.hasAnyRole(jwt, "admin", "super_admin");
    }

    /**
     * Get all roles for user (for debugging/logging)
     *
     * @param jwt User's JWT token
     * @return List of role names
     */
    public List<String> getUserRoles(Jwt jwt) {
        return roleExtractor.extractAuthorities(jwt).stream()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .toList();
    }

    /**
     * Get all groups for user
     *
     * @param jwt User's JWT token
     * @return List of group paths
     */
    public List<String> getUserGroups(Jwt jwt) {
        return roleExtractor.extractGroups(jwt);
    }

    /**
     * Get user context (for logging/audit)
     *
     * @param jwt User's JWT token
     * @return UserContext object
     */
    public UserContext getUserContext(Jwt jwt) {
        return new UserContext(
            roleExtractor.getUserId(jwt),
            roleExtractor.getUsername(jwt),
            roleExtractor.getUserEmail(jwt),
            roleExtractor.getFullName(jwt),
            roleExtractor.getDepartment(jwt),
            roleExtractor.getEmployeeId(jwt),
            roleExtractor.getDoaLevel(jwt),
            roleExtractor.isPoc(jwt),
            getUserRoles(jwt),
            getUserGroups(jwt)
        );
    }

    /**
     * User context record for passing around user information
     */
    public record UserContext(
        String userId,
        String username,
        String email,
        String fullName,
        String department,
        String employeeId,
        Integer doaLevel,
        boolean isPoc,
        List<String> roles,
        List<String> groups
    ) {
    }
}
