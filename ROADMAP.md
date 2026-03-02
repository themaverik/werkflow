# Werkflow Implementation Roadmap

**Project**: Enterprise Workflow Automation Platform
**Last Updated**: 2026-02-26
**Maintained By**: Tech Lead

---

## How To Use This File

This file is the single source of truth for task tracking and session continuity.
Claude Code must read this file at the start of every session and update it after every completed task.
See `CLAUDE.md` Section 6 for full session continuity rules.

### Task Status Markers

| Marker | Status | Rule |
|--------|--------|------|
| `[ ]` | Pending | Not yet started |
| `[~]` | In Progress | Started but incomplete — note what remains |
| `[x]` | Completed | Done and committed — note the commit hash |
| `[!]` | Blocked | Cannot proceed — note the reason and what resolves it |

### Resume Instructions

1. Find the first `[~]` task — resume from the noted stopping point
2. If no `[~]` exists, find the first `[ ]` task in the lowest-numbered active phase
3. Run `git log --oneline -10` to confirm what was last committed
4. Ask the branch question before starting any work (see CLAUDE.md Git Push Policy)

---

## Current Session State

**Active Phase**: S3 — Domain Service Consolidation
**Current Task**: S3.6 — Update Architecture Documentation
**Last Commit**: S3.6 complete (see git log)
**Stopped At**: S3.6 complete; S3 fully done

> **Decision (2026-03-01)**: S3 moved before S5. Doing S5 first would waste ~60-70% effort
> since S3 rewrites Docker Compose, service URLs, and package structure — all of which S5
> tests against. S3 first means S5 runs once against the final architecture.

> Update this section at the start and end of every session.

---

## Project Health

**System Status**: Architecture review complete. Stabilization in progress.
**Last Audit**: 2026-02-26 — Principal engineer independent codebase review.

### Audit Findings

| Documented Claim | Actual State |
|------------------|-------------|
| All priorities resolved | Dual package trees still exist in Finance and Procurement |
| BPMN migrated to RestServiceDelegate | Zero Engine BPMN files use RestServiceDelegate; v2 uses inline Groovy HTTP |
| RestServiceDelegate pattern established | Delegate exists but uses `WebClient.block()` — thread starvation risk |
| Service Registry verified | Flyway seed data only; no runtime registration or health checks |
| Flowable 7.0.1 compatibility issues | Most were misdiagnosed — wrong service interface, not missing API |

### Infrastructure Status

| Component | Port | Status |
|-----------|------|--------|
| Flowable Engine Service | 8081 | Healthy — 62 database tables, 30 BPMN workflows deployed |
| Admin Service | 8083 | Healthy |
| Keycloak Authentication | 8090 | Healthy |
| PostgreSQL | 5433 | Healthy — 6 schemas |
| HR, Finance, Procurement, Inventory | 8082-8086 | Flyway fixed — awaiting deployment verification |

### Service Inventory

| Service | Port | Flowable Dep | Flowable Usage | Action Required |
|---------|------|-------------|----------------|-----------------|
| Engine | 8081 | Required | Primary orchestrator | None |
| Admin | 8083 | None | None | None |
| HR | 8082 | Unnecessary | 1 file — embedded WorkflowService | Remove in S1.4 |
| Finance | 8084 | Unnecessary | 0 files | Remove in S1.4 |
| Procurement | 8085 | Unnecessary | 1 file — ProcessVariableInjector | Remove in S1.4 |
| Inventory | 8086 | Unnecessary | 0 files | Remove in S1.4 |

---

## Execution Order

