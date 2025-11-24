# Keycloak RBAC System Design for Werkflow Platform

## Overview

This document describes the comprehensive Role-Based Access Control (RBAC) system for Werkflow Enterprise Platform using Keycloak as the Identity and Access Management (IAM) provider.

## Architecture

### Core Principles

1. **Keycloak as Single Source of Truth**: All user identity, roles, and group memberships are managed in Keycloak
2. **Hybrid Approach**: Groups for organizational structure, Roles for functional permissions
3. **Custom Attributes**: Extend user profiles with workflow-specific attributes (DOA level, manager ID, etc.)
4. **JWT Token-Based**: All authorization decisions based on JWT token claims
5. **Zero Trust**: Every request validated against Keycloak-issued tokens

### Component Interaction

```
┌─────────────────┐
│   Browser       │
│  (User Agent)   │
└────────┬────────┘
         │ 1. Login Request
         ▼
┌─────────────────────────────────┐
│   Keycloak (Port 8090)          │
│   - Authentication              │
│   - Token Issuance              │
│   - User/Role/Group Management  │
└────────┬────────────────────────┘
         │ 2. JWT Token (with roles, groups, custom claims)
         ▼
┌─────────────────────────────────┐
│   Admin Portal (Port 4000)      │
│   - NextAuth Integration        │
│   - Client-side Authorization   │
└────────┬────────────────────────┘
         │ 3. API Request + JWT Token
         ▼
┌─────────────────────────────────┐
│   Engine Service (Port 8081)    │
│   - Token Validation            │
│   - Role Extraction             │
│   - Task Assignment             │
│   - Workflow Routing            │
└─────────────────────────────────┘
```

## Role Organization Strategy

### Selected Approach: Hybrid (Groups + Roles)

**Groups**: Represent organizational structure (departments, teams)
**Roles**: Represent functional permissions (what actions users can perform)

#### Keycloak Group Hierarchy

```
/HR Department
├── /HR Department/Managers
├── /HR Department/Specialists
└── /HR Department/POC

/IT Department
├── /IT Department/Managers
├── /IT Department/Inventory
└── /IT Department/POC

/Finance Department
├── /Finance Department/Managers
├── /Finance Department/Approvers
├── /Finance Department/Officers
└── /Finance Department/POC

/Procurement Department
├── /Procurement Department/Managers
├── /Procurement Department/Specialists
└── /Procurement Department/POC

/Transport Department
├── /Transport Department/Managers
├── /Transport Department/Coordinators
└── /Transport Department/Drivers

/Inventory Warehouse
├── /Inventory Warehouse/Central Hub
├── /Inventory Warehouse/Hub A
└── /Inventory Warehouse/Hub B
```

#### Keycloak Realm Roles

**Global Roles**:
- `admin` - System administrator (full platform access)
- `super_admin` - Super administrator (C-Suite, organization-wide authority)
- `employee` - Base employee role (login access)

**Functional Roles**:
- `asset_request_requester` - Can submit asset requests
- `asset_request_approver` - Can approve asset requests
- `doa_approver_level1` - Approve up to $1,000
- `doa_approver_level2` - Approve up to $10,000
- `doa_approver_level3` - Approve up to $100,000
- `doa_approver_level4` - Approve unlimited amounts

**Department Head Roles** (Composite Roles):
- `hr_head` = `asset_request_approver` + `doa_approver_level1` + `employee`
- `it_head` = `asset_request_approver` + `doa_approver_level1` + `employee`
- `finance_head` = `doa_approver_level3` + `doa_approver_level4` + `employee`
- `procurement_head` = `procurement_approver` + `doa_approver_level2` + `employee`
- `transport_head` = `transport_manager` + `employee`
- `inventory_head` = `central_hub_manager` + `employee`

**Specialized Roles**:
- `department_poc` - Department point of contact
- `inventory_manager` - Manage inventory operations
- `hub_manager` - Manage warehouse hub
- `central_hub_manager` - Manage central hub
- `procurement_approver` - Approve procurement
- `procurement_specialist` - Handle procurement operations
- `transport_manager` - Manage transport
- `transport_coordinator` - Coordinate transport
- `driver` - Transport driver
- `finance_officer` - Finance operations
- `workflow_designer` - Design and deploy BPMN workflows
- `workflow_admin` - Workflow administration

#### Client-Specific Roles

**werkflow-admin-portal**:
- `admin` - Admin portal administrator
- `manager` - Department manager
- `poc` - Point of contact
- `approver` - Request approver
- `requester` - Request submitter
- `viewer` - Read-only viewer

