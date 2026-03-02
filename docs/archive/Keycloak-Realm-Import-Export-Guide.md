# Keycloak Realm Import Guide

**Purpose:** Import the Werkflow realm configuration into Keycloak after Docker volume reset

**Time Required:** 10-15 minutes

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Method 1: Import via Admin Console (Recommended)](#method-1-import-via-admin-console-recommended)
4. [Method 2: Import via Docker Command](#method-2-import-via-docker-command)
5. [Method 3: Import on Startup](#method-3-import-on-startup)
6. [Verification Steps](#verification-steps)
7. [Create Test Users](#create-test-users)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The Werkflow realm configuration includes:
- **1 Realm:** werkflow
- **3 Clients:** werkflow-admin-portal, werkflow-hr-portal, workflow-engine
- **30+ Roles:** Including admin, employee, DOA levels, department-specific roles
- **6 Groups:** HR, IT, Finance, Procurement, Transport, Inventory departments
- **Hierarchical structure:** Groups contain subgroups (Managers, POC, etc.)

**Realm Export File Location:**
```
/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json
```

---

## Prerequisites

1. **Keycloak container is running:**
   ```bash
   docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml ps keycloak
   ```

2. **Keycloak is accessible:**
   - Admin Console: http://localhost:8090/admin
   - Admin credentials: `admin` / `admin`

3. **Realm export file exists:**
   ```bash
   ls -lh /Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json
   ```

---

## Method 1: Import via Admin Console (Recommended)

This is the easiest method and provides immediate visual feedback.

### Step 1: Access Keycloak Admin Console

1. Open browser: http://localhost:8090/admin
2. Login with:
   - Username: `admin`
   - Password: `admin`

### Step 2: Import Realm

1. Click the **realm dropdown** in the top-left (currently shows "master")
2. Click **"Create Realm"** button
3. In the "Create realm" page:
   - Click **"Browse"** button next to "Resource file"
   - Navigate to: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json`
   - Select the file
4. Click **"Create"** button

### Step 3: Verify Import

The page should redirect to the "werkflow" realm. You should see:
- Realm name: **werkflow**
- Display name: **Werkflow Enterprise Platform**

---

## Method 2: Import via Docker Command

Import the realm using Keycloak CLI from within the Docker container.

### Step 1: Copy Realm File to Container

```bash
docker cp /Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json werkflow-keycloak:/tmp/realm-import.json
```

### Step 2: Import Realm

```bash
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml exec keycloak \
  /opt/keycloak/bin/kc.sh import \
  --file /tmp/realm-import.json \
  --optimized
```

Expected output:
```
Importing realm from file: /tmp/realm-import.json
Realm 'werkflow' imported successfully
```

### Step 3: Restart Keycloak (Optional)

```bash
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml restart keycloak
```

---

## Method 3: Import on Startup

Configure Keycloak to automatically import the realm on container startup.

### Step 1: Update docker-compose.yml

Edit: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml`

Add volume mount and import command to Keycloak service:

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  container_name: werkflow-keycloak
  command:
    - start-dev
    - --import-realm  # Add this line
  environment:
    # ... existing environment variables ...
  volumes:
    - keycloak_data:/opt/keycloak/data
    # Add this volume mount:
    - ../../infrastructure/keycloak/keycloak-realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
  ports:
    - "8090:8080"
  networks:
    - werkflow-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 60s
```

### Step 2: Recreate Keycloak Container

```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker

# Remove existing Keycloak container
docker compose down keycloak

# Recreate with import configuration
docker compose up -d keycloak

# Monitor logs to confirm import
docker compose logs -f keycloak
```

Look for log line:
```
Imported realm werkflow from file /opt/keycloak/data/import/realm-export.json
```

---

## Verification Steps

After import, verify the realm configuration.

### 1. Check Realm Exists

**Via Admin Console:**
1. Go to: http://localhost:8090/admin
2. Check realm dropdown shows "werkflow"

**Via API:**
```bash
curl -s http://localhost:8090/realms/werkflow | jq .realm
```
Expected output: `"werkflow"`

### 2. Verify Clients

**Via Admin Console:**
1. Select "werkflow" realm
2. Navigate to: **Clients** (left sidebar)
3. Verify these clients exist:
   - `werkflow-admin-portal`
   - `werkflow-hr-portal`
   - `workflow-engine`

**Via API:**
```bash
# Get admin token
TOKEN=$(curl -s -X POST http://localhost:8090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

# List clients
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/admin/realms/werkflow/clients | jq '.[].clientId'
```

### 3. Verify Roles

**Via Admin Console:**
1. Navigate to: **Realm roles** (left sidebar)
2. Verify these roles exist:
   - `admin`
   - `super_admin`
   - `employee`
   - `asset_request_requester`
   - `asset_request_approver`
   - `doa_approver_level1` through `doa_approver_level4`
   - `hr_manager`, `hr_head`
   - `it_manager`, `it_head`
   - `finance_manager`, `finance_head`
   - And more...

**Via API:**
```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/admin/realms/werkflow/roles | jq '.[].name'
```

### 4. Verify Groups

**Via Admin Console:**
1. Navigate to: **Groups** (left sidebar)
2. Verify these top-level groups exist:
   - HR Department
   - IT Department
   - Finance Department
   - Procurement Department
   - Transport Department
   - Inventory Warehouse

**Via API:**
```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/admin/realms/werkflow/groups | jq '.[].name'
```

### 5. Verify Client Configuration

**Check Admin Portal Client:**
```bash
# Get client ID
CLIENT_UUID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8090/admin/realms/werkflow/clients?clientId=werkflow-admin-portal" | jq -r '.[0].id')

# Get client details
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8090/admin/realms/werkflow/clients/$CLIENT_UUID" | jq '{
    clientId: .clientId,
    rootUrl: .rootUrl,
    redirectUris: .redirectUris,
    webOrigins: .webOrigins
  }'
```

Expected output:
```json
{
  "clientId": "werkflow-admin-portal",
  "rootUrl": "http://localhost:4000",
  "redirectUris": [
    "http://localhost:4000/*",
    "https://admin-portal.company.com/*"
  ],
  "webOrigins": [
    "http://localhost:4000",
    "https://admin-portal.company.com"
  ]
}
```

---

## Create Test Users

After importing the realm, create test users for each role.

### Test User Structure

| Username | Email | Role | Group | DOA Level |
|----------|-------|------|-------|-----------|
| alice | alice@company.com | employee, asset_request_requester | - | 0 |
| bob.hr | bob@company.com | hr_manager | /HR Department/Managers | 1 |
| charlie.it | charlie@company.com | it_head | /IT Department/Managers | 2 |
| david.finance | david@company.com | finance_head | /Finance Department/Approvers | 4 |
| emma.admin | emma@company.com | super_admin | - | 4 |

### Step 1: Create Users via Admin Console

For each user:

1. Navigate to: **Users** → **Add user**
2. Fill in details:
   - **Username:** (from table above)
   - **Email:** (from table above)
   - **Email verified:** ON
   - **Enabled:** ON
3. Click **Create**
4. Go to **Credentials** tab:
   - Click **Set password**
   - Password: `Test1234!` (or your choice)
   - Temporary: OFF
   - Click **Save**
5. Go to **Role mapping** tab:
   - Click **Assign role**
   - Select roles from table above
   - Click **Assign**
6. Go to **Groups** tab:
   - Click **Join Group**
   - Select group from table above
   - Click **Join**
7. Go to **Attributes** tab (for users with DOA level):
   - Add attribute:
     - Key: `doa_level`
     - Value: (from table above)
   - Click **Add**
   - Click **Save**

### Step 2: Create Users via API

Or use this script to create all users at once:

```bash
#!/bin/bash

KEYCLOAK_URL="http://localhost:8090"
REALM="werkflow"

# Get admin token
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

# Create alice (Employee)
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@company.com",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "Test1234!",
      "temporary": false
    }],
    "realmRoles": ["employee", "asset_request_requester"]
  }'

# Create bob.hr (HR Manager)
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob.hr",
    "email": "bob@company.com",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "Test1234!",
      "temporary": false
    }],
    "realmRoles": ["employee", "hr_manager"],
    "attributes": {
      "doa_level": ["1"]
    }
  }'

