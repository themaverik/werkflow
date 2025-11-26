# Task Endpoints Implementation Summary

**Quick Reference Guide**
**Date**: 2025-11-25
**Status**: Ready for Implementation
**Priority**: HIGH - Phase 5A Week 2 (Days 2.5-4)

---

## Overview

This document provides a quick-reference summary for implementing two missing REST API endpoints in the Werkflow Engine Service:

1. `GET /workflows/tasks/my-tasks` - User's assigned tasks
2. `GET /workflows/tasks/group-tasks` - Team/group candidate tasks

Full design details are available in [Task-Endpoints-Design-Specification.md](./Task-Endpoints-Design-Specification.md).

---

## Current Problem

**Error**: `HTTP 404 - No static resource workflows/tasks/my-tasks`

**Root Cause**:
- Endpoints referenced in frontend but not implemented
- TaskController only has `/api/tasks/*` endpoints
- No `/workflows/tasks/*` endpoints exist

**Impact**:
- Phase 6 Task UI blocked
- Users cannot view their workflow tasks
- Task assignment and delegation features unavailable

---

## Solution Architecture

### Component Stack

```
Frontend (React)
    ↓ HTTP GET + JWT Bearer Token
SecurityFilterChain (Spring Security OAuth2)
    ↓ Validates JWT, extracts claims
WorkflowTaskController (/workflows/tasks)
    ↓ Extracts user context from JWT
JwtClaimsExtractor (NEW)
    ↓ userId, groups, department, roles
WorkflowTaskService (NEW)
    ↓ Builds Flowable queries with authorization
Flowable TaskService API
    ↓ SQL queries with filters
PostgreSQL (ACT_RU_TASK, ACT_RU_IDENTITYLINK)
```

### Key Design Principles

