# Werkflow Documentation

Enterprise Workflow Automation Platform

**Last Updated**: 2026-03-02

---

## Platform Architecture

| Component | Port | Description |
|-----------|------|-------------|
| Engine | 8081 | Flowable BPMN engine, workflow orchestration |
| Admin | 8083 | Admin APIs, route access control |
| Business | 8084 | All domain logic (HR, Finance, Procurement, Inventory) |
| Portal | 4000 | Unified Next.js frontend with module-based route groups |
| Keycloak | 8090 | OAuth2/OIDC identity provider |
| PostgreSQL | 5432 | Shared database |

---

## Documentation

### Architecture

| Document | Description |
|----------|-------------|
| [Workflow Architecture Design](Architecture/Workflow-Architecture-Design.md) | Service topology, deployment model, inter-service patterns |
| [Delegate Architecture Analysis](Architecture/Delegate-Architecture-Analysis.md) | Generic RestServiceDelegate usage and patterns |
| [Keycloak Authentication Flow](Architecture/Keycloak-Authentication-Flow.md) | OAuth2/OIDC auth flow diagrams |
| [Task Endpoints Design](Architecture/Task-Endpoints-Design-Specification.md) | /my-tasks and /group-tasks API specification |
| [BPMN Diagram Configuration](Architecture/BPMN-Diagram-Configuration.md) | BPMN diagram generation and rendering |
| [Workflow Guide](Architecture/Workflow-Guide.md) | Workflow features, REST APIs, role-based access |

### Operations

| Document | Description |
|----------|-------------|
| [Deployment Configuration Guide](Deployment-Configuration-Guide.md) | Local dev, Docker Compose, env vars, health checks |
| [Keycloak Implementation Guide](Keycloak-Implementation-Guide.md) | Backend + frontend Keycloak integration and operations |
| [Keycloak RBAC Role Matrix](Keycloak-RBAC-Role-Matrix-Design.md) | Role hierarchy, DOA system, permission model |
| [Testing Guide](Testing.md) | Health checks, sanity testing, E2E workflow test |

### Reference

| Document | Description |
|----------|-------------|
| [BPMN Quick Reference](BPMN-Quick-Reference-Guide.md) | BPMN constructs, gateways, events, variables |
| [form-js Quick Reference](Form-Js-Quick-Reference.md) | form-js component types and usage |
| [Service Registry User Guide](Service-Registry-User-Guide.md) | Service URL management for delegates |

### Troubleshooting

| Document | Description |
|----------|-------------|
| [Authentication Issues](Troubleshooting/Authentication-Issues.md) | Auth error resolution |
| [Authentication 404 Fix](Troubleshooting/Authentication-404-Callback-Fix.md) | OAuth2 callback 404 fix |
| [Flowable REST API Conflict](Troubleshooting/Flowable-REST-API-Conflict.md) | API path conflict resolution |
| [Frontend Route Issues](Troubleshooting/Frontend-Route-Issues.md) | Routing and navigation fixes |

---

## Quick Start

1. Start Keycloak: `docker compose -f infrastructure/docker/docker-compose.yml up keycloak -d`
2. Start backend: `cd services/engine && mvn spring-boot:run` (repeat for admin, business)
3. Start frontend: `cd frontends/portal && npm install && npm run dev`
4. Open http://localhost:4000
