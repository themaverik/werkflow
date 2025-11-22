# Solution Summary: Keycloak Docker Networking Fix

## Problem Fixed

Admin-portal and hr-portal containers were getting `TypeError: fetch failed` when trying to access Keycloak during OIDC discovery because they were using `localhost:8090` which resolved to the container itself, not the host where Keycloak runs.

## Root Cause

Docker networking localhost ambiguity:
- From inside a container, `localhost` refers to the container itself
- Containers cannot reach the host's `localhost` without special configuration
- Browsers run on the host, so they CAN reach `localhost:8090`
- Keycloak advertises `localhost:8090` in tokens due to `KC_HOSTNAME` and `KC_PROXY` settings

## Solution Implemented: Three-URL Strategy

We implemented a three-URL strategy that separates concerns:

### 1. KEYCLOAK_ISSUER_INTERNAL
Purpose: Server-side API calls from container to Keycloak
Value in Docker: http://keycloak:8080/realms/werkflow
Value in Local Dev: http://localhost:8090/realms/werkflow
Used for:
- OIDC discovery (.well-known/openid-configuration)
- Token exchange endpoint
- Userinfo endpoint

### 2. KEYCLOAK_ISSUER_PUBLIC
Purpose: Token validation - matches the issuer claim in JWT tokens
Value: http://localhost:8090/realms/werkflow (both Docker and local)
Used for:
- Validating JWT tokens received from Keycloak
- Must match what Keycloak puts in the iss claim

### 3. KEYCLOAK_ISSUER_BROWSER
Purpose: OAuth redirects in user's browser
Value: http://localhost:8090/realms/werkflow (both Docker and local)
Used for:
- Authorization endpoint (login page redirect)
- User-facing URLs

## Files Modified

1. docker-compose.yml - Updated environment variables for admin-portal and hr-portal
2. frontends/admin-portal/auth.config.ts - Updated NextAuth configuration
3. frontends/hr-portal/auth.ts - Updated NextAuth configuration
4. frontends/admin-portal/.env.local.example - Updated documentation

## Verification

Container can reach Keycloak via internal network: VERIFIED
OIDC discovery works from container: VERIFIED

## Date

2025-11-21
