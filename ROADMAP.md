# Werkflow Implementation Roadmap

**Project**: Enterprise Workflow Automation Platform
**Last Updated**: 2026-02-26 (Architecture Review and Consolidation)
**Status**: Phases 0-3.5 Complete | Phase 5A Backend/Frontend Complete | Phase 6 Week 4 Backend Complete | Phase 7 Form-js Backend Complete | **Architecture Consolidation In Progress**

---

## Overview

Werkflow is a multi-department workflow automation platform enabling self-service workflow creation with centralized governance. This roadmap tracks implementation progress across all phases.

---

## Current Status (2026-02-26)

**System Status**: Architecture review complete. Consolidation and stabilization in progress.
**Last Full Analysis**: 2026-02-26 (Principal Engineer independent codebase audit)

### Architecture Audit Summary

An independent audit of the codebase revealed several gaps between documented status and ground truth:

| Documented Claim | Actual State |
|------------------|-------------|
| "All priorities resolved" | Dual package trees still exist in Finance and Procurement |
| "BPMN migrated to RestServiceDelegate" | Zero Engine BPMN files use RestServiceDelegate; v2 uses inline Groovy HTTP |
| "RestServiceDelegate pattern established" | Delegate exists but uses `WebClient.block()` (thread starvation risk) |
| "Service Registry verified" | Flyway seed data only; no runtime registration or health checks |
| "Flowable 7.0.1 compatibility issues" | Most were misdiagnosed (wrong service interface, not missing API) |

### Infrastructure Health

| Component | Status |
|-----------|--------|
| Flowable Engine Service (8081) | Healthy (62 database tables) |
| Process Definitions | 30 BPMN workflows deployed (14 across 5 services) |
| Admin Service (8083) | Healthy |
| Keycloak Authentication (8090) | Healthy |
| PostgreSQL Database (5433) | Healthy (6 schemas) |
| Domain Services (HR, Finance, Procurement, Inventory) | Flyway migrations fixed; awaiting deployment verification |

### Service Inventory

| Service | Port | LOC | Entities | Endpoints | Flowable Dep | Flowable Usage |
|---------|------|-----|----------|-----------|-------------|----------------|
| Engine | 8081 | 9,154 | 0 (Flowable-managed) | 52 | Required | 11 files (primary orchestrator) |
| Admin | 8083 | 6,609 | 8 | 49 | None | 0 |
| HR | 8082 | 4,263 | 7 | 63 | Unnecessary | 1 file (embedded WorkflowService) |
| Finance | 8084 | 4,213 | 9 | 52 | Unnecessary | 0 files |
| Procurement | 8085 | 4,493 | 11 | 41 | Unnecessary | 1 file (ProcessVariableInjector) |
| Inventory | 8086 | 4,477 | 6 | 78 | Unnecessary | 0 files |

---

## Completed Phases (0-3.5)

### Phase 0: Foundation and Planning -- COMPLETE

- [x] Architecture decision records
- [x] Monorepo structure finalized
- [x] Technology stack selected (Flowable 7.0.x, Spring Boot 3.3.x, Next.js 14)
- [x] Docker Compose infrastructure
- [x] CI/CD pipelines (GitHub Actions)

### Phase 1: Core Platform Foundation -- COMPLETE

- [x] Flowable engine service deployed with PostgreSQL
- [x] OAuth2/JWT authentication via Keycloak
- [x] Shared delegates module (RestServiceDelegate)
- [x] HR service migration with workflows

### Phase 2: Department Services -- COMPLETE

- [x] Finance service (Budget, CapEx, Approvals)
- [x] Procurement service (Vendors, PR, PO)
- [x] Inventory service (Assets, Stock, Transfers)
- [x] Kafka topic definitions and event schemas

### Phase 3: Admin Portal and Governance -- 70% COMPLETE

- [x] Admin Portal React application (Next.js 14)
- [x] BPMN designer integration (bpmn-js)
- [x] Form builder interface (form-js)
- [x] User and role management UI
- [x] RestServiceDelegate with field injection pattern
- [x] Service Registry frontend UI
- [ ] BpmnDesigner integration with ServiceTaskPropertiesPanel
- [ ] Process Variable Manager
- [ ] End-to-end integration testing

