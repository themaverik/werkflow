# Keycloak Setup Guide - Creating Realm, Roles, and Users

## Current Status

✅ **Keycloak is running** at `http://localhost:8090`
✅ **Master realm created** with admin user: `admin/admin123`
❌ **werkflow realm does NOT exist yet** - This is why you get 404/access errors

---

## What You Need to Do

You need to create:
1. **werkflow Realm** - Isolated workspace for your application
2. **HR_ADMIN Role** - Required to access Studio (processes, forms)
3. **User Account** - Your login credentials
4. **Clients** - OAuth2 clients for frontend apps

---

## Step 1: Access Keycloak Admin Console

### URL
```
http://localhost:8090/admin/master/console
```

### Login Credentials
```
Username: admin
Password: admin123
```

### Initial Screen
You'll see the Keycloak admin console with:
- Left sidebar: "Master" realm selector
- Navigation: Users, Groups, Roles, Clients, etc.
- Center area: Realm settings and statistics

---

## Step 2: Create the "werkflow" Realm

### Method 1: Via Admin Console (Easiest)

**1. Click Realm Selector** (top-left, shows "Master")
```
┌─────────────────────┐
│ Master    [v]       │  ← Click dropdown arrow
└─────────────────────┘
```

**2. Click "Create realm" button**
```
┌──────────────────────────────────────┐
│ Create realm                         │
└──────────────────────────────────────┘
```

**3. Fill Form**
- **Name**: `werkflow` (exact - case-sensitive)
- **Enabled**: Toggle ON
- Leave other fields as defaults

**4. Click "Create"**

**5. Verify**
- Realm selector now shows: "werkflow"
- Left menu updates to werkflow-specific options

---

## Step 3: Create Roles

### Create HR_ADMIN Role

**1. From werkflow realm, go to "Realm Roles"**
```
Left Menu:
  Realm Settings
  Users
  Groups
  Roles  ← Click here
  Clients
  Client Roles
```

**2. Click "Create role" button**

**3. Fill Form**
```
Name: HR_ADMIN
Description: Administrator for HR portal and workflow studio
Enabled: ON
```

**4. Click "Save"**

### Create Additional Roles (Optional but Recommended)

For complete setup, create these roles:

```
Role Name          Description
─────────────────────────────────────────────────
HR_ADMIN           Full access to studio and admin features
HR_USER            Can view and complete assigned tasks
HR_APPROVER        Can approve requests and workflows
MANAGER            Can manage team and workflows
```

**Process**:
1. Click "Create role" for each
2. Fill Name and Description
3. Save

---

## Step 4: Create OAuth2 Clients

Clients are applications (your frontends) that authenticate users.

### Client 1: Admin Portal

**1. Go to "Clients" in left menu**

**2. Click "Create client" button**

**3. First Screen - General Settings**
```
Client type: OpenID Connect
Client ID: werkflow-admin-portal
Name: Werkflow Admin Portal
Description: Process Studio and Admin Interface
```
**Next**

**4. Capability Config**
```
Client authentication: ON
Authorization: ON
Authentication flow:
  ✓ Standard flow
  ✓ Direct access grants
  □ Implicit flow
  □ Service account roles
```
**Next**

**5. Login Settings**
```
Root URL: http://localhost:4000
Home URL: http://localhost:4000
Valid redirect URIs: http://localhost:4000/*
Valid post logout redirect URIs: http://localhost:4000/*
Web origins: http://localhost:4000
```
**Save**

**6. Get Credentials**
```
Go to "Credentials" tab
Copy "Client Secret" (you'll need this for backend)
```

### Client 2: HR Portal (Optional - for later)

```
Client ID: werkflow-hr-portal
Root URL: http://localhost:4001
Valid redirect URIs: http://localhost:4001/*
Web origins: http://localhost:4001
```

---

## Step 5: Create Test User

### Create User

**1. Go to "Users" in left menu**

**2. Click "Create user" button**

**3. Fill Form**
```
Username: admin
Email: admin@werkflow.local
Email verified: ON
First name: Admin
Last name: User
Enabled: ON
```
**Create**

