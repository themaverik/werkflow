# Phase 4, 5, 6 Implementation Coordination Plan

**Date**: 2025-11-24
**Status**: Active - Ready for Execution
**Duration**: 5 weeks (parallel tracks)
**Objective**: Complete Service Registry, RBAC, CapEx Migration, and Task UI Integration

---

## Executive Summary

This document provides a tactical coordination plan for implementing Phases 4, 5, and 6 of the Werkflow platform. Based on comprehensive architecture analysis and feasibility studies, this plan enables parallel frontend and backend work streams while managing critical dependencies.

### Context

**Phase 3.6/3.7 Status**:
- Three BPMN workflows broken (deployed but will crash at runtime)
- Service Registry backend completely missing
- Frontend no-code UI 70% complete (needs integration testing)
- RBAC design complete (Keycloak configuration ready)

**Target State**:
- Service Registry operational (dynamic URL management)
- Keycloak RBAC fully integrated
- All broken workflows migrated to RestServiceDelegate pattern
- Task UI fully functional for end-to-end approval flows
- Integration testing validates all components work together

---

## Phase 4: Service Registry Backend (Week 1-2)

### Overview

**Critical Path Item**: BLOCKS all frontend service integration work

**Objective**: Replace hardcoded service URLs with dynamic registry lookup

**Business Value**: Enable zero-downtime service URL changes, environment-specific configuration

### Work Breakdown

#### Backend Track (6 days)

**Day 1-2: Database Schema & Core Service**
- **Owner**: Backend Developer
- **Deliverable**: Service registry tables and JPA entities
- **Dependencies**: None - can start immediately

**Tasks**:
1. Create database migration script
   - services table (name, display_name, description, version)
   - service_endpoints table (service_id, environment, base_url, url_type, priority)
   - service_health_checks table (endpoint_id, status, response_time_ms, last_check_at)
2. Create JPA entities (Service, ServiceEndpoint, ServiceHealthCheck)
3. Create repositories (ServiceRepository, ServiceEndpointRepository)
4. Create core service layer (ServiceRegistryService)

**Validation**:
- Database migrations apply cleanly
- Can insert/retrieve services programmatically
- Unit tests pass

**Day 3-4: REST API Implementation**
- **Owner**: Backend Developer
- **Deliverable**: Complete REST API for service management
- **Dependencies**: Day 1-2 complete

**Tasks**:
1. Create ServiceRegistryController with endpoints:
   - GET /api/v1/registry/services
   - GET /api/v1/registry/services/{name}
   - POST /api/v1/registry/services
   - PUT /api/v1/registry/services/{id}
   - DELETE /api/v1/registry/services/{id}
   - GET /api/v1/registry/endpoints?environment={env}&urlType={type}
2. Add request/response DTOs
3. Add validation (JSR-303 annotations)
4. Add exception handling (@RestControllerAdvice)
5. Add Swagger/OpenAPI documentation

**Validation**:
- Postman tests pass for all endpoints
- Swagger UI shows documentation
- Error responses return proper HTTP status codes

**Day 5: ProcessVariableInjector Integration**
- **Owner**: Backend Developer
- **Deliverable**: Dynamic service URL injection at workflow start
- **Dependencies**: Day 3-4 complete

**Tasks**:
1. Update ServiceUrlConfiguration to read from registry instead of application.yml
2. Update ProcessVariableInjector to call ServiceRegistryService
3. Add caching (30-second TTL) for service URLs
4. Add fallback to application.yml if registry unavailable
5. Update Engine Service application.yml with feature flag

**Validation**:
- Start a test workflow, verify process variables contain correct URLs
- Change URL in registry, new processes get updated URL
- Existing running processes unaffected

**Day 6: Health Check Service**
- **Owner**: Backend Developer
- **Deliverable**: Automated service health monitoring
- **Dependencies**: Day 3-4 complete (runs in parallel with Day 5)

**Tasks**:
1. Create ServiceHealthCheckService with scheduled executor
2. Implement health check logic (HTTP GET to /actuator/health)
3. Add @Scheduled annotation (runs every 30 seconds)
4. Update service_health_checks table
5. Add API endpoint: GET /api/v1/registry/services/{name}/health

**Validation**:
- Health checks run automatically
- Table updates with latest status
- Can query health status via API

#### Frontend Track (3 days - starts Day 4)

**Day 4-5: Switch from Mock to Real API**
- **Owner**: Frontend Developer
- **Deliverable**: Service Registry UI connected to real backend
- **Dependencies**: Backend API deployed (Day 3-4)

**Tasks**:
1. Update /lib/api/services.ts - remove mock data fallback
2. Update useServiceRegistry hooks to use real API URL
3. Add proper error handling for API failures
4. Test CRUD operations in Service Registry UI
5. Verify service selector in ServiceTaskPropertiesPanel

**Validation**:
- Can create/edit/delete services via UI
- Service selector shows real services from database
- Health status updates in real-time

**Day 6: Integration Testing**
- **Owner**: Frontend + Backend Developer
- **Deliverable**: End-to-end validation
- **Dependencies**: Backend Day 6 + Frontend Day 5 complete

**Tasks**:
1. Register all 5 services (HR, Finance, Procurement, Inventory, Admin)
2. Configure endpoints for dev/staging/prod environments
3. Create test workflow with ServiceTask
4. Select service from registry, configure RestServiceDelegate
5. Deploy workflow and verify process variables injected correctly
6. Update service URL, verify new workflows get new URL

**Validation**:
- All services registered and healthy
- Workflow uses dynamic URLs
- URL changes don't require service restart

### Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Database migration conflicts | High | Low | Use Flyway versioning, test on fresh DB |
| API contract changes break frontend | Medium | Medium | Define OpenAPI spec first, mock before building |
| Caching causes stale URLs | Medium | Low | Use 30-second TTL, add manual cache invalidation endpoint |
| Health checks overload services | Low | Low | Throttle to 30-second intervals, add circuit breaker |

### Completion Criteria

- All API endpoints functional and documented
- Frontend UI connected to real backend
- ProcessVariableInjector reads from registry
- Health checks running automatically
- At least one workflow successfully uses dynamic URLs
- Zero regression in existing workflows

### Team Coordination

**Handoff Points**:
1. Day 3: Backend notifies frontend that API is deployed to dev environment
2. Day 4: Frontend provides feedback on API contract (any changes needed?)
3. Day 6: Joint testing session to validate end-to-end flow

**Daily Standup Focus**:
- Backend: API implementation status, any blockers?
- Frontend: Mock data removal progress, integration readiness?

---

## Phase 5: RBAC Integration & CapEx Migration (Week 2-4)

### Overview

**Objective**: Complete Keycloak RBAC integration and migrate broken workflows

**Business Value**: Role-based task routing, DOA-based approvals, working CapEx workflow

### Work Breakdown - Part A: Keycloak RBAC (Week 2-3)

#### Backend Track (10 days)

**Day 1-2: Keycloak Realm Import & Spring Security Config**
- **Owner**: Backend Developer
- **Deliverable**: Keycloak configured, JWT validation working
- **Dependencies**: None - can start in parallel with Phase 4

**Tasks**:
1. Import keycloak-realm-export.json into Keycloak instance
2. Create test users (1 employee, 1 manager, 1 head per department)
3. Update Engine Service SecurityConfig.java:
   - Add JwtAuthenticationConverter
   - Configure OAuth2 resource server
   - Add role hierarchy (composite roles)
4. Create JwtClaimsExtractor.java to parse custom attributes
5. Update application.yml with Keycloak URLs

**Validation**:
- Can authenticate with Keycloak users
- JWT token contains roles and custom attributes (department, doa_level, manager_id)
- Actuator endpoint protected by roles

**Day 3-4: Task Router Implementation**
- **Owner**: Backend Developer
- **Deliverable**: Automatic task assignment based on roles
- **Dependencies**: Day 1-2 complete

**Tasks**:
1. Create WorkflowTaskRouter.java service
2. Implement routing logic for each workflow type:
   - Asset Request: route to manager based on requester's manager_id
   - Finance Approval: route to DOA approver based on amount
   - Procurement Approval: route to procurement_approver group
3. Create TaskService.java to wrap Flowable TaskService
4. Add @PreAuthorize annotations to endpoints:
   - GET /api/tasks (requires authenticated user)
   - POST /api/tasks/{id}/complete (requires task assignee or admin)
5. Add audit logging for task assignments

**Validation**:
- Start test workflow, task automatically assigned to correct user
- User can only see tasks assigned to them (filtered by candidateUser)
- Manager can approve/reject tasks
- Non-authorized user gets 403 Forbidden

**Day 5-6: DOA Approval Logic**
- **Owner**: Backend Developer
- **Deliverable**: Amount-based routing to approval levels
- **Dependencies**: Day 3-4 complete

**Tasks**:
1. Update CapEx workflow BPMN with DOA gateway:
   - < $1K: Level 1 (Manager)
   - $1K-$10K: Level 2 (Department Head)
   - $10K-$100K: Level 3 (CFO)
   - > $100K: Level 4 (CEO/Board)
2. Create DOAApprovalService.java
3. Implement getApproverByAmount() method
4. Add Keycloak user search by custom attributes (doa_level)
5. Update TaskRouter to use DOA logic for Finance approval tasks

**Validation**:
- $500 request routes to manager (doa_level=1)
- $5K request routes to department head (doa_level=2)
- $50K request routes to CFO (doa_level=3)
- $200K request routes to CEO (doa_level=4)

**Day 7-8: Manager Delegation**
- **Owner**: Backend Developer
- **Deliverable**: Managers can delegate tasks to other managers
- **Dependencies**: Day 3-4 complete

**Tasks**:
1. Add delegation endpoints:
   - POST /api/tasks/{id}/delegate
   - POST /api/tasks/{id}/claim (take back delegated task)
2. Validate delegation permissions (only manager can delegate to another manager)
3. Add delegation audit trail (who delegated to whom, when)
4. Update TaskService to support Flowable delegation APIs

**Validation**:
- Manager delegates task to another manager
- Delegated manager sees task in their task list
- Original manager can reclaim task
- Audit log shows delegation history

**Day 9-10: Integration with All Services**
- **Owner**: Backend Developer
- **Deliverable**: RBAC applied across HR, Finance, Procurement, Inventory services
- **Dependencies**: Day 1-8 complete

**Tasks**:
1. Copy SecurityConfig to all services (HR, Finance, Procurement, Inventory)
2. Add @PreAuthorize to service-specific endpoints:
   - HR: @PreAuthorize("hasRole('hr_manager')")
   - Finance: @PreAuthorize("hasRole('finance_approver')")
   - Procurement: @PreAuthorize("hasRole('procurement_approver')")
3. Test cross-service calls with JWT propagation
4. Add Spring Cloud Gateway for centralized authorization (optional)

**Validation**:
- HR manager can access HR endpoints
- Finance approver can access Finance endpoints
- Employee cannot access admin endpoints (403 Forbidden)

#### Frontend Track (8 days - starts Day 3)