```
S1 — Critical Fixes         [2-3 hours]   <-- ACTIVE
  S1.1 RestServiceDelegate      30 min    <-- START HERE
  S1.2 Flowable 7.1.0           1 hour
  S1.3 Dual package cleanup     1 hour
  S1.4 Remove unused Flowable   1 hour

S2 — BPMN Wiring            [3-4 hours]   prerequisite: S1.1
  S2.1 CapEx RestServiceDelegate

S3 — Consolidation          [3-4 days]    prerequisite: S1.3, S1.4 — ACTIVE
  S3.1 Business service shell            COMPLETED (cc613ab)
  S3.2 Move domain packages             COMPLETED (5893a8f)
  S3.3 Consolidate infrastructure        COMPLETED (1d65d52)
  S3.4 Verify and clean up              COMPLETED
  S3.5 Frontend consolidation            <-- single app, module-based
  S3.6 Update architecture docs          COMPLETED

S4 — Frontend Completion    [5-6 days]    COMPLETED
  S4.1 Task Detail Page
  S4.2 Request Tracking Page
  S4.3 Dashboard

S5 — Integration Testing    [2-3 days]    prerequisite: S1, S2, S3
  S5.0 Pre-deployment fixes              COMPLETED (commit: 1d6b510)
  S5.1 CapEx end-to-end                 <-- Docker pre-flight required
  S5.2 Regression

Total to MVP:   ~11-13 days
```

---

## Active Phases

### Phase S1 — Critical Fixes

**Goal**: Fix verified technical debt before any new feature work.
**Duration**: 2-3 hours
**Prerequisite**: None
**Status**: COMPLETED

---

#### S1.1 — Fix RestServiceDelegate Blocking Issue

**File**: `shared/delegates/src/main/java/com/werkflow/delegates/rest/RestServiceDelegate.java`
**Problem**: Uses `WebClient.block()` inside a Flowable `JavaDelegate`. Flowable's async executor thread pool is not designed for blocking — causes thread starvation under load.
**Fix**: Replace `WebClient` with Spring 6.1 `RestClient` (synchronous, available in Spring Boot 3.3.x).

```java
// Replace WebClient with RestClient
private final RestClient restClient;

public RestServiceDelegate(RestClient.Builder builder) {
    this.restClient = builder.build();
}

// In execute():
ResponseEntity<Map> response = restClient
    .method(HttpMethod.valueOf(method.toUpperCase()))
    .uri(url)
    .headers(h -> { if (headers != null) headers.forEach(h::add); })
    .body(body)
    .retrieve()
    .toEntity(Map.class);
execution.setVariable(responseVariable, response.getBody());
```

Tasks:
- [x] Replace `WebClient` with `RestClient` in constructor and `execute()` method
- [x] Remove `spring-webflux` dependency from `shared/delegates/pom.xml` (replaced with `spring-boot-starter-web`)
- [x] Remove `reactor-core` import
- [x] Verify field injection still works with `RestClient` (Expression fields unchanged, compile passes)
- [x] Run existing delegate tests (no tests exist yet; compilation verified)
- [x] Also migrated `FormRequestDelegate` from `WebClient.block()` to `RestClient`

---

#### S1.2 — Fix Flowable Version Alignment

**Problem**: Flowable 7.0.1 was built against Spring Boot 3.1.6; project uses Spring Boot 3.3.2. Flowable 7.1.0 targets Spring Boot 3.3.4 — zero code changes required for this upgrade.
**Fix**: Bump `<flowable.version>` to 7.1.0 across all pom.xml files.

**Important context**:
- Do NOT upgrade to 7.2.0 — changes REST date formats to UTC, breaks frontend date parsing
- The `createJobQuery()` "compatibility issue" was misdiagnosed — method exists on `ManagementService`, not `RuntimeService`
- All `TaskInfo`, `HistoricTaskInstance`, `HistoricProcessInstance` interfaces are unchanged between 7.0.1 and 7.1.0

Tasks:
- [x] Update `flowable.version` to 7.1.0 in all 6 service `pom.xml` files and `shared/delegates/pom.xml`
- [x] Verify compilation with 7.1.0 (engine and delegates compile clean)
- [x] Fix `IntegrationTestBase`: replace `Thread.sleep()` hack with `managementService.createJobQuery().count()` polling
- [x] Fix redundant date conversion in `TaskService.java`: `.toInstant().atZone().toInstant()` -> `.toInstant()`
- [x] Run full test suite (unit tests pass; integration tests require running PostgreSQL)

---

#### S1.3 — Delete Dual Package Trees

**Problem**: Finance and Procurement each have two coexisting package structures from a mid-development Flowable version migration. Creates ambiguous component scanning and duplicate bean risks.

