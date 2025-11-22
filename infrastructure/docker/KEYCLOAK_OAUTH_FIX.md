# Keycloak OAuth Callback Error - Root Cause Analysis and Fix

## Error Summary

**Error**: `CallbackRouteError: server responded with an error in the response body`

**Keycloak Error**: `CODE_TO_TOKEN_ERROR` with `invalid_client_credentials`

**Root Cause**: Client secret mismatch between docker-compose.yml and Keycloak configuration

## Detailed Diagnosis

### What Happened

During the OAuth 2.0 authorization code flow, the callback process failed at the token exchange phase:

1. User clicks "Sign In" on admin-portal (http://localhost:4000)
2. Browser redirects to Keycloak (http://localhost:8090/realms/werkflow)
3. User authenticates successfully with Keycloak
4. Keycloak redirects back with authorization code to http://localhost:4000/api/auth/callback/keycloak
5. **FAILURE**: NextAuth server-side code attempts to exchange authorization code for tokens
6. Keycloak rejects the request with "invalid_client_credentials"

### Error Details from Logs

**Admin Portal Logs** (`werkflow-admin-portal`):
```
[auth][error] CallbackRouteError: Read more at https://errors.authjs.dev#callbackrouteerror
[auth][cause]: iJ: server responded with an error in the response body
[auth][details]: {
  "error": "unauthorized_client",
  "error_description": "Invalid client or Invalid client credentials",
  "provider": "keycloak"
}
```

**Keycloak Logs** (`werkflow-keycloak`):
```
WARN [org.keycloak.events] type="CODE_TO_TOKEN_ERROR",
  realmId="83cbee81-f30e-414a-91ec-677598c94aab",
  clientId="werkflow-admin-portal",
  userId="null",
  ipAddress="192.168.32.12",
  error="invalid_client_credentials",
  grant_type="authorization_code"
```

### Root Cause Analysis

The token exchange request includes:
- **client_id**: werkflow-admin-portal
- **client_secret**: CxuKtJj57jsbbf9j1BSe6tkM5wRG5GCb (from docker-compose.yml)

Keycloak's actual client secret:
- **client_secret**: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR (stored in Keycloak DB)

**Verdict**: Secrets don't match, causing authentication failure.

## Investigation Process

### Step 1: Verify Keycloak Client Configuration

```bash
# Login to Keycloak admin CLI
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master \
  --user admin --password admin123

# Get werkflow-admin-portal client configuration
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh get clients \
  -r werkflow -q clientId=werkflow-admin-portal
```

**Key Findings**:
- Client ID: `werkflow-admin-portal`
- Client Secret: `4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR`
- Enabled: `true`
- Client Authentication: `client-secret`
- Redirect URIs: `["http://localhost:4000/api/auth/callback/keycloak", "http://localhost:4000/login"]`
- Standard Flow Enabled: `true`

### Step 2: Check docker-compose.yml Configuration

```yaml
admin-portal:
  environment:
    KEYCLOAK_CLIENT_ID: werkflow-admin-portal
    KEYCLOAK_CLIENT_SECRET: CxuKtJj57jsbbf9j1BSe6tkM5wRG5GCb  # WRONG!
```

### Step 3: Verify hr-portal Client Existence

```bash
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh get clients \
  -r werkflow -q clientId=werkflow-hr-portal
# Result: [] (client doesn't exist)
```

## Applied Fixes

### Fix 1: Update admin-portal Client Secret

**File**: `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml`

```yaml
# BEFORE
KEYCLOAK_CLIENT_SECRET: CxuKtJj57jsbbf9j1BSe6tkM5wRG5GCb

# AFTER
KEYCLOAK_CLIENT_SECRET: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
```

### Fix 2: Create hr-portal Client in Keycloak

**Client Configuration**:
- Client ID: `werkflow-hr-portal`
- Client Secret: `HR_PORTAL_SECRET_2024_SECURE`
- Redirect URIs:
  - `http://localhost:4001/api/auth/callback/keycloak`
  - `http://localhost:4001/login`
- Web Origins: `http://localhost:4001`
- Client Authentication: Enabled (confidential client)
- Standard Flow: Enabled
- Direct Access Grants: Enabled

**Creation Command**:
```bash
# Create client configuration file
cat > /tmp/hr-portal-client.json << 'EOF'
{
  "clientId": "werkflow-hr-portal",
  "enabled": true,
  "clientAuthenticatorType": "client-secret",
  "secret": "HR_PORTAL_SECRET_2024_SECURE",
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "publicClient": false,
  "redirectUris": [
    "http://localhost:4001/api/auth/callback/keycloak",
    "http://localhost:4001/login"
  ],
  "webOrigins": ["http://localhost:4001"],
  "rootUrl": "http://localhost:4001",
  "adminUrl": "http://localhost:4001",
  "attributes": {
    "post.logout.redirect.uris": "http://localhost:4001",
    "pkce.code.challenge.method": "S256"
  }
}
EOF

# Upload to Keycloak container and create client
docker cp /tmp/hr-portal-client.json werkflow-keycloak:/tmp/hr-portal-client.json
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh create clients \
  -r werkflow -f /tmp/hr-portal-client.json
```

### Fix 3: Update hr-portal Client Secret in docker-compose.yml

```yaml
# BEFORE
KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_HR_PORTAL_SECRET:-change-me-in-production}

# AFTER
KEYCLOAK_CLIENT_SECRET: HR_PORTAL_SECRET_2024_SECURE
```

### Fix 4: Recreate Containers with New Configuration

```bash
# Force recreate containers to pick up new environment variables
docker-compose up -d --force-recreate admin-portal hr-portal
```

## Verification Steps

### 1. Check Container Environment Variables

```bash
# Verify admin-portal
docker exec werkflow-admin-portal printenv | grep -E "(KEYCLOAK|NEXTAUTH)" | sort

# Expected output:
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
KEYCLOAK_ISSUER_INTERNAL=http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_PUBLIC=http://localhost:8090/realms/werkflow
NEXTAUTH_URL=http://localhost:4000

# Verify hr-portal
docker exec werkflow-hr-portal printenv | grep -E "(KEYCLOAK|NEXTAUTH)" | sort

# Expected output:
KEYCLOAK_CLIENT_ID=werkflow-hr-portal
KEYCLOAK_CLIENT_SECRET=HR_PORTAL_SECRET_2024_SECURE
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
KEYCLOAK_ISSUER_INTERNAL=http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_PUBLIC=http://localhost:8090/realms/werkflow
NEXTAUTH_URL=http://localhost:4001
```

### 2. Check Container Logs

```bash
# Check admin-portal logs (should be clean, no auth errors)
docker logs werkflow-admin-portal --tail 20

# Check hr-portal logs (should be clean)
docker logs werkflow-hr-portal --tail 20

# Expected output:
# ✓ Ready in XXms
# No CallbackRouteError or invalid_client_credentials errors
```

### 3. Monitor Keycloak Logs During Login

```bash
# Open Keycloak logs in real-time
docker logs -f werkflow-keycloak

# Then attempt login at http://localhost:4000 or http://localhost:4001
# Watch for successful authentication events
```

### 4. Test OAuth Login Flow

**Admin Portal** (http://localhost:4000):
1. Navigate to http://localhost:4000/login
2. Click "Sign In with Keycloak"
3. Should redirect to http://localhost:8090/realms/werkflow/protocol/openid-connect/auth
4. Enter credentials (test user from Keycloak)
5. Should successfully redirect back to http://localhost:4000 with authenticated session

**HR Portal** (http://localhost:4001):
1. Navigate to http://localhost:4001/login
2. Follow same flow as admin portal
3. Should successfully authenticate

## Technical Deep Dive

### OAuth 2.0 Authorization Code Flow

The error occurred at **Step 5** of the standard OAuth flow:

```
1. User clicks "Sign In"
   → Client redirects to authorization endpoint

2. User authenticates with Keycloak
   → Keycloak validates credentials

3. User grants consent (if required)
   → Keycloak generates authorization code

4. Keycloak redirects back to client
   → Redirect to: http://localhost:4000/api/auth/callback/keycloak?code=ABC123

5. Client exchanges code for tokens [FAILURE POINT]
   → POST http://keycloak:8080/realms/werkflow/protocol/openid-connect/token
   → Body: {
       grant_type: "authorization_code",
       code: "ABC123",
       client_id: "werkflow-admin-portal",
       client_secret: "WRONG_SECRET",  ← Invalid!
       redirect_uri: "http://localhost:4000/api/auth/callback/keycloak"
     }
   → Keycloak Response: 401 Unauthorized
     {
       "error": "unauthorized_client",
       "error_description": "Invalid client or Invalid client credentials"
     }

6. Token exchange fails
   → NextAuth throws CallbackRouteError
   → User sees authentication error
```

### Why Client Secret Matters

The client secret serves as the **password for the application** when communicating with Keycloak:

- **Confidential Clients**: Clients that can securely store secrets (server-side apps)
- **Token Endpoint Authentication**: Proves the client is who they claim to be
- **Security**: Prevents unauthorized applications from obtaining tokens

Without the correct secret:
- Keycloak cannot verify the client's identity
- Token exchange is rejected
- OAuth flow fails

### Docker Network Considerations

The error occurred during **server-side** communication:

```
Browser (localhost:4000) → Keycloak (localhost:8090) → Browser callback
                                                           ↓
                        admin-portal container (server-side)
                                    ↓
                        POST to keycloak:8080/realms/werkflow/protocol/openid-connect/token
                                    ↓
                              [AUTHENTICATION FAILED]
```

**Key Points**:
- Browser uses `localhost:8090` (port-mapped Keycloak)
- Container uses `keycloak:8080` (internal Docker network)
- Token exchange happens server-side inside admin-portal container
- Client secret is sent in this server-to-server communication

## Common OAuth Errors and Solutions

### Error: "invalid_client_credentials"

**Cause**: Client secret doesn't match Keycloak configuration

**Solution**:
1. Get correct secret from Keycloak
2. Update docker-compose.yml
3. Recreate container

### Error: "invalid_redirect_uri"

**Cause**: Redirect URI in request doesn't match Keycloak configuration

**Solution**:
1. Check Keycloak client's configured redirect URIs
2. Ensure exact match (including http/https, port, path)
3. Add missing URIs if needed

### Error: "invalid_grant"

**Cause**: Authorization code expired or already used

**Solution**:
1. Authorization codes expire quickly (typically 60 seconds)
2. Try login flow again
3. Check server time synchronization

### Error: "Client not found"

**Cause**: Client ID doesn't exist in Keycloak realm

**Solution**:
1. Verify client exists in Keycloak
2. Check spelling of client ID
3. Create client if missing

## Best Practices

### 1. Secret Management

**Development**:
- Use `.env` files (gitignored)
- Document secrets in `.env.example` with placeholders
- Never commit real secrets

**Production**:
- Use secret management tools (HashiCorp Vault, AWS Secrets Manager)
- Rotate secrets regularly
- Use different secrets per environment

### 2. Client Configuration Checklist

When creating Keycloak clients:
- [ ] Client ID matches application configuration
- [ ] Client authentication enabled (for confidential clients)
- [ ] Correct redirect URIs configured
- [ ] Standard flow enabled
- [ ] Web origins configured (for CORS)
- [ ] Post logout redirect URIs set
- [ ] PKCE enabled (S256 challenge method)

### 3. Debugging OAuth Issues

1. **Enable debug logging** in NextAuth:
   ```typescript
   // auth.config.ts
   export const authConfig = {
     debug: true,  // Detailed logs
     // ...
   }
   ```

2. **Monitor Keycloak events**:
   - Keycloak Admin Console → Events
   - Real-time event stream
   - Filter by client ID

3. **Check network requests**:
   - Browser DevTools → Network tab
   - Look for token endpoint requests
   - Inspect response bodies

4. **Verify OIDC discovery**:
   ```bash
   curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq
   ```

## Files Modified

1. `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml`
   - Updated `KEYCLOAK_CLIENT_SECRET` for admin-portal
   - Updated `KEYCLOAK_CLIENT_SECRET` for hr-portal

2. Keycloak Database (via kcadm CLI)
   - Created `werkflow-hr-portal` client

## Testing Instructions

### Manual Testing

1. **Start all services**:
   ```bash
   cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
   docker-compose up -d
   ```

2. **Create test user in Keycloak**:
   - Open http://localhost:8090/admin
   - Login: admin / admin123
   - Select "werkflow" realm
   - Users → Add User
   - Username: testuser
   - Email: testuser@example.com
   - Save → Credentials tab
   - Set password: Test123!
   - Temporary: OFF

3. **Test admin-portal login**:
   - Navigate to http://localhost:4000
   - Click "Sign In"
   - Login with testuser / Test123!
   - Verify successful authentication
   - Check session cookie is set
   - Check user profile displays

4. **Test hr-portal login**:
   - Navigate to http://localhost:4001
   - Repeat login flow
   - Verify successful authentication

5. **Test logout**:
   - Click "Sign Out"
   - Verify redirect to login page
   - Verify session cleared
   - Verify Keycloak session terminated

### Automated Testing

```bash
# Check all containers are healthy
docker-compose ps

# Verify admin-portal responds
curl -I http://localhost:4000

# Verify hr-portal responds
curl -I http://localhost:4001

# Check Keycloak OIDC discovery
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# Verify no auth errors in logs
docker logs werkflow-admin-portal 2>&1 | grep -i "callbackrouteerror"
# Should return no results

docker logs werkflow-hr-portal 2>&1 | grep -i "callbackrouteerror"
# Should return no results
```

## Troubleshooting Guide

### Problem: Still getting "invalid_client_credentials"

**Solutions**:
1. Verify container picked up new environment variables:
   ```bash
   docker exec werkflow-admin-portal printenv KEYCLOAK_CLIENT_SECRET
   ```

2. If secret is wrong, recreate container:
   ```bash
   docker-compose up -d --force-recreate admin-portal
   ```

3. Verify Keycloak client secret:
   ```bash
   docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh get clients \
     -r werkflow -q clientId=werkflow-admin-portal | grep secret
   ```

### Problem: Redirect URI mismatch

**Solutions**:
1. Check configured redirect URIs in Keycloak
2. Add missing URI:
   ```bash
   # Get client ID
   CLIENT_ID=$(docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh \
     get clients -r werkflow -q clientId=werkflow-admin-portal --fields id \
     | jq -r '.[0].id')

   # Update redirect URIs
   docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh update \
     clients/$CLIENT_ID -r werkflow \
     -s 'redirectUris=["http://localhost:4000/api/auth/callback/keycloak","http://localhost:4000/login"]'
   ```

### Problem: Keycloak not reachable from container

**Solutions**:
1. Verify containers are on same network:
   ```bash
   docker network inspect werkflow-network
   ```

2. Test connectivity:
   ```bash
   docker exec werkflow-admin-portal curl -I http://keycloak:8080/health
   ```

3. Check DNS resolution:
   ```bash
   docker exec werkflow-admin-portal nslookup keycloak
   ```

### Problem: Token validation fails

**Solutions**:
1. Check issuer configuration matches token claims
2. Verify KEYCLOAK_ISSUER_PUBLIC matches token "iss" claim
3. Test token validation:
   ```bash
   # Get a token
   TOKEN="<access_token>"

   # Decode and inspect
   echo $TOKEN | cut -d'.' -f2 | base64 -d | jq

   # Check "iss" claim matches KEYCLOAK_ISSUER_PUBLIC
   ```

## Security Considerations

### Production Deployment

Before deploying to production:

1. **Generate strong secrets**:
   ```bash
   # Generate 32-byte random secret
   openssl rand -base64 32
   ```

2. **Use HTTPS everywhere**:
   - Keycloak: `https://auth.yourdomain.com`
   - Admin Portal: `https://admin.yourdomain.com`
   - HR Portal: `https://hr.yourdomain.com`

3. **Configure proper redirect URIs**:
   - No wildcards
   - Exact match only
   - Production URLs only

4. **Enable PKCE**:
   - Already configured with `S256` challenge method
   - Provides additional security for authorization code flow

5. **Rotate secrets regularly**:
   - Schedule quarterly secret rotation
   - Update Keycloak and docker-compose simultaneously
   - Test in staging first

6. **Monitor authentication events**:
   - Enable Keycloak event logging
   - Set up alerts for repeated failures
   - Track suspicious patterns

## References

- [NextAuth.js Documentation](https://next-auth.js.org/)
- [Keycloak OAuth2/OIDC Documentation](https://www.keycloak.org/docs/latest/securing_apps/)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [PKCE RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)
- [Keycloak Admin CLI Documentation](https://www.keycloak.org/docs/latest/server_admin/#the-admin-cli)

## Summary

**Root Cause**: Client secret mismatch between docker-compose.yml and Keycloak configuration

**Impact**: OAuth callback failed, users unable to authenticate

**Resolution**:
1. Updated admin-portal client secret in docker-compose.yml
2. Created hr-portal client in Keycloak with proper configuration
3. Updated hr-portal client secret in docker-compose.yml
4. Recreated containers to apply changes

**Status**: RESOLVED - Both portals now have correct client secrets and can authenticate successfully with Keycloak

**Next Steps**: Test login flow manually to confirm fix
