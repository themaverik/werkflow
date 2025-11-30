package com.werkflow.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User context extracted from JWT token
 * Contains all user-related information needed for authorization and task routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserContext {

    private String userId;           // preferred_username
    private String email;            // email
    private String fullName;         // name
    private String department;       // department claim
    private List<String> groups;     // groups claim
    private List<String> roles;      // realm_access.roles
    private String managerId;        // manager_id claim (for delegation)
    private Integer doaLevel;        // doa_level claim (for approvals)

    /**
     * Check if user has a specific role
     * @param role Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user is in a specific group
     * @param group Group name to check
     * @return true if user is in the group
     */
    public boolean isInGroup(String group) {
        return groups != null && groups.contains(group);
    }
}