**werkflow-engine**:
- `workflow_admin` - Workflow engine administrator
- `task_processor` - Process workflow tasks
- `event_publisher` - Publish workflow events

## Custom User Attributes

Keycloak user profiles extended with workflow-specific attributes:

| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `department` | String | User's department | "HR", "IT", "Finance" |
| `employee_id` | String | Employee identifier | "EMP001234" |
| `manager_id` | String | Keycloak user ID of manager | "a1b2c3d4-..." |
| `cost_center` | String | Cost center code | "HR-001", "IT-002" |
| `doa_level` | Integer | Delegation of Authority level (1-4) | 1, 2, 3, 4 |
| `is_poc` | Boolean | Is department POC | true, false |
| `hub_id` | String | Warehouse hub identifier | "CENTRAL", "HUB_A" |

These attributes are included in JWT tokens via custom protocol mappers.

## JWT Token Structure

Example access token issued by Keycloak:

```json
{
  "exp": 1732399200,
  "iat": 1732395600,
  "jti": "8f7e6d5c-4b3a-2190-8e7d-6c5b4a3f2e1d",
  "iss": "http://localhost:8090/realms/werkflow-platform",
  "sub": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
  "typ": "Bearer",
  "azp": "werkflow-admin-portal",
  "session_state": "x1y2z3w4-v5u6-t7s8-r9q0-p1o2n3m4l5k6",

  "email": "john.doe@company.com",
  "email_verified": true,
  "name": "John Doe",
  "preferred_username": "john.doe",
  "given_name": "John",
  "family_name": "Doe",

  "groups": [
    "/HR Department",
    "/HR Department/Managers"
  ],

  "realm_access": {
    "roles": [
      "employee",
      "asset_request_approver",
      "doa_approver_level1"
    ]
  },

  "resource_access": {
    "werkflow-admin-portal": {
      "roles": ["manager", "approver"]
    },
    "werkflow-engine": {
      "roles": ["task_processor"]
    }
  },

  "department": "HR",
  "employee_id": "EMP001234",
  "manager_id": "b2c3d4e5-f6g7-8901-b2c3-d4e5f6g78901",
  "cost_center": "HR-001",
  "doa_level": 1,
  "is_poc": false,
  "hub_id": null
}
```

## Workflow Task Role Mapping

### Asset Request Workflow

| Task | Required Roles | Group Requirements | Custom Attribute Checks |
|------|---------------|-------------------|------------------------|
| **1. Submit Request** | `asset_request_requester` OR `employee` | Any group | None |
| **2. Line Manager Approval** | `asset_request_approver` | User's manager (via `manager_id`) | `manager_id` = submitter's `manager_id` |
| **3. IT Department Approval** | `asset_request_approver` | `/IT Department/Managers` OR `/IT Department/POC` | `department` = "IT" |
| **4. Procurement Approval** (conditional) | `procurement_approver` | `/Procurement Department/POC` OR `/Procurement Department/Managers` | `department` = "Procurement" |
| **5. Finance DOA Approval** | `doa_approver_level1/2/3/4` | `/Finance Department/Approvers` | `doa_level` >= required level based on amount |
| **6. Hub Assignment** | `hub_manager` OR `central_hub_manager` | `/Inventory Warehouse/Hub*` | `hub_id` matches assignment hub |

### DOA Level Determination

```
Amount <= $1,000     → doa_level >= 1 (doa_approver_level1)
Amount <= $10,000    → doa_level >= 2 (doa_approver_level2)
Amount <= $100,000   → doa_level >= 3 (doa_approver_level3)
Amount > $100,000    → doa_level >= 4 (doa_approver_level4)
```

## Role Hierarchy

```
super_admin (all access)
  │
  ├─ department_head (all access in department)
  │   │
  │   ├─ department_poc (manage department workflows)
  │   │   │
  │   │   ├─ team_manager (manage team)
  │   │   │   │
  │   │   │   └─ team_member (execute tasks)
  │   │   │
  │   │   └─ specialist_roles (domain-specific)
  │   │       ├─ inventory_manager
  │   │       ├─ hub_manager
  │   │       ├─ procurement_specialist
  │   │       ├─ finance_officer
  │   │       └─ transport_coordinator
  │   │
  │   └─ approval_roles (can approve in department)
  │       ├─ asset_request_approver
  │       ├─ doa_approver (levels 1-4)
  │       └─ workflow_admin
  │
  └─ employee (base role)
      └─ requester (can submit requests)
```