# Create charlie.it (IT Head)
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "charlie.it",
    "email": "charlie@company.com",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "Test1234!",
      "temporary": false
    }],
    "realmRoles": ["employee", "it_head"],
    "attributes": {
      "doa_level": ["2"]
    }
  }'

# Create david.finance (Finance Head)
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "david.finance",
    "email": "david@company.com",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "Test1234!",
      "temporary": false
    }],
    "realmRoles": ["employee", "finance_head"],
    "attributes": {
      "doa_level": ["4"]
    }
  }'

# Create emma.admin (Super Admin)
curl -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "emma.admin",
    "email": "emma@company.com",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "Test1234!",
      "temporary": false
    }],
    "realmRoles": ["super_admin"],
    "attributes": {
      "doa_level": ["4"]
    }
  }'

echo "All users created successfully!"
```

Save this as `/Users/lamteiwahlang/Projects/werkflow/scripts/create-test-users.sh` and run:

```bash
chmod +x /Users/lamteiwahlang/Projects/werkflow/scripts/create-test-users.sh
/Users/lamteiwahlang/Projects/werkflow/scripts/create-test-users.sh
```

---

## Export Realm Users

You can export employees data from Keycloak using several methods:

1. Using Keycloak Admin API (Recommended)

  - Step 1: Get all users in the realm

    ```shell
    curl -X GET \
        "http://localhost:8080/auth/admin/realms/werkflow/users" \
        -H "Authorization: Bearer <ACCESS_TOKEN>" \
        -H "Content-Type: application/json" | jq .
    ```

  - Step 2: Get users with pagination

    ```shell
    curl -X GET \
        "http://localhost:8080/auth/admin/realms/werkflow/users?first=0&max=100" \
        -H "Authorization: Bearer <ACCESS_TOKEN>" | jq .
    ```

  - Step 3: Export to file

    ```shell
    curl -X GET \
        "http://localhost:8080/auth/admin/realms/werkflow/users" \
        -H "Authorization: Bearer <ACCESS_TOKEN>" | jq . > employees.json
    ```

2. Using Keycloak Admin CLI (kcadm.sh)

  - Step 1: Access Keycloak container

    ```shell
    docker exec -it keycloak bash
    ```

  - Step 2: Get access token
    ```shell
    kcadm.sh config credentials --server http://localhost:8080/auth \
      --realm master --user admin --password admin
    ```

  - Step 3: Export all users

    ```shell
    kcadm.sh get realms/werkflow/users > employees.json
    ```

  - Step 4: Export with specific fields

    ```shell
    kcadm.sh get realms/werkflow/users \
    --fields username,email,firstName,lastName,enabled > employees.json
    ```

3. Export Entire Realm (Includes Users)

    **Realm export with users**

    ```shell
    curl -X GET \
      "http://localhost:8080/auth/admin/realms/werkflow" \
      -H "Authorization: Bearer <ACCESS_TOKEN>" | jq . >
      realm-with-users.json
    ```  

  4. Direct Database Query

  - Step 1: Connect to PostgreSQL

    ```shell
    docker exec -it postgres psql -U keycloak -d keycloak
    ```

  - Step 2: Query users
  
    ```sql
    SELECT username, email, first_name, last_name, enabled
    FROM user_entity ue
    JOIN realm r ON ue.realm_id = r.id
    WHERE r.name = 'werkflow';
    ```

5. Using jq to Filter Specific Data

  - Step 1: Get only usernames and emails

    ```shell
    curl -X GET \
      "http://localhost:8080/auth/admin/realms/werkflow/users" \
      -H "Authorization: Bearer <ACCESS_TOKEN>" | \
      jq '.[] | {username, email, firstName, lastName, enabled}'
    ```

  - Step 2: Export as CSV

    ```shell
    curl -X GET \
      "http://localhost:8080/auth/admin/realms/werkflow/users" \
      -H "Authorization: Bearer <ACCESS_TOKEN>" | \
      jq -r '.[] | [.username, .email, .firstName, .lastName, .enabled] | 
      @csv' > employees.csv
    ```  

---

## Troubleshooting

### Issue 1: Import Failed - Realm Already Exists

**Error:**
```
Realm 'werkflow' already exists
```

**Solution:**
Delete the existing realm first:

```bash
# Via API
TOKEN=$(curl -s -X POST http://localhost:8090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/admin/realms/werkflow

# Then retry import
```

### Issue 2: Client Secret Mismatch

**Problem:**
Admin portal shows "Configuration" error during OAuth callback.

**Solution:**
Verify client secret matches in both places:

1. **Keycloak:** http://localhost:8090/admin → werkflow realm → Clients → werkflow-admin-portal → Credentials tab
   - Should be: `4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv`

2. **Docker Compose:** `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml`
   - Check `KEYCLOAK_CLIENT_SECRET` environment variable for admin-portal service
   - Should be: `4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv`

If they don't match, update docker-compose.yml and restart:
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
docker compose restart admin-portal
```

### Issue 3: Invalid Redirect URI

**Problem:**
Keycloak shows "Invalid redirect_uri" error.

**Solution:**
Check redirect URIs in client configuration:

1. Go to: http://localhost:8090/admin → werkflow realm → Clients → werkflow-admin-portal
2. Verify **Valid redirect URIs** includes:
   - `http://localhost:4000/*`
   - `https://admin-portal.company.com/*`
3. Verify **Web origins** includes:
   - `http://localhost:4000`
   - `https://admin-portal.company.com`
4. Click **Save**

### Issue 4: 404 on Callback URL

**Problem:**
After Keycloak login, redirect to `http://localhost:4000/api/auth/callback/keycloak` returns 404.

**Root Cause:**
- NextAuth.js catch-all route `/api/auth/[...nextauth]/route.ts` exists
- But returns 302 redirect to `/api/auth/error?error=Configuration`
- This means Keycloak realm is not configured or client credentials are wrong

**Solution:**
1. Import realm (this guide)
2. Verify client secret matches
3. Restart admin-portal container

### Issue 5: OIDC Discovery Failed

**Problem:**
NextAuth.js can't fetch `.well-known/openid-configuration`.

**Solution:**
Check Keycloak URL is accessible from admin-portal container:

```bash
# From host machine
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# From admin-portal container
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
  exec admin-portal curl http://keycloak:8080/realms/werkflow/.well-known/openid-configuration
```

Both should return JSON configuration. If not:
1. Check Keycloak is running: `docker compose ps keycloak`
2. Check Docker network: `docker network inspect werkflow-network`
3. Verify `KEYCLOAK_ISSUER_INTERNAL` environment variable

---

## Test Authentication Flow

After import and user creation, test the full authentication flow.

### Step 1: Test Login Page

1. Open: http://localhost:4000/login
2. Should show Werkflow login page with "Sign in with Keycloak" button

### Step 2: Test OAuth Redirect

1. Click "Sign in with Keycloak"
2. Should redirect to: `http://localhost:8090/realms/werkflow/protocol/openid-connect/auth?...`
3. Should show Keycloak login page

### Step 3: Test Login

1. Enter credentials:
   - Username: `emma.admin`
   - Password: `Test1234!`
2. Click "Sign In"
3. Should redirect back to: `http://localhost:4000/api/auth/callback/keycloak?code=...`
4. Should then redirect to: `http://localhost:4000/portal/tasks`

### Step 4: Verify Session

1. Open browser console
2. Check for authentication cookie
3. Navigate to authenticated pages

### Step 5: Test API with Token

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
echo $TOKEN | cut -d. -f2 | base64 -d | jq .realm_access.roles

# Call protected API
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/workflows/my-tasks
```

---

## Summary

You have successfully:
- Imported the Werkflow realm into Keycloak
- Verified all clients, roles, and groups
- Created test users with appropriate roles
- Tested the authentication flow end-to-end

**Next Steps:**
1. Configure your Spring Boot services to validate JWT tokens
2. Implement role-based authorization using `@PreAuthorize`
3. Implement workflow task routing based on Keycloak roles
4. Test the complete workflow approval chain

**Quick Reference:**
- **Keycloak Admin:** http://localhost:8090/admin (admin/admin)
- **Admin Portal:** http://localhost:4000
- **Realm Export:** `/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json`
- **Test Users:** alice, bob.hr, charlie.it, david.finance, emma.admin (all with password: Test1234!)