**Day 3-4: Keycloak Login Integration**
- **Owner**: Frontend Developer
- **Deliverable**: Admin Portal authenticates via Keycloak
- **Dependencies**: Backend Day 1-2 complete

**Tasks**:
1. Install keycloak-js library
2. Create KeycloakProvider.tsx wrapper component
3. Update _app.tsx to use KeycloakProvider
4. Add login/logout buttons
5. Store access token in memory (not localStorage for security)
6. Add token refresh logic (before expiry)

**Validation**:
- User redirected to Keycloak login page
- After login, redirected back to Admin Portal
- Access token stored and used for API calls
- Token refreshes automatically

**Day 5-6: Role-Based UI Rendering**
- **Owner**: Frontend Developer
- **Deliverable**: UI shows/hides features based on user role
- **Dependencies**: Day 3-4 complete

**Tasks**:
1. Create useAuth() hook to access user roles
2. Create ProtectedRoute component
3. Add role-based navigation:
   - Employees: see "My Tasks", "My Requests"
   - Managers: see "Team Tasks", "Approvals"
   - Admins: see "Workflow Designer", "Service Registry"
4. Add role badges in header (show current user role)
5. Hide/show buttons based on permissions:
   - "Create Workflow" (admin only)
   - "Approve" (manager/approver only)

**Validation**:
- Employee sees limited navigation
- Manager sees approval options
- Admin sees full navigation
- Unauthorized actions hidden

**Day 7-8: Task List with Filters**
- **Owner**: Frontend Developer
- **Deliverable**: Users see only their assigned tasks
- **Dependencies**: Backend Day 3-4 complete

**Tasks**:
1. Update /api/tasks API client to pass user ID
2. Create TaskList component with filters:
   - My Tasks (candidateUser)
   - Team Tasks (candidateGroup - for managers)
   - Completed Tasks
3. Add task actions:
   - View Details
   - Approve/Reject (for approval tasks)
   - Delegate (for managers)
4. Add real-time task updates (polling every 10 seconds)
5. Add task count badges in navigation

**Validation**:
- User sees only tasks assigned to them
- Manager sees team tasks (candidateGroup)
- Task count updates in real-time
- Can complete tasks via UI

**Day 9-10: Delegation UI**
- **Owner**: Frontend Developer
- **Deliverable**: Managers can delegate tasks via UI
- **Dependencies**: Backend Day 7-8 complete

**Tasks**:
1. Add "Delegate" button to task details page
2. Create DelegateTaskModal component
3. Add manager selector (query Keycloak for managers in same department)
4. Add delegation confirmation
5. Show delegation history in task details

**Validation**:
- Manager clicks "Delegate", modal opens
- Select another manager from dropdown
- Task disappears from current manager's list
- Appears in delegated manager's list
- Delegation history visible

### Work Breakdown - Part B: CapEx Workflow Migration (Week 3-4)

#### Backend Track (6 days)

**Day 1-2: Finance Service REST APIs**
- **Owner**: Backend Developer
- **Deliverable**: Finance Service exposes workflow endpoints
- **Dependencies**: Phase 4 Service Registry complete

**Tasks**:
1. Create CapExWorkflowController.java in Finance Service
2. Implement endpoints:
   - POST /api/workflow/capex/requests (create CapEx request)
   - POST /api/workflow/capex/budget/check (validate budget)
   - POST /api/workflow/capex/budget/reserve (reserve budget)
   - PUT /api/workflow/capex/requests/{id}/status (update status)
3. Add CapExService.java with business logic
4. Add CapExRepository for database operations
5. Add validation (amount > 0, valid department, etc.)

**Validation**:
- Postman tests pass for all endpoints
- Can create CapEx request and get ID back
- Budget check returns true/false based on availability
- Status updates persist to database

**Day 3-4: Migrate CapEx BPMN to RestServiceDelegate**
- **Owner**: Backend Developer
- **Deliverable**: Working CapEx workflow without broken beans
- **Dependencies**: Day 1-2 complete

**Tasks**:
1. Create new capex-approval-process-v2.bpmn20.xml
2. Replace all ${capexService.method()} with RestServiceDelegate:
   - createRequest → POST /api/workflow/capex/requests
   - checkBudget → POST /api/workflow/capex/budget/check
   - reserveBudget → POST /api/workflow/capex/budget/reserve
   - updateStatus → PUT /api/workflow/capex/requests/{id}/status
3. Add ServiceUrlConfiguration to Engine Service (if not exists)
4. Add ProcessVariableInjector to start event
5. Test each service task individually
6. Deploy new workflow to Flowable

**Validation**:
- New workflow deploys without errors
- Can start CapEx approval process
- Each service task calls Finance Service successfully
- Process completes end-to-end
- No NoSuchBeanDefinitionException errors

**Day 5-6: Migrate Remaining Workflows**
- **Owner**: Backend Developer
- **Deliverable**: Procurement and Asset Transfer workflows migrated
- **Dependencies**: Day 3-4 complete

