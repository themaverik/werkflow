# Authentication Flow Analysis and Troubleshooting Guide

## Executive Summary

This document analyzes the reported authentication issues with the Werkflow admin portal and provides comprehensive troubleshooting steps and solutions.

## Reported Issues

1. After attempting to login via Keycloak, the Keycloak admin interface is displayed instead of the Werkflow app UI
2. Keycloak redirects to `http://localhost:4000/portal/tasks` which gives a 404 error

## Root Cause Analysis

### Issue 1: Keycloak Admin Console Showing Instead of App UI

**Root Cause**: User is accessing the wrong Keycloak endpoint.

- **Incorrect**: Navigating directly to `http://localhost:8090/admin/master/console` (admin console)
- **Correct**: Should access `http://localhost:4000/login` (Werkflow app) → Click "Sign in with Keycloak"

The Keycloak admin console (`/admin/master/console`) is for managing Keycloak itself, not for user authentication in your application.

### Issue 2: 404 on /portal/tasks

**Root Cause**: This is actually NOT a 404 error. The route exists and is properly configured.

**Analysis**:
- Route exists at: `/Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal/app/(portal)/tasks/page.tsx`
- Middleware properly protects this route (requires authentication)
- The issue is likely that:
  1. User is not fully authenticated when reaching this page, OR
  2. There's an error during the authentication callback that's not being shown, OR
  3. The page is loading but showing an error state due to API call failures

## Configuration Verification

### Keycloak Configuration

**OIDC Discovery Endpoint** (Verified working):
```
http://localhost:8090/realms/werkflow/.well-known/openid-configuration
```

**Key Configuration Values**:
- Issuer: `http://localhost:8090/realms/werkflow`
- Authorization Endpoint: `http://localhost:8090/realms/werkflow/protocol/openid-connect/auth`
- Token Endpoint: `http://localhost:8090/realms/werkflow/protocol/openid-connect/token`
- Userinfo Endpoint: `http://localhost:8090/realms/werkflow/protocol/openid-connect/userinfo`

### Keycloak Client Configuration

**Client ID**: `werkflow-admin-portal`

**Configuration Details**:
```
Enabled: True
Protocol: openid-connect
Public Client: False (Confidential client)
Standard Flow Enabled: True
Implicit Flow Enabled: False
Direct Access Grants: True
Service Accounts: False

Redirect URIs:
  - http://localhost:4000/api/auth/callback/keycloak
  - http://localhost:4000/login

Web Origins:
  - http://localhost:4000

Root URL: http://localhost:4000
Base URL: (empty)
Admin URL: http://localhost:4000

PKCE Code Challenge Method: Not set
```

**Issue Found**: PKCE is not configured but there's a PKCE error in the logs.

### Admin Portal Configuration

**NextAuth Configuration** (`/Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal/auth.config.ts`):
```typescript
KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow (server-side)
KEYCLOAK_ISSUER_PUBLIC: http://localhost:8090/realms/werkflow (token validation)
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow (browser redirects)
KEYCLOAK_CLIENT_ID: werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
```

**Redirect Configuration** (`/Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal/app/login/page.tsx`):
```typescript
await signIn("keycloak", { redirectTo: "/portal/tasks" })
```

**Route Protection** (`/Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal/middleware.ts`):
- Routes starting with `/studio` and `/portal` are protected
- Unauthenticated users are redirected to `/login`

## Error Found in Logs

```
[auth][error] InvalidCheck: pkceCodeVerifier value could not be parsed.
```

This indicates a PKCE (Proof Key for Code Exchange) validation issue during the OAuth2 authorization code flow.

## Complete Authentication Flow

### Expected Flow

1. User navigates to `http://localhost:4000`
2. User clicks on "My Tasks" or navigates to `/portal/tasks`
3. Middleware detects unauthenticated user
4. User is redirected to `/login` with `callbackUrl=/portal/tasks`
5. User clicks "Sign in with Keycloak"
6. NextAuth initiates OAuth2 flow:
   - Generates PKCE code verifier and challenge (if enabled)
   - Redirects to Keycloak authorization endpoint
   - URL: `http://localhost:8090/realms/werkflow/protocol/openid-connect/auth`
   - Parameters include: client_id, redirect_uri, response_type=code, scope, state, code_challenge
