# Workflow Guide

What Werkflow runs today, how processes reach external systems, and which HTTP surfaces are
current. For authoring your first process end to end, start with
[First-Workflow-Tutorial.md](../First-Workflow-Tutorial.md).

## Status

Tagged `v1.0.0`. The engine, admin service and portal are the shipped platform; everything below
describes code present in this repository, not a roadmap.

One structural change shapes the rest of this document. Before S19, each integration had its own
Java delegate — 21 of them, one per domain operation. Those were deleted and replaced by a single
`ExternalApiCallDelegate` that resolves its target from the connector registry at runtime. Adding
an integration is now configuration, not a class.

## Services

| Service | Port | Role |
|---------|------|------|
| Engine | 8081 | Flowable BPM orchestration, process and task APIs, DMN, form schemas |
| Admin | 8083 | Connector registry, users, organisations, departments, custody mappings, routes |
| Portal | 4000 | Next.js frontend — BPMN designer, DMN editor, form builder, task inbox, admin UI |
| Keycloak | 8090 | OAuth2/OIDC identity provider |
| PostgreSQL | 5433 | Engine and admin schemas |
| Mailpit | 8025 | Email sandbox, development only |

`docker-compose.yml` starts exactly these. There is no business or ERP service in the running
stack — see [Business data](#business-data) below.

## Example processes

Four BPMN processes ship in `services/engine/src/main/resources/processes/examples/` and deploy on
startup when `WERKFLOW_DEPLOY_EXAMPLES=true`.

| Process key | Shape |
|-------------|-------|
| `general-approval` | 3 user tasks, 3 exclusive gateways — multi-level approval with routing |
| `onboarding-checklist` | 4 user tasks, 1 service task (`${emailActionDelegate}`) |
| `leave-request` | 1 user task, 1 exclusive gateway |
| `event-ticket-request` | 1 user task, 1 exclusive gateway |

Two DMN decisions back them: `general_approval_routing` and `leave_approval`, in
`services/engine/src/main/resources/dmn/`.

These are starting points to copy, not a fixed catalogue. Processes are authored in the portal
designer and deployed at runtime without a rebuild.

## Delegates

Three delegate beans exist. Everything else is configuration.

| Expression | Purpose |
|------------|---------|
| `${externalApiCallDelegate}` | All outbound REST calls, resolved through the connector registry |
| `${emailActionDelegate}` | Notification email against a template |
| `${processCallDelegate}` | Starts another process |

`TaskAssignmentDelegate` sits alongside them but is a Flowable `TaskListener`, not a service-task
delegate — it resolves assignment when a user task is created.

There is no `RestServiceDelegate` and no hard-coded service URL anywhere in the engine. A service
task names a connector key; the engine resolves base URL and credentials server-side at execution
time, through an SSRF guard.

## Business data

Business domain data belongs to your systems of record, reached through registered connectors.
The deployed platform holds process state and nothing else.

The repository does contain `services/business/` — an HR, finance, inventory and procurement module
with its own Flyway migrations. It is **not** built into the Docker image and **not** present in
`docker-compose.yml`; it was decoupled in S20 and is dormant in the tree. Treat it as a reference
data source for exercising connectors, not as part of the platform.

The same applies to the sandbox ERP used by the examples and tests: a stand-in system of record for
demonstration and testing, not a production ERP.

## HTTP API

The engine serves no servlet context path, so these are absolute. Several controllers publish more
than one path for backward compatibility; the `/api/...` form is the current one.

| Area | Current path | Also mapped |
|------|--------------|-------------|
| Process definitions | `/api/process-definitions` | `/process-definitions` |
| Process instances | `/api/process-instances` | — |
| Tasks | `/api/tasks` | `/api/v1/tasks` |
| Form schemas | `/api/forms` | `/forms`, `/api/v1/form-schemas` |
| DMN decisions | `/api/v1/dmn/decisions` | — |
| Groups | `/api/groups` | `/groups` |
| DOA thresholds | `/api/doa-thresholds` | — |
| Notification templates | `/api/notification-templates` | `/notification-templates` |
| Process drafts | `/api/process-drafts` | — |
| Service registry | `/api/services` | — |
| Audit logs | `/api/admin/audit-logs` | — |
| Process monitoring | `/workflows/processes` | — |
| Task monitoring | `/workflows/tasks` | — |

Admin service: `/api/connectors`, `/api/users`, `/api/organizations`, `/api/departments`,
`/api/roles`, `/api/routes`, `/api/custody-mappings`, plus `/api/internal/tenants` and
`/api/internal/users` for service-to-service calls.

Swagger UI is served per service at `/swagger-ui.html`.

## Identity and routing

Keycloak (realm `werkflow`) is the source of truth for identity. Two clients exist:
`werkflow-engine` and `werkflow-portal`.

Roles are semantic rather than per-process — `department_head`, `finance_manager`,
`procurement_approver`, `doa_approver_level1` through `level4`, `inventory_manager`,
`hr_manager`, `super_admin` and others. Six department groups ship in the realm: Finance, HR, IT,
Inventory Warehouse, Procurement and Transport.

Processes route to roles and groups, not to named people. Delegation of authority thresholds live
in the engine behind `/api/doa-thresholds`; custody mappings live in the admin service behind
`/api/custody-mappings`.

## Portal

Routes under `app/(platform)/`:

- **Authoring** — `processes` (BPMN designer, with `new`, `edit`, `start`), `decisions` (DMN
  editor), `forms` (form-js builder, with `preview`)
- **Operations** — `tasks` inbox, `requests`, `dashboard`, `monitoring`, `analytics`
- **Configuration** — `services` (connector registry and endpoints)

## Adding an integration

The connector model means no Java changes for a new external call.

1. Register the connector in the portal under **Services**, giving it a key, base URL and auth
   profile. Credentials are resolved server-side and never travel to the browser.
2. In the BPMN designer, add a service task and set its Action Block to `CONNECTOR_OPERATION`. The
   designer writes `${externalApiCallDelegate}` and the connector field for you.
3. Deploy from the designer. No rebuild, no restart.

The external system is not modified and does not know a workflow exists. Point the same connector
at a different ERP and the process definition is unchanged.

## Verifying a deployment

```bash
curl localhost:8081/api/process-definitions          # deployed processes
curl localhost:8081/api/process-instances            # running instances
curl localhost:8081/api/tasks                        # open tasks
curl localhost:8083/api/connectors                   # registered connectors
```

All require a bearer token from Keycloak.

## Related

- [First-Workflow-Tutorial.md](../First-Workflow-Tutorial.md) — author and run a process
- [Connector-Guide.md](../Connector-Guide.md) — register and use connectors
- [Workflow-Architecture-Design.md](Workflow-Architecture-Design.md) — component and data-flow view
- [BPMN-Quick-Reference-Guide.md](../BPMN-Quick-Reference-Guide.md) — element reference
- [Deployment-Configuration-Guide.md](../Deployment-Configuration-Guide.md) — environment and config