**Finance** — keep `com.werkflow.finance.*`, delete `com.werkflow.*`:
- `com.werkflow.controller/`, `com.werkflow.dto/`, `com.werkflow.entity/`, `com.werkflow.repository/`, `com.werkflow.service/`, `com.werkflow.config/`

**Procurement** — keep `com.werkflow.procurement.*`, delete `com.werkflow.*`:
- Same pattern — old `VendorController`, `ProcurementController` and supporting classes

Tasks:
- [x] Verify Finance `@SpringBootApplication` scans `com.werkflow.finance` as base package (default scan, no explicit scanBasePackages)
- [x] Delete legacy `com.werkflow.*` packages from Finance (20 files: config, controller, dto, entity, repository, service)
- [x] Verify Procurement `@SpringBootApplication` scans `com.werkflow.procurement` as base package (default scan)
- [x] Delete legacy `com.werkflow.*` packages from Procurement (23 files: config, controller, dto, entity, repository, service)
- [x] Compile and verify both services start cleanly (both compile clean after deleting CapExWorkflowController which depended on legacy types)
- [x] Check BPMN files for any `delegateExpression` referencing old bean names from deleted classes (none found — all BPMN refs point to correct packages)
- [x] Deleted CapExWorkflowController (used legacy CapExService/DTOs/entities) — must be rewritten in S2.1 against new Finance domain model

---

#### S1.4 — Remove Unnecessary Flowable Dependencies

**Problem**: 4 domain services include Flowable dependencies they do not use. HR has one embedded `WorkflowService` — all workflow execution must go through the Engine service.

Tasks:
- [x] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from Finance `pom.xml`; deleted `delegate/` package (BudgetAvailabilityDelegate, NotificationDelegate)
- [x] Remove `flowable-engine` from Inventory `pom.xml`; deleted `delegate/` package (InventoryAvailabilityDelegate, ReservationDelegate)
- [x] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from Procurement `pom.xml`; deleted `delegate/PurchaseOrderCreationDelegate` and `config/ProcessVariableInjector`
- [x] Remove `flowable-spring-boot-starter` and `flowable-spring-boot-starter-rest` from HR `pom.xml`; deleted entire `com.werkflow.workflow.*` package (8 delegates + WorkflowService) and `com.werkflow.config/`
- [x] Removed Flowable config from application.yml (Finance, Procurement, HR) and application-test.yml (HR)
- [x] Removed unused `flowable.version` property from all 4 service pom.xml files
- [x] Compile and verify all 4 services compile without Flowable (all pass)
- [ ] Test HR leave approval still works via Engine service after removal (requires running Docker — deferred to S5)

---

### Phase S2 — BPMN Wiring

**Goal**: Make at least one workflow use RestServiceDelegate end-to-end.
**Duration**: 3-4 hours
**Prerequisite**: S1.1 must be complete
**Status**: COMPLETED

---

#### S2.1 — Migrate CapEx Approval Process v2 to RestServiceDelegate

**File**: `services/engine/src/main/resources/processes/capex-approval-process-v2.bpmn20.xml`
**Problem**: v2 uses inline Groovy scripts with `HttpURLConnection`. No connection pooling, no retry, no JWT propagation.
**Fix**: Rewrite all service tasks to use `restServiceDelegate` with `<flowable:field>` injection.

```xml
<serviceTask id="checkBudget" name="Check Budget"
    flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:string>${financeServiceUrl}/api/budget/check</flowable:string>
    </flowable:field>
    <flowable:field name="method"><flowable:string>POST</flowable:string></flowable:field>
    <flowable:field name="responseVariable"><flowable:string>budgetCheckResponse</flowable:string></flowable:field>
  </extensionElements>
</serviceTask>
```