7. User sees Keycloak login page at `localhost:8090`
8. User enters credentials (or uses existing session)
9. Keycloak validates credentials
10. Keycloak redirects back to: `http://localhost:4000/api/auth/callback/keycloak?code=...&state=...`
11. NextAuth receives callback:
    - Validates state parameter
    - Validates PKCE code_verifier
    - Exchanges authorization code for tokens at `http://keycloak:8080/realms/werkflow/protocol/openid-connect/token`
    - Validates token issuer matches `KEYCLOAK_ISSUER_PUBLIC`
12. NextAuth creates session
13. User is redirected to `/portal/tasks`
14. User sees the tasks page

### Where the Flow is Breaking

Based on the PKCE error, the flow is breaking at step 11 - during the callback validation.

## Solutions

### Solution 1: Configure PKCE in Keycloak (Recommended)

PKCE should be enabled for better security. Here's how to configure it:

#### Via Keycloak Admin Console

1. Open Keycloak admin console: `http://localhost:8090/admin`
2. Login with: `admin` / `admin123`
3. Select `werkflow` realm (top-left dropdown)
4. Navigate to: Clients → `werkflow-admin-portal`
5. Go to the "Advanced" tab
6. Scroll to "Advanced Settings" section
7. Find "Proof Key for Code Exchange Code Challenge Method"
8. Set to: `S256` (SHA-256)
9. Click "Save"

#### Via Keycloak Admin API

```bash
# Get admin token
TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/master/protocol/openid-connect/token' \
  -d 'client_id=admin-cli' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'grant_type=password' | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

# Update client to enable PKCE
curl -X PUT "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "pkce.code.challenge.method": "S256"
    }
  }'
```

### Solution 2: Disable PKCE in NextAuth (Less Secure)

If you don't want to use PKCE, you can disable it in NextAuth configuration:

**File**: `/Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal/auth.config.ts`

