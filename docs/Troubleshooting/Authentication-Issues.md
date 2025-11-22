# Authentication Issues

Comprehensive troubleshooting guide for authentication and authorization problems in Werkflow.

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [OAuth2 Configuration Errors](#oauth2-configuration-errors)
3. [Invalid Client Errors](#invalid-client-errors)
4. [PKCE Validation Errors](#pkce-validation-errors)
5. [Docker Networking Authentication Issues](#docker-networking-authentication-issues)
6. [Role and Permission Issues](#role-and-permission-issues)
7. [Session Management Issues](#session-management-issues)

## Quick Diagnostics

### Pre-Check: Verify System Health

```bash
# 1. Check all services running
docker ps | grep werkflow

# 2. Check Keycloak accessible
curl http://localhost:8090/health/ready

# 3. Verify realm exists
curl http://localhost:8090/realms/werkflow | jq .realm

# 4. Check admin portal accessible
curl -I http://localhost:4000
```

### Authentication Flow Test

```bash
# 1. Test session endpoint
curl http://localhost:4000/api/auth/session

# Should redirect to login (302) if not authenticated
# Should return JSON session data if authenticated

# 2. Test protected route
curl -I http://localhost:4000/studio/processes

# Should return 302 redirect to login (NOT 404)
```

## OAuth2 Configuration Errors

### Error: auth/error?error=Configuration

**Full URL**: `http://localhost:4000/api/auth/error?error=Configuration`

**Symptoms**:
- Login button leads to error page
- Cannot initiate OAuth flow
- Browser shows "Configuration error"

**Root Causes**:

1. OAuth2 client not created in Keycloak
2. Client secret mismatch
3. Missing or incorrect environment variables
4. Network connectivity issues

**Resolution Steps**:

**Step 1: Verify OAuth2 Client Exists**

```
1. Access Keycloak Admin Console: http://localhost:8090/admin/master/console
2. Login with: admin / admin123
3. Select Realm: werkflow (dropdown top-left)
4. Navigate to: Clients (left menu)
5. Search for: werkflow-admin-portal
6. If not found: Create client following OAuth2 Setup Guide
```

**Step 2: Verify Client Configuration**

```
1. Click on: werkflow-admin-portal
2. Check Settings tab:
   - Client authentication: ON
   - Standard flow: ENABLED
   - Valid redirect URIs: http://localhost:4000/*
   - Web origins: http://localhost:4000
3. Click Credentials tab:
   - Note the Client secret value
```

**Step 3: Verify Environment Variables**

```bash
# Check container environment
docker exec werkflow-admin-portal env | grep KEYCLOAK

# Must have:
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<matching-keycloak-secret>
KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
```

**Step 4: Update and Restart if Needed**

```bash
# If client secret doesn't match:
# Edit docker-compose.yml or set environment variable:
export KEYCLOAK_ADMIN_PORTAL_SECRET="<correct-secret>"

# Restart service
cd infrastructure/docker
docker-compose restart admin-portal

# Wait 10 seconds and test
sleep 10
curl http://localhost:4000/login
```

**Step 5: Verify Network Connectivity**

```bash
# Test container can reach Keycloak
docker exec werkflow-admin-portal curl http://keycloak:8080/realms/werkflow/.well-known/openid-configuration

# Should return OIDC configuration JSON
```

## Invalid Client Errors

### Error: invalid_client

**Symptoms**:
- Error during token exchange
- Logs show "invalid_client" or "Client authentication failed"
- OAuth callback fails

**Root Causes**:

1. Client secret mismatch
2. Client ID incorrect
3. Client authentication not enabled in Keycloak
4. Client credentials sent incorrectly

**Resolution Steps**:

**Step 1: Verify Client Secret Match**

```
1. Keycloak: Clients → werkflow-admin-portal → Credentials
2. Copy exact client secret value
3. Compare with docker-compose.yml KEYCLOAK_CLIENT_SECRET
4. Ensure no extra spaces, quotes, or special characters
```

**Step 2: Verify Client ID**

```bash
# Check environment variable
docker exec werkflow-admin-portal env | grep KEYCLOAK_CLIENT_ID

# Must exactly match Keycloak client ID (case-sensitive)
# Expected: werkflow-admin-portal
```

**Step 3: Verify Client Authentication Enabled**

```
1. Keycloak: Clients → werkflow-admin-portal → Settings
2. Scroll to "Capability config" section
3. Verify: Client authentication = ON
4. Click "Save" if changed
```

**Step 4: Check Client Type**

```
1. Settings tab
2. Capability config:
   - Standard flow: ENABLED
   - Direct access grants: ENABLED
   - Service accounts roles: Can be OFF
3. Access Type should be: confidential (if using client auth)
```

**Step 5: Test Token Endpoint Directly**

```bash
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=<your-secret>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123"

# Should return access_token
# If error, indicates client config issue
```

## PKCE Validation Errors

### Error: PKCE validation failed

**Symptoms**:
- Error during OAuth callback
- "code_verifier" or "code_challenge" mentioned in errors
- Authentication flow fails at token exchange

**Root Causes**:

1. PKCE not properly configured in client
2. Code verifier mismatch
3. Client configuration inconsistency

**Resolution Steps**:

**Step 1: Verify Client PKCE Settings**

```
1. Keycloak: Clients → werkflow-admin-portal → Settings
2. Advanced tab → Advanced Settings
3. Proof Key for Code Exchange Code Challenge Method:
   - Set to: S256 (recommended)
   - Or: plain
   - Or: (empty) to disable PKCE
4. Click "Save"
```

**Step 2: Verify NextAuth PKCE Configuration**

```typescript
// In auth.config.ts, Keycloak provider should handle PKCE automatically
// If issues persist, check NextAuth version
// NextAuth 5+ handles PKCE by default
```

**Step 3: Clear Browser Cache**

```
1. Browser DevTools → Application tab
2. Clear Site Data for localhost:4000
3. Restart browser
4. Try login flow again
```

## Docker Networking Authentication Issues

### Issue: Containers Cannot Communicate for Auth

**Symptoms**:
- "Failed to fetch" errors during token exchange
- "Connection refused" in container logs
- "ECONNREFUSED" errors
- Container trying to reach localhost instead of service name

**Diagnostic**:

```bash
# Test 1: Container to Keycloak connectivity
docker exec werkflow-admin-portal curl http://keycloak:8080/health/ready

# Should return: {"status":"UP"}

# Test 2: Verify network
docker network inspect werkflow-network | grep -A 3 keycloak

# Should show keycloak container on network

# Test 3: Check environment variables
docker exec werkflow-admin-portal env | grep KEYCLOAK_ISSUER

# Should be: http://keycloak:8080/realms/werkflow (NOT localhost)
```

**Resolution**:

**Fix 1: Correct Internal Issuer URL**

```yaml
# In docker-compose.yml admin-portal service
environment:
  KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow  # Internal network
  KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow  # Browser access
```

**Fix 2: Verify Network Configuration**

```yaml
# In docker-compose.yml, ensure services on same network
networks:
  - werkflow-network

# At bottom of file:
networks:
  werkflow-network:
    driver: bridge
```

**Fix 3: Restart Services**

```bash
docker-compose restart keycloak admin-portal
```

### Issue: Browser Cannot Reach Authorization Endpoint

**Symptoms**:
- "ERR_NAME_NOT_RESOLVED" for keycloak:8080
- Browser timeout when redirecting to Keycloak
- Authorization redirect fails

**Diagnostic**:

```bash
# Test browser accessibility
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# Should return OIDC metadata
```

**Resolution**:

```yaml
# Verify KEYCLOAK_ISSUER_BROWSER is set
environment:
  KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow

# Restart if added
docker-compose restart admin-portal
```

## Role and Permission Issues

### Issue: Access Denied After Successful Login

**Symptoms**:
- User can login successfully
- Redirected to "Access Denied" page
- Session exists but insufficient permissions

**Cause**: User missing required role (HR_ADMIN)

**Resolution Steps**:

**Step 1: Verify User Roles**

```
1. Keycloak Admin Console
2. Realm: werkflow
3. Users → Search for user
4. Click username
5. Role mappings tab
6. Check "Assigned roles" section
7. Should include: HR_ADMIN
```

**Step 2: Assign Missing Role**

```
1. On Role mappings tab
2. Click "Assign role" button
3. Search for: HR_ADMIN
4. Select checkbox
5. Click "Assign"
6. Verify role appears in "Assigned roles"
```

**Step 3: Verify Role in Token**

```bash
# Get access token for user
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "client_id=werkflow-admin-portal" \
  -d "client_secret=<secret>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# Decode token to see roles
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .

# Should show HR_ADMIN in roles or resource_access
```

**Step 4: User Re-Login**

```
1. User must logout completely
2. Clear browser cookies for localhost:4000
3. Login again
4. New token will include role
```

### Issue: Role Not Appearing in Session

**Symptoms**:
- Role assigned in Keycloak
- Still getting Access Denied
- Session doesn't show role

**Resolution**:

**Step 1: Check Role Mapper Configuration**

```
1. Keycloak: Clients → werkflow-admin-portal
2. Client scopes tab
3. Verify dedicated scope includes roles mapper
4. Or check realm roles are included by default
```

**Step 2: Force Token Refresh**

```
1. User logout from application
2. Clear browser cookies
3. Stop browser completely
4. Restart browser
5. Login again with fresh session
```

## Session Management Issues

### Issue: Session Not Persisting

**Symptoms**:
- Login required on every page load
- Session cookie not being set
- Constant redirects to login

**Diagnostic**:

```bash
# Check if NEXTAUTH_SECRET is set
docker exec werkflow-admin-portal env | grep NEXTAUTH_SECRET

# Should return a value (base64 string)
```

**Resolution**:

**Step 1: Set NEXTAUTH_SECRET**

```bash
# Generate secret
openssl rand -base64 32

# Add to docker-compose.yml
environment:
  NEXTAUTH_SECRET: <generated-secret>

# Restart
docker-compose restart admin-portal
```

**Step 2: Verify Cookies**

```
1. Browser DevTools → Application tab
2. Cookies → http://localhost:4000
3. Should see: authjs.session-token (or similar)
4. Check cookie is not expired
5. Check cookie domain and path
```

**Step 3: Check Browser Cookie Settings**

```
1. Ensure browser allows cookies
2. Check no extensions blocking cookies
3. Try incognito/private mode
4. Test different browser
```

### Issue: Session Expires Too Quickly

**Symptoms**:
- Logged out after few minutes
- Token expired errors
- Frequent re-authentication required

**Resolution**:

**Step 1: Increase Token Lifespan in Keycloak**

```
1. Keycloak: Realm settings → Tokens tab
2. Increase values:
   - Access Token Lifespan: 15 minutes (from 5 min)
   - SSO Session Idle: 1 hour (from 30 min)
   - SSO Session Max: 10 hours (from 10 hours)
   - Client Session Idle: 1 hour (from 30 min)
3. Click "Save"
```

**Step 2: Configure NextAuth Session**

```typescript
// In auth.config.ts
export const { handlers, auth, signIn, signOut } = NextAuth({
  session: {
    strategy: "jwt",
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },
  // ... rest of config
})
```

## See Also

- [OAuth2 Setup Guide](../OAuth2/OAuth2_Setup_Guide.md) - Initial Keycloak setup
- [OAuth2 Troubleshooting](../OAuth2/OAuth2_Troubleshooting.md) - OAuth2-specific issues
- [OAuth2 Docker Configuration](../OAuth2/OAuth2_Docker_Configuration.md) - Docker networking
- [Frontend Route Issues](./Frontend_Route_Issues.md) - Frontend routing problems

## Summary

Authentication issues typically fall into these categories:

1. **Configuration**: Missing client, wrong secrets, environment variables
2. **Network**: Docker DNS, port mapping, connectivity
3. **Roles**: Missing HR_ADMIN, role not in token
4. **Session**: Cookies not set, tokens expiring, secrets missing

Most issues can be resolved by:
- Verifying Keycloak client configuration
- Checking environment variables match
- Ensuring Docker networking is correct
- Assigning proper roles to users
- Setting NEXTAUTH_SECRET

Follow diagnostic steps systematically and check logs for specific error messages.