Tasks:
- [x] Map all Groovy HTTP tasks in `capex-approval-process-v2.bpmn20.xml` (5 script tasks: createCapExRequest, checkBudget, updateApproved, reserveBudget, updateRejected)
- [x] Rewrite each as `<serviceTask flowable:delegateExpression="${restServiceDelegate}">` with `<flowable:field>` configuration
- [x] Added extract response script tasks to unpack Map response into individual process variables (capexId, requestNumber, budgetAvailable)
- [x] Created Finance workflow endpoints: `POST /api/workflow/capex/create-request`, `POST /api/workflow/capex/check-budget`, `PUT /api/workflow/capex/update-status`, `POST /api/workflow/capex/allocate` (MVP stubs in CapExWorkflowController + CapExWorkflowService)
- [x] Added JWT propagation to RestServiceDelegate via `authorizationToken` process variable (auto-forwards as Authorization header)
- [x] Fixed ProcessVariableInjector @Value property paths to match application.yml (`app.services.finance.url` not `app.services.finance-url`)
- [x] Added camelCase variable injection (e.g. `financeServiceUrl`) alongside snake_case (`finance_service_url`) for BPMN expression compatibility
- [x] All 3 modules compile clean (delegates, finance, engine)
- [ ] Deploy and test execution with DOA approval scenario (requires running Docker — deferred to S5)

---

### Phase S3 — Domain Service Consolidation

**Goal**: Merge HR, Finance, Procurement, Inventory into a single `business` service.
**Duration**: 1 day
**Prerequisite**: S1.3 and S1.4 must be complete (done)
**Status**: IN PROGRESS

**Target architecture**:
```
services/
  engine/     # Flowable BPM orchestration (8081) — unchanged
  platform/   # Admin renamed (8083) — service registry, RBAC, user/org mgmt
  business/   # Merged domain service (8084) — HR, Finance, Procurement, Inventory
```

**Rollback strategy**: Package boundaries are preserved — individual services can be extracted back out if needed.

---

#### S3.1 — Create Business Service Shell

Tasks:
- [x] Create `services/business/` with `pom.xml` consolidating dependencies from all 4 domain services
- [x] Create `BusinessServiceApplication.java` with `@SpringBootApplication(scanBasePackages = "com.werkflow.business")`
- [x] Create single `SecurityConfig.java` (all domain configs are identical)
- [x] Create single `application.yml` with `spring.flyway.enabled: false`
- [x] Create per-schema Flyway configuration beans for each domain schema

---

#### S3.2 — Move Domain Packages

Tasks:
- [x] Move `com.werkflow.hr.*` → `com.werkflow.business.hr.*`
- [x] Move `com.werkflow.finance.*` → `com.werkflow.business.finance.*`
- [x] Move `com.werkflow.procurement.*` → `com.werkflow.business.procurement.*`
- [x] Move `com.werkflow.inventory.*` → `com.werkflow.business.inventory.*`
- [x] Move Flyway migrations into per-domain directories
- [x] Update all import statements and compile

---

#### S3.3 — Consolidate Infrastructure

> **Docker pre-flight check required** — verify Docker is running before any `docker` or `docker compose` command (see CLAUDE.md Section 5).

Tasks:
- [x] Docker pre-flight check *(skipped -- no Docker commands run, config-only changes)*
- [x] Update Docker Compose: remove 4 domain containers, add 1 `business` container
- [x] Update Engine service URLs: 4 hosts → 1 host (business-service:8084)
- [x] Update frontend API client: all env files and next.config.mjs updated
- [ ] Optionally rename `services/admin/` to `services/platform/` *(deferred -- not blocking)*

---

#### S3.4 — Verify and Clean Up

> **Docker pre-flight check required** before running containers.

Tasks:
- [x] Docker pre-flight check *(Docker running, infra verified)*
- [x] Add `@Table(schema = "...")` annotations to all 24 entity classes across 4 domains
- [x] Recreate procurement Flyway migration to match entity definitions (entities had different schema than V1 migration)
- [x] Verify Flyway migrations run per schema without conflicts *(all 4 schemas migrated successfully)*
- [x] Verify all endpoints respond correctly via `business` service *(health UP, all domain endpoints return 401 — correct, auth required)*
- [x] Verify Engine service starts and reaches business service *(both services UP, engine URLs point to localhost:8084)*
- [x] Fix capex-approval-process-v2.bpmn20.xml: replace invalid JUEL map literals with Groovy script tasks
- [x] Verify `admin-portal` builds successfully against new endpoint layout *(npm run build passes)*
- [x] Delete old `services/hr/`, `services/finance/`, `services/procurement/`, `services/inventory/` *(commit: b802d08, backed up to archive/pre-consolidation/)*
- [x] Verify Docker Compose dev config (`docker-compose.dev.yml`) *(infrastructure-only, no changes needed)*

