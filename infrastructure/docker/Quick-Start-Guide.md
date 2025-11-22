# Keycloak Hostname Fix - Quick Start Guide

## What Was Fixed

The Keycloak container was running in development mode, which caused it to advertise internal Docker hostnames (`keycloak:8080`) that browsers cannot resolve. This resulted in `ERR_NAME_NOT_RESOLVED` errors when accessing the admin console or during OAuth flows.

The fix switches Keycloak to production mode with proper hostname configuration, allowing it to:
- Advertise `localhost:8090` for browser access
- Accept requests on `keycloak:8080` for internal container communication

## Quick Start

### 1. Apply the Fix

From the `infrastructure/docker` directory:

```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
./restart-keycloak.sh
```

This script will:
- Stop the existing Keycloak container
- Start Keycloak with the new configuration
- Wait for it to be ready
- Display verification information

### 2. Verify the Fix

Run the verification script to confirm everything is working:

```bash
./verify-keycloak.sh
```

This will test:
- Container status and health
- Hostname configuration
- OIDC discovery endpoints
- Browser accessibility
- Internal container access

### 3. Test in Browser

Open your browser and navigate to:

```
http://localhost:8090/admin
```

You should now be able to:
- Access the admin console without DNS errors
- See the login page load properly
- Complete the OAuth login flow
- Access all Keycloak resources

Login credentials:
- Username: `admin`
- Password: `admin123`

## What Changed

### docker-compose.yml

The Keycloak service configuration was updated:

**Before** (Development Mode):
```yaml
command: start-dev --http-relative-path /
environment:
  KC_HOSTNAME: localhost  # These were ignored in dev mode
  KC_HOSTNAME_PORT: 8090
```

**After** (Production Mode):
```yaml
command:
  - start                    # Production mode respects hostname config
  - --http-relative-path=/
environment:
  KC_HOSTNAME: localhost              # Now properly applied
  KC_HOSTNAME_PORT: 8090
  KC_HOSTNAME_STRICT: false           # Allow flexible hostname matching
  KC_HOSTNAME_STRICT_BACKCHANNEL: false  # Allow internal access
  KC_HTTP_ENABLED: true
  KC_PROXY: edge                      # Enable proxy mode
```

### Key Changes Explained

1. **Command Change**: `start-dev` → `start`
   - Development mode ignores hostname settings
   - Production mode respects hostname configuration

2. **Added `KC_HOSTNAME_STRICT_BACKCHANNEL: false`**
   - Allows backend services to access Keycloak using internal hostname
   - Maintains efficient container-to-container communication

3. **Added `KC_PROXY: edge`**
   - Tells Keycloak it's behind a reverse proxy
   - Makes Keycloak respect the configured hostname and port

## How It Works

### Browser Flow
```
Browser request → http://localhost:8090/admin
                ↓ (Docker port mapping)
         Host Port 8090 → Container Port 8080
                ↓
         Keycloak (with hostname config)
                ↓
    Response with localhost:8090 URLs
                ↓
         Browser can resolve and access
```

### Container Flow
```
Backend service → http://keycloak:8080/realms/werkflow
               ↓ (internal Docker network)
          Keycloak Container
               ↓
       Token validation succeeds
```

## Troubleshooting

### If restart-keycloak.sh fails

1. **Check if container is running**:
   ```bash
   docker ps --filter name=werkflow-keycloak
   ```

2. **View logs**:
   ```bash
   docker logs werkflow-keycloak --tail 50
   ```

3. **Check database is ready**:
   ```bash
   docker ps --filter name=werkflow-keycloak-db
   ```

4. **Port conflict**:
   ```bash
   lsof -i :8090
   ```

### If verification fails

Run individual checks:

```bash
# Test health endpoint
curl http://localhost:8090/health/ready

# Test OIDC discovery
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# Check hostname in logs
docker logs werkflow-keycloak 2>&1 | grep "Hostname settings"
```

### If browser still shows errors

1. **Clear browser cache**: The browser may have cached the old DNS resolution
2. **Use incognito mode**: Test in a fresh browser session
3. **Check browser console**: Look for specific error messages
4. **Verify OIDC issuer**: Should show `localhost:8090`, not `keycloak:8080`

## Next Steps

After applying this fix:

1. **Test OAuth Login**: Try logging into your applications (admin-portal, hr-portal)
2. **Verify Token Validation**: Backend services should still validate tokens correctly
3. **Monitor Logs**: Watch for any hostname-related warnings
4. **Update Documentation**: Inform team members about the new configuration

## Files Modified

- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/docker-compose.yml` - Updated Keycloak configuration

## Files Created

- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/restart-keycloak.sh` - Restart script
- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/verify-keycloak.sh` - Verification script
- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/Keycloak-Hostname-Configuration.md` - Detailed documentation
- `/Users/lamteiwahlang/Projects/werkflow/infrastructure/docker/Quick-Start-Guide.md` - This guide

## Additional Resources

See `Keycloak-Hostname-Configuration.md` for:
- Detailed explanation of the problem and solution
- Complete configuration reference
- Production deployment considerations
- Advanced troubleshooting steps