**4. Set Password**
```
Go to "Credentials" tab
Click "Set password"
Password: admin123 (or your choice)
Temporary: OFF (so you don't need to change on first login)
```

### Assign HR_ADMIN Role

**1. Go to "Role mappings" tab**

**2. Click "Assign role" button**

**3. Select "HR_ADMIN" from the list**

**4. Click "Assign"**

**5. Verify**
- "HR_ADMIN" now appears in "Assigned roles"

---

## Step 6: Configure Backend Services

Your Spring Boot backend needs Keycloak configuration. Check your `.env` files:

```bash
# .env.shared should contain:
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<copy from Keycloak credentials tab>

# Or in docker-compose.yml environment variables:
KC_DB_URL: jdbc:postgresql://keycloak-postgres:5432/keycloak_db
```

---

## Step 7: Test the Setup

### Test 1: Access Keycloak Realm
```bash
curl http://localhost:8090/realms/werkflow | jq .
```

Should return realm configuration (not error).

### Test 2: Login to Admin Portal
```
1. Go to http://localhost:4000
2. Click "Process Studio"
3. Should redirect to Keycloak login
4. Login with: admin / admin123
5. Redirects back to Studio
```

### Test 3: Access Processes Page
```
1. After login, you should be on /studio/processes
2. Should show:
   - "Process Designer" heading
   - "No processes deployed yet" (if empty)
   - Option to create new process
```

---

## Complete Keycloak Setup Checklist

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

---

## Architecture: Keycloak Authentication Flow

```
┌─────────────────────────────────────────────────────────┐
│  User Browser                                           │
│  ┌───────────────────────────────────────────────────┐ │
│  │  http://localhost:4000 (Admin Portal)             │ │
│  │  ┌─────────────────────────────────────────────┐ │ │
│  │  │  User clicks "Process Studio"               │ │ │
│  │  │  ↓                                            │ │ │
│  │  │  NextAuth middleware checks session         │ │ │
│  │  │  ↓                                            │ │ │
│  │  │  No session → Redirect to Keycloak login   │ │ │
│  │  └─────────────────────────────────────────────┘ │ │
└──────────────┬──────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────┐
│  Keycloak (http://localhost:8090)                       │
│  ┌───────────────────────────────────────────────────┐ │
│  │  werkflow Realm                                   │ │
│  │  ┌─────────────────────────────────────────────┐ │ │
│  │  │  Login Form                                 │ │ │
│  │  │  Username: [        ]                       │ │ │
│  │  │  Password: [        ]                       │ │ │
│  │  │  [Login]                                    │ │ │
│  │  └─────────────────────────────────────────────┘ │ │
│  │           ↓ (OAuth2 Authorization Code Flow)    │ │
│  │  Verify credentials against user database      │ │
│  │           ↓                                      │ │
│  │  Check user roles (HR_ADMIN, HR_USER, etc.)   │ │
│  │           ↓                                      │ │
│  │  Generate JWT token with roles               │ │
│  │           ↓                                      │ │
│  │  Redirect with authorization code            │ │
│  └───────────────────────────────────────────────┘ │ │
└──────────────┬──────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────┐
│  Admin Portal (Next.js)                                 │
│  ┌───────────────────────────────────────────────────┐ │
│  │  NextAuth handler receives authorization code    │ │
│  │           ↓                                        │ │
│  │  Exchange code for JWT token                     │ │
│  │           ↓                                        │ │
│  │  Extract user info + roles from token           │ │
│  │           ↓                                        │ │
│  │  Check: does user have HR_ADMIN role?           │ │
│  │  ├─ YES → Allow access to /studio/*             │ │
│  │  └─ NO  → Show "Access Denied" page             │ │
│  │           ↓                                        │ │
│  │  Create session cookie                          │ │
│  │           ↓                                        │ │
│  │  Redirect to /studio/processes                  │ │
│  └───────────────────────────────────────────────────┘ │
└──────────────┬──────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────┐
│  Studio Layout                                          │
│  ┌───────────────────────────────────────────────────┐ │
│  │  [Werkflow Studio] Processes | Forms | Services  │ │
│  │                                                   │ │
│  │  Process Designer                               │ │
│  │  [Process cards showing deployed processes]     │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## Key Concepts

### Realm
- Isolated workspace/application space
- Each realm has its own users, roles, and clients
- You only need one: "werkflow"

### Roles
- Authorization labels (what users are allowed to do)
- `HR_ADMIN` - Can access admin studio
- `HR_USER` - Can use portal and submit requests
- `MANAGER` - Can approve workflows

### Clients
- OAuth2 applications that authenticate users
- `werkflow-admin-portal` - Your frontend
- Backend services also authenticate as clients

### Users
- Human accounts that log in
- Have email, password, roles
- Can be members of groups

### Tokens (JWT)
- JSON Web Tokens issued after login
- Contains user info and roles
- Validated by backend on each API request
- Short-lived (expiration), can be refreshed

---

## File Locations for Configuration

After setting up Keycloak, verify these backend configs:

```
Project Root
├── .env.shared                    ← Keycloak client secret
├── .env.admin                     ← Admin service config
├── docker-compose.yml
│   └── keycloak environment       ← Keycloak DB config
└── infrastructure/docker/
    └── docker-compose.yml
        └── environment variables for all services
