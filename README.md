<p align="center">
  <img src="./frontends/portal/public/werkflow-logo.png" alt="Werkflow" width="200" />
</p>

<h1 align="center">Werkflow</h1>

<p align="center">
  <strong>Business process automation with contracts.</strong><br />
  A deterministic BPMN engine, a governed connector model, and cross-artifact
  verification that catches broken process bundles before they deploy.
</p>

<p align="center">
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License: Apache 2.0" /></a>
  <img src="https://img.shields.io/badge/java-21-orange.svg" alt="Java 21" />
  <img src="https://img.shields.io/badge/node-20-green.svg" alt="Node 20" />
  <img src="https://img.shields.io/badge/flowable-7.2.0-brightgreen.svg" alt="Flowable 7.2.0" />
  <img src="https://img.shields.io/badge/docker-required-blue.svg" alt="Docker required" />
  <a href="./CONTRIBUTING.md"><img src="https://img.shields.io/badge/contributions-welcome-brightgreen.svg" alt="Contributions welcome" /></a>
</p>

---

A business process is never one file. It is a BPMN diagram, a set of DMN decision
tables, the forms people fill in, and the connectors that reach outside the
organisation. Every engine validates each of those in isolation. None of them
check that the four agree.

So the failures arrive at runtime: a form field nobody ever writes, a decision
table input no upstream task produces, a connector output the process expects and
never receives. The diagram is valid. The decision table is valid. The bundle is
broken.

Werkflow treats the bundle as the unit of correctness.

## What makes it different

**Processes are deterministic. The work inside them does not have to be.**
The engine is an explicit, auditable state machine. Individual activities can call
out to systems that are messy, slow, or probabilistic. The spine stays deterministic
and replayable regardless of what happens at the edges.

**Cross-artifact contract checking.**
Werkflow verifies data-flow availability across BPMN, DMN, forms, and external
interactions before deployment: every consumer of a variable has a producer that
provably runs first. It is a static analysis with no network calls, so it runs on
every save rather than at deploy time.

**No data model of its own.**
Your ERP, your CRM, and your ledger remain the system of record. Werkflow holds the
process and nothing else, and the boundary between them is a typed contract that is
checked rather than assumed. Nothing has to be migrated into Werkflow for a process
to run against it.

**Connectors are governed, not just installed.**
Reaching an external system passes three independent gates: the operation exists in
the catalog, the tenant holds an installation grant, and an administrator has
enabled it. Credentials live in OpenBao, scoped per tenant. Contract versions are
resolved once and pinned, so a connector cannot change shape underneath a running
process.

**Audit trails by construction.**
Provenance is not a logging layer bolted on afterwards. Who approved what, on which
version of which process, with which inputs, falls out of how the engine records
execution.

**Genuinely Apache 2.0.**
No open-core bait, no feature flags gating the parts that matter. A fresh install
with zero connectors is a complete, working product.

## Who it is for

Engineering teams that need approval and orchestration workflows to be auditable
rather than merely automated: regulated industries, finance and procurement
operations, and anyone maintaining processes that outlive the team that wrote them.

If your processes are reviewed by someone who was not in the room when they were
built, that is the constraint Werkflow is designed around.

<!-- ## Screenshots

> _Add three: the BPMN designer, the form builder, and a contract-check result
> showing a caught fault. The third one is the differentiator and is worth
> capturing well._
-->

## Quick Start

```bash
git clone https://github.com/themaverik/werkflow.git
cd werkflow/infrastructure/docker
cp config/env/.env.example config/env/.env
docker compose up -d
```

First start pulls images and initialises Keycloak, so allow a few minutes.

- Portal: http://localhost:4000
- Engine API (Swagger UI): http://localhost:8081/swagger-ui.html

Eight example processes deploy automatically when `WERKFLOW_DEPLOY_EXAMPLES=true`:
Leave Request, Event Ticket Request, General Approval, Onboarding Checklist,
Procurement Approval, CapEx Approval, Finance Approval, and Asset Request. Start
with Leave Request, then open it in the designer to see how the form, the decision
table, and the process reference each other.

## Services

| Service    | Port | Description                                               |
|------------|------|-----------------------------------------------------------|
| Engine     | 8081 | Flowable BPM orchestration                                |
| Admin      | 8083 | User, organisation, connector, and integration management |
| Portal     | 4000 | Web portal (Next.js)                                      |
| Keycloak   | 8090 | Authentication and authorisation                          |
| PostgreSQL | 5433 | Primary database                                          |
| OpenBao    | 8200 | Per-tenant credential store                               |
| Mailpit    | 8025 | Email sandbox (dev only)                                  |