## Security Configuration

### Spring Security Integration

The Engine Service uses Spring Security OAuth2 Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/werkflow-platform
          jwk-set-uri: http://keycloak:8080/realms/werkflow-platform/protocol/openid-connect/certs
```

### JWT Authentication Converter

Extracts roles from token claims and converts to Spring Security authorities:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Extract realm roles from "realm_access.roles"
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            realmRoles.forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
        }

        // Extract client roles from "resource_access.{clientId}.roles"
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get("werkflow-engine");
            if (clientRoles != null) {
                List<String> roles = (List<String>) clientRoles.get("roles");
                roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
            }
        }

        return authorities;
    });
    return converter;
}
```

### Endpoint Authorization

```java
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @PostMapping
    @PreAuthorize("hasRole('ASSET_REQUEST_REQUESTER') or hasRole('EMPLOYEE')")
    public ResponseEntity<?> submitAssetRequest(@RequestBody AssetRequest request) {
        // Submit logic
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ASSET_REQUEST_APPROVER')")
    public ResponseEntity<?> approveRequest(@PathVariable String id) {
        // Approval logic
    }

    @PutMapping("/{id}/approve-finance")
    @PreAuthorize("hasAnyRole('DOA_APPROVER_LEVEL1', 'DOA_APPROVER_LEVEL2', 'DOA_APPROVER_LEVEL3', 'DOA_APPROVER_LEVEL4')")
    public ResponseEntity<?> approveFinance(@PathVariable String id) {
        // Finance approval logic
    }
}
```

## Operational Guidelines

### Adding a New User

1. **Create user in Keycloak Admin Console**:
   - Navigate to Users → Add User
   - Set username, email, first name, last name
   - Set initial password (mark as temporary if needed)

2. **Assign to Groups**:
   - Go to user → Groups tab
   - Join appropriate department group (e.g., `/HR Department`)
   - Join sub-group if applicable (e.g., `/HR Department/Managers`)

3. **Assign Roles**:
   - Go to user → Role Mappings tab
   - Assign realm roles (e.g., `employee`, `asset_request_approver`)
   - Assign client roles if needed

4. **Set Custom Attributes**:
   - Go to user → Attributes tab
   - Add:
     - `department` (e.g., "HR")
     - `employee_id` (e.g., "EMP001234")
     - `manager_id` (Keycloak user ID of manager)
     - `cost_center` (e.g., "HR-001")
     - `doa_level` (1, 2, 3, or 4)
     - `is_poc` (true/false)
     - `hub_id` (if warehouse staff)

### Changing User's DOA Level

1. Navigate to user → Attributes
2. Update `doa_level` attribute value
3. Optionally update realm role:
   - Remove old `doa_approver_levelX` role
   - Add new `doa_approver_levelY` role

### Promoting User to Manager

1. Assign to manager sub-group:
   - User → Groups → Join `/[Department]/Managers`

2. Assign manager role:
   - User → Role Mappings → Assign `asset_request_approver`

3. Update other users who report to this manager:
   - For each direct report:
     - Navigate to their Attributes
     - Set `manager_id` = new manager's Keycloak user ID

### Assigning POC Responsibilities

1. Assign to POC sub-group:
   - User → Groups → Join `/[Department]/POC`

2. Assign POC role:
   - User → Role Mappings → Assign `department_poc`

3. Set POC attribute:
   - User → Attributes → Set `is_poc` = true

### Disabling User Access

1. Navigate to user in Keycloak
2. Toggle "Enabled" to OFF
3. User's existing tokens remain valid until expiration (default 1 hour)
4. To immediately revoke: Admin Console → Sessions → Logout all sessions for user

## Multi-Tenant Considerations

### Realm Isolation

For multi-tenant deployments:

1. **One Realm per Tenant**:
   - Create separate realm for each customer/organization
   - Example: `werkflow-customer1`, `werkflow-customer2`

2. **Client Isolation**:
   - Each tenant has own clients
   - Client IDs namespaced: `customer1-admin-portal`

3. **Token Validation**:
   - Services validate `iss` claim matches expected realm
   - Prevent cross-tenant token usage

### Cross-Tenant Security

- Backend services validate tenant context from token
- Database queries scoped by tenant ID
- API endpoints enforce tenant isolation

## Database Schema (Application-Side)

While Keycloak manages users, roles, and groups, the application maintains workflow-specific data:

### Workflow Role Mappings Table

```sql
CREATE TABLE workflow_role_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_key VARCHAR(255) NOT NULL,
    task_key VARCHAR(255) NOT NULL,
    required_roles TEXT[] NOT NULL,
    required_groups TEXT[],
    custom_logic VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Example data
INSERT INTO workflow_role_mappings (workflow_key, task_key, required_roles, required_groups, custom_logic) VALUES
('asset_request', 'submit_request', ARRAY['employee'], NULL, NULL),
('asset_request', 'line_manager_approval', ARRAY['asset_request_approver'], NULL, 'manager_id_match'),
('asset_request', 'it_approval', ARRAY['asset_request_approver'], ARRAY['/IT Department/Managers', '/IT Department/POC'], NULL),
('asset_request', 'finance_doa_approval', ARRAY['doa_approver_level1', 'doa_approver_level2', 'doa_approver_level3', 'doa_approver_level4'], ARRAY['/Finance Department/Approvers'], 'doa_level_check');
```

### DOA Override Table

For temporary DOA delegation:

```sql
CREATE TABLE doa_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,  -- Keycloak user ID
    override_doa_level INT NOT NULL CHECK (override_doa_level BETWEEN 1 AND 4),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    reason TEXT,
    approved_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT valid_date_range CHECK (valid_until > valid_from)
);
```

### Audit Log Table

```sql
CREATE TABLE authorization_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    roles TEXT[],
    groups TEXT[],
    decision VARCHAR(20) NOT NULL,  -- 'ALLOWED' or 'DENIED'
    reason TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_user_id ON authorization_audit_log(user_id);
CREATE INDEX idx_audit_created_at ON authorization_audit_log(created_at);
CREATE INDEX idx_audit_decision ON authorization_audit_log(decision);
```

## Best Practices

### Token Management

1. **Short-Lived Access Tokens**: 1 hour expiration
2. **Long-Lived Refresh Tokens**: 24 hours
3. **Token Rotation**: Refresh tokens rotated on use
4. **Token Revocation**: Logout revokes sessions

### Role Assignment

1. **Least Privilege**: Assign minimum necessary roles
2. **Group-Based**: Prefer group membership over individual role assignment
3. **Composite Roles**: Use for common role combinations (e.g., department heads)
4. **Regular Audits**: Review role assignments quarterly

### Security Hardening

1. **Enable HTTPS**: All production traffic over TLS
2. **Brute Force Protection**: Keycloak built-in protection enabled
3. **Password Policy**: Enforce strong passwords
4. **MFA**: Enable for privileged accounts
5. **Session Management**: Limit concurrent sessions

### Performance Optimization

1. **Token Caching**: Cache decoded tokens (with expiration)
2. **Role Caching**: Cache user roles (invalidate on change)
3. **Connection Pooling**: Use connection pools for Keycloak Admin API
4. **Async Operations**: Background tasks for non-critical operations

## Troubleshooting

### Token Validation Failures

**Symptom**: 401 Unauthorized errors

**Causes**:
1. Token expired
2. Issuer mismatch (`iss` claim doesn't match configured issuer)
3. Invalid signature (JWK key mismatch)
4. Token not yet valid (`nbf` claim in future)

**Resolution**:
1. Check token expiration timestamp
2. Verify `KEYCLOAK_ISSUER` environment variable matches token `iss` claim
3. Ensure Keycloak is accessible at JWK URI
4. Check server time synchronization (NTP)

### Role Not Recognized

**Symptom**: User has role in Keycloak but application denies access

**Causes**:
1. Role not in token (client scope issue)
2. Role name case mismatch
3. Client role vs realm role confusion

**Resolution**:
1. Check token claims contain expected roles
2. Verify client scope mappings in Keycloak
3. Use correct role prefix in `@PreAuthorize` (`ROLE_` prefix added automatically)

### Custom Attribute Not in Token

**Symptom**: Custom attribute missing from JWT

**Causes**:
1. Protocol mapper not configured
2. Mapper not assigned to client
3. Attribute not set on user

**Resolution**:
1. Verify protocol mapper exists for client
2. Check mapper configuration (claim name, token type)
3. Confirm user has attribute value set

## References

- Keycloak Admin REST API: https://www.keycloak.org/docs-api/latest/rest-api/
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- JWT Specification: https://datatracker.ietf.org/doc/html/rfc7519
- Werkflow realm configuration: `/infrastructure/keycloak/werkflow-realm.json`
