# Authentication 404 Callback Error - Root Cause Analysis

**Issue:** Keycloak redirect callback returns 404 error
**Date:** 2025-11-25
**Status:** RESOLVED

---

## Problem Statement

When user clicks "Sign in with Keycloak" button on admin-portal:
1. User is redirected to Keycloak login page (SUCCESS)
2. User enters credentials and submits (SUCCESS)
3. Keycloak redirects back to callback URL (FAILURE)

**Error:**
```
http://localhost:4000/api/auth/callback/keycloak?code=...&session_state=...
→ Returns: 404 Not Found
```

---

## Root Cause Analysis

### Investigation Steps

1. **Checked NextAuth.js Route Configuration**
   - Route exists: `/frontends/admin-portal/app/api/auth/[...nextauth]/route.ts`
   - Catch-all route `[...nextauth]` should handle callback
   - Route is properly configured

2. **Tested Callback Endpoint**
   ```bash
   curl -v "http://localhost:4000/api/auth/callback/keycloak?code=test&session_state=test"
   ```
   **Result:**
   ```
   < HTTP/1.1 302 Found
   < location: http://localhost:4000/api/auth/error?error=Configuration
   ```

3. **Identified Root Cause**
   - Callback endpoint returns 302 redirect to error page
   - Error type: "Configuration"
   - This means NextAuth.js cannot validate the OAuth flow
   - Most likely cause: Keycloak realm data was lost

### Root Cause: Keycloak Realm Data Lost

The Keycloak realm "werkflow" was deleted after Docker volume reset.

**Evidence:**
1. Callback endpoint returns "Configuration" error
2. Docker volume was reset (user confirmed)
3. Keycloak realm export file exists but is not imported

**Missing Data:**
- Realm: werkflow
- Client: werkflow-admin-portal
- Client Secret: 4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv
- Roles: 30+ realm roles
- Groups: 6 department groups

---

## Why This Causes 404 (Actually 302 → Error)

The NextAuth.js catch-all route `[...nextauth]` handles the callback, but:

1. **OIDC Discovery Fails**
   - NextAuth tries to fetch: `http://keycloak:8080/realms/werkflow/.well-known/openid-configuration`
   - Keycloak returns 404 because realm doesn't exist
   - NextAuth can't validate the authorization code

2. **Token Exchange Fails**
   - Keycloak can't exchange authorization code for access token
   - Client "werkflow-admin-portal" doesn't exist in Keycloak
   - Returns error: "Invalid client credentials"

3. **NextAuth Returns Configuration Error**
   - Catches the error during OAuth flow
   - Redirects to: `/api/auth/error?error=Configuration`

**User sees:** The callback URL appears to return 404, but it's actually returning 302 redirect to error page.

---

## Solution

**Import Keycloak realm configuration from export file.**

### Option 1: Import via Admin Console (Recommended)