```typescript
Keycloak({
  clientId: process.env.KEYCLOAK_CLIENT_ID!,
  clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
  issuer: process.env.KEYCLOAK_ISSUER_PUBLIC || process.env.KEYCLOAK_ISSUER_INTERNAL,

  // Add this to disable PKCE
  checks: ["state"],  // Only check state, not PKCE

  authorization: {
    params: { scope: "openid email profile" },
    url: `${process.env.KEYCLOAK_ISSUER_BROWSER || process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/auth`,
  },
  token: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/token`,
  userinfo: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/userinfo`,
}),
```

**Note**: This is less secure and not recommended for production.

### Solution 3: Verify User Exists in Keycloak

Ensure you have a test user created in the `werkflow` realm:

1. Open Keycloak admin console: `http://localhost:8090/admin`
2. Login with: `admin` / `admin123`
3. Select `werkflow` realm
4. Navigate to: Users
5. If no users exist, click "Add user"
6. Create a test user:
   - Username: `testuser`
   - Email: `testuser@werkflow.com`
   - First name: `Test`
   - Last name: `User`
   - Click "Create"
7. Set password:
   - Go to "Credentials" tab
   - Click "Set password"
   - Enter password (e.g., `Test123!`)
   - Set "Temporary" to OFF
   - Click "Save"

## Testing Instructions

### Complete End-to-End Test

1. **Clear browser state**:
   ```bash
   # Use incognito/private browsing mode
   # OR clear cookies for localhost:4000 and localhost:8090
   ```

2. **Verify Keycloak is running**:
   ```bash
   curl http://localhost:8090/health/ready
   # Should return: {"status":"UP"}
   ```

3. **Verify admin-portal is running**:
   ```bash
   curl -I http://localhost:4000
   # Should return: HTTP/1.1 200 OK
   ```

4. **Test authentication flow**:
   - Open browser: `http://localhost:4000`
   - Click "My Tasks" (or any portal link)
   - You should be redirected to `/login`
   - Click "Sign in with Keycloak"
   - You should see Keycloak login page at `localhost:8090`
   - Enter credentials (testuser / Test123!)
   - You should be redirected back to `/portal/tasks`
   - Page should load successfully (may be empty if no tasks exist)

5. **Check for errors**:
   ```bash
   # Check admin-portal logs
   docker logs werkflow-admin-portal --tail 100

   # Check Keycloak logs
   docker logs werkflow-keycloak --tail 100
   ```

### Expected Behavior After Fix

1. **Login page** (`http://localhost:4000/login`):
   - Shows Werkflow branding
   - Has "Sign in with Keycloak" button

2. **Keycloak login** (`http://localhost:8090/realms/werkflow/protocol/openid-connect/auth?...`):
   - Shows Keycloak login form
   - Has Werkflow realm branding (if configured)
   - Accepts user credentials

3. **After successful login**:
   - Browser redirects to: `http://localhost:4000/api/auth/callback/keycloak?code=...`
   - Then immediately redirects to: `http://localhost:4000/portal/tasks`
   - Tasks page loads (shows "No tasks" if empty)

4. **NOT showing**:
   - Keycloak admin console (`/admin/master/console`)
   - 404 errors on `/portal/tasks`
   - PKCE validation errors

## Verification Commands

### Check Keycloak Client Configuration

```bash
# Get admin token
TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/master/protocol/openid-connect/token' \
  -d 'client_id=admin-cli' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'grant_type=password' | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

# Get client configuration
curl -s -X GET "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | python3 -m json.tool | grep -A 5 "pkce"
```

### Check OIDC Configuration

```bash
curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | python3 -m json.tool | grep -E "(issuer|authorization_endpoint|token_endpoint)"
```

### Test Token Exchange

```bash
# This requires an authorization code from the OAuth flow
# Cannot be tested directly without browser interaction
```

## Troubleshooting Decision Tree

```
Issue: Cannot login
├─ Are you accessing http://localhost:4000/login?
│  ├─ No → Use this URL, not Keycloak admin console
│  └─ Yes → Continue
├─ Does clicking "Sign in with Keycloak" redirect to Keycloak?
│  ├─ No → Check browser console for errors
│  └─ Yes → Continue
├─ Can you enter credentials and submit?
│  ├─ No → Check if user exists in Keycloak
│  └─ Yes → Continue
├─ After login, do you see errors in browser console?
│  ├─ Yes → Check for PKCE errors, apply Solution 1
│  └─ No → Continue
├─ Are you redirected back to localhost:4000?
│  ├─ No → Check redirect_uri in Keycloak client config
│  └─ Yes → Continue
└─ Does /portal/tasks show "404"?
   ├─ Yes → Check docker logs for actual error
   └─ No → Login successful!
```

## Common Mistakes

1. **Accessing Keycloak admin console directly**:
   - Don't go to `http://localhost:8090/admin`
   - Start from `http://localhost:4000`

2. **Not clearing browser cache**:
   - Old session cookies can cause issues
   - Use incognito mode for testing

3. **Wrong redirect URI**:
   - Must be `http://localhost:4000/api/auth/callback/keycloak`
   - Not `http://localhost:4000/portal/tasks`

4. **Missing PKCE configuration**:
   - NextAuth may require PKCE by default
   - Configure in Keycloak or disable in NextAuth

5. **Wrong realm**:
   - Must use `werkflow` realm, not `master`
   - Login URL should contain `/realms/werkflow`

## Additional Resources

- [NextAuth.js Documentation](https://next-auth.js.org/)
- [Keycloak OAuth2/OIDC Documentation](https://www.keycloak.org/docs/latest/securing_apps/#_oidc)
- [PKCE RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

## Support Checklist

When reporting authentication issues, provide:

- [ ] Browser console errors (F12 → Console)
- [ ] Network tab showing redirect chain (F12 → Network)
- [ ] Docker logs from admin-portal: `docker logs werkflow-admin-portal --tail 100`
- [ ] Docker logs from Keycloak: `docker logs werkflow-keycloak --tail 100`
- [ ] Screenshot of the issue
- [ ] Steps to reproduce
- [ ] Expected vs actual behavior
- [ ] OIDC issuer URL from: `curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | grep issuer`
- [ ] Client redirect URIs from Keycloak admin console

## Conclusion

The authentication system is properly configured, but there's a PKCE validation issue that needs to be resolved. Apply Solution 1 (configure PKCE in Keycloak) for the most secure setup, or Solution 2 (disable PKCE in NextAuth) for a quick fix during development.

The reported 404 error on `/portal/tasks` is likely a symptom of the PKCE error preventing successful authentication, not an actual missing route.
