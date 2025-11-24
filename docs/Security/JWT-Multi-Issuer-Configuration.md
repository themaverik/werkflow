# JWT Multi-Issuer Configuration

## Overview

The Werkflow Engine Service implements a custom JWT decoder configuration to handle tokens issued from multiple Keycloak URLs. This solves the issuer mismatch problem that occurs in Docker-based deployments where external and internal URLs differ.

## Problem Statement

### The Issuer Mismatch Issue

When running Keycloak in Docker with port mapping:

- **External Access (Browser)**: `http://localhost:8090/realms/werkflow`
- **Internal Access (Docker Network)**: `http://keycloak:8080/realms/werkflow`

JWT tokens contain an `iss` (issuer) claim that matches the URL used during authentication. This creates a problem:

1. User authenticates via browser using `localhost:8090`
2. JWT token contains issuer: `http://localhost:8090/realms/werkflow`
3. Backend service tries to validate token using `http://keycloak:8080/realms/werkflow`
4. Validation fails due to issuer mismatch

## Solution Architecture

### Custom JWT Decoder

The solution implements a custom `JwtDecoder` bean that:

1. **Fetches JWK Sets** from the internal Keycloak URL (`keycloak:8080`)
2. **Validates Signatures** using public keys from the JWK set
3. **Accepts Multiple Issuers** - both `localhost:8090` and `keycloak:8080`
4. **Maintains Security** - all standard validations remain intact

### Security Guarantees

The custom decoder maintains all critical security validations:

- **Signature Validation**: Uses JWK public keys from Keycloak
- **Expiry Validation**: Checks `exp` claim
- **Not Before Validation**: Checks `nbf` claim
- **Issuer Validation**: Ensures token comes from trusted Keycloak instance

## Implementation Details

### Component Structure

```
services/engine/src/main/java/com/werkflow/engine/config/
├── JwtDecoderConfig.java       # Custom JWT decoder configuration
└── SecurityConfig.java         # Main security configuration
```

### Key Components

#### 1. JwtDecoderConfig Class

**Location**: `services/engine/src/main/java/com/werkflow/engine/config/JwtDecoderConfig.java`

**Responsibilities**:
- Creates custom `JwtDecoder` bean
- Configures JWK set URI for public key fetching
- Sets up multi-issuer validation
- Provides comprehensive logging

#### 2. JwtIssuerValidator

**Type**: Inner static class in `JwtDecoderConfig`

**Responsibilities**:
- Validates JWT issuer claim against list of valid issuers
- Provides detailed error messages for debugging
- Logs validation results

### Configuration Flow

```
1. Spring Security initializes
   ↓
2. JwtDecoderConfig creates JwtDecoder bean
   ↓
3. NimbusJwtDecoder configured with JWK set URI
   ↓
4. Custom validators attached:
   - JwtIssuerValidator (multi-issuer support)
   - JwtTimestampValidator (expiry checking)
   ↓
5. SecurityConfig uses the custom JwtDecoder automatically
```

## Configuration