**Tasks**:
1. Create REST APIs in Procurement Service (/api/workflow/procurement/*)
2. Migrate procurement-approval-process.bpmn20.xml
3. Create REST APIs in Inventory Service (/api/workflow/asset-transfer/*)
4. Migrate asset-transfer-approval-process.bpmn20.xml
5. Test all three workflows end-to-end
6. Archive old broken BPMN files (keep for reference, don't deploy)

**Validation**:
- All three workflows deploy successfully
- Can start each workflow
- All service tasks execute without errors
- Processes complete end-to-end

#### Frontend Track (4 days - starts Day 3)

**Day 3-4: CapEx Request Form**
- **Owner**: Frontend Developer
- **Deliverable**: Users can submit CapEx requests via UI
- **Dependencies**: Backend Day 1-2 complete

**Tasks**:
1. Create CapExRequestForm.tsx component
2. Add form fields:
   - Item Description (text, required)
   - Justification (textarea, required)
   - Amount (currency, required, > 0)
   - Cost Center (dropdown from Finance API)
   - Expected Delivery Date (datepicker, required)
   - Attachments (file upload, optional)
3. Add form validation (react-hook-form + zod)
4. Connect to /api/workflow/capex/requests endpoint
5. Show success message with request ID

**Validation**:
- Form validates inputs before submission
- Submission creates process instance in Flowable
- User sees confirmation with request tracking ID
- Request appears in Finance approval queue

**Day 5-6: CapEx Approval UI**
- **Owner**: Frontend Developer
- **Deliverable**: Managers can approve/reject CapEx requests
- **Dependencies**: Backend Day 3-4 complete

**Tasks**:
1. Create CapExApprovalTask component
2. Show CapEx request details (readonly)
3. Add approval decision form:
   - Approve/Reject radio buttons
   - Comments (textarea, required if reject)
   - DOA level indicator (show user their approval authority)
4. Submit decision to /api/tasks/{id}/complete
5. Show success/error message

**Validation**:
- Manager sees CapEx request in task list
- Can view all request details
- Can approve or reject with comments
- Task completes and routes to next approver

### Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Keycloak configuration errors | High | Medium | Test realm import in dev first, validate with sample users |
| JWT token not propagating across services | High | Medium | Use Spring Cloud Sleuth for tracing, test cross-service calls early |
| DOA logic doesn't match business rules | Medium | Low | Review DOA matrix with Finance team before implementation |
| Workflow migration breaks existing processes | High | Low | Create new workflow versions (-v2), test in parallel before cutover |
| Frontend role checks out of sync with backend | Medium | Medium | Define role enum in shared TypeScript file, sync with backend |

### Completion Criteria

**RBAC**:
- Users authenticate via Keycloak
- Tasks automatically route to correct users
- DOA approvals work for all 4 levels
- Managers can delegate tasks
- Frontend shows/hides features based on roles

**CapEx Migration**:
- Old broken workflows archived (not deployed)
- New workflows use RestServiceDelegate pattern
- All three workflows (CapEx, Procurement, Asset Transfer) work end-to-end
- No NoSuchBeanDefinitionException errors
- Service URLs read from registry

### Team Coordination

**Week 2**:
- Monday: Backend starts Keycloak setup, Frontend starts login integration
- Wednesday: Backend notifies Frontend that JWT is working, Frontend starts role-based UI
- Friday: Joint review of task routing logic

**Week 3**:
- Monday: Backend starts Finance APIs, Frontend starts CapEx form
- Wednesday: Backend deploys migrated workflow, Frontend tests form submission
- Friday: Joint end-to-end testing of CapEx approval flow

**Week 4**:
- Monday-Tuesday: Migrate remaining workflows
- Wednesday-Friday: Integration testing and bug fixes

---

## Phase 6: Task UI & Integration Testing (Week 4-5)

### Overview

**Objective**: Complete task approval UI and validate all components work together

**Business Value**: End-users can submit requests and approve via UI (no manual Flowable operations)

### Work Breakdown

#### Backend Track (5 days)

**Day 1-2: Task API Enhancements**
- **Owner**: Backend Developer
- **Deliverable**: Complete task lifecycle APIs
- **Dependencies**: Phase 5 complete

**Tasks**:
1. Add missing task endpoints:
   - POST /api/tasks/{id}/claim (user claims task)
   - POST /api/tasks/{id}/unclaim (user releases task)
   - GET /api/tasks/{id}/history (task audit trail)
   - GET /api/tasks/{id}/variables (process variables for task)
   - POST /api/tasks/{id}/comment (add comment to task)
2. Add task filtering:
   - GET /api/tasks?status=pending,completed
   - GET /api/tasks?assignee={userId}
   - GET /api/tasks?processDefinitionKey={workflowType}
3. Add pagination (page, size, sort parameters)
4. Add DTO for rich task details (include requester info, amounts, dates)

**Validation**:
- Can claim/unclaim tasks
- Can view task history
- Filtering returns correct tasks
- Pagination works

**Day 3-4: Notification Service**
- **Owner**: Backend Developer
- **Deliverable**: Email notifications for task events
- **Dependencies**: Day 1-2 complete

**Tasks**:
1. Create NotificationService.java
2. Implement email notifications:
   - Task Assigned (notify assignee)
   - Task Completed (notify requester)
   - Task Delegated (notify new assignee)
   - Approval Granted (notify requester)
   - Approval Rejected (notify requester with reason)
3. Use Spring Mail or SendGrid
4. Add email templates (Thymeleaf)
5. Add @Async for non-blocking sends
6. Add retry logic (3 attempts with exponential backoff)

**Validation**:
- Users receive email when task assigned
- Requester notified when request approved/rejected
- Emails use branded templates

**Day 5: Process Monitoring APIs**
- **Owner**: Backend Developer
- **Deliverable**: APIs for tracking process instances
- **Dependencies**: None (can run in parallel)

**Tasks**:
1. Create ProcessMonitoringController.java
2. Add endpoints:
   - GET /api/processes/{instanceId} (process details)
   - GET /api/processes/{instanceId}/tasks (all tasks in process)
   - GET /api/processes/{instanceId}/history (process history)
   - GET /api/processes?businessKey={requestId}
3. Add ProcessInstanceDTO with rich details
4. Add error handling for process not found

**Validation**:
- Can query process by instance ID
- Can see all completed and active tasks
- Can track process by business key (e.g., CapEx request ID)

#### Frontend Track (8 days)

**Day 1-2: Task Details Page**
- **Owner**: Frontend Developer
- **Deliverable**: Complete task details view
- **Dependencies**: Backend Day 1-2 complete

**Tasks**:
1. Create TaskDetailsPage component (/tasks/{id})
2. Show all task information:
   - Task name, description
   - Requester info (name, department, email)
   - Request details (amount, justification, attachments)
   - Process timeline (completed tasks, pending tasks)
   - Comments history
3. Add action buttons:
   - Claim (if unassigned)
   - Complete (with decision form)
   - Delegate (for managers)
   - Add Comment
4. Add real-time updates (refresh every 10 seconds)

**Validation**:
- Task details page shows all information
- Action buttons work
- Comments appear immediately
- Timeline shows progress

**Day 3-4: Task Forms (Dynamic Form Rendering)**
- **Owner**: Frontend Developer
- **Deliverable**: Forms render based on workflow type
- **Dependencies**: Day 1-2 complete

**Tasks**:
1. Create DynamicTaskForm component
2. Map workflow types to forms:
   - capex-approval-process → CapExApprovalForm
   - asset-request → AssetRequestApprovalForm
   - leave-request → LeaveApprovalForm
3. Load form schema from /api/tasks/{id}/form
4. Render form fields dynamically (text, dropdown, datepicker, etc.)
5. Validate form inputs
6. Submit to /api/tasks/{id}/complete with form data

**Validation**:
- Different workflows show different forms
- Form fields pre-populated with process variables
- Validation works
- Submission completes task

**Day 5-6: Request Tracking Page**
- **Owner**: Frontend Developer
- **Deliverable**: Users can track their submitted requests
- **Dependencies**: Backend Day 5 complete

**Tasks**:
1. Create MyRequestsPage component (/requests)
2. Show all requests submitted by user:
   - Request type (CapEx, Asset, Leave, etc.)
   - Submission date
   - Current status (Pending, Approved, Rejected)
   - Current approver
3. Add filters (status, type, date range)
4. Add search (by request ID, amount, description)
5. Click request to see full details and timeline
6. Show process diagram with current task highlighted

**Validation**:
- User sees all their submitted requests
- Status updates in real-time
- Can drill down to see full details
- Process diagram shows current step

**Day 7-8: Dashboard & Analytics**
- **Owner**: Frontend Developer
- **Deliverable**: Summary dashboard for managers
- **Dependencies**: Day 5-6 complete

**Tasks**:
1. Create DashboardPage component (/)
2. Add dashboard widgets:
   - My Pending Tasks (count + list)
   - Team Pending Tasks (count + list)
   - Requests Approved This Week (count)
   - Requests Rejected This Week (count)
   - Average Approval Time (days)
3. Add charts:
   - Requests by Type (pie chart)
   - Approvals by Week (line chart)
   - Top Requesters (bar chart)
4. Add quick actions:
   - Submit New Request
   - View All Tasks
   - View Team Performance

**Validation**:
- Dashboard loads quickly
- Widgets show accurate counts
- Charts render correctly
- Quick actions work

### Integration Testing (5 days - All Team)

**Day 1: End-to-End Test Cases**
- **Owner**: QA + All Developers
- **Deliverable**: Test plan executed
- **Dependencies**: All Phase 4, 5, 6 work complete

**Test Scenarios**:
1. **CapEx Approval Flow**:
   - User submits $500 CapEx request
   - Manager receives task notification via email
   - Manager approves task via UI
   - Finance receives task (DOA Level 1)
   - Finance approver approves task
   - User receives approval notification
   - Request status updated to "Approved"

2. **Delegation Flow**:
   - User submits $5K CapEx request
   - Manager delegates to another manager via UI
   - Delegated manager sees task
   - Delegated manager approves
   - Request routed to Department Head (DOA Level 2)

3. **Rejection Flow**:
   - User submits $100K CapEx request
   - Manager rejects with reason
   - User receives rejection notification with comments
   - Request status updated to "Rejected"

4. **Service Registry Update**:
   - Change Finance Service URL in registry
   - Start new CapEx workflow
   - Verify new workflow uses new URL
   - Verify old running workflows still use old URL

**Day 2-3: Regression Testing**
- **Owner**: QA
- **Deliverable**: Existing workflows still work
- **Dependencies**: Day 1 complete

**Test Scenarios**:
1. HR leave approval workflow works
2. Procurement PR-to-PO workflow works
3. Inventory asset transfer workflow works
4. Form submissions work
5. Process monitoring APIs work
6. Health checks work

**Day 4-5: Bug Fixes & Performance Tuning**
- **Owner**: All Developers
- **Deliverable**: Production-ready system
- **Dependencies**: Day 2-3 complete

**Tasks**:
1. Fix all bugs found in testing
2. Optimize slow queries (add database indexes)
3. Add caching where needed
4. Reduce API response times (<500ms)
5. Add loading states to frontend
6. Add error boundaries
7. Add monitoring dashboards (Grafana)
8. Document known issues

**Validation**:
- All critical bugs fixed
- No P0/P1 bugs remaining
- Performance meets SLAs (API <500ms, page load <3s)
- Error handling graceful

### Completion Criteria

**Task UI**:
- Users can view all assigned tasks
- Users can complete tasks with approval decisions
- Users can track their submitted requests
- Managers have dashboard with team metrics
- Email notifications work for all events

**Integration**:
- All workflows (CapEx, Procurement, Asset Transfer, HR) work end-to-end
- Service Registry provides dynamic URLs
- Keycloak RBAC routes tasks correctly
- Task UI integrates with Flowable APIs
- No regression in existing functionality

### Team Coordination

**Daily Standups**:
- What did you complete yesterday?
- What are you working on today?
- Any blockers?

**Integration Testing**:
- All team members participate
- Rotate testing different workflows
- Document bugs in shared tracker
- Prioritize bugs (P0-P3)

**Sign-off**:
- Backend Lead signs off on APIs
- Frontend Lead signs off on UI
- QA signs off on testing
- Product Owner signs off on acceptance criteria

---

## Critical Path Dependencies

### Dependency Graph

```
Phase 4: Service Registry Backend (Week 1-2)
    ├─→ Backend: Database + API (Day 1-4)
    │       └─→ Frontend: Connect to Real API (Day 4-5)
    │               └─→ Integration Testing (Day 6)
    │
    └─→ ProcessVariableInjector (Day 5)
            └─→ Phase 5B: CapEx Migration (Week 3-4)

Phase 5A: Keycloak RBAC (Week 2-3)
    ├─→ Backend: Spring Security (Day 1-2)
    │       └─→ Frontend: Login Integration (Day 3-4)
    │
    ├─→ Backend: Task Router (Day 3-4)
    │       └─→ Frontend: Task List (Day 7-8)
    │
    └─→ Backend: DOA Logic (Day 5-6)
            └─→ Phase 5B: CapEx Migration (Week 3-4)

Phase 5B: CapEx Migration (Week 3-4)
    ├─→ Backend: Finance APIs (Day 1-2)
    │       └─→ Frontend: CapEx Form (Day 3-4)
    │               └─→ Frontend: Approval UI (Day 5-6)
    │
    └─→ Backend: Migrate BPMN (Day 3-4)
            └─→ Phase 6: Integration Testing (Week 4-5)

Phase 6: Task UI (Week 4-5)
    ├─→ Backend: Task APIs (Day 1-2)
    │       └─→ Frontend: Task Details (Day 1-2)
    │               └─→ Frontend: Dynamic Forms (Day 3-4)
    │
    ├─→ Backend: Notifications (Day 3-4)
    │
    └─→ All: Integration Testing (Day 1-5)
```

### Blocking Dependencies

| Task | Blocks | Earliest Start | Must Complete By |
|------|--------|----------------|------------------|
| Service Registry DB + API | Frontend API integration | Day 1 | Day 4 (Week 1) |
| ProcessVariableInjector | CapEx migration | Day 5 (Week 1) | Day 2 (Week 3) |
| Keycloak Spring Security | RBAC frontend | Day 1 (Week 2) | Day 3 (Week 2) |
| Task Router | Task List UI | Day 3 (Week 2) | Day 7 (Week 2) |
| Finance APIs | CapEx Form | Day 1 (Week 3) | Day 3 (Week 3) |
| Migrated BPMN | Integration Testing | Day 3 (Week 3) | Day 6 (Week 4) |
| Task APIs | Task Details UI | Day 1 (Week 4) | Day 2 (Week 4) |

---

## Parallel Work Tracks

### What Backend Can Do Independently

1. **Phase 4 Backend** (Week 1): Service Registry database, APIs, health checks
2. **Phase 5A Backend** (Week 2): Keycloak Spring Security, Task Router, DOA logic
3. **Phase 5B Backend** (Week 3): Finance APIs, migrate BPMN workflows
4. **Phase 6 Backend** (Week 4): Task APIs, notification service, monitoring

### What Frontend Can Do Independently

1. **Phase 4 Frontend** (Week 1, starts Day 4): Connect Service Registry UI to backend
2. **Phase 5A Frontend** (Week 2, starts Day 3): Keycloak login, role-based UI, task list
3. **Phase 5B Frontend** (Week 3, starts Day 3): CapEx form, approval UI
4. **Phase 6 Frontend** (Week 4): Task details, dynamic forms, request tracking, dashboard

### What Can Run in Parallel

| Week | Backend | Frontend |
|------|---------|----------|
| 1 | Service Registry DB + API | Preparing for integration (can start Day 4) |
| 2 | Keycloak + Task Router + DOA | Login integration (starts Day 3), Task List (starts Day 7) |
| 3 | Finance APIs + BPMN migration | CapEx Form + Approval UI (starts Day 3) |
| 4 | Task APIs + Notifications | Task Details + Dynamic Forms + Dashboard |
| 5 | Bug fixes | Bug fixes + Polish |

---

## Risk Assessment & Mitigation

### High Priority Risks

**Risk 1: Service Registry breaks existing workflows**
- **Impact**: High - All workflows fail
- **Probability**: Low - ProcessVariableInjector has fallback
- **Mitigation**:
  - Add feature flag to enable/disable registry lookup
  - Keep application.yml URLs as fallback
  - Test old workflows after deployment
- **Contingency**: Disable feature flag, rollback to hardcoded URLs

**Risk 2: Keycloak JWT propagation fails across services**
- **Impact**: High - RBAC doesn't work
- **Probability**: Medium - Microservice calls need JWT forwarding
- **Mitigation**:
  - Use Spring Cloud Gateway for centralized auth
  - Add RestTemplate interceptor to forward JWT
  - Test cross-service calls early (Week 2)
- **Contingency**: Use temporary API keys between services

**Risk 3: Migrated workflows have subtle bugs**
- **Impact**: Medium - Processes fail mid-execution
- **Probability**: Medium - New delegate pattern
- **Mitigation**:
  - Test each service task individually before integration
  - Keep old BPMN as reference
  - Version workflows (-v2) to allow rollback
  - Run parallel testing (old vs new)
- **Contingency**: Rollback to old workflow version, fix bugs offline

### Medium Priority Risks

**Risk 4: Task UI performance issues with many tasks**
- **Impact**: Medium - Slow user experience
- **Probability**: Medium - Depending on task volume
- **Mitigation**:
  - Add pagination (max 20 tasks per page)
  - Add database indexes on task queries
  - Cache frequently accessed data
  - Use lazy loading for task details
- **Contingency**: Increase page size limit, add "Load More" button

**Risk 5: DOA logic doesn't match business requirements**
- **Impact**: Medium - Wrong approvers
- **Probability**: Low - Requirements documented
- **Mitigation**:
  - Review DOA matrix with Finance team before coding
  - Make DOA thresholds configurable (not hardcoded)
  - Add admin UI to adjust DOA levels
- **Contingency**: Hotfix DOA thresholds in database

**Risk 6: Email notifications fail or delayed**
- **Impact**: Low - Users not notified
- **Probability**: Medium - SMTP issues common
- **Mitigation**:
  - Use reliable email service (SendGrid, AWS SES)
  - Add retry logic (3 attempts)
  - Add email queue monitoring
  - Add fallback to in-app notifications
- **Contingency**: Disable email notifications temporarily, add manual refresh

---

## Testing Strategy

### Unit Testing

**Backend**:
- JUnit 5 tests for all service methods
- Mockito for dependency mocking
- Test coverage >80% for critical paths
- Test repositories with H2 in-memory database

**Frontend**:
- Jest + React Testing Library
- Test user interactions (button clicks, form submission)
- Test role-based rendering
- Test API error handling

### Integration Testing

**Backend**:
- Spring Boot test slices (@WebMvcTest, @DataJpaTest)
- Testcontainers for PostgreSQL
- Mock external services (Finance, Procurement)
- Test Flowable delegate execution

**Frontend**:
- Test complete user flows (login → task → approval)
- Mock API responses (MSW - Mock Service Worker)
- Test error scenarios (network failure, 403 Forbidden)

### End-to-End Testing

**All**:
- Playwright or Cypress for browser automation
- Test complete workflows (submit request → approve → complete)
- Test across different user roles (employee, manager, admin)
- Test error recovery (failed service calls, retries)

### Performance Testing

**Backend**:
- JMeter for load testing (100 concurrent users)
- Measure API response times (target <500ms)
- Measure database query times (target <100ms)
- Test under load (1000 tasks, 100 processes)

**Frontend**:
- Lighthouse for page load performance (target <3s)
- Test task list with 1000 tasks (virtual scrolling)
- Test form rendering performance

---

## Rollout Plan

### Development Environment (Week 1-4)

**Week 1**: Service Registry deployed to dev
**Week 2**: Keycloak integrated in dev
**Week 3**: Migrated workflows deployed to dev
**Week 4**: Task UI deployed to dev
**Week 5**: Integration testing in dev

### Staging Environment (Week 5)

**Day 1-2**: Deploy all components to staging
**Day 3**: Smoke testing
**Day 4**: UAT (User Acceptance Testing) with business stakeholders
**Day 5**: Address UAT feedback

### Production Environment (Week 6)

**Day 1**: Phased rollout - Service Registry only
- Deploy Service Registry
- Register all services
- Monitor for errors
- Keep ProcessVariableInjector using application.yml (feature flag off)

**Day 2**: Enable Service Registry lookup
- Turn on feature flag
- Monitor new workflows use dynamic URLs
- Verify no impact on running workflows

**Day 3**: Keycloak integration
- Enable OAuth2 authentication
- Users login via Keycloak
- Monitor JWT validation
- Verify task routing works

**Day 4**: Deploy migrated workflows
- Deploy CapEx-v2, Procurement-v2, Asset-Transfer-v2
- Archive old broken workflows (don't deploy)
- Monitor for errors
- Test one workflow end-to-end in production

**Day 5**: Full cutover
- Enable all features
- Monitor for 24 hours
- Address any issues immediately
- Declare success

### Rollback Plan

**If Service Registry fails**:
1. Turn off feature flag (ProcessVariableInjector uses application.yml)
2. Revert code deployment
3. Keep using hardcoded URLs

**If Keycloak fails**:
1. Temporarily disable OAuth2 (@PreAuthorize("permitAll()"))
2. Allow anonymous access for critical workflows
3. Fix auth issues offline

**If migrated workflows fail**:
1. Un-deploy new workflows
2. Workflows don't start (no impact on running processes)
3. Fix issues, redeploy

**If Task UI fails**:
1. Users can still use Flowable UI (fallback)
2. Fix frontend issues
3. Redeploy frontend only (no backend impact)

---

## Success Metrics

### Technical Metrics

| Metric | Target | How to Measure |
|--------|--------|----------------|
| API Response Time (p95) | <500ms | APM tools (Prometheus) |
| Page Load Time (p95) | <3s | Google Lighthouse |
| Workflow Success Rate | >98% | Flowable process metrics |
| Task Completion Time | <5 minutes | Flowable task metrics |
| Email Delivery Rate | >95% | Email service logs |
| Service Health Check Success | >99% | Health check logs |

### Business Metrics

| Metric | Target | How to Measure |
|--------|--------|----------------|
| User Adoption | >80% of employees use UI | User analytics |
| Approval Turnaround Time | <24 hours | Flowable task metrics |
| Requests Per Day | >100 | Process start count |
| Manager Satisfaction | >4/5 | User survey |
| Error Rate (user-facing) | <2% | Error logs |

### Validation Criteria

**Phase 4 Complete**:
- [ ] Service Registry UI shows all 5 services
- [ ] Can update service URL without restart
- [ ] New workflows use dynamic URLs
- [ ] Health checks run automatically

**Phase 5 Complete**:
- [ ] Users authenticate via Keycloak
- [ ] Tasks route to correct users based on roles
- [ ] DOA approvals work for all 4 levels
- [ ] All 3 broken workflows migrated and working

**Phase 6 Complete**:
- [ ] Users can submit requests via UI
- [ ] Managers can approve/reject via UI
- [ ] Email notifications sent for all events
- [ ] Request tracking page shows real-time status

**All Phases Complete**:
- [ ] End-to-end CapEx approval flow works
- [ ] End-to-end Procurement approval flow works
- [ ] End-to-end Asset Transfer approval flow works
- [ ] No NoSuchBeanDefinitionException errors
- [ ] All tests pass (unit, integration, E2E)
- [ ] Performance meets targets
- [ ] UAT sign-off from business stakeholders

---

## Team Coordination

### Daily Standup (15 minutes)

**Time**: 9:00 AM daily

**Format**:
1. Backend Developer: Yesterday's progress, today's plan, blockers
2. Frontend Developer: Yesterday's progress, today's plan, blockers
3. QA Engineer: Testing status, bugs found, concerns
4. Tech Lead: Review dependencies, address blockers, adjust plan

**Key Questions**:
- Are we on track for this week's milestone?
- Any blockers need escalation?
- Do frontend/backend need to sync?

### Weekly Planning (1 hour)

**Time**: Monday 10:00 AM

**Agenda**:
1. Review last week's completed work
2. Demo completed features
3. Review this week's tasks (from this plan)
4. Assign owners
5. Identify dependencies
6. Commit to weekly milestone

### Integration Sync (30 minutes)

**Time**: Wednesday 3:00 PM (Week 1, 2, 3, 4)

**Purpose**: Ensure frontend/backend integration on track

**Agenda**:
1. Backend: Demo API endpoints ready for frontend
2. Frontend: Show UI mockups/progress
3. Discuss API contract changes needed
4. Test integration together (pair programming)
5. Document any issues

### Retrospective (1 hour)

**Time**: Friday 4:00 PM (Week 5)

**Purpose**: Reflect on what went well, what didn't

**Agenda**:
1. What went well?
2. What didn't go well?
3. What should we do differently next time?
4. Action items for improvement

### Communication Channels

**Slack**:
- #werkflow-dev: General development discussion
- #werkflow-blockers: Urgent blockers needing attention
- #werkflow-deployments: Deployment notifications

**Email**:
- Weekly status report (Fridays 5:00 PM)
- Deployment notifications (production only)

**Documentation**:
- Update Roadmap.md daily with task status
- Update Architecture docs as decisions made
- Create ADRs (Architecture Decision Records) for major decisions

---

## Next Steps (Immediate Actions)

### This Week (Week 1)

**Monday**:
- [ ] Review this coordination plan with team
- [ ] Create Jira/GitHub issues for all tasks
- [ ] Assign owners
- [ ] Set up Slack channels
- [ ] Backend: Start Service Registry database schema

**Tuesday**:
- [ ] Backend: Complete database migrations
- [ ] Backend: Start REST API implementation
- [ ] Frontend: Prepare for integration (review API contract)

**Wednesday**:
- [ ] Backend: Complete REST API
- [ ] Backend: Deploy to dev environment
- [ ] Integration Sync: Review API together

**Thursday**:
- [ ] Frontend: Start connecting to real API
- [ ] Backend: Start ProcessVariableInjector integration
- [ ] Backend: Start health check service (parallel)

**Friday**:
- [ ] Backend: Complete ProcessVariableInjector
- [ ] Frontend: Complete API integration
- [ ] Integration Testing: Test service registry end-to-end
- [ ] Weekly Status Report: Share progress

### Next Week (Week 2)

**Monday**:
- [ ] Backend: Import Keycloak realm
- [ ] Backend: Configure Spring Security
- [ ] Frontend: Prepare for Keycloak integration

**By Friday**:
- [ ] Keycloak authentication working
- [ ] Task Router implemented
- [ ] DOA logic completed
- [ ] Frontend login working
- [ ] Task List UI completed

---

## Conclusion

This coordination plan provides a realistic, dependency-aware roadmap for completing Phases 4, 5, and 6 in 5 weeks. Key success factors:

1. **Clear Dependencies**: Backend work enables frontend integration at defined points
2. **Parallel Tracks**: Frontend and backend can work independently with clear handoffs
3. **Risk Mitigation**: Every risk has a documented mitigation and contingency plan
4. **Testing Strategy**: Comprehensive testing at every level (unit, integration, E2E)
5. **Rollout Plan**: Phased deployment minimizes risk to production
6. **Team Coordination**: Daily standups and weekly planning keep everyone aligned

**Critical Success Factors**:
- Backend completes Service Registry by Week 1 Day 4 (unblocks frontend)
- Keycloak integration done by Week 2 (unblocks RBAC)
- CapEx migration done by Week 3 Day 4 (proof of concept for others)
- Integration testing given full week 5 (ensure quality)

**Expected Outcome**: By end of Week 5, Werkflow platform will have:
- Dynamic service URL management
- Role-based task routing with Keycloak
- All broken workflows migrated and working
- Complete task approval UI for end-users
- Validated integration across all components

**Status**: Ready for execution. Team can start immediately with Phase 4 Service Registry Backend.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-24
**Next Review**: 2025-12-01 (after Week 1 completion)
