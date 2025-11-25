package com.werkflow.admin.controller;

import com.werkflow.admin.service.RoleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@Tag(name = "Route Configuration", description = "APIs for managing route-based role access control")
@SecurityRequirement(name = "bearer-jwt")
public class RoleConfigController {

    private final RoleConfigService roleConfigService;

    public RoleConfigController(RoleConfigService roleConfigService) {
        this.roleConfigService = roleConfigService;
    }

    /**
     * Get required roles for a specific route.
     *
     * @param routePath The route path (e.g., studio, services)
     * @return List of required roles
     */
    @GetMapping("/required-roles/{routePath}")
    @Operation(
        summary = "Get required roles for a route",
        description = "Returns the list of roles required to access a specific route"
    )
    public ResponseEntity<RoleRequirementResponse> getRequiredRolesForRoute(
        @PathVariable String routePath) {

        String normalizedPath = routePath.startsWith("/") ? routePath : "/" + routePath;
        List<String> requiredRoles = roleConfigService.getRequiredRolesForRoute(normalizedPath);

        RoleRequirementResponse response = new RoleRequirementResponse(
            normalizedPath,
            requiredRoles,
            !requiredRoles.isEmpty()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user has access to a route.
     *
     * @param routePath The route path
     * @param authentication Spring Security authentication
     * @return Whether user has required roles
     */
    @GetMapping("/has-access/{routePath}")
    @Operation(
        summary = "Check if user has access to a route",
        description = "Returns whether the current authenticated user has required roles for the route"
    )
    public ResponseEntity<AccessCheckResponse> checkRouteAccess(
        @PathVariable String routePath,
        Authentication authentication) {

        String normalizedPath = routePath.startsWith("/") ? routePath : "/" + routePath;
        boolean hasAccess = roleConfigService.userHasRequiredRoles(normalizedPath, authentication);

        if (!hasAccess) {
            roleConfigService.logAccessDenial(normalizedPath, authentication);
        }

        List<String> requiredRoles = roleConfigService.getRequiredRolesForRoute(normalizedPath);

        AccessCheckResponse response = new AccessCheckResponse(
            normalizedPath,
            hasAccess,
            requiredRoles,
            authentication != null ? authentication.getName() : "ANONYMOUS"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all configured routes and their required roles.
     *
     * @return Map of all route configurations
     */
    @GetMapping("/config")
    @Operation(
        summary = "Get all route configurations",
        description = "Returns all configured routes and their required roles"
    )
    public ResponseEntity<Map<String, Object>> getAllRouteConfigs() {
        Map<String, List<String>> routeConfigs = roleConfigService.getAllConfiguredRoutes();

        Map<String, Object> response = new HashMap<>();
        response.put("routes", routeConfigs);
        response.put("totalRoutes", routeConfigs.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Response DTO for role requirement check.
     */
    public static class RoleRequirementResponse {
        private String routePath;
        private List<String> requiredRoles;
        private boolean requiresRoles;

        public RoleRequirementResponse(String routePath, List<String> requiredRoles, boolean requiresRoles) {
            this.routePath = routePath;
            this.requiredRoles = requiredRoles;
            this.requiresRoles = requiresRoles;
        }

        public String getRoutePath() {
            return routePath;
        }

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public boolean isRequiresRoles() {
            return requiresRoles;
        }
    }

    /**
     * Response DTO for access check.
     */
    public static class AccessCheckResponse {
        private String routePath;
        private boolean hasAccess;
        private List<String> requiredRoles;
        private String username;

        public AccessCheckResponse(String routePath, boolean hasAccess, List<String> requiredRoles, String username) {
            this.routePath = routePath;
            this.hasAccess = hasAccess;
            this.requiredRoles = requiredRoles;
            this.username = username;
        }

        public String getRoutePath() {
            return routePath;
        }

        public boolean isHasAccess() {
            return hasAccess;
        }

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public String getUsername() {
            return username;
        }
    }
}