### Phase 5A: Keycloak RBAC -- COMPLETE

**Backend** (8 Java files):
- [x] RoleConfigService with route-based access control
- [x] WorkflowTaskRouter with DOA routing (4 levels)
- [x] DoAApprovalService with escalation chain
- [x] TaskAssignmentDelegate for BPMN task routing
- [x] KeycloakUserService for user/role queries

**Frontend** (8 components):
- [x] auth-context.tsx (Keycloak + NextAuth)
- [x] use-authorization.ts (permission hooks)
- [x] ProtectedRoute, RoleBasedNav, RoleBadges
- [x] 403 access denied page

### Phase 6 Week 4: Task APIs and Notifications -- BACKEND COMPLETE

- [x] Task API endpoints (my-tasks, group-tasks) with pagination/filtering -- 20 tests
- [x] NotificationService with @Async and @Retryable -- 21 tests
- [x] Process Monitoring APIs (4 endpoints) -- 26 tests
- [x] Total: 67 tests passing, 0 build errors

### Phase 7: Form-js Backend Integration -- COMPLETE

- [x] FormSchemaService, TaskFormService, FormSchemaValidator
- [x] 8 REST endpoints for form CRUD and task form operations
- [x] Database migration (form_schemas table with versioning)
- [x] 5 form schemas (CapEx request/approval, Leave request/approval, Purchase requisition)

---

## Active Work: Architecture Stabilization

### Phase S1: Critical Fixes (Priority: IMMEDIATE)

**Duration**: 2-3 hours
**Status**: NOT STARTED
**Prerequisite**: None
**Goal**: Fix verified technical debt before any new feature work

#### S1.1: Fix RestServiceDelegate Blocking Issue

**File**: `shared/delegates/src/main/java/com/werkflow/delegates/rest/RestServiceDelegate.java`
**Problem**: Uses `WebClient.block()` inside Flowable JavaDelegate, causing thread starvation under load.
**Fix**: Replace WebClient with Spring 6.1 RestClient (synchronous, no reactive overhead).

Tasks:
- [ ] Replace `WebClient` with `RestClient` in constructor and execute method
- [ ] Remove `spring-webflux` dependency from shared/delegates pom.xml if unused elsewhere
- [ ] Remove `reactor-core` import
- [ ] Verify field injection still works with RestClient
- [ ] Run existing delegate tests

**Estimated time**: 30 minutes

#### S1.2: Fix Flowable Version Alignment

**Problem**: Flowable 7.0.1 was built against Spring Boot 3.1.6; project uses Spring Boot 3.3.2. Flowable 7.1.0 targets Spring Boot 3.3.4 (proper alignment).
**Fix**: Bump `<flowable.version>` from 7.0.1 to 7.1.0 across all pom.xml files.

Tasks:
- [ ] Update `flowable.version` in all 6 service pom.xml files and shared/delegates
- [ ] Verify compilation with 7.1.0
- [ ] Fix IntegrationTestBase: replace `Thread.sleep()` hack with `managementService.createJobQuery().count()` (the method exists on ManagementService, not RuntimeService)
- [ ] Fix redundant Date conversion in TaskService.java (`.toInstant().atZone().toInstant()` -> `.toInstant()`)
- [ ] Run full test suite

**Context**: The `createJobQuery()` "compatibility issue" was misdiagnosed. The method exists on `ManagementService` (confirmed via jar decompilation), not `RuntimeService`. The `Thread.sleep()` workaround should be reverted.

**Estimated time**: 1 hour

#### S1.3: Delete Dual Package Trees

**Problem**: Finance and Procurement have legacy `com.werkflow.*` alongside `com.werkflow.finance.*` / `com.werkflow.procurement.*`. Creates ambiguous component scanning and duplicate bean risks.

Tasks:
- [ ] Verify Finance `@SpringBootApplication` scan base package
- [ ] Delete `com.werkflow.controller/`, `com.werkflow.dto/`, `com.werkflow.entity/`, `com.werkflow.repository/`, `com.werkflow.service/`, `com.werkflow.config/` from Finance (keep only `com.werkflow.finance.*`)
- [ ] Verify Procurement `@SpringBootApplication` scan base package
- [ ] Delete same legacy packages from Procurement (keep only `com.werkflow.procurement.*`)
- [ ] Compile and verify both services
- [ ] Check for any BPMN expressions referencing old bean names

