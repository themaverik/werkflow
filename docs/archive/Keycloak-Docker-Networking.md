# Keycloak Docker Networking Configuration

## Problem Overview

When running Keycloak and Next.js applications (admin-portal, hr-portal) in Docker containers, a networking challenge arises due to how `localhost` resolves differently depending on context:

1. **Browser Context**: `localhost:8090` correctly resolves to the host machine where Keycloak is running
2. **Container Context**: `localhost:8090` resolves to the container itself, NOT the host or other containers
3. **Token Validation**: Keycloak returns tokens with `issuer` claim matching its configured hostname

This creates a conflict:
- Browsers need to access Keycloak at `http://localhost:8090` (port-mapped from host)
- Server-side code in containers needs to access Keycloak at `http://keycloak:8080` (Docker internal network)
- NextAuth must validate tokens against the issuer Keycloak advertises: `http://localhost:8090`

## The Three-URL Strategy

Our solution implements a three-URL approach that satisfies all requirements:

### 1. KEYCLOAK_ISSUER_INTERNAL
**Purpose**: Server-side API calls from container to Keycloak

**Used For**:
- OIDC discovery (`.well-known/openid-configuration`)
- Token exchange endpoint
- Userinfo endpoint
- Any backend-to-Keycloak communication

**Values**:
- **Local development** (outside Docker): `http://localhost:8090/realms/werkflow`
- **Docker containers**: `http://keycloak:8080/realms/werkflow`

**Why It Works**: Uses Docker's internal DNS to route container-to-container traffic

### 2. KEYCLOAK_ISSUER_PUBLIC
**Purpose**: Token validation - the issuer claim in JWT tokens

**Used For**:
- Validating JWT tokens received from Keycloak
- Must match the `iss` claim that Keycloak puts in tokens
- NextAuth checks token issuer against this value

**Values**:
- **Both local and Docker**: `http://localhost:8090/realms/werkflow`

**Why It Works**: Keycloak is configured with `KC_HOSTNAME=localhost` and `KC_PROXY=edge`, so it advertises `http://localhost:8090` as the issuer regardless of how it's accessed

### 3. KEYCLOAK_ISSUER_BROWSER
**Purpose**: OAuth redirects that happen in the user's browser

**Used For**:
- Authorization endpoint (login page redirect)
- Browser-initiated OAuth flows
- User-facing URLs

**Values**:
- **Both local and Docker**: `http://localhost:8090/realms/werkflow`

**Why It Works**: Browser runs on host machine where `localhost:8090` is port-mapped to Keycloak container

## Configuration Details

### Keycloak Configuration (docker-compose.yml)

```yaml
keycloak:
  environment:
    # Hostname that Keycloak advertises to clients
    KC_HOSTNAME: localhost
    KC_HOSTNAME_PORT: 8090
    KC_HOSTNAME_STRICT: false
    KC_HOSTNAME_STRICT_HTTPS: false
    KC_HOSTNAME_STRICT_BACKCHANNEL: false

    # Proxy mode: Respects X-Forwarded-* headers
    KC_PROXY: edge

    # Enable HTTP for local development
    KC_HTTP_ENABLED: true

  ports:
    - "8090:8080"  # Host:Container port mapping
```

**Key Points**:
- `KC_HOSTNAME=localhost` makes Keycloak advertise `http://localhost:8090` in all tokens and OIDC metadata
- `KC_PROXY=edge` enables proper handling of reverse proxy scenarios
- Port mapping `8090:8080` exposes container port 8080 as host port 8090

### Frontend Configuration (docker-compose.yml)

```yaml
admin-portal:
  environment:
    # Internal network URL for server-side calls
    KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow

    # Public issuer for token validation
    KEYCLOAK_ISSUER_PUBLIC: http://localhost:8090/realms/werkflow

    # Browser-accessible URL for OAuth redirects
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```

### NextAuth Configuration (auth.config.ts / auth.ts)

```typescript
KeycloakProvider({
  clientId: process.env.KEYCLOAK_CLIENT_ID!,
  clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,

  // Use PUBLIC issuer for token validation
  issuer: process.env.KEYCLOAK_ISSUER_PUBLIC || process.env.KEYCLOAK_ISSUER_INTERNAL,

  // Browser redirects use BROWSER URL
  authorization: {
    params: { scope: "openid email profile" },
    url: `${process.env.KEYCLOAK_ISSUER_BROWSER}/protocol/openid-connect/auth`,
  },

  // Server-side calls use INTERNAL URL
  token: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/token`,
  userinfo: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/userinfo`,
})
```

## OAuth Flow Walkthrough

Let's trace the complete OAuth flow to understand how these URLs work together:

### Step 1: User Initiates Login
- User clicks "Login" button in browser
- NextAuth redirects browser to authorization endpoint
- **URL Used**: `KEYCLOAK_ISSUER_BROWSER/protocol/openid-connect/auth`
- **Value**: `http://localhost:8090/realms/werkflow/protocol/openid-connect/auth`
- **Why**: Browser can reach localhost:8090 on host machine

### Step 2: User Authenticates with Keycloak
- Browser loads Keycloak login page
- User enters credentials
- Keycloak validates credentials

