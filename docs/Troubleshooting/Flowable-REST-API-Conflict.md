# Flowable REST API Path Conflict

## Problem Description

Custom REST controllers using `/api/**` paths return HTTP 404 with error: "No static resource api/{path}"

### Symptoms
- Controller is properly annotated with @RestController and @RequestMapping
- Component scanning includes the controller package
- SecurityConfig permits the endpoint
- Request still results in 404 "No static resource" error
- Error message indicates request is treated as static resource, not controller endpoint

## Root Cause

The issue is caused by **Flowable's built-in REST API** which creates a path collision with custom controllers.

### Technical Details

1. **Dependency**: `flowable-spring-boot-starter-rest` is included in pom.xml
   ```xml
   <dependency>
       <groupId>org.flowable</groupId>
       <artifactId>flowable-spring-boot-starter-rest</artifactId>
       <version>${flowable.version}</version>
   </dependency>
   ```

2. **Configuration**: Flowable REST API is enabled in application.yml
   ```yaml
   flowable:
     rest-api-enabled: true
   ```

3. **Path Collision**: Flowable REST API registers handlers for `/api/**` paths by default
   - Flowable REST API uses `/api/` as its base path
   - Custom controllers with `/api/**` mappings are shadowed
   - Spring Boot's error handler treats unmapped paths as static resource requests

4. **Request Flow**:
   ```
   Request: GET /api/forms/employee-onboarding
   ↓
   Flowable REST API servlet/dispatcher checks for handler
   ↓
   No Flowable handler found for /api/forms/*
   ↓
   Falls back to ResourceHttpRequestHandler
   ↓
   Result: "No static resource api/forms"
   ```

## Solutions

### Solution 1: Change Custom API Path Prefix (Recommended)

Move custom controllers to a different base path to avoid collision with Flowable REST API.

#### Implementation

**1. Update Controller Mapping:**
```java
@RestController
@RequestMapping("/werkflow/api/forms")  // Changed from "/api/forms"
public class FormController {
    // ... controller methods
}
```

**2. Update SecurityConfig:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // ... other matchers
            .requestMatchers(new AntPathRequestMatcher("/werkflow/api/**")).authenticated()
            .anyRequest().authenticated()
        );
    return http.build();
}
```

**Pros:**
- Clean separation between Flowable API and custom API
- No impact on Flowable's built-in functionality
- Clear namespace for application-specific endpoints
- No dependency changes required

**Cons:**
- Requires updating frontend/client code to use new paths
- Different base path than Flowable endpoints

**Usage:**
```bash
# New endpoint path
GET /werkflow/api/forms/employee-onboarding
```

### Solution 2: Disable Flowable REST API

Disable Flowable's built-in REST API if you're building custom controllers and don't need Flowable's standard REST endpoints.

#### Implementation

**Update application.yml:**
```yaml
flowable:
  rest-api-enabled: false
```

**Pros:**
- Custom controllers can use `/api/**` paths
- Reduces potential security surface
- Cleaner endpoint namespace

**Cons:**
- Lose access to Flowable's built-in REST API endpoints
- May need to rebuild functionality if you were using Flowable REST features
- Still have the dependency in classpath

### Solution 3: Remove Flowable REST Dependency

Remove the REST starter dependency entirely if not needed.

#### Implementation

**Remove from pom.xml:**
```xml
<!-- Remove or comment out -->
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-rest</artifactId>
    <version>${flowable.version}</version>
</dependency>
```

**Pros:**
- Cleanest solution if Flowable REST not needed
- Reduces application size
- No configuration conflicts

**Cons:**
- Requires Maven rebuild
- Cannot use any Flowable REST features

### Solution 4: Configure Flowable REST API Base Path

Change Flowable REST API's base path to avoid collision.

#### Implementation

**Create FlowableRestConfiguration:**
```java
@Configuration
public class FlowableRestConfiguration {

    @Bean
    public FlowableRestApiProperties flowableRestApiProperties() {
        FlowableRestApiProperties properties = new FlowableRestApiProperties();
        properties.setServletPath("/flowable-api");  // Change from default /api
        return properties;
    }
}
```

**Pros:**
- Both APIs can coexist
- Custom controllers can use `/api/**`
- Flowable REST still available

**Cons:**
- Requires additional configuration
- Flowable REST API clients need to update paths
- More complex setup

## Verification

After implementing the fix, verify the endpoint works:

### Using curl:
```bash
# Get access token
TOKEN=$(curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" \
  -d "client_id=werkflow-engine" \
  -d "client_secret=your-secret" | jq -r '.access_token')

# Test the endpoint (adjust path based on solution)
curl -X GET http://localhost:8081/werkflow/api/forms/employee-onboarding \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response:
```json
{
  "formKey": "employee-onboarding",
  "schema": { ... },
  "metadata": { ... }
}
```

## Prevention

To prevent this issue in the future:

1. **Document API path conventions** in project standards
2. **Reserve `/api/**` for Flowable** if using their REST starter
3. **Use namespaced paths** for custom APIs (e.g., `/werkflow/api/**`, `/custom/api/**`)
4. **Review dependencies** before adding REST/API starters
5. **Test endpoints** immediately after controller creation

## Related Files

- Controller: `/services/engine/src/main/java/com/werkflow/engine/controller/FormController.java`
- Security Config: `/services/engine/src/main/java/com/werkflow/engine/config/SecurityConfig.java`
- Application Config: `/services/engine/src/main/resources/application.yml`
- Dependencies: `/services/engine/pom.xml`

## References

- [Flowable REST API Documentation](https://www.flowable.com/open-source/docs/bpmn/ch15-REST)
- [Spring Boot REST API Best Practices](https://spring.io/guides/tutorials/rest/)
- [Spring Security Request Matchers](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
