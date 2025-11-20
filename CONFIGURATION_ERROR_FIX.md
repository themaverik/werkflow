# Configuration Error Fix - Complete Summary

## The Problem

You were getting:
```
HTTP 500 Internal Server Error
Request URL: http://localhost:4000/api/auth/error?error=Configuration
```

When attempting to login at `http://localhost:4000/login`.

---

## Root Cause Analysis

The error had **two causes**:

### Cause 1: Docker Network Communication (Now Fixed)
- KEYCLOAK_ISSUER was set to `http://localhost:8090/realms/werkflow`
- Inside Docker containers, `localhost` doesn't work (it refers to the container itself, not the host)
- The admin portal couldn't reach Keycloak to validate OAuth2 configuration
- Result: `fetch failed` errors in logs

**Fix Applied:**
- Changed KEYCLOAK_ISSUER to `http://keycloak:8080/realms/werkflow`
- Uses the Docker service name instead of localhost
- Containers can now communicate via the `werkflow-network` bridge

### Cause 2: Missing OAuth2 Client (Still Needs Setup)
- The Keycloak OAuth2 client `werkflow-admin-portal` hasn't been created
- NextAuth can't authenticate without a valid client and secret
- Result: "Configuration" error when login page tries to use Keycloak

**Fix Required:**
- Follow the setup guide to create OAuth2 clients in Keycloak

---

## What Was Fixed

### Docker Compose Configuration
- Updated `admin-portal` KEYCLOAK_ISSUER from `http://localhost:8090` to `http://keycloak:8080`
- Updated `hr-portal` KEYCLOAK_ISSUER from `http://localhost:8090` to `http://keycloak:8080`
- Containers restarted to apply changes

### Result
✅ Admin portal can now reach Keycloak
✅ No more "fetch failed" errors
✅ `/studio/processes` still returns proper 302 redirect
✅ Ready for OAuth2 client configuration

---

## What Still Needs to Be Done

You need to create OAuth2 clients in Keycloak. Follow this guide:

**File**: `KEYCLOAK_OAUTH2_CLIENT_SETUP.md`

Quick summary:
1. Go to `http://localhost:8090/admin/master/console`
2. Login with `admin` / `admin123`
3. Switch to `werkflow` realm
4. Create client: `werkflow-admin-portal`
5. Enable client authentication
6. Configure redirect URIs
7. Copy the client secret
8. Set environment variable or update docker-compose
9. Restart admin-portal

Then repeat for `werkflow-hr-portal`.

---

## Files Modified

```
infrastructure/docker/docker-compose.yml
- Line 343: KEYCLOAK_ISSUER (admin-portal)
- Line 385: KEYCLOAK_ISSUER (hr-portal)
```

Changed:
```yaml
# Before
KEYCLOAK_ISSUER: http://localhost:8090/realms/werkflow

# After
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
```

---

## Testing Checklist

After completing the OAuth2 client setup:

- [ ] Admin portal loads at `http://localhost:4000`
- [ ] Landing page displays correctly
- [ ] Click "Process Studio" button
- [ ] Redirects to login page
- [ ] Login form appears (not "Configuration" error)
- [ ] Login with `admin` / `admin123`
- [ ] Redirects to `/studio/processes`
- [ ] BPMN processes list displays

---

## Key Points to Remember

**Docker Networking:**
- Inside containers, use service names: `http://keycloak:8080`
- From host machine, use localhost: `http://localhost:8090`
- Don't mix them or containers can't reach each other

**Keycloak OAuth2 Client:**
- Client ID: `werkflow-admin-portal`
- Redirect URI: `http://localhost:4000/api/auth/callback/keycloak`
- Must be created in the `werkflow` realm (not Master)
- Client secret must be copied and set in Docker environment

**Environment Variables:**
```bash
# For admin portal
export KEYCLOAK_ADMIN_PORTAL_SECRET="<client-secret-from-keycloak>"

# For hr portal
export KEYCLOAK_HR_PORTAL_SECRET="<client-secret-from-keycloak>"

# Then restart
docker-compose restart admin-portal hr-portal
```

---

## Next Steps

1. **Read**: `KEYCLOAK_OAUTH2_CLIENT_SETUP.md`
2. **Follow**: 10 steps to create OAuth2 clients
3. **Test**: Login flow at `http://localhost:4000/login`
4. **Explore**: BPMN process designer

---

## Summary

✅ **Fixed**: Docker network connectivity for Keycloak
✅ **Cause**: Was using localhost instead of service name
✅ **Result**: Containers can now communicate properly
⏳ **Next**: Create OAuth2 clients in Keycloak (using the guide provided)

Everything is ready! Just need Keycloak client configuration.