### Step 3: Authorization Code Redirect
- Keycloak redirects browser back to callback URL with authorization code
- **Redirect URL**: `http://localhost:4000/api/auth/callback/keycloak?code=xxx`

### Step 4: Token Exchange (Server-Side)
- Next.js server receives callback with code
- Server makes POST request to token endpoint to exchange code for tokens
- **URL Used**: `KEYCLOAK_ISSUER_INTERNAL/protocol/openid-connect/token`
- **Value**: `http://keycloak:8080/realms/werkflow/protocol/openid-connect/token`
- **Why**: Server container reaches Keycloak via internal Docker network

### Step 5: Token Validation
- NextAuth receives JWT token from Keycloak
- Token contains: `{ "iss": "http://localhost:8090/realms/werkflow", ... }`
- NextAuth validates token issuer matches configured issuer
- **Issuer Used**: `KEYCLOAK_ISSUER_PUBLIC`
- **Value**: `http://localhost:8090/realms/werkflow`
- **Why**: Must match what Keycloak put in the token

### Step 6: Fetch User Info (Server-Side)
- Server makes GET request to userinfo endpoint
- **URL Used**: `KEYCLOAK_ISSUER_INTERNAL/protocol/openid-connect/userinfo`
- **Value**: `http://keycloak:8080/realms/werkflow/protocol/openid-connect/userinfo`
- **Why**: Server container reaches Keycloak via internal Docker network

## Why Previous Approaches Failed

### Attempt 1: Using localhost:8090 for everything
```yaml
KEYCLOAK_ISSUER: http://localhost:8090/realms/werkflow
```
**Problem**: Container tries to connect to its own localhost, not the host
**Error**: `TypeError: fetch failed` - connection refused

### Attempt 2: Using keycloak:8080 for everything
```yaml
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
```
**Problem**: Browser can't resolve Docker service name `keycloak`
**Error**: Browser shows DNS resolution error

### Attempt 3: Using keycloak:8080 with localhost:8090 for browser only
```yaml
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```
**Problem**: Token validation fails because issuer mismatch
**Error**: NextAuth rejects token - `iss` claim doesn't match configured issuer

## The Correct Solution (Three-URL Strategy)

```yaml
KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_PUBLIC: http://localhost:8090/realms/werkflow
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```

**Why It Works**:
1. Server-side calls use `INTERNAL` (keycloak:8080) - container can reach it
2. Browser redirects use `BROWSER` (localhost:8090) - browser can reach it
3. Token validation uses `PUBLIC` (localhost:8090) - matches what Keycloak advertises

## Troubleshooting

### Error: "fetch failed" during login
**Cause**: Server container can't reach Keycloak URL
**Check**:
- Is `KEYCLOAK_ISSUER_INTERNAL` set to `http://keycloak:8080`?
- Are both containers on same Docker network?
- Is Keycloak container running and healthy?

### Error: "Token issuer mismatch"
**Cause**: NextAuth issuer validation failing
**Check**:
- Does `KEYCLOAK_ISSUER_PUBLIC` match what's in token `iss` claim?
- Check token content: `curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration`
- Verify `KC_HOSTNAME` and `KC_PROXY` are set correctly in Keycloak

### Error: "Browser can't reach login page"
**Cause**: Browser redirect URL not accessible
**Check**:
- Is `KEYCLOAK_ISSUER_BROWSER` set to `http://localhost:8090`?
- Is port 8090 mapped correctly in docker-compose?
- Can you access Keycloak directly: `http://localhost:8090`?

### Debug Commands

```bash
# Check Keycloak advertised issuer
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer

# Check if container can reach Keycloak internally
docker exec werkflow-admin-portal curl -s http://keycloak:8080/realms/werkflow/.well-known/openid-configuration

# Check Docker network connectivity
docker exec werkflow-admin-portal ping -c 3 keycloak

# View container environment variables
docker exec werkflow-admin-portal env | grep KEYCLOAK
```

## Production Deployment Considerations

For production deployments with a public domain (e.g., `auth.werkflow.com`):

```yaml
# Keycloak Configuration
KC_HOSTNAME: auth.werkflow.com
KC_HOSTNAME_PORT: 443
KC_HOSTNAME_STRICT_HTTPS: true
KC_PROXY: edge

# Frontend Configuration
KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_PUBLIC: https://auth.werkflow.com/realms/werkflow
KEYCLOAK_ISSUER_BROWSER: https://auth.werkflow.com/realms/werkflow
```

**Key Changes**:
- `INTERNAL` still uses Docker network (keycloak:8080)
- `PUBLIC` and `BROWSER` use public HTTPS URL
- Keycloak advertises public URL in tokens
- Reverse proxy handles TLS termination

## Summary

The three-URL strategy successfully solves Docker networking challenges by:

1. **Separating concerns**: Different URLs for different use cases
2. **Matching expectations**: Token issuer matches what Keycloak advertises
3. **Leveraging Docker**: Uses internal DNS for container-to-container communication
4. **Supporting browsers**: Uses port-mapped localhost for browser access
5. **Production ready**: Easily adaptable to public domain deployments

This approach is production-grade, well-documented, and maintainable.