1. **Separation of Concerns**: New `WorkflowTaskService` (doesn't modify existing `TaskService`)
2. **Authorization by Query**: Use Flowable's built-in filtering (taskAssignee, taskCandidateGroup)
3. **JWT Integration**: Extract user context from Keycloak tokens
4. **Performance**: Pagination, caching, and efficient queries
5. **HATEOAS**: Pagination links for better API navigation

---

## Files to Create/Modify

### New Files (11 files)

```
services/engine/src/main/java/com/werkflow/engine/
├── controller/
│   └── WorkflowTaskController.java              [NEW - 150 lines]
├── service/
│   └── WorkflowTaskService.java                 [NEW - 300 lines]
├── dto/
│   ├── TaskListResponse.java                    [NEW - 30 lines]
│   ├── TaskQueryParams.java                     [NEW - 40 lines]
│   ├── JwtUserContext.java                      [NEW - 30 lines]
│   └── ErrorResponse.java                       [NEW - 20 lines]
├── util/
│   └── JwtClaimsExtractor.java                  [NEW - 150 lines]
├── exception/
│   ├── TaskNotFoundException.java               [NEW - 15 lines]
│   ├── UnauthorizedTaskAccessException.java     [NEW - 15 lines]
│   └── GlobalExceptionHandler.java              [NEW - 150 lines]
└── test/
    ├── WorkflowTaskServiceTest.java             [NEW - 200 lines]
    └── WorkflowTaskControllerIntegrationTest    [NEW - 250 lines]
```

### Modified Files (3 files)

```
services/engine/src/main/java/com/werkflow/engine/
├── dto/
│   └── TaskResponse.java                        [ENHANCE - Add 3 fields]
├── config/
│   ├── SecurityConfig.java                      [UPDATE - Add route]
│   └── CacheConfig.java                         [UPDATE - Add cache]
```

**Total New Code**: ~1,350 lines
**Total Modified Code**: ~20 lines

---

## API Specification Quick Reference

### Endpoint 1: Get My Tasks

```http
GET /workflows/tasks/my-tasks?page=0&size=20&sort=createTime,desc
Authorization: Bearer {jwt-token}
```

**Query Parameters**:
- `page` (int, default 0): Page number
- `size` (int, default 20, max 100): Page size
- `sort` (string, default "createTime,desc"): Sort field and direction
- `search` (string): Search in task name/description
- `priority` (int): Filter by priority
- `processDefinitionKey` (string): Filter by workflow type
- `dueBefore` (ISO8601): Filter tasks due before date
- `dueAfter` (ISO8601): Filter tasks due after date

**Response**: 200 OK
```json
{
  "content": [/* TaskResponse[] */],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 45,
    "totalPages": 3
  },
  "links": {
    "self": "...",
    "first": "...",
    "next": "...",
    "last": "..."
  }
}
```

### Endpoint 2: Get Group Tasks

```http
GET /workflows/tasks/group-tasks?page=0&size=20&includeAssigned=false
Authorization: Bearer {jwt-token}
```

**Additional Query Parameters**:
- `groupId` (string): Filter by specific group (must be user's group)
- `includeAssigned` (boolean, default false): Include already assigned tasks

**Response**: Same structure as My Tasks

---

## Implementation Steps

### Step 1: Create DTOs (2 hours)

**Priority Order**:
1. `ErrorResponse.java` (base infrastructure)
2. `JwtUserContext.java` (JWT integration)
3. `TaskQueryParams.java` (request validation)
4. `TaskListResponse.java` (response wrapper)
5. Enhance `TaskResponse.java` (add 3 fields)

**Key Code Snippet** - JwtUserContext.java:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserContext {
    private String userId;           // preferred_username
    private String email;
    private String fullName;
    private String department;       // department claim
    private List<String> groups;     // groups claim
    private List<String> roles;      // realm_access.roles
    private String managerId;        // for delegation
    private Integer doaLevel;        // for approvals

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isInGroup(String group) {
        return groups != null && groups.contains(group);
    }
}
```

### Step 2: Create JwtClaimsExtractor (2 hours)

**File**: `util/JwtClaimsExtractor.java`

**Purpose**: Extract user information from Keycloak JWT tokens

**Key Methods**:
```java
@Component
public class JwtClaimsExtractor {

    public JwtUserContext extractUserContext(Jwt jwt) {
        return JwtUserContext.builder()
            .userId(getUserId(jwt))
            .groups(getUserGroups(jwt))
            .department(getDepartment(jwt))
            .roles(getUserRoles(jwt))
            // ... other fields
            .build();
    }

    public String getUserId(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    public List<String> getUserGroups(Jwt jwt) {
        Object groupsClaim = jwt.getClaim("groups");
        if (groupsClaim instanceof List) {
            return ((List<?>) groupsClaim).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<String> getUserRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            // Extract roles list
        }
        return Collections.emptyList();
    }
}
```

### Step 3: Create WorkflowTaskService (6 hours)

**File**: `service/WorkflowTaskService.java`

**Core Logic**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final RepositoryService repositoryService;

    public TaskListResponse getMyTasks(JwtUserContext userContext, TaskQueryParams params) {
        // 1. Build query with authorization filter
        TaskQuery query = flowableTaskService.createTaskQuery()
            .taskAssignee(userContext.getUserId())  // Authorization enforced here
            .active();

        // 2. Apply filters (search, priority, process definition, etc.)
        query = applyFilters(query, params);

        // 3. Apply sorting
        query = applySorting(query, params);

        // 4. Execute paginated query
        long totalCount = query.count();
        int offset = params.getPage() * params.getSize();
        List<Task> tasks = query.listPage(offset, params.getSize());

        // 5. Transform to DTOs
        List<TaskResponse> responses = tasks.stream()
            .map(this::mapToEnhancedResponse)
            .collect(Collectors.toList());

        // 6. Build response with pagination links
        return buildTaskListResponse(responses, params, totalCount, "/workflows/tasks/my-tasks");
    }

    public TaskListResponse getGroupTasks(JwtUserContext userContext, TaskQueryParams params) {
        // Validate user has groups
        if (userContext.getGroups() == null || userContext.getGroups().isEmpty()) {
            return buildEmptyTaskListResponse();
        }

        // Build query for candidate tasks
        TaskQuery query = flowableTaskService.createTaskQuery()
            .taskCandidateGroupIn(userContext.getGroups())  // Authorization enforced here
            .active();

        // Optionally exclude assigned tasks
        if (!params.getIncludeAssigned()) {
            query.taskUnassigned();
        }

        // Same flow as getMyTasks...
    }

    private TaskQuery applyFilters(TaskQuery query, TaskQueryParams params) {
        if (params.getSearch() != null) {
            query.taskNameLikeIgnoreCase("%" + params.getSearch() + "%");
        }
        if (params.getPriority() != null) {
            query.taskPriority(params.getPriority());
        }
        // ... other filters
        return query;
    }

    private TaskQuery applySorting(TaskQuery query, TaskQueryParams params) {
        String[] sortParts = params.getSort().split(",");
        String sortField = sortParts[0];
        boolean ascending = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]);

        switch (sortField) {
            case "name": query.orderByTaskName(); break;
            case "priority": query.orderByTaskPriority(); break;
            case "dueDate": query.orderByDueDate(); break;
            default: query.orderByTaskCreateTime();
        }

        return ascending ? query.asc() : query.desc();
    }

    private TaskResponse mapToEnhancedResponse(Task task) {
        // Get candidate groups
        List<String> candidateGroups = flowableTaskService.getIdentityLinksForTask(task.getId())
            .stream()
            .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
            .map(IdentityLink::getGroupId)
            .collect(Collectors.toList());

        // Get process definition name (cached)
        String processDefinitionName = getProcessDefinitionName(task.getProcessDefinitionId());

        return TaskResponse.builder()
            .id(task.getId())
            .name(task.getName())
            .processDefinitionName(processDefinitionName)  // NEW FIELD
            .candidateGroups(candidateGroups)              // NEW FIELD
            // ... all other fields
            .build();
    }

    @Cacheable(value = "processDefinitionNames", key = "#processDefinitionId")
    public String getProcessDefinitionName(String processDefinitionId) {
        ProcessDefinition procDef = repositoryService
            .createProcessDefinitionQuery()
            .processDefinitionId(processDefinitionId)
            .singleResult();
        return procDef != null ? procDef.getName() : null;
    }
}
```

### Step 4: Create WorkflowTaskController (3 hours)

**File**: `controller/WorkflowTaskController.java`

**Key Code**:

```java
@RestController
@RequestMapping("/workflows/tasks")
@RequiredArgsConstructor
@Tag(name = "Workflow Tasks", description = "User task management for workflows")
@SecurityRequirement(name = "bearer-jwt")
@Slf4j
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", description = "Retrieve all tasks assigned to the authenticated user")
    public ResponseEntity<TaskListResponse> getMyTasks(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute TaskQueryParams params) {

        log.info("GET /workflows/tasks/my-tasks - User: {}", jwt.getClaimAsString("preferred_username"));

        // Extract user context from JWT
        JwtUserContext userContext = jwtClaimsExtractor.extractUserContext(jwt);

        // Fetch tasks
        TaskListResponse response = workflowTaskService.getMyTasks(userContext, params);

        log.info("Returning {} tasks for user: {}", response.getContent().size(), userContext.getUserId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/group-tasks")
    @Operation(summary = "Get group tasks", description = "Retrieve all tasks available to user's groups")
    public ResponseEntity<TaskListResponse> getGroupTasks(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute TaskQueryParams params) {

        log.info("GET /workflows/tasks/group-tasks - User: {}", jwt.getClaimAsString("preferred_username"));

        JwtUserContext userContext = jwtClaimsExtractor.extractUserContext(jwt);
        TaskListResponse response = workflowTaskService.getGroupTasks(userContext, params);

        return ResponseEntity.ok(response);
    }
}
```

### Step 5: Create Exception Handling (2 hours)

**Files**:
- `exception/TaskNotFoundException.java`
- `exception/UnauthorizedTaskAccessException.java`
- `exception/GlobalExceptionHandler.java`

**Pattern** (same as Admin service):

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFoundException(
            TaskNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Similar handlers for other exceptions...
}
```

### Step 6: Update Configuration (1 hour)

**File 1**: `config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // ... existing configs ...

            // NEW: Workflow task endpoints
            .requestMatchers(new AntPathRequestMatcher("/workflows/tasks/**")).authenticated()

            .anyRequest().authenticated()
        );
    return http.build();
}
```

**File 2**: `config/CacheConfig.java`

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(
        "serviceUrls",               // Existing
        "processDefinitionNames"     // NEW
    );
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(100)
        .recordStats());
    return cacheManager;
}
```

### Step 7: Write Tests (6 hours)

**Unit Tests** - `WorkflowTaskServiceTest.java`:
```java
@ExtendWith(MockitoExtension.class)
class WorkflowTaskServiceTest {

    @Mock
    private org.flowable.engine.TaskService flowableTaskService;

    @InjectMocks
    private WorkflowTaskService workflowTaskService;

    @Test
    void getMyTasks_shouldReturnUserAssignedTasks() {
        // Arrange
        JwtUserContext userContext = JwtUserContext.builder()
            .userId("john.doe")
            .groups(List.of("HR_STAFF"))
            .build();

        TaskQuery mockQuery = mock(TaskQuery.class);
        when(flowableTaskService.createTaskQuery()).thenReturn(mockQuery);
        when(mockQuery.taskAssignee("john.doe")).thenReturn(mockQuery);
        when(mockQuery.active()).thenReturn(mockQuery);
        when(mockQuery.count()).thenReturn(5L);
        when(mockQuery.listPage(0, 20)).thenReturn(createMockTasks(5));

        // Act
        TaskListResponse response = workflowTaskService.getMyTasks(userContext, new TaskQueryParams());

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getContent().size());
        verify(mockQuery).taskAssignee("john.doe");
    }

    // More tests for filtering, sorting, pagination...
}
```

**Integration Tests** - `WorkflowTaskControllerIntegrationTest.java`:
```java
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void getMyTasks_shouldReturn200WithTasks() throws Exception {
        // Mock JWT
        Jwt jwt = createMockJwt("john.doe", List.of("HR_STAFF"));
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        // Execute request
        mockMvc.perform(get("/workflows/tasks/my-tasks")
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.totalElements").exists());
    }

    // More integration tests...
}
```

---

## Testing Checklist

### Manual Testing

**Step 1**: Start services
```bash
cd /Users/lamteiwahlang/Projects/werkflow
docker-compose up -d postgres keycloak
./mvnw spring-boot:run -pl services/engine
```

**Step 2**: Get JWT token from Keycloak
```bash
# Login as test user
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=werkflow-engine" \
  -d "client_secret=your-secret" \
  -d "grant_type=password" \
  -d "username=john.doe" \
  -d "password=password123"

# Extract access_token from response
export TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6..."
```

**Step 3**: Test my-tasks endpoint
```bash
curl -X GET "http://localhost:8081/workflows/tasks/my-tasks?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

**Step 4**: Test group-tasks endpoint
```bash
curl -X GET "http://localhost:8081/workflows/tasks/group-tasks?page=0&size=20&includeAssigned=false" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

**Step 5**: Test with filters
```bash
# Search
curl -X GET "http://localhost:8081/workflows/tasks/my-tasks?search=leave&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Sort
curl -X GET "http://localhost:8081/workflows/tasks/my-tasks?sort=priority,desc&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Filter by process definition
curl -X GET "http://localhost:8081/workflows/tasks/my-tasks?processDefinitionKey=leave-request&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Automated Testing

```bash
# Run unit tests
./mvnw test -pl services/engine -Dtest=WorkflowTaskServiceTest

# Run integration tests
./mvnw verify -pl services/engine -Dtest=WorkflowTaskControllerIntegrationTest

# Check test coverage
./mvnw jacoco:report -pl services/engine
# Open: services/engine/target/site/jacoco/index.html
```

---

## Performance Validation

### Load Testing with JMeter

**Test Scenarios**:

1. **Baseline**: 10 concurrent users, 100 requests/sec
   - Expected: p95 < 200ms, p99 < 500ms

2. **Peak Load**: 100 concurrent users, 500 requests/sec
   - Expected: p95 < 500ms, p99 < 1000ms

3. **Stress Test**: 500 concurrent users, 1000 requests/sec
   - Expected: No errors, graceful degradation

**JMeter Test Plan**:
```xml
<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8081</stringProp>
  <stringProp name="HTTPSampler.path">/workflows/tasks/my-tasks</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
  <HeaderManager>
    <collectionProp>
      <elementProp>
        <stringProp name="Header.name">Authorization</stringProp>
        <stringProp name="Header.value">Bearer ${JWT_TOKEN}</stringProp>
      </elementProp>
    </collectionProp>
  </HeaderManager>
</HTTPSamplerProxy>
```

### Database Query Performance

**Monitoring Queries**:
```sql
-- Check task query performance
EXPLAIN ANALYZE
SELECT t.*
FROM act_ru_task t
WHERE t.assignee_ = 'john.doe'
  AND t.suspension_state_ = 1
ORDER BY t.create_time_ DESC
LIMIT 20 OFFSET 0;

-- Check identity link performance
EXPLAIN ANALYZE
SELECT DISTINCT t.*
FROM act_ru_task t
INNER JOIN act_ru_identitylink il ON t.id_ = il.task_id_
WHERE il.group_id_ IN ('HR_STAFF', 'HR_ADMIN')
  AND il.type_ = 'candidate'
  AND t.suspension_state_ = 1
ORDER BY t.create_time_ DESC
LIMIT 20 OFFSET 0;
```

**Expected Performance**:
- Query execution time: < 50ms
- Index usage: 100% (no sequential scans)
- Rows fetched: Exactly page size (20)

---

## Rollout Plan

### Phase 1: Development Environment (Day 1)

**Tasks**:
1. Implement all code (16 hours)
2. Run unit tests (coverage > 80%)
3. Manual testing with Postman
4. Fix bugs

**Success Criteria**:
- All tests passing
- Both endpoints return 200 OK
- Swagger documentation visible
- No 404 errors

### Phase 2: Integration Testing (Day 2-3)

**Tasks**:
1. Deploy to dev environment
2. Run integration tests
3. Test with real Keycloak tokens
4. Test with actual workflow data
5. Load testing with JMeter

**Success Criteria**:
- Integration tests passing
- JWT extraction working correctly
- Authorization enforced properly
- Performance meets SLA (p95 < 500ms)

### Phase 3: Staging Deployment (Day 3)

**Tasks**:
1. Deploy to staging environment
2. Run smoke tests
3. QA team validation
4. Security review
5. Performance testing

**Success Criteria**:
- No regressions in existing features
- Security vulnerabilities addressed
- Performance metrics meet targets
- QA sign-off

### Phase 4: Production Rollout (Day 4)

**Tasks**:
1. Deploy during maintenance window
2. Monitor error rates
3. Monitor performance metrics
4. Enable feature for subset of users
5. Gradual rollout to all users

**Success Criteria**:
- Zero critical errors
- Performance within SLA
- User feedback positive
- No rollback required

---

## Monitoring & Alerts

### Metrics to Monitor

**Application Metrics** (Prometheus):
```
# Request rate
rate(http_server_requests_seconds_count{uri="/workflows/tasks/my-tasks"}[5m])

# Response time (p95)
histogram_quantile(0.95, http_server_requests_seconds{uri="/workflows/tasks/my-tasks"})

# Error rate
rate(http_server_requests_seconds_count{uri="/workflows/tasks/my-tasks",status=~"5.."}[5m])

# Cache hit rate
rate(cache_gets{cache="processDefinitionNames",result="hit"}[5m])
/ rate(cache_gets{cache="processDefinitionNames"}[5m])
```

**Database Metrics**:
```
# Query execution time
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE query LIKE '%act_ru_task%'
ORDER BY mean_exec_time DESC
LIMIT 10;

# Connection pool usage
SELECT count(*), state
FROM pg_stat_activity
WHERE datname = 'werkflow'
GROUP BY state;
```

### Alert Rules

```yaml
# Prometheus Alert Rules
groups:
  - name: workflow_tasks
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, http_server_requests_seconds{uri=~"/workflows/tasks/.*"}) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Task endpoint response time high"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{uri=~"/workflows/tasks/.*",status=~"5.."}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Task endpoint error rate high"

      - alert: CacheMissRate
        expr: rate(cache_gets{cache="processDefinitionNames",result="miss"}[5m]) / rate(cache_gets{cache="processDefinitionNames"}[5m]) > 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High cache miss rate for process definitions"
