# Authentication Issues - Root Cause and Resolution

## Issues Reported

1. After attempting to login via Keycloak, the Keycloak admin interface is displayed instead of the Werkflow app UI
2. Keycloak redirects to `http://localhost:4000/portal/tasks` which gives a 404 error

## Root Cause Identified

### Primary Issue: PKCE Configuration Mismatch

The authentication flow was failing due to a **PKCE (Proof Key for Code Exchange)** configuration mismatch:

- **NextAuth** (the authentication library used in admin-portal) was generating PKCE code challenges by default
- **Keycloak** client `werkflow-admin-portal` was NOT configured to validate PKCE
- This caused the error: `InvalidCheck: pkceCodeVerifier value could not be parsed`

### Secondary Issue: User Confusion

The "Keycloak admin showing" issue was due to:
- User accessing `http://localhost:8090/admin` directly (Keycloak admin console)
- Instead of `http://localhost:4000` (Werkflow app) → "Sign in with Keycloak"

### Tertiary Issue: 404 Misinterpretation

The `/portal/tasks` route actually exists and works correctly. The perceived "404" was likely:
- Authentication failing silently
- Middleware redirecting unauthenticated users
- Error state showing in the UI without proper error messaging

## Configuration Analysis

### Current Configuration (Verified Correct)

**Keycloak OIDC Configuration**:
```
Issuer: http://localhost:8090/realms/werkflow
Authorization: http://localhost:8090/realms/werkflow/protocol/openid-connect/auth
Token: http://localhost:8090/realms/werkflow/protocol/openid-connect/token
Userinfo: http://localhost:8090/realms/werkflow/protocol/openid-connect/userinfo
```

**Keycloak Client `werkflow-admin-portal`**:
```
Client ID: werkflow-admin-portal
Enabled: True
Standard Flow: Enabled
Redirect URIs:
  - http://localhost:4000/api/auth/callback/keycloak
  - http://localhost:4000/login
Web Origins:
  - http://localhost:4000
```

**Admin Portal Configuration**:
```
NEXTAUTH_URL: http://localhost:4000
KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_PUBLIC: http://localhost:8090/realms/werkflow
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID: werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
```

### What Was Missing

**Before Fix**:
```
PKCE Code Challenge Method: Not set
```

**After Fix**:
```
PKCE Code Challenge Method: S256
```

## Resolution Applied

### Fix Applied on: 2025-11-21

The PKCE configuration has been successfully applied to the Keycloak client:

```bash
# Executed: ./fix-auth-pkce.sh
# Result: PKCE configured with S256 method
# Verification: Confirmed via Keycloak Admin API
```

### What the Fix Does

1. **Before (Without PKCE)**:
   ```
   Browser → Keycloak → Authorization Code → NextAuth
   NextAuth sends: code + code_verifier
   Keycloak ignores code_verifier
   NextAuth expects validation → FAILS
   Error: "InvalidCheck: pkceCodeVerifier value could not be parsed"
   ```

2. **After (With PKCE S256)**:
   ```
   Browser → Keycloak → Authorization Code → NextAuth
   NextAuth sends: code + code_verifier
   Keycloak validates: SHA256(code_verifier) == stored_code_challenge
   Validation succeeds → Issues tokens
   NextAuth creates session → SUCCESS
   ```

## Expected Behavior After Fix

### Correct Login Flow

1. User navigates to `http://localhost:4000`
2. User clicks "My Tasks" (or any protected route)
3. Middleware detects no authentication
4. User redirected to `/login`
5. User clicks "Sign in with Keycloak"
6. Browser redirects to Keycloak login page:
   ```
   http://localhost:8090/realms/werkflow/protocol/openid-connect/auth?...
   ```
7. User enters credentials and submits
8. Keycloak validates credentials
9. Keycloak redirects back with authorization code:
   ```
   http://localhost:4000/api/auth/callback/keycloak?code=...&state=...
   ```
10. NextAuth exchanges code for tokens (PKCE validation succeeds)
11. NextAuth creates session
12. User redirected to `/portal/tasks`
13. Page loads successfully

### What User Should NOT See

- Keycloak admin console (`/admin/master/console`)
- PKCE validation errors
- 404 errors on `/portal/tasks`
- Infinite redirect loops
- "Access denied" errors (unless user has no roles)

## Testing Instructions

### Complete End-to-End Test

1. **Clear browser state**:
   ```bash
   # Option 1: Use incognito/private browsing mode
   # Option 2: Clear cookies for localhost:4000 and localhost:8090
   ```

2. **Access the application**:
   ```
   http://localhost:4000
   ```

3. **Initiate login**:
   - Click "My Tasks" or any portal feature
   - You should be redirected to `/login`
   - Click "Sign in with Keycloak"

4. **Complete authentication**:
   - Should see Keycloak login page at `localhost:8090`
   - Enter credentials (e.g., testuser / Test123!)
   - Submit form

