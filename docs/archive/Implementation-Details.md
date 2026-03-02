# Phase 4-6 Implementation Details

**Document**: Technical Decision Log & Complex Implementation Guidance
**Last Updated**: 2025-11-24
**Scope**: Phase 4 (Service Registry), Phase 5 (RBAC + CapEx Migration), Phase 6 (Task UI)

---

## Critical Implementation Decisions

### Decision 1: Service Registry Architecture Pattern

**Decision**: Use database-backed service registry with 30-second cache in ProcessVariableInjector instead of in-memory registry.

**Rationale**:
- Allows dynamic URL updates without service restart
- Supports multi-environment configuration (dev/staging/prod)
- Health check data provides operational visibility
- Distributes discovery queries across services

**Implementation Constraints**:
- Must add fallback to application.yml if registry unavailable
- Health checks must not block workflow execution
- Cache expiration triggers on service status change

**Integration Point**: ProcessVariableInjector runs at workflow start, injects all resolved URLs as process variables for delegate access.

---

### Decision 2: Keycloak Custom Attributes Strategy

**Decision**: Store DOA level, manager_id, hub_id as Keycloak user attributes rather than database.

**Rationale**:
- Single source of truth for identity attributes
- Reduces database joins in task router
- Simplifies user sync between systems
- JWT claims provide context in every request

**Implementation Constraints**:
- Attribute mappers must be configured per client (admin-portal, workflow-engine)
- Attributes must be included in JWT token (add to "Token Mapper")
- Initial bulk user setup requires Keycloak Admin API script

**Integration Point**: JwtClaimsExtractor parses custom attributes from JWT at request time. TaskRoutingService uses extracted claims for approval routing.

---

### Decision 3: BPMN Workflow Versioning Strategy

**Decision**: Keep old broken workflows as archive (non-deployed), deploy -v2 variants using RestServiceDelegate pattern.

**Rationale**:
- Provides rollback capability if new workflows have issues
- Maintains audit trail of migration changes
- Allows parallel testing of old vs new
- Clear version history for troubleshooting

**Implementation Constraints**:
- Old workflows must be undeployed before v2 deployment
- Process instance history links to original workflow key
- Form submissions still reference v2 workflow keys

**Integration Point**: BPMN Designer shows both old and new workflows during migration period. Archive folder structure: `/old/capex-approval-process.bpmn20.xml`.

---

### Decision 4: Task Router Priority Logic

**Decision**: Implement explicit priority-ordered routing (Line Manager → Finance → Department Head) rather than generic rule engine.

**Rationale**:
- Clear business logic expressible in code
- Easier to test and maintain than rule engine
- Sufficient for identified business requirements
- Avoids complexity of DMN (Decision Model Notation)

**Implementation Constraints**:
- Adding new routing patterns requires code changes
- Limited to task assignment (not conditional logic)
- Configuration via role/group membership only

**Integration Point**: TaskRoutingService.routeByWorkflowType() returns specific user ID or candidate group name for task assignment.

---

### Decision 5: Email Notification Reliability

**Decision**: Use async email with 3-retry logic and in-app notification fallback.

**Rationale**:
- @Async prevents email outages from blocking workflow
- Retry logic handles transient mail server failures
- In-app notifications ensure users see updates regardless

**Implementation Constraints**:
- Email failures logged but not escalated
- Users must check in-app notifications
- SMTP connection timeout set to 10 seconds

**Integration Point**: NotificationService called via Spring event listener on task assignment. Failures don't block task routing.

---

## Complex Implementation Sections

### 1. ProcessVariableInjector Caching Strategy

**Challenge**: Service Registry endpoint resolution must be fast but accurate.

**Solution**: 30-second TTL cache with event-driven invalidation.

```
processInstanceStart
  ↓
ProcessVariableInjector.onProcessStart()
  ↓
Check cache: serviceRegistry_cache.get("finance-service:development")
  ├─ Hit: Use cached URL
  └─ Miss: Query service_environment_urls table
       ↓
       Store in cache with 30s TTL
       ↓
       Set process variable: finance_service_url = http://finance:8084
```

