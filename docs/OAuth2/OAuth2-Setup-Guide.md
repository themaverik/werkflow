# OAuth2 Setup Guide

## Overview

This guide provides complete instructions for configuring Keycloak OAuth2 authentication in the Werkflow platform. It covers realm creation, role setup, client configuration, and user management.

## Prerequisites

- Keycloak running at http://localhost:8090
- Docker environment configured with werkflow network
- Admin console access with credentials: admin/admin123

## Quick Start

For users who need immediate setup, follow these abbreviated steps:

### Step 1: Access Keycloak Admin Console (1 minute)

URL: http://localhost:8090/admin/master/console
Username: admin
Password: admin123

### Step 2: Create werkflow Realm (2 minutes)

1. Click dropdown that says "Master" in top left
2. Click "Create realm" button
3. Fill form:
   - Name: werkflow
   - Enabled: Toggle ON
4. Click "Create"

Result: Realm selector now shows "werkflow"

### Step 3: Create HR_ADMIN Role (1 minute)

1. In left menu, click "Realm Roles"
2. Click "Create role" button
3. Fill form:
   - Name: HR_ADMIN
   - Description: Studio Admin Access
4. Click "Save"

Result: HR_ADMIN role exists

### Step 4: Create OAuth2 Client (1 minute)

1. In left menu, click "Clients"
2. Click "Create client" button
3. Fill form:
   - Client type: OpenID Connect
   - Client ID: werkflow-admin-portal
4. Click "Next"
5. Capability Config:
   - Keep defaults (Standard flow + Direct access checked)
6. Click "Next"
7. Login Settings:
   - Root URL: http://localhost:4000
   - Valid redirect URIs: http://localhost:4000/*
   - Web origins: http://localhost:4000
8. Click "Save"

Result: OAuth2 client created

### Step 5: Create User and Assign Role (1 minute)

1. In left menu, click "Users"
2. Click "Create user" button
3. Fill form:
   - Username: admin
   - Email: admin@test.local
   - Email verified: ON
   - Enabled: ON
4. Click "Create"
5. Go to "Credentials" tab
6. Click "Set password"
   - Password: admin123
   - Temporary: OFF
7. Click "Set Password"
8. Go to "Role mappings" tab
9. Click "Assign role" button
10. Find and select "HR_ADMIN"
11. Click "Assign"

Result: User created with HR_ADMIN role

## Detailed Configuration

### Realm Configuration

#### What is a Realm

A realm in Keycloak is an isolated workspace or application space. Each realm has its own:
- Users and groups
- Roles and permissions
- OAuth2 clients and applications
- Authentication policies
- Security settings

For Werkflow, you only need one realm: "werkflow"

#### Creating the Realm via Admin Console

1. Click the realm selector dropdown in top-left corner showing "Master"
2. Click the "Create realm" button
3. In the realm creation form:
   - Name: werkflow (exact, case-sensitive)
   - Enabled: Toggle switch ON
   - Leave other fields as defaults
4. Click "Create" button
5. Verify realm selector now shows: "werkflow"
6. Verify left menu updates to werkflow-specific options

### Role Configuration

#### Understanding Roles

Roles are authorization labels that define what users are allowed to do in the system. Werkflow uses these standard roles:

- HR_ADMIN: Full access to studio and admin features
- HR_USER: Can view and complete assigned tasks
- HR_APPROVER: Can approve requests and workflows
- MANAGER: Can manage team and workflows

#### Creating Roles

For each role you need:

1. From werkflow realm, navigate to "Realm Roles" in left menu
2. Click "Create role" button
3. Fill the form:
   - Name: HR_ADMIN (or other role name)
   - Description: Administrator for HR portal and workflow studio
   - Enabled: Toggle ON
4. Click "Save"

Repeat this process for each additional role your system requires.

### OAuth2 Client Configuration

#### Client 1: Admin Portal

OAuth2 clients represent applications that authenticate users. The admin portal client configuration:

1. Navigate to "Clients" in left menu
2. Click "Create client" button
3. General Settings screen:
   - Client type: OpenID Connect
   - Client ID: werkflow-admin-portal
   - Name: Werkflow Admin Portal
   - Description: Process Studio and Admin Interface
