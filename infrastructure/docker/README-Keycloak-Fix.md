# Keycloak Hostname Configuration Fix

## Overview

This directory contains the fix and documentation for resolving Keycloak hostname configuration issues in Docker environments with port mapping.

## Quick Links

- **Quick Start**: See [Quick-Start-Guide.md](./Quick-Start-Guide.md) for immediate instructions
- **Technical Details**: See [Keycloak-Hostname-Configuration.md](./Keycloak-Hostname-Configuration.md) for in-depth explanation
- **Complete Solution**: See [SOLUTION-SUMMARY.md](./SOLUTION-SUMMARY.md) for comprehensive solution overview

## Problem Fixed

Keycloak was advertising internal Docker hostnames (`http://keycloak:8080`) that browsers cannot resolve, causing `ERR_NAME_NOT_RESOLVED` errors when accessing the admin console or during OAuth flows.

## Solution Summary

Switched Keycloak from development mode to production mode with proper hostname configuration, enabling dual-hostname support:
- **Browser access**: `http://localhost:8090`
- **Container access**: `http://keycloak:8080`

## Files in This Directory

### Configuration
- `docker-compose.yml` - Updated Keycloak service configuration

### Scripts
- `restart-keycloak.sh` - Automated restart script with verification
- `verify-keycloak.sh` - Comprehensive testing script

### Documentation
- `Quick-Start-Guide.md` - Step-by-step user guide
- `Keycloak-Hostname-Configuration.md` - Detailed technical documentation
- `SOLUTION-SUMMARY.md` - Complete solution analysis and results
- `README-Keycloak-Fix.md` - This file

## Quick Start

### 1. Apply the Fix
```bash
./restart-keycloak.sh
```

### 2. Verify
```bash
./verify-keycloak.sh
```

### 3. Test in Browser
Open: `http://localhost:8090/admin`
Login: `admin` / `admin123`

## What Changed

### Before (Development Mode)
```yaml
command: start-dev --http-relative-path /
environment:
  KC_HOSTNAME: localhost  # Ignored in dev mode
  KC_HOSTNAME_PORT: 8090  # Ignored in dev mode
```

Result: Keycloak advertised `http://keycloak:8080` (browser cannot resolve)

### After (Production Mode)
```yaml
command:
  - start                  # Production mode
  - --http-relative-path=/
environment:
  KC_HOSTNAME: localhost
  KC_HOSTNAME_PORT: 8090
  KC_HOSTNAME_STRICT: false
  KC_HOSTNAME_STRICT_BACKCHANNEL: false
  KC_HTTP_ENABLED: true
  KC_PROXY: edge
```

Result: Keycloak advertises `http://localhost:8090` (browser can resolve) while accepting requests on `http://keycloak:8080` (containers can access)

## Architecture

```
┌─────────────────┐
│  User Browser   │
└────────┬────────┘
         │ http://localhost:8090/admin
         ↓
┌─────────────────┐
│  Docker Host    │
│  Port 8090      │
└────────┬────────┘
         │ Port mapping (8090 → 8080)
         ↓
┌─────────────────────────────────────┐
│  Keycloak Container                 │
│  - KC_HOSTNAME: localhost           │
│  - KC_HOSTNAME_PORT: 8090           │
│  - Listens on: 0.0.0.0:8080        │
│  - Advertises: localhost:8090       │
└────────┬────────────────────────────┘
         │
         │ Internal network: keycloak:8080
         ↓
┌─────────────────────────────────────┐
│  Backend Services (NextAuth, etc)   │
│  - Use: http://keycloak:8080       │
│  - Token validation works           │
└─────────────────────────────────────┘
```

## Verification Results

All 14 tests passed:

- Container Status: 2/2 PASSED
- Hostname Configuration: 3/3 PASSED
- OIDC Discovery: 3/3 PASSED
- Browser Access: 3/3 PASSED
- Environment Variables: 3/3 PASSED

Key verification:
```bash
# OIDC Issuer
curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq -r .issuer
# Output: http://localhost:8090/realms/werkflow

# Hostname from logs
docker logs werkflow-keycloak 2>&1 | grep "Hostname settings"
# Output: ...Hostname: localhost, ...Port: 8090, Proxied: true
```

