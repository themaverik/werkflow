# Security Documentation

This directory contains security-related documentation for the Werkflow platform.

## Overview

The Werkflow platform implements enterprise-grade security using:

- OAuth2/OpenID Connect authentication via Keycloak
- JWT token-based authorization
- Role-based access control (RBAC)
- Multi-issuer JWT validation for Docker deployments

## Documentation Index

### Authentication & Authorization

- **[JWT Multi-Issuer Configuration](./JWT-Multi-Issuer-Configuration.md)**
  - Comprehensive guide to custom JWT decoder configuration
  - Solves issuer mismatch in Docker deployments
  - Supports both external and internal Keycloak URLs
  - Production-ready security validations

### Related Documentation

For complete security setup, also refer to:

- [Keycloak Configuration](../Keycloak/Configuration.md) - Keycloak server setup
- [OAuth2 Setup Guide](../Keycloak/OAuth2-Setup.md) - OAuth2 client configuration
- [Security Configuration](./Security-Configuration.md) - Spring Security configuration

## Quick Start

### Local Development Setup

1. **Start Keycloak**:
```bash
cd infrastructure/docker
docker-compose up -d keycloak
```

2. **Configure Environment**:
```bash
export KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
```

3. **Start Engine Service**:
```bash
cd services/engine
mvn spring-boot:run
```

### Docker Deployment

1. **Configure Environment**:
```bash
export KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
```

2. **Start All Services**:
```bash
cd infrastructure/docker
docker-compose up -d
```

## Security Features

### JWT Token Validation

The platform validates JWT tokens against multiple criteria:

- **Signature Verification**: Uses JWK public keys from Keycloak
- **Expiry Validation**: Ensures tokens are not expired
- **Issuer Validation**: Accepts tokens from configured Keycloak instances
- **Audience Validation**: Verifies tokens are intended for this service

### Multi-Issuer Support

Unique to the Werkflow platform, the JWT decoder accepts tokens from:

- External URLs (browser/frontend access)
- Internal Docker network URLs (service-to-service)

This solves common Docker deployment issues without compromising security.

### Role-Based Access Control

The platform enforces authorization using Keycloak roles:

- **SUPER_ADMIN**: Full system access
- **WORKFLOW_DESIGNER**: Create and manage workflow definitions
- **HR_MANAGER**: HR-specific operations
- **EMPLOYEE**: Basic employee operations

## Security Best Practices

### 1. Token Lifetime

Configure appropriate token lifetimes in Keycloak:

- **Access Token**: 5-15 minutes
- **Refresh Token**: 30 minutes to 8 hours
- **SSO Session**: 10 hours

### 2. HTTPS in Production

Always use HTTPS in production:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/realms/werkflow
```

### 3. Secret Management

Never commit secrets to version control:

- Use environment variables for sensitive configuration
- Rotate secrets regularly
- Use secret management tools (Vault, AWS Secrets Manager)

### 4. Logging and Monitoring

Enable security event logging:

```yaml
logging:
  level:
    org.springframework.security: INFO
    org.springframework.security.oauth2: DEBUG
```

Monitor for:
- Failed authentication attempts
- Invalid token usage
- Unauthorized access attempts

## Troubleshooting

### Common Issues

1. **Issuer Mismatch Error**
   - See: [JWT Multi-Issuer Configuration](./JWT-Multi-Issuer-Configuration.md#troubleshooting)

2. **Invalid Signature Error**
   - Verify Keycloak is accessible
   - Check JWK set URI configuration

3. **401 Unauthorized**
   - Verify token is included in Authorization header
   - Check token expiry
   - Validate user has required roles

### Debug Logging

Enable debug logging for detailed security information:

```yaml
logging:
  level:
    com.werkflow.engine.config.JwtDecoderConfig: DEBUG
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: TRACE
```

## Testing Security

### Manual Testing

1. **Obtain Token**:
```bash
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=werkflow-engine" \
  -d "username=admin" \
  -d "password=admin"
```

2. **Test API Access**:
```bash
TOKEN="your-jwt-token"
curl -X GET http://localhost:8081/api/process-definitions \
  -H "Authorization: Bearer $TOKEN"
```

### Automated Testing

Run security tests:

```bash
cd services/engine
mvn test -Dtest=SecurityConfigTest,JwtDecoderConfigTest
```

## Additional Resources

### External Documentation

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OAuth2 RFC](https://datatracker.ietf.org/doc/html/rfc6749)

### Internal Resources

- [Architecture Overview](../Architecture/Overview.md)
- [Deployment Guide](../Deployment/Docker-Deployment.md)
- [API Documentation](../API/README.md)