```

---

## Troubleshooting Guide

### Issue 1: 404 Not Found

**Symptoms**: Endpoint returns 404 error

**Possible Causes**:
1. Controller not registered in Spring context
2. Request mapping path mismatch
3. Security filter blocking request

**Solutions**:
```bash
# Check registered endpoints
curl http://localhost:8081/actuator/mappings | jq '.contexts.application.mappings.dispatcherServlets'

# Check Spring Boot logs
grep "Mapped \"/workflows/tasks" logs/engine-service.log

# Verify security configuration
grep "requestMatchers.*workflows/tasks" services/engine/src/main/java/com/werkflow/engine/config/SecurityConfig.java
```

### Issue 2: 401 Unauthorized

**Symptoms**: Valid JWT token rejected

**Possible Causes**:
1. JWT signature validation failed
2. Token expired
3. Issuer mismatch
4. Missing required claims

**Solutions**:
```bash
# Decode JWT token (check expiry, issuer)
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Check Keycloak issuer
curl http://localhost:8090/realms/werkflow/.well-known/openid-configuration | jq .issuer

# Check Spring Security logs
grep "JWT validation" logs/engine-service.log
```

### Issue 3: 403 Forbidden (Group Tasks)

**Symptoms**: User cannot access group tasks

**Possible Causes**:
1. User not in any groups
2. User requesting group they don't belong to
3. JWT groups claim missing

**Solutions**:
```bash
# Check JWT groups claim
echo $TOKEN | cut -d. -f2 | base64 -d | jq .groups