4. Click "Next"
5. Capability Configuration:
   - Client authentication: ON
   - Authorization: ON
   - Authentication flow:
     - Standard flow: CHECKED
     - Direct access grants: CHECKED
     - Implicit flow: UNCHECKED
     - Service account roles: UNCHECKED
6. Click "Next"
7. Login Settings:
   - Root URL: http://localhost:4000
   - Home URL: http://localhost:4000
   - Valid redirect URIs: http://localhost:4000/*
   - Valid post logout redirect URIs: http://localhost:4000/*
   - Web origins: http://localhost:4000
8. Click "Save"
9. Navigate to "Credentials" tab
10. Copy the "Client Secret" (you will need this for backend configuration)

#### Client 2: HR Portal (Optional - For Later)

When ready to configure the HR portal:

Configuration identical to Admin Portal except:
- Client ID: werkflow-hr-portal
- Root URL: http://localhost:4001
- Valid redirect URIs: http://localhost:4001/*
- Valid post logout redirect URIs: http://localhost:4001/*
- Web origins: http://localhost:4001

### User Management

#### Creating Users

1. Navigate to "Users" in left menu
2. Click "Create user" button
3. Fill the user creation form:
   - Username: admin (required)
   - Email: admin@werkflow.local (optional but recommended)
   - Email verified: Toggle ON (if using email)
   - First name: Admin
   - Last name: User
   - Enabled: Toggle ON
4. Click "Create"

#### Setting User Password

1. After creating user, go to "Credentials" tab
2. Click "Set password" button
3. Enter password information:
   - Password: admin123 (or your secure choice)
   - Password confirmation: admin123
   - Temporary: Toggle OFF (prevents forced password change on first login)
4. Click "Set Password"
5. Confirm the action in the dialog

#### Assigning Roles to Users

1. Navigate to user detail page
2. Click "Role mappings" tab
3. Click "Assign role" button
4. Select "HR_ADMIN" from the role list
5. Click "Assign"
6. Verify "HR_ADMIN" now appears in "Assigned roles" section

## Backend Service Configuration

After Keycloak setup, configure your backend services with environment variables.

### Environment Variables

Required variables for backend integration:

```bash
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<copy from Keycloak credentials tab>
```

In Docker environment (docker-compose.yml):

```yaml
admin-portal:
  environment:
    KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
    KEYCLOAK_CLIENT_ID: werkflow-admin-portal
    KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_ADMIN_PORTAL_SECRET}
```

### File Locations for Configuration

After setting up Keycloak, verify these backend configuration files:

- .env.shared: Keycloak client secret
- .env.admin: Admin service configuration
- infrastructure/docker/docker-compose.yml: Environment variables for all services

## Testing the Setup

### Test 1: Access Keycloak Realm

```bash
curl http://localhost:8090/realms/werkflow | jq .
```

Should return realm configuration JSON, not an error.

### Test 2: Login to Admin Portal

1. Navigate to http://localhost:4000
2. Click "Process Studio"
3. Browser should redirect to Keycloak login
4. Enter credentials: admin / admin123
5. Browser should redirect back to Studio

### Test 3: Access Processes Page

1. After successful login, you should be on /studio/processes
2. Page should display:
   - "Process Designer" heading
   - "No processes deployed yet" (if database is empty)
   - Option to create new process

### Test 4: Verify OIDC Discovery

```bash
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration
```

Should return OpenID Connect discovery metadata.

## OAuth2 Flow Architecture

### Authentication Flow Diagram

```
User Browser
  |
  | 1. Visits http://localhost:4000
  v
Admin Portal (Next.js)
  |
  | 2. User clicks "Sign In"
  v
NextAuth middleware checks session
  |
  | 3. No session found
  v
Redirect to Keycloak
  |
  | http://localhost:8090/realms/werkflow/protocol/openid-connect/auth
  v
Keycloak Login Form
  |
  | 4. User enters credentials
  v
Keycloak validates credentials
  |
  | 5. Checks user roles (HR_ADMIN, etc.)
  v
Generate JWT token with roles
  |
  | 6. Redirect with authorization code
  v
Admin Portal receives callback
  |
  | http://localhost:4000/api/auth/callback?code=...
  v
NextAuth exchanges code for tokens
  |
  | 7. Server-side token validation
  v
Extract user info and roles from token
  |
  | 8. Check user has HR_ADMIN role
  v
Create session cookie
  |
  | 9. Redirect to /studio/processes
  v
User authenticated and authorized
```