5. **Verify successful redirect**:
   - Browser should redirect to: `http://localhost:4000/portal/tasks`
   - Page should load without errors
   - No console errors about PKCE

6. **Verify session**:
   - Navigate to other protected routes (e.g., `/studio/processes`)
   - Should not be prompted to login again
   - User is authenticated

### Verification Commands

```bash
# Check PKCE is configured
TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/master/protocol/openid-connect/token' \
  -d 'client_id=admin-cli' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'grant_type=password' | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

curl -s -X GET "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
  -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys, json; c = json.load(sys.stdin); print("PKCE:", c.get("attributes", {}).get("pkce.code.challenge.method", "Not set"))'

# Expected output: PKCE: S256
```

```bash
# Check no PKCE errors in logs
docker logs werkflow-admin-portal --tail 100 | grep -i pkce

# Should return empty or old errors (before fix)
```

```bash
# Check Keycloak is healthy
curl http://localhost:8090/health/ready

# Expected: {"status":"UP"}
```

## Files Created

### Documentation

1. **Authentication-Flow-Analysis.md**
   - Complete technical analysis
   - Detailed configuration verification
   - Troubleshooting decision tree
   - Testing instructions

2. **Quick-Fix-Authentication.md**
   - Quick start guide
   - Step-by-step fix instructions
   - Common issues and solutions
   - Testing checklist

3. **AUTHENTICATION-ISSUES-RESOLVED.md** (this file)
   - Executive summary
   - Root cause analysis
   - Resolution confirmation
   - Expected behavior

### Scripts

1. **fix-auth-pkce.sh**
   - Automated PKCE configuration script
   - Connects to Keycloak Admin API
   - Updates client configuration
   - Verifies changes applied

## Resolution Checklist

- [x] Root cause identified (PKCE configuration missing)
- [x] Fix applied via Keycloak Admin API
- [x] PKCE configured with S256 method
- [x] Configuration verified
- [x] Documentation created
- [x] Fix script provided for reproducibility
- [x] Testing instructions provided
- [ ] User testing completed (requires user action)

## Next Steps for User

1. **Clear browser cookies** or use incognito mode

2. **Test the login flow**:
   ```
   http://localhost:4000 → My Tasks → Sign in with Keycloak → Login → Success
   ```

3. **Verify no errors**:
   ```bash
   docker logs werkflow-admin-portal --tail 50
   ```

4. **Create test user if needed**:
   - Access: `http://localhost:8090/admin`
   - Realm: `werkflow`
   - Users → Add user → Set credentials

5. **Report any remaining issues** with:
   - Browser console errors
   - Docker logs
   - Screenshot of issue
   - Steps to reproduce

## Additional Resources

- **Full Analysis**: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/Authentication-Flow-Analysis.md`
- **Quick Fix Guide**: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/Quick-Fix-Authentication.md`
- **Fix Script**: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/fix-auth-pkce.sh`
- **Keycloak Config**: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/Keycloak-Hostname-Configuration.md`

## Technical Details

### PKCE (Proof Key for Code Exchange)

PKCE is an OAuth 2.0 security extension (RFC 7636) that prevents authorization code interception attacks:

1. Client generates random `code_verifier` (43-128 characters)
2. Client creates `code_challenge = SHA256(code_verifier)`
3. Authorization request includes `code_challenge` and `code_challenge_method=S256`
4. Authorization server stores `code_challenge`
5. Token request includes original `code_verifier`
6. Authorization server verifies: `SHA256(code_verifier) == stored_code_challenge`
7. If match, issues tokens

This ensures that even if the authorization code is intercepted, it cannot be used without the original `code_verifier`.

### Why This Matters

- **Security**: Protects against authorization code interception
- **Best Practice**: Recommended by OAuth 2.0 Security Best Practices
- **Required by NextAuth**: NextAuth.js uses PKCE by default for confidential clients
- **Standards Compliance**: Aligns with modern OAuth 2.0 implementations

## Support

If issues persist after applying this fix:

1. **Check logs**:
   ```bash
   docker logs werkflow-admin-portal --tail 100 | grep -E "error|Error|ERROR"
   docker logs werkflow-keycloak --tail 100 | grep -E "error|Error|ERROR"
   ```

2. **Verify configuration**:
   ```bash
   cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
   ./verify-keycloak.sh
   ```

3. **Re-apply fix**:
   ```bash
   cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
   ./fix-auth-pkce.sh
   ```

4. **Review documentation**:
   - `Authentication-Flow-Analysis.md` - Complete technical details
   - `Quick-Fix-Authentication.md` - Step-by-step guide

## Status

**Resolution Status**: APPLIED

**Date Fixed**: 2025-11-21

**Verification**: PKCE S256 configured and confirmed

**Testing**: Pending user validation

**Documentation**: Complete

**Scripts**: Provided and tested

---

**NOTE**: The fix has been applied to the Keycloak server. Users must clear browser cookies or use incognito mode for the fix to take effect on their first login attempt after the fix.
