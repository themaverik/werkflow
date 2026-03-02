# Keycloak Realm Import Instructions

This document describes how to import the Werkflow Keycloak realm configuration for Phase 5A RBAC integration.

## Prerequisites

- Keycloak 23.0+ running on http://localhost:8090
- Admin credentials (default: admin/admin)
- Realm JSON file: `/infrastructure/keycloak/keycloak-realm-export.json`

## Import Methods

### Method 1: Admin Console Import (RECOMMENDED)

**Best for**: Initial setup, manual verification

**Steps**:

1. **Login to Keycloak Admin Console**
   ```
   URL: http://localhost:8090
   Username: admin
   Password: admin
   ```

2. **Navigate to Realm Import**
   - Click on realm dropdown (top-left, shows "Master")
   - Click "Create Realm"
   - OR if "werkflow" realm exists: Select "werkflow" → "Action" → "Partial Import"

3. **Upload Realm JSON**
   - Click "Browse" or drag-and-drop
   - Select: `/infrastructure/keycloak/keycloak-realm-export.json`
   - Import Strategy: Select "Skip" (don't overwrite existing users/roles if any)

4. **Execute Import**
   - Review import summary
   - Click "Import"
   - Wait for completion message

5. **Verification**
   - Navigate to "Realm Settings" → Verify realm name is "werkflow"
   - Navigate to "Realm Roles" → Should see 31 roles
   - Navigate to "Groups" → Should see 6 department groups
   - Navigate to "Clients" → Should see 3 clients:
     - werkflow-admin-portal
     - werkflow-hr-portal
     - workflow-engine

### Method 2: Docker Auto-Import (AUTOMATED)

**Best for**: Development, CI/CD

**Steps**:

1. **Update docker-compose.yml**
   ```yaml
   keycloak:
     image: quay.io/keycloak/keycloak:23.0
     volumes:
       - ./infrastructure/keycloak/keycloak-realm-export.json:/opt/keycloak/data/import/werkflow-realm.json
     command:
       - start-dev
       - --import-realm
     environment:
       - KEYCLOAK_ADMIN=admin
       - KEYCLOAK_ADMIN_PASSWORD=admin
     ports:
       - "8090:8080"
   ```

2. **Restart Keycloak container**
   ```bash
   docker-compose down keycloak
   docker-compose up -d keycloak
   ```

3. **Verify import logs**
   ```bash
   docker-compose logs keycloak | grep "werkflow"
   ```

## Post-Import Tasks

### 1. Create Test Users

Run the test user creation script:
```bash
./scripts/create-keycloak-test-users.sh
```

This creates 10 test users across all departments.

### 2. Verify Realm Configuration

Check that all expected components were imported:

```bash
# Check realm exists
curl -s http://localhost:8090/admin/realms/werkflow \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.realm'

# Check roles (should be 31)
curl -s http://localhost:8090/admin/realms/werkflow/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '. | length'

# Check clients (should be 3)
curl -s http://localhost:8090/admin/realms/werkflow/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[] | .clientId'
```

### 3. Update Application Configuration

Update application.yml files with Keycloak URLs (if using Docker Compose):

**Admin Service**: Services use service names in Docker network
```yaml
app:
  keycloak:
    auth-server-url: http://keycloak:8080
```

**For Local Development**: Use localhost
```yaml
app:
  keycloak:
    auth-server-url: http://localhost:8090
```

### 4. Test Token Generation

```bash
# Get token for admin user
curl -X POST "http://localhost:8090/realms/werkflow/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv" \
  -d "grant_type=password" \
  -d "username=admin.user" \
  -d "password=password123" | jq '.'
```

Expected response should include:
- `access_token`
- `refresh_token`
- `expires_in: 3600`

### 5. Verify Token Claims

Decode the access token at https://jwt.io and verify:
- `iss`: `http://localhost:8090/realms/werkflow`
- `realm_access.roles`: Should include user's roles
- Custom attributes: `department`, `doa_level`, etc.

## Troubleshooting

### Error: "Realm already exists"

**Solution**: Use "Partial Import" instead of "Create Realm"

### Error: "Invalid token"

**Solution**: Check Keycloak URL in application.yml matches actual Keycloak instance

### Error: "Role not found"

**Solution**: Verify realm import completed successfully, check "Realm Roles" in admin console

### Error: "Group not found"

**Solution**: Verify groups were imported, check "Groups" in admin console

### Error: "Client secret mismatch"

**Solution**: Update application.yml with correct client secret from Keycloak admin console

## Security Considerations

1. **Change Default Passwords**: Replace all default secrets in production:
   - Keycloak admin password
   - Client secrets (werkflow-admin-portal, workflow-engine)
   - Test user passwords

2. **Enable SSL**: Set `sslRequired: "external"` in realm settings for production

3. **Token Lifespan**: Adjust `accessTokenLifespan` (default: 3600s) based on security requirements

4. **Audit Logging**: Enable realm events for security monitoring:
   - Realm Settings → Events → Save Events: ON
   - Events Config → Event Types: ALL

## Test Users Created

| Username | Password | Role | DOA Level | Department |
|----------|----------|------|-----------|-----------|
| admin.user | password123 | Admin, Super Admin | 4 | Admin |
| hr.employee | password123 | Employee | 0 | HR |
| hr.manager | password123 | HR Manager | 1 | HR |
| hr.head | password123 | HR Head | 2 | HR |
| it.employee | password123 | Employee | 0 | IT |
| it.manager | password123 | IT Manager | 1 | IT |
| finance.manager | password123 | Finance Manager | 2 | Finance |
| finance.head | password123 | Finance Head | 4 | Finance |
| procurement.manager | password123 | Procurement Manager | 1 | Procurement |
| procurement.admin | password123 | Procurement Admin | 2 | Procurement |

**Note**: All users must change password on first login (temporary=true)

## Next Steps

After realm import:
1. Create test users using the script
2. Update application.yml files with Keycloak URLs
3. Test JWT token generation
4. Verify Spring Security integration with admin service
5. Test end-to-end authentication flow