**Estimated time**: 1 hour

#### S1.4: Remove Unnecessary Flowable Dependencies

**Problem**: 4 domain services include `flowable-spring-boot-starter` but 3 have zero Flowable API usage. HR has one embedded WorkflowService.

Tasks:
- [ ] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from Finance pom.xml
- [ ] Remove `flowable-engine` from Inventory pom.xml
- [ ] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from Procurement pom.xml; delete ProcessVariableInjector if unused
- [ ] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from HR pom.xml; delete `com.werkflow.workflow.*` (WorkflowService, delegates, controller)
- [ ] Compile and verify all 4 services start without Flowable

**Estimated time**: 1 hour

---

### Phase S2: BPMN Wiring (Priority: HIGH)

**Duration**: 3-4 hours
**Status**: NOT STARTED
**Prerequisite**: S1.1 (RestServiceDelegate fix)
**Goal**: Make at least one workflow actually use RestServiceDelegate end-to-end

#### S2.1: Migrate capex-approval-process-v2 to RestServiceDelegate

**Problem**: v2 uses inline Groovy scripts with `HttpURLConnection` for REST calls. Fragile, no connection pooling, no retry, no JWT propagation.
**Fix**: Rewrite service tasks to use `restServiceDelegate` with field injection (Mode 1).

Tasks:
- [ ] Map all Groovy HTTP tasks in capex-approval-process-v2.bpmn20.xml
- [ ] Rewrite each as `<serviceTask flowable:delegateExpression="${restServiceDelegate}">` with `<flowable:field>` configuration
- [ ] Verify Finance service has the required REST endpoints (create-request, check-budget, allocate, update-status)
- [ ] Create any missing Finance endpoints
- [ ] Test deployment to Flowable
- [ ] Test execution with at least one DOA scenario

**Estimated time**: 3-4 hours

---

### Phase S3: Domain Service Consolidation (Priority: MEDIUM)

**Duration**: 1 day
**Status**: PLANNED
**Prerequisite**: S1.3 (dual package cleanup), S1.4 (Flowable dep removal)
**Goal**: Merge HR, Finance, Procurement, Inventory into a single `business` service

#### Rationale

Current state: 6 Spring Boot JVMs + PostgreSQL + Keycloak in development.
- 1 developer, no independent scaling needs, no separate release cadences
- 4 domain services share identical security config, Flyway setup, and Docker boilerplate
- ~90 files of duplicated infrastructure config across domain services
- ~1.5-3GB memory footprint for 4 domain JVMs

Target state: 3 services (Engine, Platform, Business).

#### Target Architecture

```
services/
  engine/       # Flowable BPM orchestration (8081) -- UNCHANGED
  platform/     # Admin renamed (8083) -- service registry, RBAC, user/org mgmt
  business/     # Merged domain service (8084) -- HR, Finance, Procurement, Inventory
```

Package structure for business service:
```
com.werkflow.business/
  BusinessServiceApplication.java
  config/           # Single SecurityConfig, single DB config
  hr/
    controller/
    entity/
    repository/
    service/
  finance/
    controller/
    entity/
    repository/
    service/
  procurement/
    controller/
    entity/
    repository/
    service/
  inventory/
    controller/
    entity/
    repository/
    service/
```

#### S3.1: Create Business Service Shell

Tasks:
- [ ] Create `services/business/` directory with pom.xml
- [ ] Single pom.xml pulling dependencies from all 4 domains
- [ ] Create `BusinessServiceApplication.java` with `@SpringBootApplication(scanBasePackages = "com.werkflow.business")`
- [ ] Create single `SecurityConfig.java` (merge from any domain service)
- [ ] Create single `application.yml` with multi-schema Flyway:
  ```yaml
  spring:
    flyway:
      enabled: false  # Use per-schema beans instead
  ```
- [ ] Create Flyway configuration bean for each schema (hr, finance, procurement, inventory)

**Estimated time**: 2 hours

#### S3.2: Move Domain Packages