### Key Concepts

#### Realm
- Isolated workspace or application space
- Each realm has its own users, roles, and clients
- Werkflow uses one realm: "werkflow"

#### Roles
- Authorization labels defining user permissions
- HR_ADMIN: Can access admin studio
- HR_USER: Can use portal and submit requests
- MANAGER: Can approve workflows

#### Clients
- OAuth2 applications that authenticate users
- werkflow-admin-portal: Frontend application
- Backend services also authenticate as clients

#### Users
- Human accounts that log in
- Have email, password, roles
- Can be members of groups

#### Tokens (JWT)
- JSON Web Tokens issued after login
- Contains user information and roles
- Validated by backend on each API request
- Short-lived with expiration, can be refreshed

## Complete Setup Checklist

### Realm Setup
- [ ] werkflow realm created
- [ ] Realm enabled
- [ ] Realm settings configured

### Roles
- [ ] HR_ADMIN role created
- [ ] HR_USER role created (optional)
- [ ] HR_APPROVER role created (optional)
- [ ] MANAGER role created (optional)

### OAuth2 Clients
- [ ] werkflow-admin-portal client created
  - [ ] Client ID correct
  - [ ] Client secret generated
  - [ ] Valid redirect URIs configured
  - [ ] Web origins configured
- [ ] werkflow-hr-portal client created (optional, for later)

### Users
- [ ] Admin user created (username: admin)
- [ ] Admin user enabled
- [ ] Admin user password set (not temporary)
- [ ] Admin user has HR_ADMIN role assigned
- [ ] Email verified (if using email)

### Integration
- [ ] Backend services configured with Keycloak endpoints
- [ ] Client secrets stored in environment variables
- [ ] Services tested and restarted

## Troubleshooting

### Issue: "Realm does not exist" error

Cause: werkflow realm not created
Fix: Follow realm creation steps in this guide

### Issue: "Access Denied" after login

Cause: User does not have HR_ADMIN role
Fix: Assign HR_ADMIN role in user's "Role mappings" tab

### Issue: Cannot login (invalid credentials)

Cause: Wrong username/password or user does not exist
Fix:
- Check username spelling (case-sensitive)
- Reset password: Users → Find user → Credentials → Set password
- Verify user is enabled (toggle in user detail page)

### Issue: "Client not found"

Cause: werkflow-admin-portal client not created
Fix: Follow OAuth2 client creation steps in this guide

### Issue: Redirect loop (keeps redirecting to login)

Cause: Valid redirect URIs misconfigured
Fix:
- Go to Clients → werkflow-admin-portal → Settings
- Check "Valid redirect URIs" includes: http://localhost:4000/*
- Check "Web origins" includes: http://localhost:4000

### Issue: Token validation errors in backend logs

Cause: Client secret mismatch
Fix:
- In Keycloak: Clients → werkflow-admin-portal → Credentials
- Copy exact client secret
- Update .env.shared and .env.admin
- Restart services: docker-compose restart

## Quick Reference

### Keycloak URLs

| Purpose | URL |
|---------|-----|
| Admin Console | http://localhost:8090/admin/master/console |
| Realm Endpoint | http://localhost:8090/realms/werkflow |
| Metrics | http://localhost:8090/metrics |
| Health | http://localhost:8090/health/ready |
| Token Endpoint | http://localhost:8090/realms/werkflow/protocol/openid-connect/token |
| Authorization | http://localhost:8090/realms/werkflow/protocol/openid-connect/auth |
| Userinfo | http://localhost:8090/realms/werkflow/protocol/openid-connect/userinfo |

### Common Commands

```bash
# Get access token via CLI
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=<YOUR_CLIENT_SECRET>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# Decode token to see roles
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq .

# Use token to call protected API
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/flowable/process-definitions
```

## See Also

- OAuth2 Docker Configuration: Details on Docker networking setup
- NextAuth Configuration: Next.js integration guide
- OAuth2 Troubleshooting: Common errors and solutions
- Authentication Issues: Specific auth error resolution procedures