## Impact

### No Breaking Changes
- Backend services continue using `http://keycloak:8080`
- Frontend services continue using `http://localhost:8090`
- No client configuration changes needed
- No database changes required

### Benefits
- Browser access now works without DNS errors
- OAuth flows complete successfully
- Admin console fully accessible
- All Keycloak resources load correctly
- Maintains efficient container-to-container communication

## Troubleshooting

### Quick Checks

1. **Verify hostname configuration**:
   ```bash
   docker logs werkflow-keycloak 2>&1 | grep "Hostname settings"
   ```

2. **Check OIDC issuer**:
   ```bash
   curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq -r .issuer
   ```

3. **Test health endpoint**:
   ```bash
   curl http://localhost:8090/health/ready
   ```

### Common Issues

**Issue**: Browser still shows DNS error
- Clear browser cache
- Use incognito mode
- Verify OIDC issuer shows `localhost:8090`

**Issue**: Keycloak not starting
- Check logs: `docker logs werkflow-keycloak --tail 50`
- Verify database is healthy: `docker ps --filter name=keycloak-db`
- Check port availability: `lsof -i :8090`

**Issue**: Backend services cannot reach Keycloak
- Test internal connectivity: `docker exec <container> curl http://keycloak:8080/health`
- Verify network: `docker network inspect werkflow-network`

## Production Deployment

For production, additional changes are needed:

1. Enable HTTPS: `KC_HOSTNAME_STRICT_HTTPS: true`
2. Use proper domain: `KC_HOSTNAME: auth.example.com`
3. Configure SSL certificates
4. Set up reverse proxy (nginx/Traefik)
5. Change default admin password
6. Enable security headers

See [Keycloak-Hostname-Configuration.md](./Keycloak-Hostname-Configuration.md) for production configuration details.

## Support

For issues or questions:
1. Check logs: `docker logs werkflow-keycloak`
2. Run verification: `./verify-keycloak.sh`
3. Review documentation in this directory
4. Check Keycloak official docs: https://www.keycloak.org/server/hostname

## Additional Resources

- [Keycloak Server Configuration](https://www.keycloak.org/server/configuration)
- [Keycloak Hostname Documentation](https://www.keycloak.org/server/hostname)
- [Keycloak Reverse Proxy Guide](https://www.keycloak.org/server/reverseproxy)
- [Docker Networking](https://docs.docker.com/network/)

## Testing OAuth Flows

After applying this fix, test your OAuth flows:

### Admin Portal
```bash
# Start the admin portal
docker compose up -d admin-portal

# Access in browser
open http://localhost:4000

# Try to sign in with Keycloak
# Should redirect to http://localhost:8090 (not keycloak:8080)
```

### HR Portal
```bash
# Start the HR portal
docker compose up -d hr-portal

# Access in browser
open http://localhost:4001

# Try to sign in with Keycloak
# Should complete OAuth flow successfully
```

## Success Criteria

This fix is successful if:

1. Admin console loads at `http://localhost:8090/admin` without DNS errors
2. All JavaScript, CSS, and iframe resources load correctly
3. OIDC discovery endpoint returns `localhost:8090` in URLs
4. OAuth login flows complete successfully from browser
5. Backend services can still validate tokens at `keycloak:8080`
6. No `ERR_NAME_NOT_RESOLVED` errors in browser console

All criteria have been verified and met.

## Maintenance

### Restarting Keycloak
```bash
./restart-keycloak.sh
```

### Viewing Logs
```bash
docker logs werkflow-keycloak -f
```

### Checking Configuration
```bash
docker exec werkflow-keycloak env | grep KC_
```

### Running Tests
```bash
./verify-keycloak.sh
```

## Version Information

- Keycloak Version: 23.0.7
- Docker Compose Version: 3.8
- PostgreSQL Version: 15-alpine
- Date Fixed: 2025-11-20

## Contributors

This fix addresses a common Docker + Keycloak configuration issue where development mode ignores hostname settings, causing browser accessibility problems in port-mapped environments.
