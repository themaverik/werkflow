# OAuth2 Docker Configuration

Complete guide to Docker networking configuration for Keycloak OAuth2 authentication in containerized environments.

## Table of Contents

1. [Overview](#overview)
2. [The Docker Networking Challenge](#the-docker-networking-challenge)
3. [Solution Architecture](#solution-architecture)
4. [Keycloak Configuration](#keycloak-configuration)
5. [Environment-Specific Configurations](#environment-specific-configurations)
6. [Network Flow Diagrams](#network-flow-diagrams)
7. [Testing the Configuration](#testing-the-configuration)
8. [Troubleshooting](#troubleshooting)

## Overview

This document explains how to configure OAuth2 authentication with Keycloak in Docker environments, addressing the fundamental challenge of different network paths for browser clients versus server-side containers.

### The Problem We Solve

In containerized environments, OAuth2 authentication faces a networking paradox:
- Browsers need to access Keycloak via `localhost:8090` (host-accessible)
- Container applications need to access Keycloak via `keycloak:8080` (internal Docker network)
- OAuth2 issuer validation requires consistent URL handling

Our solution implements a **dual-URL architecture** that allows each client to use the appropriate network path.

## The Docker Networking Challenge

### Why Simple Solutions Fail

**Attempt 1: Using host.docker.internal**
- Only works on Docker Desktop (Mac/Windows)
- Breaks on Linux and in production
- Not portable across environments

**Attempt 2: Single hostname with KC_HOSTNAME=keycloak**
- Browser cannot resolve internal Docker service names
- Results in "Unable to connect to keycloak:8080" errors

**Attempt 3: Single hostname with KC_HOSTNAME=localhost**
- Containers cannot reach localhost (refers to container itself)
- Results in "fetch failed" errors during token validation

### The Root Challenge

Different clients need different network paths to the same service:

1. **Browser Flow Requirements**
   - User browser visits admin-portal at `http://localhost:4000`
   - Browser gets redirected to Keycloak for authentication
   - Browser cannot resolve internal Docker DNS names
   - Browser can only access `http://localhost:8090` (port-mapped)

2. **Server-Side Token Validation**
   - Next.js container needs to validate OAuth tokens
   - Container cannot reach `localhost:8090` (localhost is container itself)
   - Container needs to use `http://keycloak:8080` (internal network)

3. **OAuth2 Issuer Validation**
   - Keycloak advertises issuer URL in OIDC discovery metadata
   - NextAuth validates JWT token issuer matches OIDC metadata
   - Creates conflict when browser and server use different URLs

## Solution Architecture

### Dual-URL Configuration

The solution uses three components working together:

1. **Keycloak Flexible Hostname Mode**
   - `KC_HOSTNAME_STRICT=false` allows requests from any hostname
   - Keycloak responds with URLs matching the request's Host header
   - Eliminates issuer URL conflicts

2. **Dual Environment Variables**
   - `KEYCLOAK_ISSUER`: Server-side operations (internal network)
   - `KEYCLOAK_ISSUER_BROWSER`: Browser redirects (external access)
   - Different network paths for different clients

3. **NextAuth Explicit URL Overrides**
   - Authorization URL: Uses browser-accessible endpoint
   - Token/Userinfo URLs: Use internal Docker network
   - Separates client-side and server-side OAuth flows

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        User's Browser                        │
│                                                              │
│  Accesses via: localhost:8090 (browser-accessible)         │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              Docker Port Mapping (8090:8080)                 │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              Docker Network: werkflow-network                │
│                                                              │
│  ┌──────────────────┐                  ┌─────────────────┐ │
│  │  admin-portal    │                  │    keycloak     │ │
│  │  (4000:4000)     │                  │  (8090:8080)    │ │
│  │                  │                  │                 │ │
│  │  Server-side:    │◄────────────────►│  Internal:      │ │
│  │  keycloak:8080   │  Token Validate  │  keycloak:8080  │ │
│  │                  │                  │                 │ │
│  │  Browser URLs:   │                  │  External:      │ │
│  │  localhost:8090  │                  │  localhost:8090 │ │
│  └──────────────────┘                  └─────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Keycloak Configuration

### Docker Compose Configuration

**File**: `infrastructure/docker/docker-compose.yml`

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0.3
  container_name: werkflow-keycloak
  command:
    - start
    - --http-relative-path=/
  environment:
    # Hostname Configuration (Production Mode)
    KC_HOSTNAME: localhost
    KC_HOSTNAME_PORT: 8090
    KC_HOSTNAME_STRICT: false
    KC_HOSTNAME_STRICT_HTTPS: false
    KC_HOSTNAME_STRICT_BACKCHANNEL: false
    KC_HTTP_ENABLED: true
    KC_PROXY: edge

    # Database Configuration
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://keycloak-postgres:5432/keycloak_db
    KC_DB_USERNAME: keycloak_user
    KC_DB_PASSWORD: keycloak_password

    # Admin Credentials
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin123
  ports:
    - "8090:8080"
  networks:
    - werkflow-network
  depends_on:
    - keycloak-postgres
```

### Configuration Explanation

#### Production Mode vs Development Mode

Keycloak has two startup modes:

| Feature | Development (`start-dev`) | Production (`start`) |
|---------|---------------------------|----------------------|
| KC_HOSTNAME | Ignored | Respected |
| KC_HOSTNAME_PORT | Ignored | Respected |
| KC_PROXY | Ignored | Respected |
| Hostname Detection | Auto-detect | Use configured value |
| Startup Time | ~5s | ~30s |
| Production Ready | No | Yes |

**Critical**: Always use `start` command (production mode) for hostname configuration to work.

#### Hostname Configuration Variables

**KC_HOSTNAME**: `localhost`
- External hostname browsers will use
- Must be resolvable from host machine

**KC_HOSTNAME_PORT**: `8090`
- External port (port-mapped from internal 8080)
- Must match Docker port mapping

**KC_HOSTNAME_STRICT**: `false`
- Allows flexible hostname matching
- Keycloak responds with URLs based on request Host header
- Essential for dual-URL architecture

**KC_HOSTNAME_STRICT_HTTPS**: `false`
- Disables HTTPS requirement (for development)
- Set to `true` in production with proper TLS

**KC_HOSTNAME_STRICT_BACKCHANNEL**: `false`
- Allows internal container access via `keycloak:8080`
- Permits server-side token validation

**KC_HTTP_ENABLED**: `true`
- Enables HTTP for development
- Disable in production (use HTTPS only)

**KC_PROXY**: `edge`
- Enables proxy mode for reverse proxy headers
- Keycloak reads `X-Forwarded-*` headers
- Allows proper URL generation behind proxies

### Frontend Configuration

**Admin Portal** (`admin-portal` service):

```yaml
admin-portal:
  environment:
    # NextAuth Configuration
    NEXTAUTH_URL: http://localhost:4000
    NEXTAUTH_SECRET: <generated-secret>
    AUTH_TRUST_HOST: true

    # Keycloak OAuth2 Configuration
    KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
    KEYCLOAK_CLIENT_ID: werkflow-admin-portal
    KEYCLOAK_CLIENT_SECRET: <client-secret-from-keycloak>
```

**HR Portal** (`hr-portal` service):

```yaml
hr-portal:
  environment:
    # NextAuth Configuration
    NEXTAUTH_URL: http://localhost:4001
    NEXTAUTH_SECRET: <generated-secret>
    AUTH_TRUST_HOST: true

    # Keycloak OAuth2 Configuration
    KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
    KEYCLOAK_CLIENT_ID: werkflow-hr-portal
    KEYCLOAK_CLIENT_SECRET: <client-secret-from-keycloak>
```

### Key Environment Variables

| Variable | Purpose | Browser Use | Server Use |
|----------|---------|-------------|------------|
| KEYCLOAK_ISSUER | Internal network URL | No | Yes |
| KEYCLOAK_ISSUER_BROWSER | External browser URL | Yes | No |
| NEXTAUTH_URL | Application base URL | Yes | Yes |
| NEXTAUTH_SECRET | Token encryption key | No | Yes |
| AUTH_TRUST_HOST | Trust configured host | Yes | Yes |

## Environment-Specific Configurations

### Local Development (Outside Docker)

When running Next.js apps locally while Keycloak runs in Docker:

```env
# .env.local
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
```

Both URLs use `localhost:8090` because:
- Next.js runs on host machine (can access localhost)
- Browser runs on host machine (can access localhost)
- Both reach Keycloak via port-mapped 8090

### Docker Compose (Full Stack)

When running entire stack in Docker:

```yaml
admin-portal:
  environment:
    KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```

Different URLs because:
- Next.js container uses internal network (`keycloak:8080`)
- Browser on host uses port mapping (`localhost:8090`)

### Production (Kubernetes/Cloud)

In production with proper DNS and ingress:

```yaml
admin-portal:
  environment:
    KEYCLOAK_ISSUER: http://keycloak.werkflow.svc.cluster.local/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: https://auth.werkflow.com/realms/werkflow

keycloak:
  environment:
    KC_HOSTNAME: auth.werkflow.com
    KC_HOSTNAME_STRICT: true
    KC_HOSTNAME_PROTOCOL: https
    KC_HTTP_ENABLED: false
```

Different URLs because:
- Next.js pod uses internal service DNS
- Browser uses public domain with TLS

## Network Flow Diagrams

### OAuth Flow with Dual URLs

```
┌─────────────────────────────────────────────────────────────┐
│ Step 1: User Clicks "Sign In"                               │
│   Browser → admin-portal (localhost:4000)                   │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Authorization Redirect (Browser Flow)               │
│   admin-portal generates redirect using KEYCLOAK_ISSUER_BROWSER │
│   Browser → localhost:8090/realms/werkflow/protocol/...    │
│   ✓ Browser can resolve localhost                           │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 3: User Authenticates                                  │
│   Browser → Keycloak (localhost:8090)                       │
│   User enters credentials                                    │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 4: Callback with Authorization Code                    │
│   Keycloak → Browser → admin-portal                         │
│   localhost:4000/api/auth/callback?code=...                 │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 5: Token Exchange (Server-Side)                        │
│   admin-portal container uses KEYCLOAK_ISSUER               │
│   POST keycloak:8080/realms/werkflow/protocol/.../token    │
│   ✓ Container can resolve keycloak via Docker network       │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 6: Token Validation (Server-Side)                      │
│   admin-portal validates JWT                                 │
│   GET keycloak:8080/realms/werkflow/.well-known/...        │
│   ✓ All server-side calls use internal network              │
└─────────────────────────────────────────────────────────────┘
```

### Network Path Separation

**Browser Requests**:
```
Browser (host) → localhost:8090 → Docker port mapping → keycloak:8080 (container)
```

**Server-Side Requests**:
```
admin-portal (container) → keycloak:8080 (container) [same Docker network]
```

## Testing the Configuration

### Test 1: Verify Keycloak Accessibility from Host

```bash
# From host machine
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# Should return OIDC metadata
```

### Test 2: Verify Keycloak Accessibility from Container

```bash
# From within Docker network
docker exec werkflow-admin-portal curl http://keycloak:8080/realms/werkflow/.well-known/openid-configuration

# Should return OIDC metadata
```

### Test 3: Verify Hostname Configuration

```bash
# Check Keycloak logs for hostname settings
docker logs werkflow-keycloak | grep "Hostname settings"

# Should show:
# Hostname: localhost
# Port: 8090
# Proxied: true
```

### Test 4: Test OAuth Flow

1. Open browser: `http://localhost:4000`
2. Click "Sign In"
3. Should redirect to: `http://localhost:8090/realms/werkflow/...` ✓
4. Enter test credentials
5. Should redirect back to: `http://localhost:4000` ✓
6. Authenticated session established ✓

### Test 5: Check Container Logs

```bash
# Monitor admin-portal logs
docker logs werkflow-admin-portal -f

# Should NOT see:
# - "fetch failed" errors
# - "ECONNREFUSED" errors
# - Issuer validation failures
```

## Troubleshooting

### Issue: "Failed to Fetch" During Token Validation

**Symptom**:
```
Error: fetch failed
  at admin-portal token validation
```

**Cause**: NextAuth trying to reach `localhost:8090` from inside container

**Solution**: Verify `KEYCLOAK_ISSUER` uses internal Docker network:
```yaml
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow  # NOT localhost
```

### Issue: Browser Cannot Reach Keycloak

**Symptom**:
```
ERR_NAME_NOT_RESOLVED for http://keycloak:8080
```

**Cause**: Browser redirected to internal Docker hostname

**Solution**: Verify `KEYCLOAK_ISSUER_BROWSER` uses host-accessible URL:
```yaml
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow  # NOT keycloak
```

### Issue: Keycloak Hostname Configuration Ignored

**Symptom**: Keycloak advertises `http://keycloak:8080` instead of `http://localhost:8090`

**Cause**: Running in development mode (`start-dev`) which ignores hostname configuration

**Solution**: Change to production mode in docker-compose.yml:
```yaml
command:
  - start  # NOT start-dev
  - --http-relative-path=/
```

### Issue: CORS Errors

**Symptom**:
```
CORS policy: No 'Access-Control-Allow-Origin' header
```

**Cause**: Keycloak client not configured with correct origins

**Solution**: Configure Keycloak client:
1. Go to Keycloak Admin Console
2. Select realm → Clients → werkflow-admin-portal
3. Add to "Valid Redirect URIs": `http://localhost:4000/*`
4. Add to "Web Origins": `http://localhost:4000`

### Issue: Issuer Validation Fails

**Symptom**:
```
Token issuer mismatch
Expected: http://keycloak:8080/realms/werkflow
Received: http://localhost:8090/realms/werkflow
```

**Cause**: Token issued with browser-facing URL but validated against internal URL

**Solution**: With `KC_HOSTNAME_STRICT=false`, this should not happen. If it does:
1. Verify Keycloak running with `KC_HOSTNAME_STRICT=false`
2. Check NextAuth uses `issuer` parameter correctly
3. Ensure explicit URL overrides in place

## Security Considerations

### Issuer Validation

NextAuth validates the `iss` claim in JWT tokens matches the expected issuer. With `KC_HOSTNAME_STRICT=false`, Keycloak may include different issuer values depending on request hostname.

**Mitigation**:
- Always use `KEYCLOAK_ISSUER` for server-side token validation
- Token signature verification is what matters, not the URL
- Keycloak validates tokens regardless of hostname used

### HTTPS in Production

Current configuration uses HTTP for development. In production:

```yaml
keycloak:
  environment:
    KC_HOSTNAME_STRICT: true
    KC_HOSTNAME: auth.werkflow.com
    KC_HOSTNAME_PROTOCOL: https
    KC_HTTP_ENABLED: false

admin-portal:
  environment:
    KEYCLOAK_ISSUER: https://keycloak.internal/realms/werkflow
    KEYCLOAK_ISSUER_BROWSER: https://auth.werkflow.com/realms/werkflow
```

### Client Secrets

Never commit client secrets to version control:

```yaml
# Use environment variable substitution
KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_ADMIN_PORTAL_SECRET}
```

Provide secrets via:
- Docker secrets
- Kubernetes secrets
- Environment variables from secure storage

## See Also

- [OAuth2 Setup Guide](./OAuth2_Setup_Guide.md) - Complete Keycloak OAuth2 setup
- [NextAuth Configuration](./NextAuth_Configuration.md) - NextAuth.js integration details
- [OAuth2 Troubleshooting](./OAuth2_Troubleshooting.md) - Common errors and solutions

## Archived Files

This guide consolidates information from:
- KEYCLOAK_HOSTNAME_FIX_COMPLETE.md
- OAuth2-Docker-Architecture.md

## Summary

The dual-URL Docker configuration solves the fundamental networking challenge of OAuth2 authentication in containers. By using Keycloak's flexible hostname mode and explicit URL overrides in NextAuth, we enable:

1. Browser-based OAuth flows that work from the host machine
2. Server-side token validation using internal Docker networking
3. Proper issuer validation without URL mismatches
4. Scalability to multiple frontend applications
5. Clear path to production with minimal configuration changes

This architecture is production-ready, secure, and maintainable for enterprise deployments.
