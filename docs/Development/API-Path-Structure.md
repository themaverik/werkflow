# API Path Structure

## Overview

The Werkflow platform uses a structured API path convention to organize endpoints and avoid conflicts between Flowable's built-in REST API and custom application endpoints.

## Path Conventions

### Flowable Built-in REST API
**Base Path:** `/api/*`

Provided by `flowable-spring-boot-starter-rest` dependency. These are Flowable's standard BPMN engine endpoints.

**Examples:**
```
GET    /api/repository/process-definitions
GET    /api/runtime/process-instances
GET    /api/runtime/tasks
POST   /api/runtime/process-instances
DELETE /api/repository/deployments/{deploymentId}
```

**Configuration:**
```yaml
flowable:
  rest-api-enabled: true
```

### Werkflow Custom APIs
**Base Path:** `/werkflow/api/*`

Custom application-specific endpoints that extend Flowable functionality or provide domain-specific features.

**Examples:**
```
GET    /werkflow/api/forms/{formKey}
GET    /werkflow/api/analytics/dashboard
POST   /werkflow/api/approvals/{approvalId}/escalate
GET    /werkflow/api/reports/workflow-metrics
```

## Path Conflict Resolution

### Problem
When both Flowable REST starter and custom controllers use `/api/*` paths, routing conflicts occur:

1. Flowable REST API registers handlers for `/api/**`
2. Custom `@RestController` with `/api/forms` is shadowed
3. Requests to custom endpoints hit Flowable's resource handler
4. Result: HTTP 404 "No static resource api/forms" error

### Solution
Separate custom APIs to `/werkflow/api/*` namespace:

```java
@RestController
@RequestMapping("/werkflow/api/forms")  // Not /api/forms
public class FormController {
    // Custom endpoints
}
```

## Security Configuration

Both path patterns require proper security configuration:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Flowable built-in API endpoints
            .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()

            // Werkflow custom API endpoints
            .requestMatchers(new AntPathRequestMatcher("/werkflow/api/**")).authenticated()

            .anyRequest().authenticated()
        );
    return http.build();
}
```

## Frontend Integration

### Environment Configuration

**Backend Service URL (without /api suffix):**
```env
# .env.local
NEXT_PUBLIC_ENGINE_API_URL=http://localhost:8081
```

### API Client Usage

```typescript
// For Flowable built-in APIs
const engineBaseUrl = process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8081';
const response = await fetch(`${engineBaseUrl}/api/tasks/${taskId}`);

// For Werkflow custom APIs
const response = await fetch(`${engineBaseUrl}/werkflow/api/forms/${formKey}`);
```

### Axios Client Configuration

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8081',
  headers: { 'Content-Type': 'application/json' },
});

// Usage
apiClient.get('/api/tasks/123');                    // Flowable API
apiClient.get('/werkflow/api/forms/employee-form'); // Custom API
```

## Directory Structure

### Backend Controllers

```
services/engine/src/main/java/com/werkflow/engine/
├── controller/
│   ├── FormController.java          → /werkflow/api/forms
│   ├── AnalyticsController.java     → /werkflow/api/analytics
│   └── ReportController.java        → /werkflow/api/reports
```

### Controller Template

```java
package com.werkflow.engine.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/werkflow/api/domain")
@RequiredArgsConstructor
@Tag(name = "Domain", description = "Domain management")
@SecurityRequirement(name = "bearer-jwt")
public class DomainController {

    @GetMapping("/{id}")
    public ResponseEntity<DomainResponse> getById(@PathVariable String id) {
        // Implementation
    }
}
```

## Migration Guide

### Migrating Existing Controllers

**Before:**
```java
@RestController
@RequestMapping("/api/forms")
public class FormController {
    // ...
}
```

**After:**
```java
@RestController
@RequestMapping("/werkflow/api/forms")
public class FormController {
    // ...
}
```

### Updating Frontend Code

**Before:**
```typescript
fetch('/api/forms/employee-form')
```

**After:**
```typescript
const engineBaseUrl = process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8081';
fetch(`${engineBaseUrl}/werkflow/api/forms/employee-form`)
```

### Updating Security Configuration

**Before:**
```java
.requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
```

**After:**
```java
// Flowable APIs
.requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
// Custom APIs
.requestMatchers(new AntPathRequestMatcher("/werkflow/api/**")).authenticated()
```

## API Documentation (Swagger/OpenAPI)

Both API namespaces appear in Swagger UI:

```
http://localhost:8081/swagger-ui.html
```

**Tags:**
- Flowable Process APIs → `/api/repository/**`, `/api/runtime/**`
- Werkflow Custom APIs → `/werkflow/api/**`

Configure in OpenAPI:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Werkflow Engine API")
                .version("1.0.0"))
            .addTagsItem(new Tag()
                .name("Flowable Built-in APIs")
                .description("Standard Flowable BPMN engine endpoints"))
            .addTagsItem(new Tag()
                .name("Werkflow Custom APIs")
                .description("Application-specific workflow extensions"));
    }
}
```

## Testing

### Test Endpoint Routing

```bash
#!/bin/bash
# Test script for API path resolution

ENGINE_URL="http://localhost:8081"

# Test Werkflow custom API
curl -i "${ENGINE_URL}/werkflow/api/forms/test"
# Expected: 401 Unauthorized (endpoint exists, needs auth)

# Test Flowable built-in API
curl -i "${ENGINE_URL}/api/repository/process-definitions"
# Expected: 401 Unauthorized (endpoint exists, needs auth)

# Test non-existent endpoint
curl -i "${ENGINE_URL}/api/nonexistent"
# Expected: 404 Not Found
```

### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void testGetFormByKey() throws Exception {
        mockMvc.perform(get("/werkflow/api/forms/employee-onboarding"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formKey").value("employee-onboarding"));
    }
}
```

## Best Practices

1. **Always use `/werkflow/api/*` for custom endpoints**
   - Prevents conflicts with Flowable REST API
   - Clear namespace separation
   - Easier to identify custom vs built-in functionality

2. **Configure environment variables without /api suffix**
   ```env
   NEXT_PUBLIC_ENGINE_API_URL=http://localhost:8081
   ```
   Not: `http://localhost:8081/api`

3. **Document API paths in controller annotations**
   ```java
   /**
    * Custom form management endpoints
    * Base path: /werkflow/api/forms
    */
   @RestController
   @RequestMapping("/werkflow/api/forms")
   public class FormController { }
   ```

4. **Use consistent base URLs in frontend**
   ```typescript
   const ENGINE_BASE_URL = process.env.NEXT_PUBLIC_ENGINE_API_URL;
   // Not hardcoded: 'http://localhost:8081/api'
   ```

5. **Add security matchers for both namespaces**
   - `/api/**` for Flowable
   - `/werkflow/api/**` for custom

## Related Documentation

- [Flowable REST API Conflict](../Troubleshooting/Flowable-REST-API-Conflict.md) - Detailed troubleshooting guide
- [Security Configuration](./Security-Configuration.md) - Complete security setup
- [Frontend Integration](./Frontend-Integration.md) - Client-side API usage

## References

- [Flowable REST API Documentation](https://www.flowable.com/open-source/docs/bpmn/ch15-REST)
- [Spring Boot REST API Design](https://spring.io/guides/tutorials/rest/)
- [Next.js Environment Variables](https://nextjs.org/docs/basic-features/environment-variables)
