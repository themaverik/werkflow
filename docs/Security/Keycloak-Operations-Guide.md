# Keycloak Operations Guide

## Overview

This guide provides step-by-step instructions for common Keycloak administrative operations in the Werkflow platform.

## Prerequisites

- Keycloak Admin Console access: http://localhost:8090
- Admin credentials: admin / admin123 (change in production)
- Understanding of role/group structure (see Keycloak-RBAC-Design.md)

## Table of Contents

1. [User Management](#user-management)
2. [Role Management](#role-management)
3. [Group Management](#group-management)
4. [Custom Attributes](#custom-attributes)
5. [DOA Management](#doa-management)
6. [Troubleshooting](#troubleshooting)

---

## User Management

### Adding a New Employee

**Scenario**: New employee joins HR department as a specialist

**Steps**:

1. **Login to Keycloak Admin Console**
   - Navigate to http://localhost:8090
   - Login with admin credentials
   - Select "werkflow-platform" realm

2. **Create User**
   - Navigate to: Users → Add User
   - Fill in details:
     - Username: `jane.smith`
     - Email: `jane.smith@company.com`
     - First Name: `Jane`
     - Last Name: `Smith`
     - Email Verified: ON
     - Enabled: ON
   - Click "Save"

3. **Set Password**
   - Go to "Credentials" tab
   - Click "Set Password"
   - Enter password: `TempPassword123!`
   - Temporary: ON (user must change on first login)
   - Click "Set Password"

4. **Assign to Groups**
   - Go to "Groups" tab
   - Click "Join Group"
   - Select `/HR Department`
   - Click "Join"
   - Repeat for sub-group: `/HR Department/Specialists`

5. **Assign Roles**
   - Go to "Role Mappings" tab
   - Under "Realm Roles", select:
     - `employee` (base role)
     - `asset_request_requester` (can submit requests)
   - Click "Add Selected"

6. **Set Custom Attributes**
   - Go to "Attributes" tab
   - Add attributes:
     | Key | Value |
     |-----|-------|
     | `department` | `HR` |
     | `employee_id` | `EMP002456` |
     | `cost_center` | `HR-001` |
     | `manager_id` | `a1b2c3d4-e5f6-7890-...` (Keycloak ID of manager) |
     | `doa_level` | `0` (no approval authority) |
     | `is_poc` | `false` |
   - Click "Save"

7. **Verify User Creation**
   - Test login at http://localhost:4000
   - Verify token contains correct roles and attributes

---

### Finding a User's Manager ID

To set the `manager_id` attribute, you need the manager's Keycloak user ID:

**Steps**:

1. Navigate to: Users
2. Search for manager by name or email
3. Click on the user
4. Copy the UUID from the URL:
   ```
   http://localhost:8090/admin/master/console/#/werkflow-platform/users/{USER_ID}/settings
                                                                      ^^^^^^^^^^^^^^^^
   ```
5. Use this ID as `manager_id` for direct reports

**Alternative**: Use Keycloak Admin REST API:

```bash
# Get user ID by username
curl -X GET "http://localhost:8090/admin/realms/werkflow-platform/users?username=john.manager" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq '.[0].id'
```

---

### Promoting Employee to Manager

**Scenario**: Specialist promoted to manager role

**Steps**:

1. **Update Group Membership**
   - Go to user → Groups tab
   - Leave current group: `/HR Department/Specialists`
   - Join new group: `/HR Department/Managers`

2. **Update Roles**
   - Go to user → Role Mappings
   - Add roles:
     - `asset_request_approver`
     - `doa_approver_level1` (if applicable)

3. **Update Attributes**
   - Go to user → Attributes
   - Update `doa_level` from `0` to `1`

4. **Update Direct Reports**
   - For each employee reporting to this user:
     - Navigate to their profile
     - Go to Attributes tab
     - Set `manager_id` = new manager's Keycloak user ID

5. **Notify User**
   - Send email about role change
   - Instruct to logout and login again to refresh token

---

### Assigning Point of Contact (POC) Role

**Scenario**: Designate user as department POC

**Steps**:

1. **Join POC Group**
   - Go to user → Groups
   - Join: `/[Department] Department/POC`
   - Example: `/HR Department/POC`

2. **Assign POC Role**
   - Go to user → Role Mappings
   - Add realm role: `department_poc`

3. **Set POC Attribute**
   - Go to user → Attributes
   - Set `is_poc` = `true`

4. **Verify**
   - Check token contains:
     - Role: `department_poc`
     - Group: `/HR Department/POC`
     - Attribute: `is_poc: true`

---

### Disabling User Account

**Scenario**: Employee leaves company or extended leave

**Steps**:

1. **Disable Account**
   - Go to user profile
   - Toggle "Enabled" to OFF
   - Click "Save"

2. **Revoke Active Sessions** (Optional - immediate logout)
   - Go to user → Sessions tab
   - Click "Logout all sessions"

3. **Remove from Groups** (Optional - preserve for audit)
   - Go to user → Groups
   - Leave all groups

4. **Document Reason** (Best Practice)
   - Go to user → Attributes
   - Add attribute:
     - Key: `disabled_reason`
     - Value: `Resigned - Last day 2025-11-30`

---

## Role Management

### Creating a New Role

**Scenario**: Create custom role for travel approval

**Steps**:

1. **Create Realm Role**
   - Navigate to: Roles → Add Role
   - Role Name: `travel_approver`
   - Description: `Can approve employee travel requests`
   - Click "Save"

2. **Configure Composite Role** (Optional)
   - If role should include other roles:
   - Go to role → Composite Roles
   - Toggle "Composite Roles" ON
   - Select roles to include (e.g., `employee`)

3. **Assign to Users**
   - Go to user → Role Mappings
   - Select `travel_approver`
   - Click "Add Selected"

---

### Understanding Composite Roles

Composite roles automatically grant all child roles.

**Example**: `hr_head` composite role

```
hr_head (composite)
├── employee
├── asset_request_approver
└── doa_approver_level1
```

When user assigned `hr_head`, they automatically get all 3 roles.

**Creating Composite Role**:

1. Navigate to: Roles → Add Role
2. Name: `hr_head`
3. Toggle "Composite Roles" ON
4. Select child roles:
   - `employee`
   - `asset_request_approver`
   - `doa_approver_level1`
5. Save

---

## Group Management

### Creating Department Structure

**Scenario**: New department "Legal" joining organization

**Steps**:

1. **Create Parent Group**
   - Navigate to: Groups → New
   - Name: `Legal Department`
   - Click "Save"

2. **Set Group Attributes**
   - Go to group → Attributes
   - Add:
     - Key: `department`, Value: `Legal`
     - Key: `cost_center_prefix`, Value: `LEG`
   - Save

3. **Assign Realm Roles to Group**
   - Go to group → Role Mappings
   - Assign: `employee` (all group members get this role)

4. **Create Sub-Groups**
   - Click on parent group "Legal Department"
   - Click "New" to create sub-group
   - Create:
     - `Managers`
     - `Specialists`
     - `POC`

5. **Configure Sub-Group Roles**
   - For "Managers" sub-group:
     - Role Mappings → Add `asset_request_approver`
   - For "POC" sub-group:
     - Role Mappings → Add `department_poc`

**Result**:
```
/Legal Department
├── /Legal Department/Managers (role: asset_request_approver)
├── /Legal Department/Specialists
└── /Legal Department/POC (role: department_poc)
```

---

## Custom Attributes

### Setting DOA Level

**Scenario**: Grant user DOA approval authority

**Steps**:

1. **Determine DOA Level**
   - Level 1: Up to $1,000
   - Level 2: Up to $10,000
   - Level 3: Up to $100,000
   - Level 4: Unlimited

2. **Set Attribute**
   - Go to user → Attributes
   - Add/Update:
     - Key: `doa_level`
     - Value: `2` (for level 2)
   - Save

3. **Assign Corresponding Role**
   - Go to user → Role Mappings
   - Add: `doa_approver_level2`

4. **Add to Finance Approvers Group**
   - Go to user → Groups
   - Join: `/Finance Department/Approvers`

**Important**: All three must match:
- Attribute `doa_level` = 2
- Role `doa_approver_level2`
- Group `/Finance Department/Approvers`

---

### Setting Manager Relationship

**Scenario**: Assign manager to employee

**Steps**:

1. **Find Manager's User ID**
   - Navigate to manager's profile
   - Copy user ID from URL

2. **Set Employee's Manager**
   - Go to employee → Attributes
   - Add/Update:
     - Key: `manager_id`
     - Value: `{manager_user_id}`
   - Save

3. **Verify Hierarchy**
   - Test workflow: Submit request as employee
   - Verify task routed to manager

---

### Assigning Warehouse Hub

**Scenario**: Assign hub manager to specific warehouse

**Steps**:

1. **Set Hub Attribute**
   - Go to user → Attributes
   - Add:
     - Key: `hub_id`
     - Value: `HUB_A` (or `CENTRAL`, `HUB_B`)
   - Save

2. **Assign Hub Role**
   - Go to user → Role Mappings
   - Add: `hub_manager` (or `central_hub_manager` for central hub)

3. **Join Hub Group**
   - Go to user → Groups
   - Join: `/Inventory Warehouse/Hub A` (or appropriate hub)

---

## DOA Management

### Temporary DOA Delegation

**Scenario**: Manager on leave, delegate approval authority temporarily

**Option A: Using Database Override** (Recommended)

1. **Insert DOA Override Record**

```sql
INSERT INTO doa_overrides (
    user_id,
    user_email,
    override_doa_level,
    original_doa_level,
    valid_from,
    valid_until,
    reason,
    approved_by,
    approved_at
) VALUES (
    'temp-approver-user-id',
    'temp.approver@company.com',
    3,  -- Override level
    1,  -- Original level
    '2025-12-01 00:00:00',
    '2025-12-15 23:59:59',
    'Manager on annual leave - emergency coverage',
    'admin-user-id',
    NOW()
);
```

2. **Workflow Checks Override Table**
   - Application checks `get_effective_doa_level()` function
   - Returns override level if active, else uses Keycloak attribute

**Option B: Temporarily Update Keycloak** (Not Recommended - No Audit Trail)

1. **Update User Attribute**
   - Go to user → Attributes
   - Update `doa_level` from `1` to `3`
   - Note original value

2. **Assign Higher Role**
   - Go to user → Role Mappings
   - Add `doa_approver_level3`

3. **Set Reminder to Revert**
   - After delegation period, revert changes
   - No automatic expiration

---

### Revoking DOA Override

**Scenario**: Early termination of temporary delegation

**Steps**:

```sql
UPDATE doa_overrides
SET revoked = TRUE,
    revoked_by = 'admin-user-id',
    revoked_at = NOW(),
    revoke_reason = 'Manager returned early from leave'
WHERE id = '{override_id}';
```

---

## Troubleshooting

### User Cannot Login

**Symptoms**:
- "Invalid credentials" error
- User exists in Keycloak

**Checklist**:
1. Verify user "Enabled" is ON
2. Check email verified status
3. Verify password is not expired
4. Check brute force protection didn't lock account
5. Verify realm is correct

**Reset Account**:
```
Users → Select user → Credentials → Reset Password
Users → Select user → Settings → Enabled = ON
```

---

### User Has Role But Access Denied

**Symptoms**:
- User has role in Keycloak
- Application returns 403 Forbidden

**Checklist**:

1. **Verify Token Contains Role**
   - Decode JWT token at jwt.io
   - Check `realm_access.roles` array
   - Check `resource_access.{client}.roles`

2. **Check Client Scope Mappings**
   - Clients → Select client → Client Scopes
   - Verify roles are in token

3. **Verify Role Name Case**
   - Application adds `ROLE_` prefix
   - `asset_request_approver` becomes `ROLE_ASSET_REQUEST_APPROVER`

4. **Force Token Refresh**
   - Logout and login again
   - New token will have updated roles

---

### Custom Attribute Not in Token

**Symptoms**:
- Attribute set on user
- Not visible in JWT token

**Fix**:

1. **Check Protocol Mapper Exists**
   - Clients → Select client → Mappers
   - Verify mapper for attribute exists

2. **Create Protocol Mapper** (if missing)
   - Click "Create"
   - Mapper Type: User Attribute
   - Name: `department`
   - User Attribute: `department`
   - Token Claim Name: `department`
   - Claim JSON Type: String
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON
   - Save

3. **Verify Attribute on User**
   - Users → Select user → Attributes
   - Ensure attribute has value

4. **Test**
   - Logout and login
   - Decode new token
   - Verify attribute present

---

### Task Not Routed to Correct User

**Symptoms**:
- Workflow task not appearing in user's task list
- Task assigned to wrong person

**Debug Steps**:

1. **Check Manager ID**
   ```sql
   -- Verify manager relationship
   SELECT u.username, u.attributes->>'manager_id' as manager_id
   FROM keycloak.user_entity u
   WHERE u.username = 'employee.name';
   ```

2. **Verify Group Membership**
   - Users → Select user → Groups
   - Ensure user in correct groups

3. **Check Task Assignment Logic**
   - Review `WorkflowTaskRouter` service
   - Check `workflow_role_mappings` table

4. **Check Audit Log**
   ```sql
   SELECT * FROM authorization_audit_log
   WHERE user_id = '{user_id}'
   ORDER BY created_at DESC
   LIMIT 10;
   ```

---

### DOA Level Not Working

**Symptoms**:
- User has DOA level 2
- Cannot approve $5,000 request

**Checklist**:

1. **Verify All 3 Components Match**
   - Attribute: `doa_level` = 2
   - Role: `doa_approver_level2` assigned
   - Group: Member of `/Finance Department/Approvers`

2. **Check DOA Override**
   ```sql
   SELECT * FROM doa_overrides
   WHERE user_id = '{user_id}'
     AND NOT revoked
     AND NOW() BETWEEN valid_from AND valid_until;
   ```

3. **Verify Amount Calculation**
   - $5,000 requires DOA level 2
   - Check workflow variables contain correct amount

---

## Best Practices

### Security

1. **Least Privilege**: Only assign necessary roles
2. **Regular Audits**: Review role assignments quarterly
3. **Document Changes**: Use attributes to track changes
4. **MFA**: Enable for admin and privileged accounts
5. **Strong Passwords**: Enforce password policy

### Performance

1. **Use Groups**: Assign roles to groups, not individual users
2. **Cache Tokens**: Cache decoded tokens (with expiration)
3. **Batch Operations**: Use Admin API for bulk user operations
4. **Cleanup**: Remove expired DOA overrides and cache entries

### Operational

1. **Document User Changes**: Track in `authorization_audit_log`
2. **Backup**: Regular backups of Keycloak database
3. **Test Changes**: Test role/group changes in dev environment first
4. **Communication**: Notify users of role changes
5. **Monitoring**: Monitor failed authorization attempts

---

## Useful SQL Queries

### Find All Users with Specific Role

```sql
-- This requires direct Keycloak database access
SELECT u.username, u.email
FROM keycloak.user_entity u
JOIN keycloak.user_role_mapping urm ON u.id = urm.user_id
JOIN keycloak.keycloak_role kr ON urm.role_id = kr.id
WHERE kr.name = 'asset_request_approver';
```

### Find All Users in Department

```sql
SELECT username, email, attributes->>'department' as department
FROM keycloak.user_entity
WHERE attributes->>'department' = 'HR';
```

### Find Users by DOA Level

```sql
SELECT username, email, attributes->>'doa_level' as doa_level
FROM keycloak.user_entity
WHERE attributes->>'doa_level' >= '2'
ORDER BY (attributes->>'doa_level')::int DESC;
```

### Active DOA Overrides

```sql
SELECT
    do.user_email,
    do.override_doa_level,
    do.valid_from,
    do.valid_until,
    do.reason
FROM doa_overrides do
WHERE NOT do.revoked
  AND NOW() BETWEEN do.valid_from AND do.valid_until;
```

---

## References

- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- Werkflow RBAC Design: `/docs/Security/Keycloak-RBAC-Design.md`
- Realm Configuration: `/infrastructure/keycloak/werkflow-realm.json`