### Environment Variables

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://localhost:8090/realms/werkflow}
          jwk-set-uri: ${KEYCLOAK_ISSUER:http://localhost:8090/realms/werkflow}/protocol/openid-connect/certs
```

### Docker Environment

For Docker deployments, set:

```bash
KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
```

This ensures the backend fetches JWK sets from the internal Docker hostname.

### Valid Issuers

The decoder is configured to accept tokens from:

1. `http://localhost:8090/realms/werkflow` - External/browser access
2. `http://keycloak:8080/realms/werkflow` - Internal Docker network

## Logging Configuration

### Debug Logging

Enable detailed JWT validation logging:

```yaml
logging:
  level:
    com.werkflow.engine.config.JwtDecoderConfig: DEBUG
    org.springframework.security.oauth2: DEBUG
```

### Log Messages

**Successful Validation**:
```
INFO - Configuring custom JWT decoder with JWK Set URI: http://keycloak:8080/realms/werkflow/protocol/openid-connect/certs
INFO - Configured valid JWT issuers: [http://localhost:8090/realms/werkflow, http://keycloak:8080/realms/werkflow]
DEBUG - JWT issuer validation passed for issuer: http://localhost:8090/realms/werkflow
```

**Failed Validation**:
```
ERROR - JWT validation failed: Token issuer 'http://invalid:8080/realms/werkflow' not in list of valid issuers
```

## Testing

### Manual Testing

1. **Obtain JWT Token**:
```bash
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=werkflow-engine" \
  -d "username=testuser" \
  -d "password=password"
```

2. **Test API with Token**:
```bash
curl -X GET http://localhost:8081/api/process-definitions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. **Check Logs**:
```bash
docker-compose logs -f engine-service | grep JWT
```

### Expected Behavior

- Tokens from `localhost:8090` should be accepted
- Tokens from `keycloak:8080` should be accepted
- Invalid signatures should be rejected
- Expired tokens should be rejected
- Tokens from unknown issuers should be rejected

## Integration with Spring Security

### Automatic Bean Detection

Spring Security automatically detects the custom `JwtDecoder` bean and uses it instead of the default decoder. No changes to `SecurityConfig.java` are required.

### Authentication Flow

```
1. Request arrives with Authorization: Bearer <token>
   ↓
2. Spring Security extracts JWT
   ↓
3. Custom JwtDecoder validates:
   - Fetches public keys from JWK set
   - Verifies signature
   - Checks expiry
   - Validates issuer (multi-issuer support)
   ↓
4. If valid, JwtAuthenticationConverter extracts authorities
   ↓
5. Request proceeds with authenticated principal
```

## Troubleshooting

### Issue: "Invalid issuer" Error

**Symptom**: 401 Unauthorized with "Invalid issuer" message

**Solution**: Check that:
1. Token was issued by Keycloak at one of the valid URLs
2. `KEYCLOAK_ISSUER` environment variable is set correctly
3. Valid issuers list in `JwtDecoderConfig` includes the token's issuer

### Issue: "Invalid signature" Error

**Symptom**: 401 Unauthorized with signature validation failure

**Solution**: Verify:
1. JWK set URI is accessible from the backend
2. Keycloak is running and healthy
3. Token hasn't been modified or corrupted

### Issue: Token Expired

**Symptom**: 401 Unauthorized with "Jwt expired" message

**Solution**:
1. Check system time synchronization
2. Verify Keycloak token lifetime settings
3. Obtain a fresh token

## Performance Considerations

### JWK Set Caching

The `NimbusJwtDecoder` automatically caches JWK sets to minimize requests to Keycloak:

- **Default Cache Duration**: 5 minutes
- **Cache Refresh**: On signature validation failure
- **Performance Impact**: Minimal - validation is in-memory after initial fetch

### Validation Performance

- **Signature Verification**: ~1-2ms per request
- **Issuer Validation**: <0.1ms per request
- **Total Overhead**: Negligible for typical workloads

## Security Best Practices

### 1. HTTPS in Production

For production deployments:

```yaml
# Update valid issuers to HTTPS
validIssuers:
  - https://keycloak.example.com/realms/werkflow
  - https://internal-keycloak.example.com/realms/werkflow
```

### 2. Restrict Valid Issuers

Only include issuers you control and trust:

```java
// DO NOT add unknown or third-party issuers
List<String> validIssuers = Arrays.asList(
    "https://your-keycloak.com/realms/werkflow"
);
```

### 3. Monitor Failed Validations

Set up alerts for:
- High rate of signature validation failures
- Unknown issuer attempts
- Expired token usage patterns

### 4. Token Rotation

Implement token refresh strategies:
- Short-lived access tokens (5-15 minutes)
- Long-lived refresh tokens (hours/days)
- Automatic token refresh before expiry

## Migration Guide

### Upgrading from Default Configuration

If upgrading from the default Spring Security JWT configuration:

1. **No Code Changes Required**: The custom decoder is automatically detected
2. **Environment Variable**: Set `KEYCLOAK_ISSUER` for Docker deployments
3. **Testing**: Verify tokens from both URLs are accepted
4. **Monitoring**: Check logs for validation success/failure

### Rollback Procedure

To rollback to default behavior:

1. Remove `JwtDecoderConfig.java`
2. Ensure `KEYCLOAK_ISSUER` matches the token issuer exactly
3. Restart the service

## Additional Resources

### Related Documentation

- [Keycloak Configuration](../Keycloak/Configuration.md)
- [OAuth2 Setup Guide](../Keycloak/OAuth2-Setup.md)
- [Security Configuration](../Security/Security-Configuration.md)

### Spring Security References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [JWT Validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-validation)
- [Custom JWT Decoder](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-decoder-bean)

### Keycloak References

- [Keycloak JWT Token Format](https://www.keycloak.org/docs/latest/securing_apps/#_token-exchange)
- [OpenID Connect Discovery](https://www.keycloak.org/docs/latest/securing_apps/#_client_registration)
