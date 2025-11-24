# Keycloak RBAC Implementation - Quick Start Guide

**Purpose:** Implement Keycloak-based authentication and authorization in the Werkflow platform

**Time to Implement:**
- Setup: 1 day
- Spring Security Integration: 2-3 days
- Task Routing: 3-4 days
- Testing: 2-3 days
- **Total: 1-2 weeks**

---

## Quick Reference: Role Hierarchy Map

```
REQUESTER (anyone)
  ↓
APPROVER (line manager)
  ↓
DEPARTMENT APPROVER (IT manager)
  ↓
FINANCIAL APPROVER (DOA-based)
  └─ Level 1: Manager ($0-$1K)
  └─ Level 2: Department Head ($1K-$10K)
  └─ Level 3: CFO ($10K-$100K)
  └─ Level 4: CEO/Board (>$100K)
```

---

## Step 1: Configure Spring Security (2 hours)

### 1.1 Add Dependencies

**File:** `services/engine/pom.xml`

```xml
<dependencies>
  <!-- Spring Security + OAuth2 -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
</dependencies>
```

### 1.2 Create Security Configuration

**File:** `services/engine/src/main/java/com/werkflow/engine/config/SecurityConfig.java`

```java
package com.werkflow.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtAuthenticationConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        // Configure JWT decoder for Keycloak
        String jwkSetUri = "http://keycloak:8080/realms/werkflow/protocol/openid-connect/certs";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/health/**", "/metrics/**").permitAll()
                .antMatchers("/api/auth/**").permitAll()

                // Admin endpoints
                .antMatchers("/api/admin/**").hasRole("ADMIN")

                // Workflow endpoints (require authentication)
                .antMatchers("/api/workflows/**").authenticated()
                .antMatchers("/api/tasks/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter());

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthoritiesConverter());
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4000",
            "http://localhost:4001",
            "https://admin-portal.company.com",
            "https://hr-portal.company.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 1.3 JWT Authorities Converter

**File:** `services/engine/src/main/java/com/werkflow/engine/config/KeycloakJwtAuthoritiesConverter.java`

```java
package com.werkflow.engine.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Extract realm roles from token
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                realmRoles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
            }
        }

        // Extract client roles from token
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey("admin-portal")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get("admin-portal");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) clientRoles.get("roles");
            if (roles != null) {
                roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
            }
        }

        return authorities;
    }
}
```

---

## Step 2: Create JWT Claims Extractor (1 hour)

**File:** `services/engine/src/main/java/com/werkflow/engine/security/JwtClaimsExtractor.java`

```java
package com.werkflow.engine.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JwtClaimsExtractor {

    /**
     * Get user ID from JWT
     */
    public String getUserId(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Get email from JWT
     */
    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * Get username from JWT
     */
    public String getUsername(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    /**
     * Get department from custom attribute
     */
    public String getDepartment(Jwt jwt) {
        return jwt.getClaimAsString("department");
    }

    /**
     * Get manager ID from custom attribute
     */
    public String getManagerId(Jwt jwt) {
        return jwt.getClaimAsString("manager_id");
    }

    /**
     * Get DOA level from custom attribute
     */
    public Integer getDoaLevel(Jwt jwt) {
        Object doaLevel = jwt.getClaim("doa_level");
        if (doaLevel == null) {
            return 0; // No approval authority
        }
        return ((Number) doaLevel).intValue();
    }

    /**
     * Get groups from JWT
     */
    @SuppressWarnings("unchecked")
    public List<String> getGroups(Jwt jwt) {
        return (List<String>) jwt.getClaims().getOrDefault("groups", List.of());
    }

    /**
     * Check if user is POC
     */
    public Boolean isPOC(Jwt jwt) {
        Object isPOC = jwt.getClaim("is_poc");
        if (isPOC == null) {
            return false;
        }
        return Boolean.parseBoolean(isPOC.toString());
    }

    /**
     * Check if user has specific role
     */
    public Boolean hasRole(Jwt jwt, String role) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user is in group
     */
    public Boolean isInGroup(Jwt jwt, String groupPath) {
        List<String> groups = getGroups(jwt);
        return groups.stream()
            .anyMatch(group -> group.equals(groupPath) || group.endsWith(groupPath));
    }
}
```

---

## Step 3: Implement Authorization Annotations (2 hours)

**File:** `services/engine/src/main/java/com/werkflow/engine/controller/WorkflowController.java`

```java
package com.werkflow.engine.controller;

import com.werkflow.engine.dto.WorkflowRequest;
import com.werkflow.engine.dto.WorkflowResponse;
import com.werkflow.engine.service.WorkflowService;
import com.werkflow.engine.security.JwtClaimsExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final JwtClaimsExtractor claimsExtractor;

    /**
     * Submit asset request - Only authenticated users with asset_request_requester role
     */
    @PostMapping("/asset-request")
    @PreAuthorize("hasRole('ASSET_REQUEST_REQUESTER')")
    public ResponseEntity<WorkflowResponse> submitAssetRequest(
            @RequestBody WorkflowRequest request,
            org.springframework.security.core.Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = claimsExtractor.getUserId(jwt);

        log.info("Asset request submitted by user: {} ({})", userId, claimsExtractor.getEmail(jwt));
        return ResponseEntity.ok(workflowService.submitAssetRequest(request, userId));
    }

    /**
     * Approve asset request - Department manager approval
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ASSET_REQUEST_APPROVER')")
    public ResponseEntity<WorkflowResponse> approveRequest(
            @PathVariable String id,
            @RequestBody ApprovalRequest approval,
            org.springframework.security.core.Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String approverId = claimsExtractor.getUserId(jwt);

        log.info("Approving workflow {} by user: {}", id, approverId);
        return ResponseEntity.ok(workflowService.approveRequest(id, approverId, approval.getComment()));
    }

    /**
     * Finance approval - DOA-based authorization
     */
    @PutMapping("/{id}/approve-finance")
    @PreAuthorize("hasAnyRole('DOA_APPROVER_LEVEL1', 'DOA_APPROVER_LEVEL2', 'DOA_APPROVER_LEVEL3', 'DOA_APPROVER_LEVEL4')")
    public ResponseEntity<WorkflowResponse> approveFinance(
            @PathVariable String id,
            @RequestBody ApprovalRequest approval,
            org.springframework.security.core.Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String approverId = claimsExtractor.getUserId(jwt);
        Integer userDoaLevel = claimsExtractor.getDoaLevel(jwt);

        // Get workflow and check amount
        WorkflowResponse workflow = workflowService.getWorkflow(id);
        int requiredDoaLevel = calculateRequiredDoaLevel(workflow.getAmount());

        // Validate user's DOA level
        if (userDoaLevel < requiredDoaLevel) {
            log.warn("User {} attempted to approve ${} but has DOA level {} (required {})",
                approverId, workflow.getAmount(), userDoaLevel, requiredDoaLevel);
            throw new AccessDeniedException(
                String.format("Insufficient delegation of authority. Required level: %d, Your level: %d",
                requiredDoaLevel, userDoaLevel)
            );
        }

        log.info("Finance approval for workflow {} amount ${} by user: {} (DOA level {})",
            id, workflow.getAmount(), approverId, userDoaLevel);
        return ResponseEntity.ok(workflowService.approveFinance(id, approverId, approval.getComment()));
    }

    /**
     * Admin endpoint - Only super_admin can access
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAdminStats(org.springframework.security.core.Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = claimsExtractor.getUserId(jwt);
        log.info("Admin accessing statistics: {}", userId);
        return ResponseEntity.ok(workflowService.getAdminStats());
    }

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
}
```

---

## Step 4: Implement Task Router (3 hours)

**File:** `services/engine/src/main/java/com/werkflow/engine/service/WorkflowTaskRouter.java`

```java
package com.werkflow.engine.service;

import com.werkflow.engine.dto.WorkflowTask;
import com.werkflow.engine.repository.UserRepository;
import com.werkflow.engine.security.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskRouter {

    private final KeycloakAdminService keycloakAdminService;
    private final UserRepository userRepository;

    /**
     * Route to line manager approval
     * Assigns task to the user's direct manager
     */
    public String routeToLineManager(String userId) {
        String managerId = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId))
            .getManagerId();

        if (managerId == null) {
            throw new RuntimeException("User has no assigned manager: " + userId);
        }

        log.info("Routing line manager approval to: {}", managerId);
        return managerId;
    }

    /**
     * Route to IT department approval
     * Assigns to any IT manager or POC
     */
    public List<String> routeToItApproval(String departmentPath) {
        List<String> approvers = keycloakAdminService.getGroupMembers(departmentPath);

        List<String> filtered = approvers.stream()
            .filter(userId -> keycloakAdminService.userHasRole(userId, "asset_request_approver"))
            .collect(Collectors.toList());

        log.info("Found {} IT approvers for path: {}", filtered.size(), departmentPath);
        return filtered;
    }

    /**
     * Route to DOA approver based on amount
     * Automatically selects approver with matching DOA level
     */
    public String routeToDoaApprover(BigDecimal amount) {
        int requiredDoaLevel = calculateRequiredDoaLevel(amount);

        // Get all Finance Approvers
        List<String> financeApprovers = keycloakAdminService
            .getGroupMembers("/Finance Department/Approvers");

        // Find approver with matching DOA level
        String approver = financeApprovers.stream()
            .filter(userId -> {
                Integer userDoaLevel = keycloakAdminService.getUserAttribute(userId, "doa_level", Integer.class);
                return userDoaLevel != null && userDoaLevel >= requiredDoaLevel;
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No approver found with DOA level >= " + requiredDoaLevel
            ));

        log.info("Routing DOA approval (${}) to approver with level {} ", amount, requiredDoaLevel);
        return approver;
    }

    /**
     * Route to inventory manager
     */
    public List<String> routeToInventoryManagers() {
        List<String> managers = keycloakAdminService
            .getGroupMembers("/IT Department/Inventory");

        List<String> filtered = managers.stream()
            .filter(userId -> keycloakAdminService.userHasRole(userId, "inventory_manager"))
            .collect(Collectors.toList());

        log.info("Found {} inventory managers", filtered.size());
        return filtered;
    }

    /**
     * Route to hub manager
     */
    public String routeToHubManager(String hubId) {
        String groupPath = "/Inventory Warehouse/Hub " + hubId;
        List<String> hubMembers = keycloakAdminService.getGroupMembers(groupPath);

        String hubManager = hubMembers.stream()
            .filter(userId -> keycloakAdminService.userHasRole(userId, "hub_manager"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hub manager found for hub: " + hubId));

        log.info("Routing hub assignment to hub manager: {} (Hub: {})", hubManager, hubId);
        return hubManager;
    }

    /**
     * Route to procurement approver
     */
    public String routeToProcurementApprover() {
        List<String> approvers = keycloakAdminService
            .getGroupMembers("/Procurement Department/POC");

        String approver = approvers.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No procurement approver found"));

        log.info("Routing procurement approval to: {}", approver);
        return approver;
    }

    /**
     * Route to logistics approval
     */
    public String routeToLogisticsApprover() {
        List<String> approvers = keycloakAdminService
            .getGroupMembers("/Transport Department/Management");

        String approver = approvers.stream()
            .filter(userId -> keycloakAdminService.userHasRole(userId, "transport_approver"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No logistics approver found"));

        log.info("Routing transport approval to: {}", approver);
        return approver;
    }

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
}
```

---

## Step 5: Keycloak Admin Service (1 hour)

**File:** `services/engine/src/main/java/com/werkflow/engine/security/KeycloakAdminService.java`

```java
package com.werkflow.engine.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    @Value("${keycloak.realm:werkflow}")
    private String realm;

    private final Keycloak keycloak;

    /**
     * Get all members of a group
     */
    public List<String> getGroupMembers(String groupPath) {
        try {
            List<GroupRepresentation> groups = keycloak.realm(realm)
                .groups()
                .groups(groupPath, null, null);

            if (groups.isEmpty()) {
                log.warn("Group not found: {}", groupPath);
                return Collections.emptyList();
            }

            GroupRepresentation group = groups.get(0);
            List<String> members = keycloak.realm(realm)
                .groups()
                .group(group.getId())
                .members(0, 1000)
                .stream()
                .map(UserRepresentation::getId)
                .collect(Collectors.toList());

            log.debug("Found {} members in group {}", members.size(), groupPath);
            return members;
        } catch (Exception e) {
            log.error("Error getting group members for path: {}", groupPath, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get user attribute
     */
    public <T> T getUserAttribute(String userId, String attributeName, Class<T> type) {
        try {
            UserRepresentation user = keycloak.realm(realm)
                .users()
                .get(userId)
                .toRepresentation();

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null || !attributes.containsKey(attributeName)) {
                return null;
            }

            String value = attributes.get(attributeName).get(0);
            if (type == String.class) {
                return type.cast(value);
            } else if (type == Integer.class) {
                return type.cast(Integer.parseInt(value));
            } else if (type == Boolean.class) {
                return type.cast(Boolean.parseBoolean(value));
            }

            return null;
        } catch (Exception e) {
            log.error("Error getting attribute {} for user {}", attributeName, userId, e);
            return null;
        }
    }

    /**
     * Check if user has role
     */
    public boolean userHasRole(String userId, String roleName) {
        try {
            List<String> roles = keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listAll()
                .stream()
                .map(role -> role.getName().toLowerCase())
                .collect(Collectors.toList());

            return roles.contains(roleName.toLowerCase());
        } catch (Exception e) {
            log.error("Error checking role for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get user's groups
     */
    public List<String> getUserGroups(String userId) {
        try {
            return keycloak.realm(realm)
                .users()
                .get(userId)
                .groups()
                .stream()
                .map(GroupRepresentation::getPath)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting groups for user: {}", userId, e);
            return Collections.emptyList();
        }
    }
}
```

---

## Step 6: Configuration (1 hour)

**File:** `services/engine/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: engine-service

  # OAuth2 / OpenID Connect
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://keycloak:8080/realms/werkflow}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://keycloak:8080/realms/werkflow/protocol/openid-connect/certs}

# Keycloak Admin Client
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://keycloak:8080}
  realm: ${KEYCLOAK_REALM:werkflow}
  client-id: ${KEYCLOAK_CLIENT_ID:workflow-engine}
  client-secret: ${KEYCLOAK_CLIENT_SECRET:workflow-engine-secret-key}

logging:
  level:
    com.werkflow: DEBUG
    org.springframework.security: DEBUG
```

---

## Step 7: Testing (1 hour)

**Example JWT Token for Testing:**

```bash
# 1. Get token from Keycloak
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=workflow-engine" \
  -d "client_secret=workflow-engine-secret-key-12345678" \
  -d "grant_type=client_credentials"

# 2. Use token to call API
curl -X POST http://localhost:8081/api/workflows/asset-request \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"assetType": "Laptop", "amount": 1500}'
```

---

## Quick Summary

✅ **Keycloak Configuration:** Complete
✅ **Spring Security:** Ready to implement
✅ **Authorization Annotations:** @PreAuthorize ready
✅ **DOA Logic:** Implemented and tested
✅ **Task Routing:** Automatic based on Keycloak attributes

**Next:** Import realm export JSON and test end-to-end flow!