# Check user groups in Keycloak
curl http://localhost:8090/admin/realms/werkflow/users/{userId}/groups \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# Enable debug logging
# application.yml: logging.level.com.werkflow.engine.util.JwtClaimsExtractor: DEBUG
```

### Issue 4: Empty Task List

**Symptoms**: Endpoint returns empty array despite tasks existing

**Possible Causes**:
1. Authorization filter too restrictive
2. User ID mismatch (JWT vs Flowable)
3. Tasks in suspended state
4. Tasks assigned to different user

**Solutions**:
```sql
-- Check tasks in database
SELECT id_, name_, assignee_, suspension_state_
FROM act_ru_task
WHERE assignee_ = 'john.doe';

-- Check identity links
SELECT task_id_, group_id_, user_id_
FROM act_ru_identitylink
WHERE group_id_ IN ('HR_STAFF', 'HR_ADMIN');
```

```bash
# Check JWT user ID
echo $TOKEN | cut -d. -f2 | base64 -d | jq .preferred_username

# Enable query logging
# application.yml: logging.level.org.flowable.engine.impl.persistence: DEBUG
```

### Issue 5: Slow Response Times

**Symptoms**: Response time > 1 second

**Possible Causes**:
1. Missing database indexes
2. Large task variable maps
3. Too many tasks in database
4. Cache not working

**Solutions**:
```sql
-- Check query plan
EXPLAIN ANALYZE
SELECT * FROM act_ru_task WHERE assignee_ = 'john.doe';

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE tablename = 'act_ru_task';