Tasks:
- [ ] Move `com.werkflow.hr.*` -> `com.werkflow.business.hr.*`
- [ ] Move `com.werkflow.finance.*` -> `com.werkflow.business.finance.*`
- [ ] Move `com.werkflow.procurement.*` -> `com.werkflow.business.procurement.*`
- [ ] Move `com.werkflow.inventory.*` -> `com.werkflow.business.inventory.*`
- [ ] Move Flyway migrations into per-domain directories:
  - `db/migration/hr/`
  - `db/migration/finance/`
  - `db/migration/procurement/`
  - `db/migration/inventory/`
- [ ] Update all import statements
- [ ] Compile and verify

**Estimated time**: 2 hours

#### S3.3: Consolidate Infrastructure

Tasks:
- [ ] Merge Docker Compose: remove 4 domain containers, add 1 business container
- [ ] Update Engine service URLs: 4 separate hosts become 1 (different API paths)
- [ ] Update `application.yml` service URL configuration
- [ ] Update frontend API client if any direct domain service calls exist
- [ ] Rename `services/admin/` to `services/platform/` (optional, cosmetic)

**Estimated time**: 2 hours

#### S3.4: Verify and Clean Up

Tasks:
- [ ] All endpoints respond correctly via business service
- [ ] Flyway migrations run per schema without conflicts
- [ ] Engine RestServiceDelegate can reach business service endpoints
- [ ] Frontend admin-portal works against new endpoint layout
- [ ] Delete old `services/hr/`, `services/finance/`, `services/procurement/`, `services/inventory/` directories
- [ ] Update Docker Compose dev and prod configs

**Estimated time**: 2 hours

#### Consolidation Benefits

| Metric | Before (6 services) | After (3 services) |
|--------|---------------------|---------------------|
| JVMs in dev | 6 | 3 |
| Memory footprint | ~3GB | ~1.2GB |
| Security configs | 6 copies | 3 |
| pom.xml files | 6 | 3 |
| Docker images | 6 | 3 |
| Dev startup time | ~60-90s | ~25-35s |
| Flowable dependencies | 5 services | 1 service (Engine only) |

#### Rollback Strategy

If consolidation causes unforeseen issues, domain services can be extracted back out since package boundaries are preserved. The merge is structurally reversible.

---

### Phase S4: Frontend Completion (Priority: HIGH)

**Duration**: 5-6 days
**Status**: NOT STARTED
**Prerequisite**: S1 (backend stabilization)
**Goal**: Complete the task management UI that wires existing backend APIs to frontend

**Context**: Backend APIs exist (Task APIs, Process Monitoring, Form Schema, Notifications) but frontend components are incomplete. ApprovalPanel, DOAIndicator, and DelegationModal exist but have no host page.

#### S4.1: Task Detail Page (2 days)

Tasks:
- [ ] Create TaskDetailsPage component
- [ ] Wire ApprovalPanel, DOAIndicator, DelegationModal into page
- [ ] Load task data from GET /workflows/tasks/my-tasks
- [ ] Load task form from GET /api/tasks/{taskId}/form
- [ ] Render form-js form with data binding
- [ ] Submit form via POST /api/tasks/{taskId}/form/submit
- [ ] Show process timeline from GET /workflows/processes/{id}/history
- [ ] Handle claim/unclaim/complete actions

#### S4.2: Request Tracking Page (2 days)

Tasks:
- [ ] Create MyRequestsPage component
- [ ] List user's submitted process instances
- [ ] Add status filters (active, completed, suspended)
- [ ] Add search by business key
- [ ] Show process diagram with current task highlighted
- [ ] Wire to Process Monitoring APIs

#### S4.3: Dashboard (1-2 days)

Tasks:
- [ ] Create DashboardPage component
- [ ] Pending tasks count widget
- [ ] Team tasks widget
- [ ] Recent activity feed
- [ ] Quick action buttons (New CapEx, New Leave Request, etc.)

---

### Phase S5: Integration Testing (Priority: HIGH)

**Duration**: 2-3 days
**Status**: NOT STARTED
**Prerequisite**: S1, S2
**Goal**: Verify at least the CapEx workflow works end-to-end

#### S5.1: CapEx End-to-End (1-2 days)