```

### Required Environment Variables

```bash
# .env.shared
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<get from Keycloak UI>

# .env.admin
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8090/realms/werkflow
```

---

## Troubleshooting

### Issue: "Realm does not exist" error

**Cause**: werkflow realm not created
**Fix**: Follow Step 2 above to create it

### Issue: "Access Denied" after login

**Cause**: User doesn't have HR_ADMIN role
**Fix**: Assign HR_ADMIN role in "Role mappings" tab

### Issue: Can't login (invalid credentials)

**Cause**: Wrong username/password or user doesn't exist
**Fix**:
- Check username spelling (case-sensitive)
- Reset password: Users → Find user → Credentials → Set password
- Verify user is enabled (toggle in user detail page)

### Issue: "Client not found"

**Cause**: werkflow-admin-portal client not created
**Fix**: Follow Step 4 above to create client

### Issue: Redirect loop (keeps redirecting to login)

**Cause**: Valid redirect URIs misconfigured
**Fix**:
- Go to Clients → werkflow-admin-portal → Settings
- Check "Valid redirect URIs" includes: `http://localhost:4000/*`
- Check "Web origins" includes: `http://localhost:4000`

### Issue: Token validation errors in backend logs

**Cause**: Client secret mismatch
**Fix**:
- In Keycloak: Clients → werkflow-admin-portal → Credentials
- Copy exact client secret
- Update `.env.shared` and `.env.admin`
- Restart services: `docker-compose restart`

---

## Quick Reference: Keycloak URLs

| Purpose | URL |
|---------|-----|
| Admin Console | http://localhost:8090/admin/master/console |
| Realm Endpoint | http://localhost:8090/realms/werkflow |
| Metrics | http://localhost:8090/metrics |
| Health | http://localhost:8090/health/ready |
| Token Endpoint | http://localhost:8090/realms/werkflow/protocol/openid-connect/token |
| Authorization | http://localhost:8090/realms/werkflow/protocol/openid-connect/auth |
| Userinfo | http://localhost:8090/realms/werkflow/protocol/openid-connect/userinfo |

---

## Testing Without UI (Advanced)

### Get Access Token via CLI

```bash
# 1. Request token from Keycloak
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=<YOUR_CLIENT_SECRET>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# 2. Decode token to see roles
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq .

# 3. Use token to call protected API
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/flowable/process-definitions
```

---

## Summary

**What you need**:
1. ✅ Keycloak running (you have this)
2. ❌ werkflow realm (create in Step 2)
3. ❌ HR_ADMIN role (create in Step 3)
4. ❌ OAuth2 client (create in Step 4)
5. ❌ Test user with HR_ADMIN role (create in Step 5)

**Why it matters**:
- Without werkflow realm → 404 errors
- Without HR_ADMIN role → "Access Denied" pages
- Without OAuth2 client → Can't authenticate

**After setup**:
- Login via Keycloak at http://localhost:8090
- Access Studio at http://localhost:4000
- View processes at http://localhost:4000/studio/processes
- Everything works!

