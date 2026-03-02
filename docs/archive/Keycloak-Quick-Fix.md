# Keycloak Authentication Quick Fix

**Issue:** 404 error on callback URL after Keycloak login
**Root Cause:** Keycloak realm data was deleted during Docker volume reset
**Solution:** Import realm from backup file

---

## Quick Steps (5 minutes)

### Step 1: Import Realm via Admin Console

1. Open Keycloak Admin Console:
   ```
   http://localhost:8090/admin
   ```

2. Login:
   - Username: `admin`
   - Password: `admin`

3. Import Realm:
   - Click **realm dropdown** (top-left, shows "master")
   - Click **"Create Realm"**
   - Click **"Browse"** button
   - Select file: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json`
   - Click **"Create"**

4. Verify:
   - Should redirect to "werkflow" realm
   - Check left sidebar shows "werkflow" realm

### Step 2: Create Test Users

```bash
cd /Users/lamteiwahlang/Projects/werkflow
./scripts/create-test-users.sh
```

This creates 10 test users with different roles:
- `alice` - Employee (Requester)
- `bob.hr` - HR Manager
- `charlie.it` - IT Head
- `david.finance` - Finance Head
- `emma.admin` - Super Admin
- And 5 more...

All users have password: `Test1234!`

### Step 3: Test Authentication

1. Open: http://localhost:4000/login
2. Click "Sign in with Keycloak"
3. Login with:
   - Username: `emma.admin`
   - Password: `Test1234!`
4. Should redirect to: http://localhost:4000/portal/tasks

**Done!** Authentication is now working.

---

## Alternative: Import via Docker Command

If Admin Console doesn't work:

```bash
# Copy realm file to container
docker cp /Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json \
  werkflow-keycloak:/tmp/realm-import.json

# Import realm
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
  exec keycloak /opt/keycloak/bin/kc.sh import --file /tmp/realm-import.json --optimized

# Restart Keycloak
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
  restart keycloak
```

---

## Verification Commands

### Check Realm Exists
```bash
curl -s http://localhost:8090/realms/werkflow | jq .realm
```
Expected: `"werkflow"`

### Check Client Exists
```bash
curl -s http://localhost:8090/admin/realms/werkflow/clients | jq '.[].clientId' 2>/dev/null || echo "Need to get admin token first"
```

### Test Login
```bash
# Get token
TOKEN=$(curl -s -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=emma.admin" \
  -d "password=Test1234!" \
  -d "grant_type=password" \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv" | jq -r .access_token)

# Decode token to see roles
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq .realm_access.roles
```

---

## What Was Imported

The realm export includes:

### Clients (3)
- `werkflow-admin-portal` - Admin portal OAuth client
- `werkflow-hr-portal` - HR portal OAuth client
- `workflow-engine` - Backend service client

### Roles (30+)
- **Admin Roles:** admin, super_admin
- **Employee Roles:** employee, asset_request_requester
- **Approver Roles:** asset_request_approver, asset_assignment_approver
- **DOA Levels:** doa_approver_level1 through level4
- **Department Roles:** hr_manager, it_manager, finance_head, etc.
- **Specialized Roles:** inventory_manager, hub_manager, driver, warehouse_staff

### Groups (6 Departments)
- HR Department → Managers, Specialists, POC
- IT Department → Managers, Inventory, Field Technicians, POC
- Finance Department → Approvers, Analysts, POC
- Procurement Department → Managers, Specialists, POC
- Transport Department → Drivers, Coordinators, Management
- Inventory Warehouse → Hub A, Hub B, Hub C, Central, Management

---

## Troubleshooting

### Issue: "Realm already exists"

Delete existing realm first:
```bash
# Get admin token
TOKEN=$(curl -s -X POST http://localhost:8090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

# Delete realm
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/admin/realms/werkflow

# Try import again
```

### Issue: "Client secret mismatch"

Check docker-compose.yml:
```bash
grep "KEYCLOAK_CLIENT_SECRET" /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml
```

Should be: `4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv`

If wrong, update docker-compose.yml and restart:
```bash
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
  restart admin-portal
```

### Issue: "Invalid redirect URI"

Check Keycloak client config:
1. Go to: http://localhost:8090/admin
2. Select "werkflow" realm
3. Navigate to: Clients → werkflow-admin-portal
4. Verify **Valid redirect URIs** includes: `http://localhost:4000/*`
5. Click **Save**

---

## Detailed Documentation

For complete guide with explanations:
- **Full Import Guide:** `/Users/lamteiwahlang/Projects/werkflow/docs/Keycloak-Realm-Import-Guide.md`
- **Root Cause Analysis:** `/Users/lamteiwahlang/Projects/werkflow/docs/Troubleshooting/Authentication-404-Callback-Fix.md`
- **RBAC Implementation:** `/Users/lamteiwahlang/Projects/werkflow/docs/Keycloak-Implementation-Quick-Start.md`

---

## Test Users Reference

| Username | Password | Role | DOA Level |
|----------|----------|------|-----------|
| alice | Test1234! | Employee | 0 |
| bob.hr | Test1234! | HR Manager | 1 |
| charlie.it | Test1234! | IT Head | 2 |
| david.finance | Test1234! | Finance Head | 4 |
| emma.admin | Test1234! | Super Admin | 4 |
| frank.procurement | Test1234! | Procurement Manager | 1 |
| grace.inventory | Test1234! | Inventory Manager | 0 |
| henry.transport | Test1234! | Transport Manager | 1 |
| iris.warehouse | Test1234! | Warehouse Staff | 0 |
| jack.driver | Test1234! | Driver | 0 |

---

## Next Steps

After fixing authentication:

1. **Configure Spring Boot Services**
   - Add Spring Security dependencies
   - Configure JWT validation
   - Implement `@PreAuthorize` annotations

2. **Implement Task Routing**
   - Extract roles from JWT tokens
   - Route tasks based on DOA levels
   - Implement group-based assignment

3. **Test Workflow Flow**
   - Submit asset request as `alice`
   - Approve as line manager `bob.hr`
   - Approve as IT manager `charlie.it`
   - Approve as finance `david.finance`

4. **Monitor Logs**
   ```bash
   docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
     logs -f admin-portal engine-service keycloak
   ```

---

**Time to Fix:** 5 minutes
**Difficulty:** Easy
**Status:** Ready to implement
