# JWT Multi-Issuer Deployment Checklist

## Pre-Deployment Verification

### Code Review
- [ ] Review `JwtDecoderConfig.java` implementation
- [ ] Verify valid issuers list is correct for target environment
- [ ] Confirm logging configuration is appropriate
- [ ] Check unit tests pass successfully

### Configuration Review
- [ ] Verify `application.yml` logging configuration
- [ ] Confirm `KEYCLOAK_ISSUER` environment variable is set correctly
- [ ] Validate JWK set URI is accessible from backend
- [ ] Review security configuration in `SecurityConfig.java`

### Documentation Review
- [ ] Read [JWT Multi-Issuer Configuration](./JWT-Multi-Issuer-Configuration.md)
- [ ] Review [Quick Reference](./Quick-Reference-JWT.md)
- [ ] Understand [Implementation Summary](./Implementation-Summary.md)
- [ ] Familiarize with troubleshooting procedures

## Local Development Deployment

### Environment Setup
```bash
# Set Keycloak issuer for local development
export KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
```

### Pre-Deployment Tests
- [ ] Start Keycloak: `docker-compose up -d keycloak`
- [ ] Verify Keycloak is accessible at `http://localhost:8090`
- [ ] Obtain test JWT token using admin credentials
- [ ] Verify token contains correct issuer claim

### Service Startup
```bash
cd services/engine
mvn clean install -DskipTests
mvn spring-boot:run
```

### Post-Deployment Verification
- [ ] Service starts without errors
- [ ] Check logs for JWT decoder initialization messages
- [ ] Verify "Configured valid JWT issuers" log entry
- [ ] Test health endpoint: `curl http://localhost:8081/actuator/health`

### Functional Testing
- [ ] Obtain JWT token from Keycloak
- [ ] Test authenticated endpoint with token
- [ ] Verify token validation succeeds
- [ ] Check logs for successful validation messages
- [ ] Test with expired token (should fail)
- [ ] Test with invalid signature (should fail)

### Expected Log Messages
```
INFO - Configuring custom JWT decoder with JWK Set URI: http://localhost:8090/realms/werkflow/protocol/openid-connect/certs
INFO - Configured valid JWT issuers: [http://localhost:8090/realms/werkflow, http://keycloak:8080/realms/werkflow]
INFO - JWT decoder successfully configured with multi-issuer support
DEBUG - JWT issuer validation passed for issuer: http://localhost:8090/realms/werkflow
```

## Docker Deployment

### Environment Setup
```bash
# Set Keycloak issuer for Docker network
export KEYCLOAK_ISSUER=http://keycloak:8080/realms/werkflow
```

### Pre-Deployment Tests
- [ ] Build Docker image: `docker-compose build engine-service`
- [ ] Verify Dockerfile includes latest code
- [ ] Check docker-compose.yml configuration
- [ ] Verify network configuration allows backend to reach Keycloak

### Service Startup
```bash
cd infrastructure/docker
docker-compose up -d keycloak
# Wait for Keycloak to be fully ready
docker-compose up -d engine-service
```

### Post-Deployment Verification
- [ ] Service container is running: `docker-compose ps`
- [ ] Check service logs: `docker-compose logs -f engine-service`
- [ ] Verify JWT decoder initialization in logs
- [ ] Confirm no errors in startup logs

### Network Connectivity Tests
```bash
# Test from host to Keycloak (external URL)
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration

# Test from engine service to Keycloak (internal URL)
docker-compose exec engine-service curl http://keycloak:8080/realms/werkflow/.well-known/openid-configuration
```

### Functional Testing
- [ ] Obtain JWT token from `localhost:8090` (browser perspective)
- [ ] Test engine service API with token
- [ ] Verify multi-issuer validation works
- [ ] Check that tokens from both issuers are accepted
- [ ] Test role-based access control

### Expected Behavior
- ✅ Tokens issued via `localhost:8090` are accepted
- ✅ JWK sets fetched from `keycloak:8080`
- ✅ Signature validation succeeds
- ✅ No issuer mismatch errors

## Production Deployment

### Pre-Production Checklist

#### Security Configuration
- [ ] Update valid issuers to use HTTPS URLs
- [ ] Remove development issuer URLs (localhost)
- [ ] Configure SSL/TLS certificates
- [ ] Set up secrets management (no hardcoded credentials)
- [ ] Enable security headers
- [ ] Configure CORS for production origins

#### Code Updates Required
Update `JwtDecoderConfig.java`:
```java
List<String> validIssuers = Arrays.asList(
    "https://auth.example.com/realms/werkflow"
);
```

#### Environment Variables
```bash
# Production environment
export KEYCLOAK_ISSUER=https://auth.example.com/realms/werkflow
export LOG_LEVEL=INFO
export OAUTH2_LOG_LEVEL=INFO
```

#### Monitoring Setup
- [ ] Configure application performance monitoring (APM)
- [ ] Set up log aggregation (ELK, Splunk, etc.)
- [ ] Create dashboards for JWT validation metrics
- [ ] Configure alerts for validation failures
- [ ] Set up security event notifications

#### Backup and Rollback
- [ ] Document current configuration
- [ ] Create rollback plan
- [ ] Test rollback procedure in staging
- [ ] Prepare emergency contact list
- [ ] Document known issues and workarounds

### Production Deployment Steps