**Edge Case Handling**:
- Service registered AFTER workflow starts: Variable not injected, RestServiceDelegate fails with clear error
- Service URL changes during workflow: Delegates use injected value (won't see change until next workflow start)
- Service registry unavailable: Fallback to application.yml if configured, else delegate throws error

**Testing**: Mock service registry unavailable → verify fallback to application.yml works

---

### 2. Keycloak Group Synchronization

**Challenge**: Keep Keycloak groups in sync with organizational structure as users are added/promoted.

**Current Approach**: Manual group assignment via Admin Console.

**Future Enhancement** (Post-Phase 6):
- Keycloak Mapper syncs from admin-service users table to groups
- Department changes trigger group membership updates
- Manager relationships create hierarchical groups

**For Now**: Operational procedure documents user promotion path:
1. Create new user in Keycloak
2. Assign to department group (e.g., /HR Department/Specialists)
3. Add roles (employee, asset_request_requester)
4. Set attributes (department, manager_id)

---

### 3. DOA Approval Escalation

**Challenge**: CapEx request for $15,000 requires Level 3 approval, but assigned approver (Level 1) cannot approve.

**Solution**: Implemented in TaskRouter.escalateIfNeeded():

```
1. Get task amount: $15,000
2. Calculate required level: Level 3 (>$10K, <$100K)
3. Check assignee DOA level: Finance Manager = Level 1
4. Assignee insufficient → Search for Level 3+ approver
5. Found: CFO (Level 3) → Reassign task
6. Audit log: "Task escalated from Finance Manager (L1) to CFO (L3) due to amount"
```

**Testing**: Create requests at DOA boundaries ($999, $1000, $1001, $9999, $10000, $10001) → verify correct approver assigned

---

### 4. JWT Token Size Management

**Challenge**: Including all groups in JWT token can exceed HTTP header limits (8KB typical, some browsers 16KB).

**Current Implementation**: Include all groups in token (departments have ~5-10 groups each).

**Monitoring**: Log JWT token size at authentication.

**If Token Size Exceeds Limits**:
- Option A: Store large groups claim in Redis, return reference in JWT
- Option B: Create computed roles instead of storing group list
- Option C: Paginate groups (fetch on demand)

**For Now**: Document issue and monitor. Most organizations won't hit limit.

---

### 5. Workflow Concurrent Execution Handling

**Challenge**: Multiple workflow instances running simultaneously for same user/department.

**Example**: Two CapEx approval workflows both reach Finance Manager approval task simultaneously.

**Solution**: Flowable TaskService handles concurrent execution natively.

**Considerations**:
- Each task has unique ID (even for same workflow type)
- Task assignment is task-specific (TaskRouter assigns individual tasks)
- No cross-task dependencies (except START event triggering ProcessVariableInjector)

**Testing**: Create 3 CapEx requests simultaneously → verify all route correctly without conflicts

---

### 6. Service URL Resolution in Multi-Environment

**Challenge**: Same workflow deployed in dev/staging/prod but must resolve correct service URLs.

**Solution**: ProcessVariableInjector uses `app.environment` property injected from environment.

```
In dev: finance_service_url = http://localhost:8084
In staging: finance_service_url = http://finance-staging.internal
In prod: finance_service_url = https://api.company.com/finance
```

**Configuration**:
- `application.yml`: app.environment=${APP_ENVIRONMENT:development}
- Docker Compose: APP_ENVIRONMENT=development
- Kubernetes Deployment: APP_ENVIRONMENT=production

**Testing**: Deploy same BPMN to dev → verify dev URLs injected, not staging/prod URLs

---

### 7. Broken Workflow Migration Validation

**Challenge**: Three workflows with non-existent bean references must be migrated to RestServiceDelegate.

**Reference Workflow**: pr-to-po.bpmn20.xml (working example in Procurement service)

**Migration Process**:
1. Copy working BPMN structure
2. Replace each ${capexService} call with ${restServiceDelegate}
3. Externalize service URLs to RestServiceDelegate configuration
4. Test each service task with Postman before BPMN deployment
5. Deploy BPMN and test end-to-end

**Validation Checklist**:
- No SpEL expressions with hardcoded beans (pattern: ${[a-z]+Service\.)
- All RestServiceDelegate fields populated (url, method, responseVariable)
- Service URLs match registry entries
- Gateway conditions don't reference missing beans

---

### 8. Task Portal Performance Optimization

**Challenge**: Task List page loading 500+ tasks impacts UI responsiveness.

**Solution**: Implement multi-level optimization:

**Level 1 - Server-Side**:
- Add indexes: task_assignee, task_processInstanceId, task_created_at
- Implement pagination: 10 tasks/page by default
- Query optimization: N+1 prevention via JPA joins

**Level 2 - Client-Side**:
- Infinite scroll for task list (load next page on scroll)
- Debounce filter inputs (wait 300ms after user stops typing)
- Cache task list (10-minute TTL, invalidate on new task)
- Virtual scrolling for 1000+ task list (render visible only)

**Level 3 - API Contract**:
- Add `lastModified` field to tasks
- Support range queries: `?modifiedAfter=2025-11-24T00:00:00Z`
- Return minimal fields in list, full details in get-by-id

---

### 9. Service Health Check Frequency Tuning

**Challenge**: Health checks must be timely (detect failures) but not overload services.

**Solution**: Configurable check intervals based on service criticality:

```
Critical (Finance, Procurement): Every 30 seconds
Important (HR, Inventory): Every 60 seconds
Optional (Admin): Every 5 minutes
```

**Configuration**:
- `service_registry.health_check_interval_seconds` per service
- Backoff on consecutive failures: 30s → 60s → 300s
- Reset to normal on recovery

**Skipped Checks**: Don't check health if workflow just completed successfully (assume healthy)

---

### 10. BPMN Editor Integration Points

**Challenge**: ExtensionElementsEditor exists but not integrated into BpmnDesigner main UI.

**Solution**: Phased integration approach:

**Phase 4** (Week 1): Service Registry API ready, frontend prep
**Phase 4.2** (Week 2): Embed ServiceTaskPropertiesPanel in BpmnDesigner properties panel
**Phase 5** (Week 3): Add Pattern Detector for visual UI switching
**Phase 6** (Week 4): Full template library integration

**Current Gap**: Users must manually edit BPMN XML to configure RestServiceDelegate. After Phase 4.2, all via UI.

---

## Database Migration Strategy

### Pre-Migration (Week 1 Day 1)
- Backup existing admin service database
- Review V3__create_service_registry_tables.sql
- Test migration in dev database

### Migration Execution (Week 1 Day 2)
- Run V3__create_service_registry_tables.sql (creates 6 tables, 10 indexes)
- Run V4__seed_service_registry.sql (inserts 5 services, 5 environment URLs)
- Verify schema: `\dt service_*` in PostgreSQL

### Post-Migration (Week 1 Day 3)
- Update ProcessVariableInjector to query registry
- Test with mock service registry entries
- Deploy to staging

### Rollback Plan (if needed)
- Drop service_* tables: `DROP TABLE service_health_checks, service_tags, service_environment_urls, service_endpoints, service_registry CASCADE;`
- Restore from backup: `psql -d werkflow_admin < admin_service_backup_2025-11-24.sql`

---

## Testing Data & Fixtures

### Service Registry Seed Data
```
HR Service (http://localhost:8082)
Finance Service (http://localhost:8084)
Procurement Service (http://localhost:8085)
Inventory Service (http://localhost:8086)
Admin Service (http://localhost:8083)
```

### Test Users for RBAC
```
Employee (No DOA)
  - Username: employee1
  - Department: HR
  - Manager: manager1
  - DOA Level: 0

Department Manager (DOA Level 1, <$1K)
  - Username: manager1
  - Department: HR
  - Manager: hr_head
  - DOA Level: 1

Department Head (DOA Level 2, <$10K)
  - Username: hr_head
  - Department: HR
  - Manager: cfo
  - DOA Level: 2
  - is_poc: true

Finance CFO (DOA Level 3, <$100K)
  - Username: cfo
  - Department: Finance
  - DOA Level: 3
```

### Test CapEx Requests
```
Small ($500): Route to Department Manager
Medium ($5,000): Route to Department Head → Finance Manager
Large ($50,000): Route to Finance CFO (Level 3)
X-Large ($200,000): Route to CEO/Board (Level 4)
```

---

## Known Limitations & Future Work

### Phase 4-6 Limitations
1. Service Registry doesn't auto-discover services (manual registration required)
2. Health checks are basic HTTP GET (no custom check logic)
3. Task routing doesn't support complex conditional rules
4. Email notifications don't support custom templates per approval type
5. BPMN Editor doesn't validate RestServiceDelegate field values

### Phase 7+ Enhancements (Post-MVP)
1. Service auto-discovery via Kubernetes API or Consul
2. Advanced health checks with custom scripts
3. DMN (Decision Model Notation) for complex routing rules
4. Email template management with variable substitution
5. Full BPMN Editor with real-time validation
6. Process mining and optimization recommendations
7. Mobile app for task approval
8. Webhook support for external system integration

---

## Troubleshooting Guide

### Issue: ProcessVariableInjector not injecting URLs

**Cause 1**: Service not registered in registry
- Check: `SELECT * FROM service_registry WHERE service_name='finance-service';`
- Fix: Register service via POST /api/services

**Cause 2**: Environment not configured
- Check: `SELECT * FROM service_environment_urls WHERE environment='development';`
- Fix: Configure environment via POST /api/services/{id}/urls

**Cause 3**: ProcessVariableInjector not triggered
- Check: Spring logs for `Injecting service URLs for process instance`
- Fix: Ensure @EventListener annotated and Spring Event enabled

### Issue: Task not routing to correct approver

**Cause 1**: User DOA level not set in Keycloak
- Check: User attributes in Keycloak Admin Console
- Fix: Set doa_level attribute to 1-4

**Cause 2**: Group membership wrong
- Check: User Groups tab in Keycloak Admin Console
- Fix: Add to correct department group (e.g., /Finance Department/Approvers)

**Cause 3**: TaskRoutingService not called
- Check: BPMN workflow has TaskAssignmentDelegate on approval task
- Fix: Add delegate expression: ${taskAssignmentDelegate}

### Issue: JWT token validation failing

**Cause 1**: Keycloak issuer URL wrong
- Check: `app.security.oauth2.resourceserver.jwt.issuer-uri` in application.yml
- Fix: Use internal Keycloak URL (http://keycloak:8090), not external

**Cause 2**: JWT secret not matching
- Check: Keycloak realm keys vs Spring Security config
- Fix: Spring auto-fetches from `issuer-uri/.well-known/openid-configuration`

**Cause 3**: Custom claims not in token
- Check: Keycloak Client Mappers for custom attributes
- Fix: Add User Attribute Mapper for each custom claim (doa_level, manager_id, etc.)

---

## Performance Baselines & Tuning

### API Response Time Targets
- Service Registry CRUD: <100ms
- Task Router decision: <200ms
- Process start with variable injection: <500ms
- Task list query (10 items): <300ms

### Database Query Optimization
```sql
-- Add these indexes
CREATE INDEX idx_service_registry_name ON service_registry(service_name);
CREATE INDEX idx_service_environment_urls_environment ON service_environment_urls(environment);
CREATE INDEX idx_task_assignee ON act_ru_task(assignee_);
CREATE INDEX idx_task_created_at ON act_ru_task(create_time_);
```

### Caching Strategy
- Service Registry: 30-second TTL (invalidate on update)
- User DOA levels: 5-minute TTL (cached in SecurityConfig)
- Service health checks: 1-minute TTL (latest 10 checks stored)

---

## Security Considerations

### JWT Token Validation
- Tokens validated at each request via Spring Security
- Custom attributes (DOA, department) included in JWT (no database lookup needed)
- Token refresh handled by Keycloak (frontend responsible for refresh)

### Service-to-Service Communication
- Service calls via HTTP with JWT authorization header
- RestTemplate configured with OAuth2 client credentials flow
- Each service verifies JWT signature using Keycloak public key

### Sensitive Data
- No passwords stored in BPMN variables
- Approval decisions logged to audit table (not process history)
- Email addresses not exposed via REST API (only used internally)

---

**End of Implementation Details Document**