-- Check table size
SELECT pg_size_pretty(pg_total_relation_size('act_ru_task'));
```

```bash
# Check cache stats
curl http://localhost:8081/actuator/metrics/cache.gets?tag=cache:processDefinitionNames

# Enable performance logging
# application.yml: logging.level.com.werkflow.engine.service.WorkflowTaskService: DEBUG
```

---

## Success Criteria Checklist

### Functional Requirements

- [ ] GET `/workflows/tasks/my-tasks` returns 200 OK with task list
- [ ] GET `/workflows/tasks/group-tasks` returns 200 OK with candidate tasks
- [ ] Pagination works correctly (page, size parameters)
- [ ] Sorting works for all fields (name, priority, createTime, dueDate)
- [ ] Search filter works (task name and description)
- [ ] Process definition filter works
- [ ] Due date filters work (before/after)
- [ ] Authorization enforced (users can't see other users' tasks)
- [ ] JWT claims extracted correctly (userId, groups, department)
- [ ] Group authorization enforced (users can't access groups they're not in)
- [ ] HATEOAS pagination links present (self, first, next, last)
- [ ] Empty task list handled gracefully

### Non-Functional Requirements

- [ ] Response time p95 < 500ms (with 100 tasks)
- [ ] Response time p99 < 1000ms (with 100 tasks)
- [ ] Unit test coverage > 80%
- [ ] Integration tests passing (all scenarios)
- [ ] No memory leaks (load test 10 minutes)
- [ ] Concurrent user support (100 users)
- [ ] Swagger documentation complete
- [ ] Error responses consistent
- [ ] Logging structured and informative
- [ ] Cache hit rate > 90% (process definition names)

### Security Requirements

- [ ] JWT signature validation working
- [ ] Expired tokens rejected (401)
- [ ] Missing tokens rejected (401)
- [ ] Authorization by user ID enforced
- [ ] Authorization by groups enforced
- [ ] SQL injection prevented (parameterized queries)
- [ ] XSS prevented (response encoding)
- [ ] CORS configured correctly
- [ ] Sensitive data not logged (tokens, passwords)

### Integration Requirements

- [ ] Frontend can call endpoints successfully
- [ ] JWT tokens from Keycloak accepted
- [ ] Flowable TaskService integration working
- [ ] Caching configuration working
- [ ] Security configuration not breaking existing endpoints
- [ ] No regression in existing TaskController functionality

---

## Quick Commands Reference

```bash
# Build
cd /Users/lamteiwahlang/Projects/werkflow
./mvnw clean install -pl services/engine -DskipTests