Tasks:
- [ ] Deploy all services in Docker
- [ ] Authenticate via Keycloak, obtain JWT
- [ ] Start CapEx process via Engine API
- [ ] Verify RestServiceDelegate calls Finance service
- [ ] Verify task routing by DOA level
- [ ] Complete approval task
- [ ] Verify notification sent
- [ ] Verify process history recorded
- [ ] Test all 4 DOA scenarios ($500, $7.5K, $75K, $250K)

#### S5.2: Regression (1 day)

Tasks:
- [ ] Verify HR leave approval workflow (if HR still has embedded Flowable, test that path; if removed, test via Engine)
- [ ] Verify Procurement PR-to-PO workflow
- [ ] Verify all API endpoints return correct data
- [ ] Verify Keycloak RBAC enforcement on endpoints
- [ ] Verify form-js form load and submit

---

## Deferred (Post-MVP)

### Service Registry Backend (Phase 4 from original roadmap)

**Status**: Frontend complete, backend is Flyway seed data only
**Current workaround**: Hardcoded URLs in application.yml -- sufficient for MVP with 3 services
**When to revisit**: When deploying to multiple environments or adding services dynamically

### BPMN Designer No-Code Enhancement (Phase 3.7 remaining)

**Status**: 70% complete (ServiceTaskPropertiesPanel, ExpressionBuilder, ExtensionElementsEditor built)
**Remaining**: BpmnDesigner integration, Process Variable Manager, Gateway condition integration
**When to revisit**: After MVP launch, when citizen developers need self-service process creation

### Phase 8 Options A and B (Metadata APIs, Frontend UI Templates)

**Status**: Option C (Deploy and Test) complete; Options A and B pending
**When to revisit**: Post-MVP, when dynamic service discovery and template-based process creation become priorities

### Remaining Workflow Migrations

**Status**: Only CapEx targeted for RestServiceDelegate migration
**Remaining**: Procurement approval, Asset transfer approval, HR workflows
**When to revisit**: After CapEx proves the pattern end-to-end

### Performance and Security Hardening

- [ ] Load testing (100 concurrent users)
- [ ] Database indexing based on query profiling
- [ ] Security audit (cross-user data access, OWASP top 10)
- [ ] Non-unique request number fix (PostgreSQL sequences)
- [ ] Inventory DTO layer (currently leaks JPA entities)

---

## Flowable Version Strategy

### Current: 7.0.1 (Built Against Spring Boot 3.1.6)

Known issues:
- Spring Boot version mismatch (project uses 3.3.2, Flowable built for 3.1.6)
- `Expression` class moved from `org.flowable.engine.delegate` to `org.flowable.common.engine.api.delegate` (6.x to 7.x breaking change)
- `createJobQuery()` exists on `ManagementService`, not `RuntimeService` (was misdiagnosed as missing)
- All date methods (`getDueDate`, `getClaimTime`, `getCreateTime`) return `java.util.Date` across all 7.x versions

### Target: 7.1.0 (Built Against Spring Boot 3.3.4)

| Factor | 7.0.1 (current) | 7.1.0 (target) | 7.2.0 (skip) |
|--------|-----------------|-----------------|---------------|
| Spring Boot target | 3.1.6 | 3.3.4 | Unknown |
| Liquibase dep | Present | Removed | Removed |
| API changes | Baseline | Additive only | REST dates changed to UTC |
| Risk | Known quirks | Low | Medium (UTC date change) |

### Migration Notes

- Zero code changes required for 7.0.1 to 7.1.0 upgrade
- All `TaskInfo`, `HistoricTaskInstance`, `HistoricProcessInstance` interfaces unchanged
- `ManagementService.createJobQuery()` unchanged
- Do NOT upgrade to 7.2.0 until frontend date parsing is verified (UTC change)

---

## Risk Register

### High Risks

1. **RestServiceDelegate thread starvation** (ACTIVE)
   - Probability: High under load
   - Impact: Engine service becomes unresponsive
   - Mitigation: Phase S1.1 -- replace WebClient.block() with RestClient
   - Status: Fix designed, not yet implemented

2. **Flowable/Spring Boot version mismatch** (ACTIVE)
   - Probability: Low (works today, may break on edge cases)
   - Impact: Medium (subtle runtime failures)
   - Mitigation: Phase S1.2 -- upgrade to Flowable 7.1.0
   - Status: Fix designed, not yet implemented