---

#### S3.5 — Frontend Consolidation (Module-Based Single App)

> **Decision (2026-03-01)**: Consolidate `admin-portal` + `hr-portal` into a single
> Next.js app (`frontends/portal/`) with route-group modules. Mirrors the backend
> consolidation. Industry standard for ERP platforms (SAP, Oracle, Workday).
> Keeps things simple, scalable, and reliable.

**Architecture**:
- Single Next.js app with route groups per domain
- Shared auth (one Keycloak client), shared layout, shared components
- Role-based module visibility via Keycloak roles
- Single API client pointing to business-service on port 8084
- Next.js code splitting per route group = no performance penalty

**Proposed structure**:
```
frontends/portal/
  app/
    (auth)/login/                    -- shared Keycloak auth
    (platform)/                      -- admin/platform features (from admin-portal studio)
      dashboard/
      processes/
      forms/
      services/
      monitoring/
      analytics/
    (hr)/                            -- HR module (from hr-portal + admin-portal portal)
      employees/
      leave/
      attendance/
      payroll/
      performance/
    (finance)/                       -- Finance module
      budgets/
      expenses/
      capex/
    (procurement)/                   -- Procurement module
      vendors/
      purchase-requests/
      purchase-orders/
      receipts/
    (inventory)/                     -- Inventory module
      assets/
      transfers/
      maintenance/
  components/
    shared/                          -- shared UI components (sidebar, header, notifications)
    hr/                              -- HR-specific components
    finance/
    procurement/
    inventory/
  lib/
    api/                             -- single API client to business-service
    auth/                            -- Keycloak auth config (one client)
```

**Navigation**: Sidebar with module sections, role-gated:
- Admin/Super Admin: Platform + all modules
- HR Manager: HR module + related
- Finance: Finance + Procurement
- Employee: HR self-service only

Tasks:
- [x] Copy admin-portal as base, restructure into `frontends/portal/`
- [x] Update package.json (name, port 4000, HR deps), next.config.mjs (rewrites, app name), env files
- [x] Restructure route groups: `(studio)` -> `(platform)`, create `(hr)`, `(finance)`, `(procurement)`, `(inventory)`, `(auth)`
- [x] Create sidebar component with role-gated sections (General, Studio, HR, Finance, Procurement, Inventory, System)
- [x] Create app-shell layout (header + sidebar + content area), used by all route group layouts
- [x] Simplify middleware (84 -> 40 lines, legacy URL redirects, simple auth gate)
- [x] Update auth.config.ts with new route patterns (dashboard, hr, finance, procurement, inventory)
- [x] Migrate 5 HR pages from hr-portal into `(hr)/hr/` with shadcn Card/Button
- [x] Create placeholder pages for finance, procurement, inventory modules
- [x] Fix all `/studio/` and `/portal/` link references across 15+ files
- [x] `npm install && npm run build` -- zero errors, 26 pages
- [ ] Update Docker Compose and Dockerfile for single `portal` frontend *(deferred -- separate step)*

---

#### S3.6 — Update Architecture Documentation

> All docs in `docs/` must reflect the consolidated architecture post-S3.

Tasks:
- [x] Update `docs/Architecture/Workflow-Architecture-Design.md` -- rewritten for 3-service topology
- [x] Update `docs/Architecture/Delegate-Architecture-Analysis.md` -- simplified, URLs point to business-service:8084
- [x] Update `docs/Deployment/Deployment-Configuration-Guide.md` -- rewritten with new Docker Compose, env vars
- [x] Update `docs/Development/API-Path-Structure.md` -- updated frontend integration section
- [x] Update `docs/Security/Keycloak-Implementation-Guide.md` -- werkflow-portal client, single frontend
- [x] Update `docs/OAuth2/OAuth2-Setup-Guide.md` -- werkflow-portal client, removed HR portal client
- [x] Archive 32 stale phase docs to `docs/archive/`
- [x] Update `docs/README.md` -- rewritten as consolidated documentation index

