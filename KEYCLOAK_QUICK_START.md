# Keycloak Quick Start - 5 Minute Setup

## The Problem

You're getting **404 errors** on `/studio/processes` because:
1. **werkflow realm doesn't exist** in Keycloak
2. **HR_ADMIN role not created**
3. **Users not assigned roles**

---

## The Solution - Quick Steps

### Step 1: Open Keycloak Admin Console (1 minute)

```
URL: http://localhost:8090/admin/master/console
Username: admin
Password: admin123
```

### Step 2: Create "werkflow" Realm (2 minutes)

```
1. Click dropdown that says "Master" (top left)
2. Click "Create realm" button
3. Fill form:
   - Name: werkflow
   - Enabled: Toggle ON
4. Click "Create"
```

**Result**: Realm selector now shows "werkflow" ✅

### Step 3: Create "HR_ADMIN" Role (1 minute)

```
1. In left menu, click "Realm Roles"
2. Click "Create role" button
3. Fill form:
   - Name: HR_ADMIN
   - Description: Studio Admin Access
4. Click "Save"
```

**Result**: HR_ADMIN role exists ✅

### Step 4: Create OAuth2 Client (1 minute)

```
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
```

**Result**: OAuth2 client created ✅

### Step 5: Create User & Assign Role (1 minute)

```
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
```

**Result**: User created with HR_ADMIN role ✅

---

## Test It Works

### Test 1: Can you see the realm?
```bash
curl http://localhost:8090/realms/werkflow
```
Should return JSON (not error message)

### Test 2: Can you login to Studio?
```
1. Go to http://localhost:4000
2. Click "Process Studio"
3. Should redirect to Keycloak login
4. Login: admin / admin123
5. Should redirect to /studio/processes
6. Should show "Process Designer" page
```

---

## If Something Goes Wrong

| Problem | Fix |
|---------|-----|
| 404 on `/studio/processes` | Make sure werkflow realm exists |
| "Access Denied" page | Make sure user has HR_ADMIN role assigned |
| Can't login | Make sure user password is set (not temporary) |
| Keeps redirecting to login | Check Valid redirect URIs in client settings |
| "Client not found" error | Make sure Client ID is exactly: `werkflow-admin-portal` |

---

## All Setup Complete ✅

When you're done:
1. ✅ werkflow realm created
2. ✅ HR_ADMIN role created
3. ✅ OAuth2 client created
4. ✅ User created with HR_ADMIN role
5. ✅ Can login and access `/studio/processes`

---

## Next: Access Your Admin Portal

```
URL: http://localhost:4000
Login: admin / admin123
```

You should now see:
- Landing page with 3 buttons
- Click "Process Studio"
- See process designer page
- View any existing BPMN processes
- Create new processes

---

## Reference: Keycloak Admin Console Navigation

```
┌─────────────────────────────────────────────────┐
│ Keycloak Admin Console                          │
│                                                  │
│ Left Menu:                                      │
│ ├─ Realm Settings                              │
│ ├─ Users              ← Create/manage users     │
│ ├─ Groups             ← Organize users          │
│ ├─ Roles              ← Create HR_ADMIN role   │
│ ├─ Clients            ← Create OAuth2 client    │
│ ├─ Client Roles       ← Fine-grained roles     │
│ ├─ Identity Providers ← Social login setup     │
│ ├─ Mappers            ← Token customization    │
│ ├─ Mappers            ← User claim mapping     │
│ ├─ Sessions           ← Manage active sessions │
│ ├─ Events             ← Audit trail            │
│ └─ Realm Settings     ← Advanced config        │
│                                                  │
│ Top Info:                                       │
│ ├─ Realm Dropdown (shows "Master" → "werkflow") │
│ ├─ Help                                         │
│ └─ Admin User Menu                              │
└─────────────────────────────────────────────────┘
```

---

## Video Tutorial (if needed)

If you prefer a visual guide, see the Keycloak documentation:
```
https://www.keycloak.org/docs/latest/server_admin/
```

Sections to read:
- Creating and managing realms
- Creating and managing roles
- Creating and managing users
- Configuring OAuth 2.0 / OpenID Connect

---

## Environment Variables (For Reference)

After Keycloak setup, your backend will use:

```bash
# .env.shared (or docker-compose environment)
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<copy from Keycloak Credentials tab>
```

---

## That's It!

You now have:
- ✅ Keycloak realm
- ✅ Roles defined
- ✅ Users created
- ✅ OAuth2 configured
- ✅ Ready to access Studio

Go to: **http://localhost:4000/studio/processes** and enjoy! 🎉