### Prerequisites

- Docker and Docker Compose
- Java 21+ (only for local service development)
- Node.js 20+ (only for local portal development)

### Local development credentials

> **Development sandbox only.** These values ship in `.env.example` so that a
> clone starts on the first try. They are public, they are not secrets, and they
> must be replaced before any deployment reachable by anyone other than you. See
> [Deployment Configuration](./docs/Deployment-Configuration-Guide.md) for
> generating real credentials.

| Service        | URL                   | Username           |
|----------------|-----------------------|--------------------|
| Portal (admin) | http://localhost:4000 | admin              |
| Keycloak admin | http://localhost:8090 | admin              |
| pgAdmin        | http://localhost:5050 | admin@werkflow.com |
| PostgreSQL     | localhost:5433        | werkflow_admin     |

Passwords for each are in `config/env/.env.example`.

## Connectors

Connectors are how a process reaches anything outside itself. Each declares its
operations, their input and output shapes, and an interaction pattern. Those
declarations are what the contract checker reads, which is why a miswired
integration is caught in the designer rather than in production.

Three levels of testing are available and none substitutes for the others:

- **Credential test.** Does the handshake succeed?
- **Operation test.** Does the call work? Write operations are gated separately.
- **Wiring test.** Does the process actually supply what the operation needs? Static,
  no network.

See the [Connector Guide](./docs/CONNECTOR-GUIDE.md) to build one, and the
[Marketplace](./marketplace/README.md) for seed connectors and the contribution path.

### Reference integrations

<!-- > _Placeholder until the connector ships._

**[werkflow-connector-erpnext](https://github.com/themaverik/werkflow-connector-erpnext)**
drives a procurement approval in Werkflow against a real, third-party ERP. The
approval runs in Werkflow, the purchase order lives in ERPNext, and the connector is
the only thing between them. Neither system knows anything about the other's
internals. ERPNext is GPL-3.0; the connector calls its HTTP API and links nothing, so
it remains Apache 2.0. -->

**[werkflow-erp-sandbox](https://github.com/themaverik/werkflow-erp)** is a first-party
fake ERP with a real API, used as a deterministic fixture for connector conformance
tests. It is a test target, not an ERP.

## How it fits together

```
werkflow/
├── services/
│   ├── engine/       # Flowable BPM orchestration (8081)
│   └── admin/        # Org, connector, and integration management (8083)
├── frontends/
│   └── portal/       # Next.js portal (4000)
├── marketplace/      # Seed connectors and contribution guide
├── infrastructure/
│   └── docker/       # Docker Compose and Dockerfiles
└── docs/             # Architecture decisions, guides, ADRs
```

Engine artifacts stay canonical. The BPMN, DMN, and form definitions are the source
of truth for structure and execution, exactly as the standards intend. Werkflow adds
a contract layer above them rather than replacing them with a proprietary model, so
your processes remain portable.

### Roles

| Role             | Access                                     |
|------------------|--------------------------------------------|
| `super_admin`    | Full platform access                       |
| `admin`          | Workflow designer, form builder, all tasks |
| `workflow_admin` | Workflow designer, form builder            |
| `employee`       | My Tasks, My Requests, Service Catalog     |

## Documentation

- [Quick Start](./docs/QUICKSTART.md)
- [Connector Guide](./docs/CONNECTOR-GUIDE.md)
- [Connector Marketplace](./marketplace/README.md), including the contribution guide
- [Deployment Configuration](./docs/Deployment-Configuration-Guide.md)
- [Keycloak Setup](./docs/Keycloak-Implementation-Guide.md)
- [Architecture Decisions](./docs/Architecture/)

## Contributing

Issues and pull requests are welcome. Connector contributions have their own path
through the [marketplace guide](./marketplace/README.md). See
[CONTRIBUTING.md](./CONTRIBUTING.md) for development setup and conventions.

## License

Licensed under the [Apache License 2.0](./LICENSE). Third-party components (bpmn-js,
form-js, dmn-js) are subject to the [bpmn.io license](./LICENSES/bpmn.io.txt). See
[NOTICE](./NOTICE) for full attribution.