---

### Phase S4 — Frontend Completion

**Goal**: Complete the task management UI wiring existing backend APIs to frontend.
**Duration**: 5-6 days
**Prerequisite**: S1 complete
**Status**: COMPLETE

> **Brainstorm required** before starting each sub-phase — apply the feature brainstorm protocol from CLAUDE.md Section 4. Wait for user sign-off before writing any code.

**Context**: Backend APIs exist but frontend components are incomplete. `ApprovalPanel`, `DOAIndicator`, and `DelegationModal` exist but have no host page.

---

#### S4.1 — Task Detail Page (2 days)

Tasks:
- [x] Brainstorm *(incremental extraction approach confirmed)*
- [x] Create `TaskDetailsPage` component *(existing page enhanced, not rewritten)*
- [x] Wire `ApprovalPanel`, `DOAIndicator`, `DelegationModal` into page *(already wired from prior work)*
- [x] Load task data from `GET /api/v1/tasks/{taskId}` *(useTask hook)*
- [x] Load task form from `GET /api/v1/tasks/{taskId}/form` *(useTaskFormData hook)*
- [x] Render form-js form with data binding *(FormSection + FormJsViewer)*
- [x] Submit form via `POST /api/tasks/{taskId}/form/submit` *(submitTaskForm API + handleFormSubmit)*
- [x] Show process timeline from `GET /workflows/processes/{id}/history` *(ProcessTimeline component)*
- [x] Handle claim, unclaim, and complete actions *(unclaim added this session)*
- [x] Code review fixes: removed no-op form submit button, fixed API URL, memoized isApproval, added hasForm guard, lazy ProcessTimeline rendering

---

#### S4.2 — Request Tracking Page (2 days)

Tasks:
- [x] Brainstorm *(standalone route at /requests confirmed)*
- [x] Create `MyRequestsPage` component *(requests/page.tsx with table, filters, search)*
- [x] List user's submitted process instances *(getAllWorkflowInstances via React Query)*
- [x] Add status filters: active, completed, suspended *(Tabs component with status query)*
- [x] Add search by business key *(client-side Input filter)*
- [x] Show process timeline on detail page *(reuses ProcessTimeline component)*
- [x] Wire to Process Monitoring APIs *(getProcessInstance, getTasksByProcessInstance)*

---

#### S4.3 — Dashboard (1-2 days)

Tasks:
- [x] Brainstorm *(standalone page at /dashboard confirmed)*
- [x] Create `DashboardPage` component *(dashboard/page.tsx)*
- [x] Pending tasks count widget *(My Tasks stat card via useTaskSummary)*
- [x] Team tasks widget *(Team Tasks + Overdue + High Priority stat cards)*
- [x] Recent activity feed *(getActivityLogs with type-based icons)*
- [x] Quick action buttons *(View My Tasks, My Requests, Start New Process)*
- [x] Code review fixes: added error states for summary and activity queries

#### S4 — Cross-cutting Review Fixes

- [x] Extracted shared `formatDate`/`formatDateTime` utilities into `lib/utils/format.ts` (removed duplication across 3 files)
- [x] Fixed `submitTaskForm` API URL from `/api/tasks/` to `/api/v1/tasks/`
- [x] Added `processHistory` to `TASK_QUERY_KEYS` factory in `useTasks.ts`
- [x] Removed dead `statusBadgeVariant` function and unused `Badge` import from `ProcessTimeline.tsx`
- [x] Fixed `Math.random()` key to stable `activity-${index}` in ProcessTimeline
- [x] Added TODO documenting requests page data scoping limitation

---

### Phase S5 — Integration Testing

**Goal**: Verify CapEx workflow works end-to-end with no regressions.
**Duration**: 2-3 days
**Prerequisite**: S1 and S2 complete
**Status**: IN PROGRESS

---

#### S5.0 — Pre-deployment Fixes