1. **Staging Environment Testing**
   - [ ] Deploy to staging environment
   - [ ] Run full integration test suite
   - [ ] Perform load testing
   - [ ] Test failover scenarios
   - [ ] Verify monitoring and alerts work

2. **Production Deployment**
   - [ ] Schedule maintenance window
   - [ ] Notify stakeholders
   - [ ] Deploy database changes (if any)
   - [ ] Deploy application updates
   - [ ] Verify service health

3. **Post-Deployment Verification**
   - [ ] Check application logs for errors
   - [ ] Verify JWT validation works correctly
   - [ ] Test authentication flow end-to-end
   - [ ] Monitor error rates
   - [ ] Check performance metrics

4. **Smoke Tests**
   - [ ] User login via frontend
   - [ ] API access with JWT token
   - [ ] Role-based access control
   - [ ] Token refresh flow
   - [ ] Logout functionality

### Production Monitoring

#### Key Metrics to Monitor
- JWT validation success rate
- JWT validation failure rate
- Invalid issuer attempts
- Signature validation failures
- Token expiry rate
- API response times
- Error rate by endpoint

#### Alert Thresholds
- JWT validation failure rate > 5%
- Invalid issuer attempts > 10/minute
- Signature validation failures > 1%
- API error rate > 2%

#### Log Monitoring
Monitor for these patterns:
```bash
# Failed validations
grep -i "JWT validation failed" /var/log/engine-service.log

# Invalid issuer attempts
grep -i "Invalid issuer" /var/log/engine-service.log

# Authentication errors
grep -i "401\|403" /var/log/engine-service.log
```

## Rollback Procedures

### Emergency Rollback (< 5 minutes)

If critical issues occur:

1. **Immediate Actions**
```bash
# Revert to previous Docker image
docker-compose down
docker-compose pull engine-service:previous-tag
docker-compose up -d
```

2. **Verify Rollback**
   - [ ] Check service health
   - [ ] Verify authentication works
   - [ ] Monitor error logs

### Planned Rollback (with testing)

1. **Disable Custom JWT Decoder**
   - Remove or comment out `JwtDecoderConfig.java`
   - Update `KEYCLOAK_ISSUER` to match exact token issuer
   - Rebuild and redeploy

2. **Configuration-Only Rollback**
   - Update environment variables
   - Restart service
   - No code changes needed

### Post-Rollback Actions
- [ ] Document reason for rollback
- [ ] Create incident report
- [ ] Plan remediation steps
- [ ] Schedule retry deployment

## Troubleshooting

### Common Issues

#### Issue: "Invalid issuer" errors
**Symptoms**: 401 errors, "Invalid issuer" in logs

**Debug Steps**:
```bash
# Check token issuer claim
echo $TOKEN | cut -d. -f2 | base64 -d | jq .iss

# Check configured issuers in logs
docker-compose logs engine-service | grep "Configured valid JWT issuers"

# Verify Keycloak configuration
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer
```

**Resolution**:
- Verify token issuer matches one of the valid issuers
- Check `KEYCLOAK_ISSUER` environment variable
- Confirm Keycloak realm configuration

#### Issue: "Invalid signature" errors
**Symptoms**: 401 errors, signature validation failures

**Debug Steps**:
```bash
# Test JWK set URI accessibility
curl http://keycloak:8080/realms/werkflow/protocol/openid-connect/certs

# Check network connectivity
docker-compose exec engine-service ping keycloak

# Verify Keycloak is healthy
curl http://localhost:8090/health
```

**Resolution**:
- Ensure Keycloak is running and accessible
- Verify JWK set URI is correct
- Check network configuration

#### Issue: Service won't start
**Symptoms**: Container exits, startup errors

**Debug Steps**:
```bash
# Check service logs
docker-compose logs engine-service

# Verify configuration
docker-compose exec engine-service env | grep KEYCLOAK

# Test configuration syntax
docker-compose config
```

**Resolution**:
- Review application logs for errors
- Verify environment variables
- Check Java version compatibility

## Sign-Off Checklist

### Development Team
- [ ] Code reviewed and approved
- [ ] Unit tests pass
- [ ] Documentation complete
- [ ] Integration tests pass (if available)

### QA Team
- [ ] Functional testing complete
- [ ] Security testing complete
- [ ] Performance testing complete
- [ ] Test cases documented

### Operations Team
- [ ] Deployment procedure documented
- [ ] Monitoring configured
- [ ] Alerts set up
- [ ] Rollback procedure tested
- [ ] Backup verified

### Security Team
- [ ] Security review complete
- [ ] Vulnerability scan passed
- [ ] Compliance requirements met
- [ ] Security logging verified

### Management
- [ ] Business approval obtained
- [ ] Stakeholders notified
- [ ] Risk assessment complete
- [ ] Go/No-Go decision made

## Additional Resources

### Documentation
- [JWT Multi-Issuer Configuration](./JWT-Multi-Issuer-Configuration.md)
- [Implementation Summary](./Implementation-Summary.md)
- [Quick Reference](./Quick-Reference-JWT.md)
- [Security Overview](./README.md)

### External Resources
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/)
- [Keycloak Docs](https://www.keycloak.org/documentation)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

### Support Contacts
- Development Team: [team-email]
- Operations Team: [ops-email]
- Security Team: [security-email]
- Emergency Contact: [emergency-contact]
