# 🔐 Keycloak Setup Guide for werkflow

This guide walks you through setting up Keycloak for authentication and role-based access control in the werkflow HR platform.

## 📋 Table of Contents
1. [Overview](#overview)
2. [Start Keycloak](#start-keycloak)
3. [Initial Keycloak Setup](#initial-keycloak-setup)
4. [Create Realm](#create-realm)
5. [Configure Client](#configure-client)
6. [Define Roles](#define-roles)
7. [Create Users](#create-users)
8. [Test Authentication](#test-authentication)
9. [Integration with Application](#integration-with-application)

---

## 🎯 Overview

### HR Roles & Permissions

| Role | Permissions |
|------|-------------|
| **HR_ADMIN** | Full access to all modules, can delete employees, manage all settings |
| **HR_MANAGER** | Manage employees, approve leaves, create payrolls, manage performance reviews |
| **MANAGER** | Approve/reject leave requests for team members, view team data, create performance reviews |
| **EMPLOYEE** | View own data, create leave requests, view own payroll, acknowledge reviews |

### Architecture
- **Keycloak**: Identity and Access Management (IAM)
- **JWT Tokens**: OAuth2/OpenID Connect authentication
- **Spring Security**: Resource server validating JWT tokens
- **Role-based Access**: Method and endpoint-level security

---

## 🚀 Start Keycloak

### Step 1: Start All Services

```bash
# Stop existing services first
docker-compose down

# Start all services including Keycloak
docker-compose up -d

# Wait for Keycloak to be ready (takes ~60 seconds)
docker-compose logs -f keycloak
```

Wait for this message:
```
Keycloak 23.0.0 started
```

### Step 2: Verify Services

```bash
# Check all services are running
docker-compose ps

# You should see:
# - werkflow-postgres (port 5432)
# - werkflow-keycloak-db (internal)
# - werkflow-keycloak (port 8090)
# - werkflow-pgadmin (port 5050)
```

---

## 🔧 Initial Keycloak Setup

### Step 1: Access Keycloak Admin Console

1. Open browser: **http://localhost:8090**
2. Click "Administration Console"
3. Login with:
   - **Username**: `admin`
   - **Password**: `admin123`

### Step 2: Verify Admin Access

You should see the Keycloak admin dashboard with the "master" realm selected.

---

## 🏢 Create Realm

A realm manages a set of users, credentials, roles, and groups. We'll create a `werkflow` realm.

### Step 1: Create Realm

1. Click the dropdown "master" in the top-left corner
2. Click "Create Realm"
3. Enter:
   - **Realm name**: `werkflow`
   - **Enabled**: ON
4. Click "Create"

### Step 2: Configure Realm Settings

1. Go to "Realm settings"
2. Configure:
   - **Display name**: `werkflow HR Platform`
   - **User registration**: OFF (we'll create users manually)
   - **Email as username**: OFF
   - **Edit username**: OFF
   - **Login with email**: ON

3. Click "Save"

---

## 🔌 Configure Client

Clients are applications that can request authentication. We'll create a client for our Spring Boot API.

### Step 1: Create Client

1. In the `werkflow` realm, go to "Clients"
2. Click "Create client"
3. **General Settings**:
   - **Client type**: OpenID Connect
   - **Client ID**: `werkflow-api`
   - Click "Next"

4. **Capability config**:
   - **Client authentication**: ON
   - **Authorization**: OFF
   - **Authentication flow**:
     - ✅ Standard flow
     - ✅ Direct access grants
     - ✅ Service accounts roles
   - Click "Next"

5. **Login settings**:
   - **Root URL**: `http://localhost:8080`
   - **Valid redirect URIs**:
     - `http://localhost:8080/*`
     - `http://localhost:8080/api/*`
   - **Web origins**: `http://localhost:8080`
   - Click "Save"

### Step 2: Get Client Secret

1. Go to "Clients" → "werkflow-api"
2. Click "Credentials" tab
3. Copy the "Client secret" value
4. Update `src/main/resources/application.yml`:
   ```yaml
   keycloak:
     credentials:
       secret: <PASTE-YOUR-CLIENT-SECRET-HERE>
   ```

---

## 👥 Define Roles

### Step 1: Create Realm Roles

1. Go to "Realm roles"
2. Click "Create role"
3. Create these 4 roles (one by one):

**Role 1: HR_ADMIN**
- **Role name**: `HR_ADMIN`
- **Description**: `HR Administrator with full system access`

**Role 2: HR_MANAGER**
- **Role name**: `HR_MANAGER`
- **Description**: `HR Manager - manages employees, approves leaves, handles payroll`

**Role 3: MANAGER**
- **Role name**: `MANAGER`
- **Description**: `Team Manager - approves team leaves, creates reviews`

**Role 4: EMPLOYEE**
- **Role name**: `EMPLOYEE`
- **Description**: `Regular Employee - view own data, create leave requests`

---

## 👤 Create Users

Let's create test users for each role.

### User 1: HR Admin (Alice)

1. Go to "Users"
2. Click "Create new user"
3. Fill in:
   - **Username**: `alice.admin`
   - **Email**: `alice.admin@werkflow.com`
   - **First name**: `Alice`
   - **Last name**: `Admin`
   - **Email verified**: ON
   - **Enabled**: ON
4. Click "Create"

5. **Set Password**:
   - Go to "Credentials" tab
   - Click "Set password"
   - **Password**: `alice123`
   - **Temporary**: OFF
   - Click "Save"

6. **Assign Role**:
   - Go to "Role mappings" tab
   - Click "Assign role"
   - Select `HR_ADMIN`
   - Click "Assign"

### User 2: HR Manager (Bob)

Repeat the same process:
- **Username**: `bob.hrmanager`
- **Email**: `bob.hrmanager@werkflow.com`
- **First name**: `Bob`, **Last name**: `HR Manager`
- **Password**: `bob123`
- **Role**: `HR_MANAGER`

### User 3: Manager (Charlie)

- **Username**: `charlie.manager`
- **Email**: `charlie.manager@werkflow.com`
- **First name**: `Charlie`, **Last name**: `Manager`
- **Password**: `charlie123`
- **Role**: `MANAGER`

### User 4: Employee (Diana)

- **Username**: `diana.employee`
- **Email**: `diana.employee@werkflow.com`
- **First name**: `Diana`, **Last name**: `Employee`
- **Password**: `diana123`
- **Role**: `EMPLOYEE`

---

## 🧪 Test Authentication

### Step 1: Get Access Token (Using cURL)

```bash
# Get token for HR Admin (Alice)
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=alice.admin" \
  -d "password=alice123" | jq
```

**Response** (example):
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

### Step 2: Save Access Token

```bash
# Save token to variable
export TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 3: Test API with Token

```bash
# Test with authentication
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" | jq

# Should return list of employees
```

### Step 4: Test Without Token (Should Fail)

```bash
# Test without token
curl -X GET http://localhost:8080/api/employees

# Should return 401 Unauthorized
```

---

## 🔗 Integration with Application

### Step 1: Restart Application

```bash
# Stop the application (Ctrl+C)
# Restart with updated configuration
mvn spring-boot:run
```

### Step 2: Verify Keycloak Integration

Check application logs for:
```
Using issuer-uri: http://localhost:8090/realms/werkflow
```

### Step 3: Test Role-Based Access

**Test 1: HR Admin can access everything**
```bash
# Get Alice's token
TOKEN=$(curl -s -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "client_id=werkflow-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=alice.admin" \
  -d "password=alice123" | jq -r '.access_token')

# Access departments (HR_ADMIN only)
curl -X GET http://localhost:8080/api/departments \
  -H "Authorization: Bearer $TOKEN" | jq

# Create employee (HR_ADMIN/HR_MANAGER only)
curl -X POST http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"EMP999","firstName":"Test","lastName":"User",...}' | jq
```

**Test 2: Employee has limited access**
```bash
# Get Diana's token (EMPLOYEE role)
TOKEN=$(curl -s -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "client_id=werkflow-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=diana.employee" \
  -d "password=diana123" | jq -r '.access_token')

# Can view employees
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" | jq

# Cannot create employee (should get 403 Forbidden)
curl -X POST http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"EMP888",...}' | jq
```

---

## 📊 Role Access Matrix

| Endpoint | HR_ADMIN | HR_MANAGER | MANAGER | EMPLOYEE |
|----------|----------|------------|---------|----------|
| GET /departments | ✅ | ✅ | ❌ | ❌ |
| POST /departments | ✅ | ✅ | ❌ | ❌ |
| GET /employees | ✅ | ✅ | ✅ | ✅ |
| POST /employees | ✅ | ✅ | ❌ | ❌ |
| DELETE /employees | ✅ | ❌ | ❌ | ❌ |
| GET /leaves | ✅ | ✅ | ✅ | ✅ |
| POST /leaves | ✅ | ✅ | ✅ | ✅ |
| PUT /leaves/{id}/approve | ✅ | ✅ | ✅ | ❌ |
| GET /payrolls | ✅ | ✅ | ❌ | ❌ |
| POST /payrolls | ✅ | ✅ | ❌ | ❌ |
| GET /performance-reviews | ✅ | ✅ | ✅ | ✅ |
| POST /performance-reviews | ✅ | ✅ | ✅ | ❌ |

---

## 🛠️ Troubleshooting

### Issue: Cannot access Keycloak

**Solution**:
```bash
# Check if Keycloak is running
docker-compose ps keycloak

# Check logs
docker-compose logs keycloak

# Restart Keycloak
docker-compose restart keycloak
```

### Issue: Invalid token error

**Symptoms**: `401 Unauthorized` or "Invalid token"

**Solution**:
1. Verify realm name in application.yml matches Keycloak realm
2. Check client secret is correct
3. Ensure token hasn't expired (tokens expire in 5 minutes)
4. Generate a new token

### Issue: 403 Forbidden

**Symptoms**: Token valid but access denied

**Solution**:
1. Verify user has the correct role assigned in Keycloak
2. Check role name matches exactly (case-sensitive)
3. Verify endpoint security rules in `KeycloakSecurityConfig.java`

### Issue: Realm not found

**Solution**:
1. Go to Keycloak admin console
2. Verify realm "werkflow" exists
3. Check realm is enabled
4. Verify issuer-uri in application.yml

---

## 🔐 Security Best Practices

1. **Change Default Passwords**: Change Keycloak admin password in production
2. **Use HTTPS**: Enable SSL/TLS for production
3. **Token Expiration**: Keep access token lifetime short (5-15 minutes)
4. **Refresh Tokens**: Implement refresh token rotation
5. **Client Secrets**: Store client secrets securely (environment variables)
6. **MFA**: Enable multi-factor authentication for admin users
7. **Audit Logs**: Enable Keycloak audit logging
8. **Regular Updates**: Keep Keycloak updated

---

## 📚 Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [JWT.io](https://jwt.io/) - Decode and inspect JWTs

---

## 🎉 Next Steps

After Keycloak is set up:

1. ✅ Test all endpoints with different user roles
2. ✅ Implement method-level security with `@PreAuthorize`
3. ✅ Add user context to audit fields
4. 🚀 Proceed to Phase 3: Flowable BPM Workflows

---

**Questions or Issues?** Check the troubleshooting section or application logs.