Tasks:
- [x] Fix double `/api` path in Finance controllers — removed `/api` prefix from 6 controllers (context-path already provides it)
- [x] Add `FINANCE_SERVICE_URL=http://finance-service:8084` to `.env.engine`
- [x] Verify Finance service compiles after path fix

---

#### S5.1 — CapEx End-to-End (1-2 days)

> **Docker pre-flight check required** — verify Docker is running before deploying.

Tasks:
- [ ] Docker pre-flight check *(run: `docker info` — stop and prompt user if not running)*
- [ ] Deploy all services in Docker
- [ ] Authenticate via Keycloak, obtain JWT
- [ ] Start CapEx process via Engine API
- [ ] Verify RestServiceDelegate calls Finance service correctly
- [ ] Verify task routing by DOA level
- [ ] Complete approval task
- [ ] Verify notification sent
- [ ] Verify process history recorded
- [ ] Test all 4 DOA scenarios ($500, $7.5K, $75K, $250K)

---

#### S5.2 — Regression (1 day)

Tasks:
- [ ] Verify HR leave approval workflow (via Engine after S1.4 removes embedded Flowable)
- [ ] Verify Procurement PR-to-PO workflow
- [ ] Verify all API endpoints return correct data
- [ ] Verify Keycloak RBAC enforcement on endpoints
- [ ] Verify form-js form load and submit

---

## Completed Phases

### Phase 0 — Foundation and Planning

- [x] Architecture decision records
- [x] Monorepo structure finalized
- [x] Technology stack selected (Flowable 7.0.x, Spring Boot 3.3.x, Next.js 14)
- [x] Docker Compose infrastructure
- [x] CI/CD pipelines (GitHub Actions)

### Phase 1 — Core Platform Foundation

- [x] Flowable engine service deployed with PostgreSQL
- [x] OAuth2/JWT authentication via Keycloak
- [x] Shared delegates module (RestServiceDelegate)
- [x] HR service migration with workflows

### Phase 2 — Department Services

- [x] Finance service (Budget, CapEx, Approvals)
- [x] Procurement service (Vendors, PR, PO)
- [x] Inventory service (Assets, Stock, Transfers)
- [x] Kafka topic definitions and event schemas

### Phase 3 — Admin Portal and Governance (70% Complete)

- [x] Admin Portal React application (Next.js 14)
- [x] BPMN designer integration (bpmn-js)
- [x] Form builder interface (form-js)
- [x] User and role management UI
- [x] RestServiceDelegate with field injection pattern
- [x] Service Registry frontend UI
- [ ] BpmnDesigner integration with ServiceTaskPropertiesPanel — deferred post-MVP
- [ ] Process Variable Manager — deferred post-MVP
- [ ] End-to-end integration testing — covered in S5

### Phase 5A — Keycloak RBAC

- [x] RoleConfigService with route-based access control
- [x] WorkflowTaskRouter with DOA routing (4 levels)
- [x] DoAApprovalService with escalation chain
- [x] TaskAssignmentDelegate for BPMN task routing
- [x] KeycloakUserService for user/role queries
- [x] auth-context.tsx (Keycloak + NextAuth)
- [x] use-authorization.ts (permission hooks)
- [x] ProtectedRoute, RoleBasedNav, RoleBadges components
- [x] 403 access denied page

### Phase 6 Week 4 — Task APIs and Notifications

- [x] Task API endpoints (my-tasks, group-tasks) with pagination and filtering — 20 tests
- [x] NotificationService with @Async and @Retryable — 21 tests
- [x] Process Monitoring APIs (4 endpoints) — 26 tests
- [x] Total: 67 tests passing, 0 build errors

### Phase 7 — Form-js Backend Integration

- [x] FormSchemaService, TaskFormService, FormSchemaValidator
- [x] 8 REST endpoints for form CRUD and task form operations
- [x] Database migration (form_schemas table with versioning)
- [x] 5 form schemas (CapEx request/approval, Leave request/approval, Purchase requisition)

---

## Deferred — Post-MVP

