# OAuth2 Troubleshooting

Quick reference guide for resolving common OAuth2 and Keycloak authentication issues in Werkflow.

## Table of Contents

1. [Quick Diagnostic Steps](#quick-diagnostic-steps)
2. [Common Errors](#common-errors)
3. [Configuration Issues](#configuration-issues)
4. [Network Issues](#network-issues)
5. [Client Setup Issues](#client-setup-issues)
6. [Token Validation Issues](#token-validation-issues)
7. [Diagnostic Commands](#diagnostic-commands)

## Quick Diagnostic Steps

When encountering authentication issues, run these checks in order:

### Step 1: Verify Services Are Running

```bash
docker ps | grep werkflow

# Should show all services running:
# werkflow-keycloak
# werkflow-admin-portal
# werkflow-hr-portal
# werkflow-keycloak-db
```

### Step 2: Check Keycloak Health

```bash
curl http://localhost:8090/health/ready

# Should return: {"status":"UP"}
```

### Step 3: Verify Realm Exists

```bash
curl http://localhost:8090/realms/werkflow | jq .

# Should return realm configuration, not 404
```

### Step 4: Test OIDC Discovery

```bash
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer

# Should return: "http://localhost:8090/realms/werkflow"
```

### Step 5: Check Container Logs

```bash
docker logs werkflow-admin-portal --tail 50
docker logs werkflow-keycloak --tail 50

# Look for error messages
```

## Common Errors

### Error: "Configuration" Error Page

**URL**: `http://localhost:4000/api/auth/error?error=Configuration`

**Symptoms**: Login button redirects to error page

**Causes**:
1. OAuth2 client not created in Keycloak
2. Client secret mismatch
3. Missing environment variables

**Solutions**:

**Check 1: Verify Client Exists**
```
1. Access Keycloak: http://localhost:8090/admin/master/console
2. Login: admin / admin123
3. Select realm: werkflow
4. Navigate to: Clients
5. Verify: werkflow-admin-portal exists
```

**Check 2: Verify Client Secret**
```
1. In Keycloak: Clients → werkflow-admin-portal → Credentials
2. Copy client secret
3. Compare with docker-compose.yml KEYCLOAK_CLIENT_SECRET
4. If different, update and restart:
   docker-compose restart admin-portal
```

**Check 3: Verify Environment Variables**
```bash
docker exec werkflow-admin-portal env | grep KEYCLOAK

# Should show:
# KEYCLOAK_CLIENT_ID=werkflow-admin-portal
# KEYCLOAK_CLIENT_SECRET=<secret>
# KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
# KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow
```

### Error: "Realm Does Not Exist"

**Symptoms**: 404 error when accessing realm endpoints

**Cause**: werkflow realm not created in Keycloak

**Solution**:
```
1. Access Keycloak admin console
2. Click realm dropdown (shows "Master")
3. Click "Create realm"
4. Name: werkflow
5. Enable: ON
6. Click "Create"
```

### Error: "Access Denied"

**Symptoms**: User sees "Access Denied" page after successful login

**Cause**: User doesn't have required role (HR_ADMIN)

**Solution**:
```
1. Access Keycloak: http://localhost:8090/admin/master/console
2. Select realm: werkflow
3. Navigate to: Users
4. Find user who got "Access Denied"
5. Click user → Role mappings tab
6. Click "Assign role"
7. Select: HR_ADMIN
8. Click "Assign"
9. User logout and login again
```

### Error: "Invalid Redirect URI"

**Symptoms**: Keycloak error after login attempt

**Cause**: Redirect URI not configured in Keycloak client

**Solution**:
```
1. Keycloak: Clients → werkflow-admin-portal → Settings
2. Verify "Valid redirect URIs" includes:
   - http://localhost:4000/*
   - http://localhost:4000/api/auth/callback/keycloak
3. Verify "Web origins" includes:
   - http://localhost:4000
4. Click "Save"
```

### Error: "Failed to Fetch"

**Symptoms**: Error during token exchange, logs show "fetch failed"

**Cause**: Container trying to reach localhost:8090 instead of keycloak:8080

**Solution**:
```yaml
# In docker-compose.yml, verify:
KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow

# NOT:
KEYCLOAK_ISSUER: http://localhost:8090/realms/werkflow

# Restart after fix:
docker-compose restart admin-portal
```

### Error: "ERR_NAME_NOT_RESOLVED"

**Symptoms**: Browser cannot connect to keycloak:8080

**Cause**: Browser redirected to internal Docker hostname

**Solution**:
```yaml
# Verify KEYCLOAK_ISSUER_BROWSER is set:
KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow

# Restart if missing:
docker-compose restart admin-portal
```

## Configuration Issues

### Issue: UntrustedHost Error

**Symptoms**: HTTP 404 on protected routes, logs show "UntrustedHost: Host must be trusted"

**Cause**: NextAuth not trusting localhost

**Solution**:

**Option 1: auth.config.ts**
```typescript
export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [...],
  trustHost: true,  // Add this
})
```

**Option 2: Environment Variable**
```yaml
# In docker-compose.yml
environment:
  AUTH_TRUST_HOST: true
```

Restart: `docker-compose restart admin-portal`

### Issue: Missing NEXTAUTH_SECRET

**Symptoms**: Session not persisting, login required on every page

**Cause**: NEXTAUTH_SECRET not set

**Solution**:
```bash
# Generate secret
openssl rand -base64 32

# Add to docker-compose.yml
NEXTAUTH_SECRET: <generated-secret>

# Restart
docker-compose restart admin-portal
```

### Issue: Keycloak Hostname Configuration Ignored

**Symptoms**: Keycloak advertises keycloak:8080 instead of localhost:8090

**Cause**: Running in development mode which ignores hostname config

**Solution**:
```yaml
# In docker-compose.yml keycloak service, change:
command: start-dev

# To:
command:
  - start
  - --http-relative-path=/

# Restart:
docker-compose restart keycloak
```

## Network Issues

### Issue: Container Cannot Reach Keycloak

**Symptoms**: "Connection refused" or "fetch failed" in container logs

**Diagnostic**:
```bash
# Test from inside container
docker exec werkflow-admin-portal curl http://keycloak:8080/health/ready

# Should return: {"status":"UP"}
```

**Solutions**:

**Check 1: Verify Network**
```bash
docker network ls | grep werkflow

# Should show: werkflow-network
```

**Check 2: Verify Service on Network**
```bash
docker network inspect werkflow-network | grep -A 5 keycloak

# Should show keycloak container
```

**Check 3: Restart Services**
```bash
docker-compose restart keycloak admin-portal
```

### Issue: Browser Cannot Reach Keycloak

**Symptoms**: "This site can't be reached" when accessing localhost:8090

**Diagnostic**:
```bash
# Test from host
curl http://localhost:8090/health/ready

# Should return: {"status":"UP"}
```

**Solutions**:

**Check 1: Verify Port Mapping**
```bash
docker port werkflow-keycloak

# Should show: 8080/tcp -> 0.0.0.0:8090
```

**Check 2: Verify Keycloak Running**
```bash
docker ps | grep keycloak

# Should show werkflow-keycloak running
```

**Check 3: Check Firewall**
```bash
# macOS
sudo lsof -i :8090

# Linux
sudo netstat -tulpn | grep 8090

# Should show Docker proxy listening
```

## Client Setup Issues

### Issue: Client Not Found in Keycloak

**Symptoms**: "Configuration" error or "Client not found" in logs

**Verification**:
```
1. Keycloak Admin Console
2. Realm: werkflow
3. Clients menu
4. Search for: werkflow-admin-portal
```

**Solution**: If not found, create the client following the setup guide.

### Issue: Client Secret Mismatch

**Symptoms**: Authentication fails after client creation

**Verification**:
```
1. Keycloak: Clients → werkflow-admin-portal → Credentials
2. Note the "Client secret"
3. Compare with docker-compose.yml KEYCLOAK_CLIENT_SECRET
```

**Solution**:
```bash
# Update docker-compose.yml or use environment variable
export KEYCLOAK_ADMIN_PORTAL_SECRET="<correct-secret>"
docker-compose restart admin-portal
```

### Issue: Wrong Client Type

**Symptoms**: Public client error or unauthorized client

**Verification**:
```
1. Keycloak: Clients → werkflow-admin-portal → Settings
2. Check "Client authentication" is ON
3. Check capability config has:
   - Standard flow: ENABLED
   - Direct access grants: ENABLED
```

**Solution**: Update client settings and save.

## Token Validation Issues

### Issue: Issuer Mismatch

**Symptoms**: Token rejected, issuer validation error in logs

**Cause**: Token issuer doesn't match expected issuer

**Diagnostic**:
```bash
# Check what Keycloak advertises
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer

# Compare with container's KEYCLOAK_ISSUER
docker exec werkflow-admin-portal env | grep KEYCLOAK_ISSUER
```

**Solution**: Ensure `KC_HOSTNAME_STRICT=false` in Keycloak configuration.

### Issue: Expired Token

**Symptoms**: Logout after short time, "Token expired" errors

**Cause**: Token lifetime too short

**Solution**:
```
1. Keycloak: Realm settings → Tokens
2. Increase "Access Token Lifespan" (default: 5 minutes)
3. Increase "SSO Session Idle" (default: 30 minutes)
4. Click "Save"
```

### Issue: Invalid Token Signature

**Symptoms**: Token signature verification failed

**Cause**: Client secret mismatch or realm key changed

**Solution**:
1. Verify client secret matches
2. Check Keycloak logs for key rotation
3. Restart services to refresh keys

## Diagnostic Commands

### Complete Health Check

```bash
#!/bin/bash

echo "=== Werkflow OAuth2 Health Check ==="

echo -e "\n1. Docker Services:"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep werkflow

echo -e "\n2. Keycloak Health:"
curl -s http://localhost:8090/health/ready | jq .

echo -e "\n3. Realm Existence:"
curl -s http://localhost:8090/realms/werkflow | jq -r '.realm // "ERROR: Realm not found"'

echo -e "\n4. OIDC Issuer:"
curl -s http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq -r '.issuer'

echo -e "\n5. Admin Portal Environment:"
docker exec werkflow-admin-portal env 2>/dev/null | grep -E 'KEYCLOAK|NEXTAUTH' | sort

echo -e "\n6. Container Connectivity:"
docker exec werkflow-admin-portal curl -s http://keycloak:8080/health/ready 2>/dev/null | jq .

echo -e "\n=== Health Check Complete ==="
```

### Log Analysis

```bash
# Check for common errors
docker logs werkflow-admin-portal 2>&1 | grep -i -E 'error|failed|denied|403|404|500'

# Check Keycloak errors
docker logs werkflow-keycloak 2>&1 | grep -i -E 'error|warn|failed'

# Real-time monitoring
docker logs -f werkflow-admin-portal | grep -i auth
```

### Network Diagnostics

```bash
# Test DNS resolution inside container
docker exec werkflow-admin-portal nslookup keycloak

# Test HTTP connectivity
docker exec werkflow-admin-portal curl -v http://keycloak:8080/health/ready

# Test from host
curl -v http://localhost:8090/health/ready
```

## See Also

- [OAuth2 Setup Guide](./OAuth2_Setup_Guide.md) - Initial setup instructions
- [OAuth2 Docker Configuration](./OAuth2_Docker_Configuration.md) - Docker networking details
- [NextAuth Configuration](./NextAuth_Configuration.md) - NextAuth.js configuration
- [Authentication Issues](../Troubleshooting/Authentication_Issues.md) - General auth troubleshooting

## Archived Files

This guide consolidates troubleshooting information from:
- SOLUTION_SUMMARY.md
- CONFIGURATION_ERROR_FIX.md
- Various setup and fix documents

## Summary

Most OAuth2 issues fall into these categories:

1. **Configuration**: Missing client, wrong secret, missing environment variables
2. **Network**: Docker DNS resolution, port mapping, firewall
3. **Setup**: Realm doesn't exist, roles not assigned, redirect URIs wrong
4. **Tokens**: Issuer mismatch, expiration, signature validation

Follow the diagnostic steps methodically, check logs thoroughly, and verify each component is properly configured. Most issues can be resolved by ensuring environment variables match between Keycloak and the frontends.
