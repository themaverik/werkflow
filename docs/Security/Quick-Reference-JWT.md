# JWT Configuration Quick Reference

## Environment Variables

### Local Development
```bash
export KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
```

### Docker Deployment
```bash
export KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
```

## Valid Issuers

The system automatically accepts JWT tokens from:

1. `http://localhost:8090/realms/werkflow` (External/Browser)
2. `http://keycloak:8080/realms/werkflow` (Internal Docker)

## Quick Testing

### 1. Get JWT Token
```bash
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=werkflow-engine" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=admin" \
  -d "password=admin"
```

### 2. Extract Token
```bash
TOKEN=$(curl -s -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=werkflow-engine" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')
```

### 3. Test API
```bash
curl -X GET http://localhost:8081/api/process-definitions \
  -H "Authorization: Bearer $TOKEN"
```

## Debug Logging

### Enable Debug Logs
Add to `application.yml`:
```yaml
logging:
  level:
    com.werkflow.engine.config.JwtDecoderConfig: DEBUG
    org.springframework.security.oauth2: DEBUG
```

### View Logs in Docker
```bash
docker-compose logs -f engine-service | grep -i "jwt\|issuer\|token"
```

## Common Errors

### Invalid Issuer
**Error**: `Invalid issuer: http://unknown:8080/realms/werkflow`

**Fix**: Token must come from Keycloak at localhost:8090 or keycloak:8080

### Invalid Signature
**Error**: `An error occurred while attempting to decode the Jwt: Signed JWT rejected`

**Fix**:
- Verify Keycloak is running
- Check JWK set URI is accessible
- Ensure token hasn't been modified

### Token Expired
**Error**: `Jwt expired at...`

**Fix**: Obtain a new token

## File Locations

### Configuration
- Main Config: `services/engine/src/main/java/com/werkflow/engine/config/JwtDecoderConfig.java`
- Security Config: `services/engine/src/main/java/com/werkflow/engine/config/SecurityConfig.java`
- Application Config: `services/engine/src/main/resources/application.yml`

### Tests
- Unit Tests: `services/engine/src/test/java/com/werkflow/engine/config/JwtDecoderConfigTest.java`

### Documentation
- Detailed Guide: `docs/Security/JWT-Multi-Issuer-Configuration.md`
- Security Overview: `docs/Security/README.md`

## Architecture

```
Request → Spring Security → Custom JwtDecoder → Multi-Issuer Validation
                                ↓
                         Fetch JWK Sets (keycloak:8080)
                                ↓
                         Validate Signature
                                ↓
                         Validate Expiry
                                ↓
                         Validate Issuer (localhost:8090 OR keycloak:8080)
                                ↓
                         Extract Authorities
                                ↓
                         Authenticated Request
```

## Security Validations

The custom decoder maintains all security validations:

- ✅ Signature verification using JWK public keys
- ✅ Token expiry validation
- ✅ Not-before validation
- ✅ Issuer validation (multi-issuer support)
- ✅ Token format validation

## Production Checklist

Before deploying to production:

- [ ] Update issuers to use HTTPS URLs
- [ ] Set appropriate token lifetimes in Keycloak
- [ ] Configure secret management (no hardcoded secrets)
- [ ] Enable security event logging
- [ ] Set up monitoring and alerts
- [ ] Test with production-like Keycloak setup
- [ ] Document emergency rollback procedure
- [ ] Verify certificate validity

## Additional Resources

- [Full JWT Documentation](./JWT-Multi-Issuer-Configuration.md)
- [Spring Security OAuth2 Docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