3. **No workflow uses RestServiceDelegate end-to-end** (ACTIVE)
   - Probability: N/A (known gap)
   - Impact: High (core architectural pattern unvalidated)
   - Mitigation: Phase S2.1 -- migrate CapEx v2 BPMN
   - Status: Planned

### Medium Risks

4. **Domain service consolidation breaks Flyway**
   - Probability: Medium
   - Impact: Medium (migration ordering conflicts)
   - Mitigation: Per-schema Flyway beans with separate migration directories
   - Status: Planned for Phase S3

5. **Keycloak JWT propagation across services**
   - Probability: Medium
   - Impact: High (auth failures in cross-service calls)
   - Mitigation: RestServiceDelegate should forward Authorization header
   - Status: Not yet addressed

6. **HR embedded Flowable removal breaks HR workflows**
   - Probability: Low (workflows are simple)
   - Impact: Medium (HR leave/onboarding stops working)
   - Mitigation: Test HR workflows after removal; if broken, route through Engine
   - Status: Planned for Phase S1.4

---

## Execution Priority

```
S1 (Critical Fixes)          [2-3 hours]  <-- START HERE
  S1.1 RestServiceDelegate       30 min
  S1.2 Flowable 7.1.0            1 hour
  S1.3 Dual package cleanup      1 hour
  S1.4 Remove unused Flowable    1 hour

S2 (BPMN Wiring)             [3-4 hours]
  S2.1 CapEx RestServiceDelegate  3-4 hours

S3 (Consolidation)           [1 day]      <-- OPTIONAL for MVP
  S3.1 Business service shell     2 hours
  S3.2 Move domain packages       2 hours
  S3.3 Consolidate infra          2 hours
  S3.4 Verify and clean up        2 hours

S4 (Frontend Completion)     [5-6 days]
  S4.1 Task Detail Page           2 days
  S4.2 Request Tracking           2 days
  S4.3 Dashboard                  1-2 days

S5 (Integration Testing)     [2-3 days]
  S5.1 CapEx end-to-end           1-2 days
  S5.2 Regression                 1 day
```

**Total to MVP**: S1 + S2 + S4 + S5 = ~10-12 days
**Total with consolidation**: S1 + S2 + S3 + S4 + S5 = ~11-13 days

---

## Team Structure

- **Developer** (1): All phases
- **Tech Lead** (0.25): Architecture decisions, code reviews

---

## Changelog

### 2026-02-26 (Architecture Review and Roadmap Reset)

- Independent codebase audit by principal engineer
- Identified RestServiceDelegate blocking issue (WebClient.block())
- Confirmed Flowable 7.0.1 compatibility issues were misdiagnosed
- Recommended Flowable 7.1.0 upgrade (Spring Boot 3.3.4 alignment)
- Proposed 3-service consolidation (Engine, Platform, Business)
- Created phased stabilization plan (S1-S5)
- Backed up previous ROADMAP.md to ROADMAP.backup.md

### 2025-12-30 (Previous: All Priority Issues Resolved)

- Fixed Flyway migration checksums for all 4 domain services
- Fixed BPMN workflows (capex, procurement, asset-transfer) with Groovy scripts
- Fixed admin-portal API connectivity and environment configuration
- Fixed BPMN rendering with layout coordinates
- Verified service registry (manual registration by design)
- Seeded 6 sample form schemas via V7 Flyway migration

### 2025-12-01 (Frontend UI Improvements)

- Service Registry error handling fix
- Forms page 6-step creation guide
- Process page 6-step creation guide

### 2025-11-30 (Phase 6 Week 4 + Phase 7 Completion)

- Task API endpoints (my-tasks, group-tasks) -- 20 tests
- Notification service (3 email types) -- 21 tests
- Process monitoring APIs (4 endpoints) -- 26 tests
- Form-js backend (8 endpoints, 5 form schemas)
- Build fixes: 100+ DTO compilation errors, 22 Flowable API compatibility fixes

### 2025-11-25 (Phase 5A RBAC Complete)

- Backend: RoleConfigService, WorkflowTaskRouter, DoAApprovalService
- Frontend: auth-context, use-authorization, ProtectedRoute, RoleBadges

---

**Document Maintained By**: Tech Lead
**Update Frequency**: Per phase completion
