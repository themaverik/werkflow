# Keycloak OAuth Fix - Quick Reference

## Problem
OAuth callback error: "Invalid client or Invalid client credentials"

## Solution Applied

### 1. Updated Client Secrets in docker-compose.yml

**Admin Portal**:
```yaml
KEYCLOAK_CLIENT_SECRET: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
```

**HR Portal**:
```yaml
KEYCLOAK_CLIENT_SECRET: HR_PORTAL_SECRET_2024_SECURE
```

### 2. Created HR Portal Client in Keycloak

Client created with ID: `werkflow-hr-portal`

### 3. Recreated Containers

```bash
docker-compose up -d --force-recreate admin-portal hr-portal
```

## Quick Verification

### Check Container Status
```bash
docker-compose ps
```

### Verify Environment Variables
```bash
# Admin Portal
docker exec werkflow-admin-portal printenv | grep KEYCLOAK_CLIENT_SECRET
# Should show: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR

# HR Portal
docker exec werkflow-hr-portal printenv | grep KEYCLOAK_CLIENT_SECRET
# Should show: HR_PORTAL_SECRET_2024_SECURE
```

### Check for Auth Errors
```bash
# Should return no results (clean logs)
docker logs werkflow-admin-portal 2>&1 | grep "CallbackRouteError"
docker logs werkflow-hr-portal 2>&1 | grep "CallbackRouteError"
```

## Testing OAuth Login

### Admin Portal (http://localhost:4000)
1. Open http://localhost:4000
2. Click "Sign In"
3. Login with Keycloak credentials
4. Should successfully authenticate

### HR Portal (http://localhost:4001)
1. Open http://localhost:4001
2. Click "Sign In"
3. Login with Keycloak credentials
4. Should successfully authenticate

## Keycloak Client Configuration

### Admin Portal Client
- **Client ID**: werkflow-admin-portal
- **Client Secret**: 4kwrHZC0rZMfmeazfxpXG9UXW0gXqmRR
- **Redirect URIs**:
  - http://localhost:4000/api/auth/callback/keycloak
  - http://localhost:4000/login

### HR Portal Client
- **Client ID**: werkflow-hr-portal
- **Client Secret**: HR_PORTAL_SECRET_2024_SECURE
- **Redirect URIs**:
  - http://localhost:4001/api/auth/callback/keycloak
  - http://localhost:4001/login

## Quick Commands

### View Keycloak Client Configuration
```bash
# Admin Portal
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin123

docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh get clients \
  -r werkflow -q clientId=werkflow-admin-portal

# HR Portal
docker exec werkflow-keycloak /opt/keycloak/bin/kcadm.sh get clients \
  -r werkflow -q clientId=werkflow-hr-portal
```

### Restart Portals
```bash
docker-compose restart admin-portal hr-portal
```

### View Logs
```bash
docker logs -f werkflow-admin-portal
docker logs -f werkflow-hr-portal
docker logs -f werkflow-keycloak
```

## Files Modified
- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml`

## Documentation
- Full details: `KEYCLOAK_OAUTH_FIX.md`
- Quick reference: This file

## Status
RESOLVED - Both portals configured with correct client secrets