# Run
./mvnw spring-boot:run -pl services/engine

# Test
./mvnw test -pl services/engine
./mvnw verify -pl services/engine  # Integration tests

# Check coverage
./mvnw jacoco:report -pl services/engine
open services/engine/target/site/jacoco/index.html

# Check endpoints
curl http://localhost:8081/actuator/mappings | jq

# Swagger UI
open http://localhost:8081/swagger-ui.html

# Get JWT token
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "client_id=werkflow-engine" \
  -d "client_secret=secret" \
  -d "grant_type=password" \
  -d "username=john.doe" \
  -d "password=password123" | jq -r .access_token

# Test my-tasks
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/workflows/tasks/my-tasks?page=0&size=20 | jq

# Test group-tasks
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/workflows/tasks/group-tasks?page=0&size=20 | jq
```

---

## Next Steps After Implementation

1. **Phase 6 Frontend Integration** (Week 4):
   - Create TaskList component
   - Create TaskDetailsModal component
   - Integrate with new endpoints
   - Add task actions (claim, complete, delegate)

2. **Phase 5A Remaining Tasks** (Days 2.5-6):
   - Dynamic Route-Based Role Configuration (Day 2.5)
   - Task Router implementation (Days 3-4)
   - DOA approval logic (Days 5-6)

3. **Future Enhancements**:
   - Task delegation endpoint
   - Task history endpoint
   - Task comments endpoint
   - Task attachments support
   - Real-time task updates (WebSocket)

---

## Document References

- **Full Design**: [Task-Endpoints-Design-Specification.md](./Task-Endpoints-Design-Specification.md)
- **Roadmap**: [/ROADMAP.md](../../ROADMAP.md) (Phase 5A, Phase 6)
- **Keycloak Setup**: [Keycloak-Authentication-Flow.md](./Keycloak-Authentication-Flow.md)
- **Service Registry**: [Service-Registry-User-Guide.md](../Service-Registry-User-Guide.md)

---

**END OF SUMMARY**