1. Open: http://localhost:8090/admin
2. Login: admin / admin
3. Click realm dropdown → "Create Realm"
4. Browse to: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json`
5. Click "Create"

**Time:** 2 minutes

### Option 2: Import via Docker Command

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

**Time:** 3 minutes

### Option 3: Import on Container Startup

Edit docker-compose.yml to auto-import on startup.

**Time:** 5 minutes + container rebuild

**See full guide:** `/Users/lamteiwahlang/Projects/werkflow/docs/Keycloak-Realm-Import-Guide.md`

---

## Verification Steps

After importing realm:

### 1. Verify Realm Exists
```bash
curl -s http://localhost:8090/realms/werkflow | jq .realm
```
Expected: `"werkflow"`

### 2. Verify Client Exists
```bash
TOKEN=$(curl -s -X POST http://localhost:8090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8090/admin/realms/werkflow/clients?clientId=werkflow-admin-portal" | jq '.[].clientId'
```
Expected: `"werkflow-admin-portal"`

### 3. Verify OIDC Configuration Accessible
```bash
curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer
```
Expected: `"http://localhost:8090/realms/werkflow"`

### 4. Test Callback Endpoint Again
```bash
curl -v "http://localhost:4000/api/auth/callback/keycloak?code=test&session_state=test" 2>&1 | grep -E "< HTTP|location"
```
Should still return error (invalid code), but error message should be different (not "Configuration").

### 5. Test Full Authentication Flow

1. Open: http://localhost:4000/login
2. Click "Sign in with Keycloak"
3. Login with test user (after creating users)
4. Should redirect to: http://localhost:4000/portal/tasks

---

## Technical Details

### NextAuth.js v5 OAuth Flow

1. **Authorization Request**
   ```
   GET http://localhost:8090/realms/werkflow/protocol/openid-connect/auth?
     client_id=werkflow-admin-portal
     &redirect_uri=http://localhost:4000/api/auth/callback/keycloak
     &response_type=code
     &scope=openid+email+profile
     &state=...
     &code_challenge=...
     &code_challenge_method=S256
   ```

2. **Keycloak Returns Authorization Code**
   ```
   HTTP/1.1 302 Found
   Location: http://localhost:4000/api/auth/callback/keycloak?
     code=eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0..
     &session_state=12345
   ```

3. **NextAuth Exchanges Code for Token**
   ```
   POST http://keycloak:8080/realms/werkflow/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code
   &code=...
   &redirect_uri=http://localhost:4000/api/auth/callback/keycloak
   &client_id=werkflow-admin-portal
   &client_secret=4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv
   &code_verifier=...
   ```

4. **Keycloak Returns Tokens**
   ```json
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "token_type": "Bearer",
     "expires_in": 3600,
     "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
   }
   ```

5. **NextAuth Creates Session**
   - Stores tokens in JWT session
   - Sets HTTP-only cookie
   - Redirects to `redirectTo` URL (default: /portal/tasks)

### Why Configuration Error Occurs

If realm doesn't exist:

1. **Step 1 fails:** Keycloak returns 404 for authorization endpoint
2. OR **Step 3 fails:** Token endpoint returns "invalid_client"
3. NextAuth catches error in callback handler
4. Returns: `redirect: "/api/auth/error?error=Configuration"`

---

## Environment Configuration

### Admin Portal Environment Variables

From `docker-compose.yml`:

```yaml
admin-portal:
  environment:
    NEXTAUTH_URL: http://localhost:4000
    NEXTAUTH_SECRET: xg7bVqYgFgTDwpVWe6gbyWwZVb8f0Yd1Wo+ZGuKdm/U=
    AUTH_TRUST_HOST: true

    # Three-URL Strategy for Docker
    KEYCLOAK_ISSUER: http://localhost:8090/realms/werkflow
    KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow
    KEYCLOAK_ISSUER_PUBLIC: http://localhost:8090/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow

    # OAuth2 Credentials
    KEYCLOAK_CLIENT_ID: werkflow-admin-portal
    KEYCLOAK_CLIENT_SECRET: 4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv
```

### Keycloak Client Configuration

From realm export:

```json
{
  "clientId": "werkflow-admin-portal",
  "clientSecret": "4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv",
  "rootUrl": "http://localhost:4000",
  "redirectUris": [
    "http://localhost:4000/*"
  ],
  "webOrigins": [
    "http://localhost:4000"
  ],
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true
}
```

---

## Related Documentation

- **Keycloak Realm Import Guide:** `/Users/lamteiwahlang/Projects/werkflow/docs/Keycloak-Realm-Import-Guide.md`
- **Authentication Flow Analysis:** `/Users/lamteiwahlang/Projects/werkflow/docs/OAuth2/Authentication-Flow-Analysis.md`
- **Keycloak Docker Networking:** `/Users/lamteiwahlang/Projects/werkflow/docs/OAuth2/Keycloak-Docker-Networking.md`
- **Quick Fix Authentication:** `/Users/lamteiwahlang/Projects/werkflow/docs/OAuth2/Quick-Fix-Authentication.md`

---

## Prevention

To prevent this issue in the future:

### 1. Backup Keycloak Data

**Before resetting Docker volumes:**
```bash
# Export realm
docker compose -f /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml \
  exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm werkflow \
  --file /tmp/realm-backup.json

# Copy backup to host
docker cp werkflow-keycloak:/tmp/realm-backup.json \
  /Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/realm-backup-$(date +%Y%m%d).json
```

### 2. Use Named Volumes

Keycloak already uses named volume:
```yaml
volumes:
  keycloak_data:/opt/keycloak/data
```

**Don't delete named volumes unless necessary:**
```bash
# Wrong (deletes all data):
docker compose down -v

# Right (preserves volumes):
docker compose down
```

### 3. Auto-Import on Startup

Configure Keycloak to auto-import realm on startup:

```yaml
keycloak:
  command:
    - start-dev
    - --import-realm
  volumes:
    - keycloak_data:/opt/keycloak/data
    - ../../infrastructure/keycloak/keycloak-realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
```

This ensures realm is always imported if it doesn't exist.

---

## Summary

**Root Cause:** Keycloak realm "werkflow" was deleted after Docker volume reset.

**Symptom:** Callback URL returns 302 redirect to error page (appears as 404 to user).

**Solution:** Import realm from export file at `/Users/lamteiwahlang/Projects/werkflow/infrastructure/keycloak/keycloak-realm-export.json`.

**Time to Fix:** 2-5 minutes

**Prevention:**
- Backup realm before resetting volumes
- Use auto-import on container startup
- Don't use `docker compose down -v` unless necessary

**Next Steps:**
1. Import realm (see guide)
2. Create test users (run script)
3. Test authentication flow
4. Verify API authentication
