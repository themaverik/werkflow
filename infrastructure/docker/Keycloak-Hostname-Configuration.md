# Keycloak Hostname Configuration

## Problem Summary

When running Keycloak in Docker with port mapping, browsers received `ERR_NAME_NOT_RESOLVED` errors when trying to access the admin console or during OAuth flows. The browser was attempting to reach `http://keycloak:8080` (internal Docker hostname) which cannot be resolved from the host machine.

## Root Cause

Keycloak's `start-dev` command ignores hostname configuration environment variables:
- `KC_HOSTNAME`
- `KC_HOSTNAME_PORT`
- `KC_PROXY`

In development mode, Keycloak auto-detects hostnames from the container network, resulting in:
- Advertised hostname: `keycloak` (internal Docker hostname)
- Advertised port: `8080` (internal container port)
- OIDC issuer URL: `http://keycloak:8080/realms/werkflow`

This causes browser-based flows to fail because `keycloak:8080` is not resolvable from the host machine.

## Solution Architecture

### Dual-Hostname Strategy

The solution implements a dual-hostname configuration:

1. **External/Browser Access**: `http://localhost:8090`
   - Used by user browsers accessing admin console
   - Used during OAuth login/callback flows
   - Used by frontend JavaScript and iframes

2. **Internal/Container Access**: `http://keycloak:8080`
   - Used by backend services for token validation
   - Used for container-to-container communication
   - Maintains service mesh efficiency

### Key Configuration Changes

#### 1. Switch to Production Mode
```yaml
command:
  - start                    # Production mode (was: start-dev)
  - --http-relative-path=/
```

**Why**: Only production mode respects hostname configuration environment variables.

#### 2. Hostname Environment Variables
```yaml
environment:
  KC_HOSTNAME: localhost              # Frontend URL hostname
  KC_HOSTNAME_PORT: 8090              # Frontend URL port (mapped port)
  KC_HOSTNAME_STRICT: false           # Allow flexible hostname matching
  KC_HOSTNAME_STRICT_HTTPS: false     # Allow HTTP for local dev
  KC_HOSTNAME_STRICT_BACKCHANNEL: false  # Allow internal network access
  KC_HTTP_ENABLED: true               # Enable HTTP protocol
  KC_PROXY: edge                      # Enable proxy headers support
```

**Result**: Keycloak now advertises `http://localhost:8090` for all frontend URLs while still accepting requests on the internal `keycloak:8080` address.

## How It Works

### Request Flow

#### Browser-Initiated Requests
```
Browser → http://localhost:8090/admin
         ↓ (port mapping)
Docker Host Port 8090 → Container Port 8080
         ↓
Keycloak responds with URLs using localhost:8090
         ↓
Browser can resolve and access all resources
```

#### Container-to-Container Requests
```
NextAuth (in container) → http://keycloak:8080/realms/werkflow
                        ↓ (internal network)
                  Keycloak Container
                        ↓
Token validation succeeds
```

### OIDC Discovery

With the fix, the OIDC discovery endpoint returns:
```json
{
  "issuer": "http://localhost:8090/realms/werkflow",
  "authorization_endpoint": "http://localhost:8090/realms/werkflow/protocol/openid-connect/auth",
  "token_endpoint": "http://localhost:8090/realms/werkflow/protocol/openid-connect/token",
  ...
}
```

All URLs are now browser-accessible.

## Configuration Reference

### Environment Variables Explained

| Variable | Value | Purpose |
|----------|-------|---------|
| `KC_HOSTNAME` | `localhost` | Sets the hostname used in frontend URLs |
| `KC_HOSTNAME_PORT` | `8090` | Sets the port used in frontend URLs |
| `KC_HOSTNAME_STRICT` | `false` | Allows Keycloak to accept requests on different hostnames (like `keycloak`) |
| `KC_HOSTNAME_STRICT_HTTPS` | `false` | Allows HTTP protocol for local development |
| `KC_HOSTNAME_STRICT_BACKCHANNEL` | `false` | Allows backend services to use internal hostname |
| `KC_HTTP_ENABLED` | `true` | Enables HTTP protocol (default is HTTPS only) |
| `KC_PROXY` | `edge` | Tells Keycloak to trust X-Forwarded headers |

### Command Arguments

| Argument | Purpose |
|----------|---------|
| `start` | Production mode - enables hostname configuration |
| `--http-relative-path=/` | Serves Keycloak at root path instead of /auth |

## Testing the Configuration

### 1. Apply the Configuration
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
./restart-keycloak.sh
```

### 2. Verify Hostname Settings
```bash
# Check logs for hostname configuration
docker logs werkflow-keycloak 2>&1 | grep "Hostname settings"

# Expected output:
# Hostname settings: ... Hostname: localhost, ... Port: 8090, Proxied: true
```

### 3. Verify OIDC Issuer
```bash
# Check OIDC discovery endpoint
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer

# Expected output:
# "http://localhost:8090/realms/werkflow"
```

### 4. Test Browser Access
```bash
# Test admin console
open http://localhost:8090/admin

# Should load without ERR_NAME_NOT_RESOLVED errors
```

### 5. Test Internal Container Access
```bash
# From within a container in the same network
docker exec werkflow-hr-portal curl -s http://keycloak:8080/realms/werkflow/.well-known/openid-configuration | grep issuer

# Expected: Should return the discovery document
```

## NextAuth Configuration

The frontend applications use two separate environment variables:

### Server-Side Token Validation
```yaml
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
```
Used by NextAuth server-side code running inside containers to validate tokens directly against the internal Keycloak endpoint.

### Browser-Side OAuth Redirects
```yaml
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow
```
Used for OAuth authorization/callback flows that require browser access.

This dual-issuer approach is implemented in the NextAuth configuration and works seamlessly with Keycloak's hostname settings.

## Troubleshooting

### Issue: Still seeing `keycloak:8080` in browser
**Check**: Verify Keycloak is running in production mode
```bash
docker logs werkflow-keycloak 2>&1 | grep "started in"
# Should NOT say "start-dev"
```

**Check**: Verify hostname environment variables are set
```bash
docker exec werkflow-keycloak env | grep KC_HOSTNAME
```

### Issue: Keycloak not starting
**Check**: View startup logs
```bash
docker logs werkflow-keycloak --tail 50
```

**Common causes**:
- Database not ready (check `keycloak-postgres` health)
- Port 8090 already in use
- Invalid environment variable format

### Issue: Backend services cannot reach Keycloak
**Check**: Verify internal network connectivity
```bash
docker exec werkflow-hr-portal ping -c 2 keycloak
docker exec werkflow-hr-portal curl -s http://keycloak:8080/health
```

## Production Considerations

For production deployments, additional changes are recommended:

1. **Enable HTTPS**
   - Set `KC_HOSTNAME_STRICT_HTTPS: true`
   - Configure SSL certificates
   - Use a proper domain name instead of `localhost`

2. **Use a Reverse Proxy**
   - Deploy nginx or Traefik in front of Keycloak
   - Configure proper SSL termination
   - Set `KC_PROXY: edge` or `KC_PROXY: reencrypt`

3. **Secure Admin Console**
   - Change default admin credentials
   - Use separate admin port or path
   - Enable admin console HTTPS requirement

4. **Database Optimization**
   - Use a managed PostgreSQL service
   - Configure connection pooling
   - Enable SSL for database connections

## References

- [Keycloak Server Configuration](https://www.keycloak.org/server/configuration)
- [Keycloak Hostname Configuration](https://www.keycloak.org/server/hostname)
- [Keycloak Reverse Proxy Setup](https://www.keycloak.org/server/reverseproxy)
