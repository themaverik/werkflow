# Quick Fix for Authentication Issues

## Problem Summary

After clicking "Sign in with Keycloak" in the Werkflow admin portal, users experience:
1. Keycloak admin console showing instead of returning to the app
2. Error: "InvalidCheck: pkceCodeVerifier value could not be parsed"
3. Redirect to `/portal/tasks` that appears to fail

## Root Cause

The Keycloak client `werkflow-admin-portal` does not have PKCE (Proof Key for Code Exchange) configured, but NextAuth is trying to use it for enhanced security.

## Quick Fix (5 minutes)

### Option 1: Automated Fix (Recommended)

Run the provided fix script:

```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
./fix-auth-pkce.sh
```

This will:
- Connect to Keycloak admin API
- Configure PKCE with S256 method
- Verify the configuration

### Option 2: Manual Fix via Admin Console

1. Open Keycloak admin console:
   ```
   http://localhost:8090/admin
   ```

2. Login with:
   - Username: `admin`
   - Password: `admin123`

3. Select `werkflow` realm (top-left dropdown)

4. Navigate to: **Clients** → `werkflow-admin-portal`

5. Click on the **"Advanced"** tab

6. Scroll down to **"Advanced Settings"** section

7. Find **"Proof Key for Code Exchange Code Challenge Method"**

8. Set to: **`S256`**

9. Click **"Save"** at the bottom

### Option 3: Manual Fix via API

```bash
# Get admin token
TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/master/protocol/openid-connect/token' \
  -d 'client_id=admin-cli' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'grant_type=password' | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

# Get current client config
curl -s -X GET "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" > /tmp/client.json

# Add PKCE configuration
python3 << 'PYEOF'
import json
with open('/tmp/client.json', 'r+') as f:
    config = json.load(f)
    if 'attributes' not in config:
        config['attributes'] = {}
    config['attributes']['pkce.code.challenge.method'] = 'S256'
    f.seek(0)
    json.dump(config, f)
    f.truncate()
PYEOF

# Update client
curl -X PUT "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @/tmp/client.json
```

## Testing the Fix

1. **Clear browser state** (important!):
   - Use incognito/private browsing mode, OR
   - Clear cookies for `localhost:4000` and `localhost:8090`

2. **Navigate to the app**:
   ```
   http://localhost:4000
   ```

3. **Click on "My Tasks"** (or any portal link)
   - You should be redirected to `/login`

4. **Click "Sign in with Keycloak"**
   - You should see the Keycloak login page at `localhost:8090`
   - URL should contain `/realms/werkflow/protocol/openid-connect/auth`

5. **Login with your credentials**:
   - If no user exists, see "Create Test User" section below

6. **After successful login**:
   - You should be redirected back to `http://localhost:4000/portal/tasks`
   - Page should load successfully (may show "No tasks" if empty)

## Create Test User (if needed)

If you don't have a user in the `werkflow` realm:

1. Open Keycloak admin console: `http://localhost:8090/admin`
2. Login with: `admin` / `admin123`
3. Select `werkflow` realm
4. Navigate to: **Users**
5. Click **"Add user"**
6. Fill in:
   - Username: `testuser`
   - Email: `testuser@werkflow.com`
   - First name: `Test`
   - Last name: `User`
   - Email verified: ON
7. Click **"Create"**
8. Go to **"Credentials"** tab
9. Click **"Set password"**
10. Enter password: `Test123!`
11. Set **"Temporary"** to **OFF**
12. Click **"Save"**

## Verification

After applying the fix:

1. **Check PKCE is configured**:
   ```bash
   TOKEN=$(curl -s -X POST 'http://localhost:8090/realms/master/protocol/openid-connect/token' \
     -d 'client_id=admin-cli' \
     -d 'username=admin' \
     -d 'password=admin123' \
     -d 'grant_type=password' | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

   curl -s -X GET "http://localhost:8090/admin/realms/werkflow/clients/b13f4946-99d0-4c4d-9c54-7d6ad398ed4a" \
     -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys, json; c = json.load(sys.stdin); print("PKCE:", c.get("attributes", {}).get("pkce.code.challenge.method", "Not set"))'
   ```

   Should output: `PKCE: S256`

2. **Check no errors in logs**:
   ```bash
   docker logs werkflow-admin-portal --tail 50 2>&1 | grep -i "pkce\|error"
   ```

   Should not show PKCE errors after testing login

## Common Issues

### Issue: Still seeing Keycloak admin console

**Cause**: You're accessing the wrong URL.

**Fix**:
- Don't go to `http://localhost:8090/admin`
- Start from `http://localhost:4000` and click "Sign in with Keycloak"

### Issue: 404 error on /portal/tasks

**Cause**: Authentication failed, so middleware redirected you before the page could load.

**Fix**:
- Apply the PKCE fix above
- Clear browser cookies
- Check browser console for errors (F12 → Console)
- Check docker logs: `docker logs werkflow-admin-portal --tail 100`

### Issue: "Invalid credentials" when logging in

**Cause**: User doesn't exist or wrong password.

**Fix**:
- Create a test user (see "Create Test User" section)
- Or reset password in Keycloak admin console

### Issue: Script fails with "Keycloak is not running"

**Cause**: Keycloak container is not running.

**Fix**:
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
docker compose up -d keycloak
# Wait 30 seconds for Keycloak to start
docker logs werkflow-keycloak --tail 20
```

## What This Fix Does

### Before (Without PKCE)

NextAuth generates a PKCE code challenge and expects Keycloak to validate it, but Keycloak is not configured to do so. This causes a mismatch during the callback, resulting in the error:

```
InvalidCheck: pkceCodeVerifier value could not be parsed
```

### After (With PKCE)

1. NextAuth generates a random `code_verifier`
2. NextAuth creates a SHA-256 hash of it as `code_challenge`
3. Authorization request includes `code_challenge` and `code_challenge_method=S256`
4. Keycloak stores the `code_challenge`
5. After login, Keycloak redirects with authorization `code`
6. NextAuth sends `code` and original `code_verifier` to token endpoint
7. Keycloak verifies: `SHA256(code_verifier) == code_challenge`
8. If match, Keycloak issues tokens
9. NextAuth receives tokens and creates session
10. User is redirected to `/portal/tasks` successfully

This prevents authorization code interception attacks and is considered a security best practice.

## Additional Resources

- Full analysis: `Authentication-Flow-Analysis.md`
- Keycloak verification: `./verify-keycloak.sh`
- Keycloak configuration: `Keycloak-Hostname-Configuration.md`

## Need More Help?

If the issue persists after applying this fix:

1. Review the full analysis document: `Authentication-Flow-Analysis.md`
2. Check docker logs:
   ```bash
   docker logs werkflow-admin-portal --tail 100
   docker logs werkflow-keycloak --tail 100
   ```
3. Check browser console (F12 → Console) for JavaScript errors
4. Verify OIDC configuration:
   ```bash
   curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | python3 -m json.tool | grep issuer
   ```
   Should show: `http://localhost:8090/realms/werkflow`

## Success Criteria

After applying the fix, you should be able to:

- [x] Access `http://localhost:4000` without errors
- [x] Click "Sign in with Keycloak" and see Keycloak login page
- [x] Login with credentials
- [x] Be redirected back to `http://localhost:4000/portal/tasks`
- [x] See the tasks page (even if empty)
- [x] No PKCE errors in docker logs
- [x] No 404 errors
- [x] No Keycloak admin console showing unexpectedly