| Item | Reason Deferred | When to Revisit |
|------|----------------|-----------------|
| Service Registry backend | Frontend complete; hardcoded URLs sufficient for MVP | When deploying to multiple environments |
| BPMN Designer no-code enhancement (Phase 3.7 remaining) | 70% done; remaining work non-blocking | After MVP, for citizen developer self-service |
| Phase 8 Options A and B | Option C complete; A and B are enhancements | Post-MVP |
| Remaining BPMN migrations (Procurement, Asset Transfer, HR) | Only CapEx targeted to prove the pattern | After CapEx proves end-to-end |
| Load testing and security hardening | Pre-production concern | Before production deployment |
| PostgreSQL sequences for request numbers | `System.currentTimeMillis()` sufficient for single dev | Phase 4 cleanup |
| Inventory DTO layer | Currently leaks JPA entities | Phase 4 cleanup |

---

## Risk Register

| # | Risk | Probability | Impact | Mitigation | Status |
|---|------|------------|--------|------------|--------|
| 1 | RestServiceDelegate thread starvation | High under load | Engine unresponsive | S1.1 — replace WebClient.block() with RestClient | Fix designed, not implemented |
| 2 | Flowable/Spring Boot version mismatch | Low (works today) | Subtle runtime failures | S1.2 — upgrade to Flowable 7.1.0 | Fix designed, not implemented |
| 3 | No workflow uses RestServiceDelegate end-to-end | Known gap | Core pattern unvalidated | S2.1 — migrate CapEx v2 BPMN | Planned |
| 4 | Consolidation breaks Flyway migration ordering | Medium | Migration conflicts | Per-schema Flyway beans with separate directories | Planned for S3 |
| 5 | Keycloak JWT not propagated across services | Medium | Auth failures in cross-service calls | RestServiceDelegate must forward Authorization header | Not yet addressed — add to S2.1 |
| 6 | HR embedded Flowable removal breaks HR workflows | Low | HR leave/onboarding stops | Test HR workflows after S1.4; route through Engine if broken | Planned for S1.4 |

---

## Flowable Version Reference

| Factor | 7.0.1 (current) | 7.1.0 (target) | 7.2.0 (skip) |
|--------|-----------------|-----------------|---------------|
| Spring Boot target | 3.1.6 | 3.3.4 | Unknown |
| Liquibase dep | Present | Removed | Removed |
| API changes | Baseline | Additive only | REST dates changed to UTC |
| Risk | Known quirks | Low | Medium — UTC date change breaks frontend |

**Known API facts**:
- `Expression` class moved from `org.flowable.engine.delegate` to `org.flowable.common.engine.api.delegate` (6.x to 7.x — already handled)
- `createJobQuery()` is on `ManagementService`, NOT `RuntimeService`
- All date methods return `java.util.Date` across all 7.x versions

---

## Changelog

### 2026-02-26 — Architecture Review and Roadmap Reset

- Independent codebase audit by principal engineer
- Identified RestServiceDelegate blocking issue (WebClient.block())
- Confirmed Flowable 7.0.1 compatibility issues were misdiagnosed
- Recommended Flowable 7.1.0 upgrade (Spring Boot 3.3.4 alignment)
- Proposed 3-service consolidation (Engine, Platform, Business)
- Reformatted ROADMAP.md for Claude Code session continuity
- Added Docker pre-flight check markers to S3 and S5
- Added brainstorm markers to S4 sub-phases
- Created phased stabilization plan (S1-S5)

### 2025-12-30 — All Priority Issues Resolved (Previous)

- Fixed Flyway migration checksums for all 4 domain services
- Fixed BPMN workflows with Groovy scripts
- Fixed admin-portal API connectivity
- Seeded 6 sample form schemas via V7 Flyway migration

### 2025-11-30 — Phase 6 Week 4 and Phase 7 Completion

- Task API endpoints, notification service, process monitoring APIs — 67 tests
- Form-js backend: 8 endpoints, 5 form schemas

### 2025-11-25 — Phase 5A RBAC Complete

- Backend: RoleConfigService, WorkflowTaskRouter, DoAApprovalService
- Frontend: auth-context, use-authorization, ProtectedRoute, RoleBadges
