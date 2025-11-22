# NextAuth Configuration

Complete guide to NextAuth.js configuration for Keycloak OAuth2 integration in Werkflow frontends.

## Table of Contents

1. [Overview](#overview)
2. [Configuration Files](#configuration-files)
3. [Environment Variables](#environment-variables)
4. [Provider Configuration](#provider-configuration)
5. [URL Override Configuration](#url-override-configuration)
6. [TrustHost Configuration](#trusthost-configuration)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

## Overview

NextAuth.js is the authentication library used in Werkflow's Next.js frontends (Admin Portal and HR Portal). This guide covers the Keycloak provider configuration and Docker-specific settings.

### What NextAuth Handles

- OAuth2 authorization code flow with Keycloak
- Session management with encrypted cookies
- JWT token validation
- Role-based access control
- Callback handling and redirects

## Configuration Files

### Admin Portal

**Location**: `frontends/admin-portal/auth.config.ts`

This file contains the NextAuth configuration exported for use throughout the application.

**Location**: `frontends/admin-portal/auth.ts`

This file exports the NextAuth handlers for API routes and middleware.

### HR Portal

**Location**: `frontends/hr-portal/auth.config.ts`

**Location**: `frontends/hr-portal/auth.ts`

Configuration is identical to admin-portal with different client IDs and ports.

## Environment Variables

### Required Variables

```env
# NextAuth Configuration
NEXTAUTH_URL=http://localhost:4000
NEXTAUTH_SECRET=<generated-with-openssl-rand-base64-32>
AUTH_TRUST_HOST=true

# Keycloak OAuth2 Configuration
KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
KEYCLOAK_CLIENT_ID=werkflow-admin-portal
KEYCLOAK_CLIENT_SECRET=<from-keycloak-credentials-tab>
```

### Variable Descriptions

**NEXTAUTH_URL**
- Purpose: Base URL of your Next.js application
- Admin Portal: `http://localhost:4000`
- HR Portal: `http://localhost:4001`
- Used for: Generating callback URLs, validating requests

**NEXTAUTH_SECRET**
- Purpose: Encryption key for session cookies and JWT tokens
- Generate with: `openssl rand -base64 32`
- Requirements: Minimum 32 characters, random, kept secret
- Example: `xg7bVqYgFgTDwpVWe6gbyWwZVb8f0Yd1Wo+ZGuKdm/U=`

**AUTH_TRUST_HOST**
- Purpose: Trust the host configured in NEXTAUTH_URL
- Value: `true` (always in Docker environments)
- Why needed: NextAuth 5+ has strict host validation

**KEYCLOAK_ISSUER**
- Purpose: Keycloak URL for server-side operations
- Docker: `http://keycloak:8080/realms/werkflow`
- Local dev: `http://localhost:8090/realms/werkflow`
- Used for: Token validation, userinfo endpoint

**KEYCLOAK_ISSUER_BROWSER**
- Purpose: Keycloak URL for browser redirects
- Value: `http://localhost:8090/realms/werkflow`
- Used for: Authorization endpoint (login redirect)

**KEYCLOAK_CLIENT_ID**
- Purpose: OAuth2 client identifier in Keycloak
- Admin Portal: `werkflow-admin-portal`
- HR Portal: `werkflow-hr-portal`

**KEYCLOAK_CLIENT_SECRET**
- Purpose: OAuth2 client secret for secure authentication
- Obtain from: Keycloak Admin Console → Clients → Credentials tab
- Security: Never commit to version control

## Provider Configuration

### Basic Keycloak Provider Setup

**File**: `auth.config.ts`

```typescript
import NextAuth from "next-auth"
import Keycloak from "next-auth/providers/keycloak"

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER,

      // Explicit URL overrides for Docker dual-URL architecture
      authorization: {
        params: { scope: "openid email profile" },
        url: `${process.env.KEYCLOAK_ISSUER_BROWSER || process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/auth`,
      },
      token: `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
      userinfo: `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/userinfo`,
    }),
  ],

  // Trust the configured host
  trustHost: true,
})
```

### Configuration Explanation

**clientId and clientSecret**
- Read from environment variables
- Must match values configured in Keycloak
- Client secret kept secure server-side only

**issuer**
- Base URL for server-side OIDC operations
- Uses `KEYCLOAK_ISSUER` (internal Docker network)
- NextAuth fetches OIDC discovery metadata from this URL

## URL Override Configuration

### Why URL Overrides Are Needed

In Docker environments, browser and server need different network paths to Keycloak. URL overrides allow explicit control over which URL is used for each OAuth2 endpoint.

### Authorization URL Override

```typescript
authorization: {
  params: { scope: "openid email profile" },
  url: `${process.env.KEYCLOAK_ISSUER_BROWSER || process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/auth`,
},
```

**Purpose**: Browser redirect for user authentication

**Uses**: `KEYCLOAK_ISSUER_BROWSER` (browser-accessible URL)

**Why**: Browser needs to reach `localhost:8090`, not internal `keycloak:8080`

**Fallback**: Uses `KEYCLOAK_ISSUER` if browser URL not set (for local dev)

### Token URL Override

```typescript
token: `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
```

**Purpose**: Server-side token exchange

**Uses**: `KEYCLOAK_ISSUER` (internal Docker network)

**Why**: Next.js container communicates directly with Keycloak container

**Security**: Happens server-side, browser never sees this request

### Userinfo URL Override

```typescript
userinfo: `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/userinfo`,
```

**Purpose**: Fetch user profile information

**Uses**: `KEYCLOAK_ISSUER` (internal Docker network)

**Why**: Server-side request after token validation

**Contains**: User details, email, roles from Keycloak

### Complete Flow with URL Overrides

```
1. User clicks "Sign In"
   ↓
2. NextAuth generates authorization URL using KEYCLOAK_ISSUER_BROWSER
   Browser redirected to: http://localhost:8090/realms/werkflow/protocol/openid-connect/auth
   ↓
3. User authenticates with Keycloak
   ↓
4. Keycloak redirects to callback: http://localhost:4000/api/auth/callback?code=...
   ↓
5. NextAuth exchanges code for tokens using KEYCLOAK_ISSUER
   Server requests: http://keycloak:8080/realms/werkflow/protocol/openid-connect/token
   ↓
6. NextAuth fetches user info using KEYCLOAK_ISSUER
   Server requests: http://keycloak:8080/realms/werkflow/protocol/openid-connect/userinfo
   ↓
7. Session created with user data and roles
```

## TrustHost Configuration

### The UntrustedHost Issue

NextAuth 5+ has strict host validation for security. By default:
- Only trusts production domains from environment configuration
- Does not trust localhost/127.0.0.1 unless explicitly enabled
- Results in HTTP 404 errors if host not trusted

### Solution: trustHost Setting

**In auth.config.ts**:

```typescript
export const { handlers, auth, signIn, signOut } = NextAuth({
  // ... providers configuration ...

  trustHost: true,  // Trust the host from NEXTAUTH_URL
})
```

**Alternative: Environment Variable**:

```yaml
# In docker-compose.yml
environment:
  AUTH_TRUST_HOST: true
```

### How It Works

**Before Fix**:
```
GET http://localhost:4000/studio/processes
↓
NextAuth checks request host
↓
Host "localhost" not in trusted list
↓
Blocks request with UntrustedHost error
↓
Returns HTTP 404
```

**After Fix**:
```
GET http://localhost:4000/studio/processes
↓
NextAuth checks request host
↓
AUTH_TRUST_HOST=true + NEXTAUTH_URL=http://localhost:4000
↓
Host is trusted, allow request
↓
Check session/authentication
↓
Redirect or serve content
```

### Security Considerations

**Development (localhost)**:
- `trustHost: true` is safe for localhost
- NEXTAUTH_SECRET should still be random
- These settings are secure for local development

**Production**:
- Remove `trustHost: true` from auth.config.ts
- Use real domain name in NEXTAUTH_URL
- Store NEXTAUTH_SECRET in secure environment variable
- Use HTTPS only (not HTTP)
- Rotate NEXTAUTH_SECRET regularly

## Testing

### Test 1: Verify Configuration Loaded

```bash
# Check environment variables in container
docker exec werkflow-admin-portal env | grep -E 'KEYCLOAK|NEXTAUTH'

# Should show all configured variables
```

### Test 2: Test Auth Endpoints

```bash
# Check NextAuth session endpoint
curl http://localhost:4000/api/auth/session

# Should return either:
# - Redirect to login (if not authenticated)
# - JSON session data (if authenticated)
```

### Test 3: Test Protected Routes

```bash
# Access protected route
curl -I http://localhost:4000/studio/processes

# Should return:
# HTTP/1.1 302 Found (redirect to login)
# NOT: HTTP/1.1 404 Not Found
```

### Test 4: Complete Login Flow

1. Open browser: `http://localhost:4000/login`
2. Click "Sign in with Keycloak"
3. Should redirect to: `http://localhost:8090/realms/werkflow/...`
4. Enter credentials: admin / admin123
5. Should redirect back to: `http://localhost:4000`
6. Session cookie should be set
7. Check session: `http://localhost:4000/api/auth/session`
8. Should show user data with roles

### Test 5: Verify Logs

```bash
# Monitor NextAuth logs
docker logs werkflow-admin-portal -f | grep -i auth

# Should NOT see:
# - UntrustedHost errors
# - fetch failed errors
# - Configuration errors
```

## Troubleshooting

### Issue: HTTP 404 on Protected Routes

**Error**: Routes return 404 instead of redirecting to login

**Cause**: `trustHost` not set, NextAuth blocking requests

**Solution**:
```typescript
// In auth.config.ts
export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [...],
  trustHost: true,  // Add this line
})
```

### Issue: "Configuration" Error on Login

**Error**: `/api/auth/error?error=Configuration`

**Cause**: Missing or incorrect client secret

**Solution**:
1. Verify client secret in Keycloak: Clients → werkflow-admin-portal → Credentials
2. Update KEYCLOAK_CLIENT_SECRET in docker-compose.yml
3. Restart container: `docker-compose restart admin-portal`

### Issue: "Failed to Fetch" During Token Exchange

**Error**: fetch failed at token endpoint

**Cause**: Container trying to reach localhost instead of keycloak

**Solution**: Verify KEYCLOAK_ISSUER uses internal network:
```yaml
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow  # NOT localhost
```

### Issue: Browser Cannot Reach Authorization Endpoint

**Error**: ERR_NAME_NOT_RESOLVED for keycloak:8080

**Cause**: Authorization URL using internal hostname

**Solution**: Verify KEYCLOAK_ISSUER_BROWSER is set:
```yaml
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```

### Issue: Session Not Persisting

**Error**: User has to login on every page

**Cause**: NEXTAUTH_SECRET not set or changing between requests

**Solution**:
1. Generate stable secret: `openssl rand -base64 32`
2. Set in environment variables
3. Restart services

## See Also

- [OAuth2 Setup Guide](./OAuth2_Setup_Guide.md) - Complete Keycloak setup
- [OAuth2 Docker Configuration](./OAuth2_Docker_Configuration.md) - Docker networking details
- [OAuth2 Troubleshooting](./OAuth2_Troubleshooting.md) - Common errors and solutions

## Archived Files

This guide consolidates information from:
- KEYCLOAK_OAUTH2_CLIENT_SETUP.md
- NEXTAUTH_FIX_SUMMARY.md

## Summary

NextAuth configuration for Werkflow implements:

1. Keycloak provider with explicit URL overrides
2. Dual-URL architecture for Docker environments
3. TrustHost configuration for localhost development
4. Secure session management with encrypted cookies
5. Role-based access control integration

The configuration is production-ready when combined with proper environment-specific settings for HTTPS, domain names, and secret management.
