# Werkflow Documentation Hub

Enterprise Workflow Automation Platform -- consolidated documentation index.

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

## Architecture

| Document | Description |
|----------|-------------|
| [Workflow Architecture Design](Architecture/Workflow-Architecture-Design.md) | Service topology, deployment model, inter-service patterns |
| [Delegate Architecture Analysis](Architecture/Delegate-Architecture-Analysis.md) | Generic RestServiceDelegate usage and patterns |
| [Keycloak Authentication Flow](Architecture/Keycloak-Authentication-Flow.md) | OAuth2/OIDC auth flow diagrams |
| [Task Endpoints Design](Architecture/Task-Endpoints-Design-Specification.md) | /my-tasks and /group-tasks API specification |
| [BPMN Diagram Configuration](Architecture/BPMN-Diagram-Configuration.md) | BPMN diagram generation and rendering |
| [Workflow Guide](Architecture/Workflow-Guide.md) | Workflow features, REST APIs, role-based access |

## Deployment and Configuration

| Document | Description |
|----------|-------------|
| [Deployment Configuration Guide](Deployment-Configuration-Guide.md) | Local dev, Docker Compose, env vars, health checks |
| [API Path Structure](API-Path-Structure.md) | `/api/*` vs `/werkflow/api/*` conventions |
| [Security Deployment Checklist](Security-Deployment-Checklist.md) | Pre-deployment security validation |

## Authentication and Security

| Document | Description |
|----------|-------------|
| [OAuth2 Setup Guide](OAuth2-Setup-Guide.md) | Keycloak realm, client, user setup |
| [Keycloak Implementation Guide](Keycloak-Implementation-Guide.md) | Backend + frontend Keycloak integration |
| [Keycloak Operations Guide](Keycloak-Operations-Guide.md) | User lifecycle, group management |
| [Keycloak RBAC Role Matrix](Keycloak-RBAC-Role-Matrix-Design.md) | Role hierarchy, DOA system, custom attributes |
| [NextAuth Configuration](NextAuth-Configuration.md) | NextAuth.js v5 + Keycloak integration |
| [OAuth2 Docker Configuration](OAuth2-Docker-Configuration.md) | Three-URL strategy for Docker networking |
| [JWT Multi-Issuer Configuration](JWT-Multi-Issuer-Configuration.md) | Multiple JWT issuer validation |
| [Quick Reference JWT](Quick-Reference-JWT.md) | JWT claims and validation cheat sheet |
| [OAuth2 Troubleshooting](OAuth2-Troubleshooting.md) | Common auth errors and fixes |

## BPMN and Forms

| Document | Description |
|----------|-------------|
| [BPMN Quick Reference Guide](BPMN-Quick-Reference-Guide.md) | BPMN constructs, gateways, events, variables |
| [BPMN Delegate Quick Start](BPMN-Delegate-Implementation-Quick-Start.md) | Custom Java delegate implementation |
| [Form-js Quick Reference](Form-Js-Quick-Reference.md) | form-js component types and usage |
| [Service Registry User Guide](Service-Registry-User-Guide.md) | Service URL management for delegates |

## Testing

| Document | Description |
|----------|-------------|
| [Testing Guide](Testing.md) | Unit, integration, E2E testing |
| [Sanity Testing](Sanity-Testing.md) | Pre-deployment validation checklist |

## Troubleshooting

| Document | Description |
|----------|-------------|
| [Authentication Issues](Troubleshooting/Authentication-Issues.md) | Comprehensive auth error resolution |
| [Authentication 404 Callback Fix](Troubleshooting/Authentication-404-Callback-Fix.md) | OAuth2 callback 404 fix |
| [Flowable REST API Conflict](Troubleshooting/Flowable-REST-API-Conflict.md) | API path conflict resolution |
| [Frontend Route Issues](Troubleshooting/Frontend-Route-Issues.md) | Routing and navigation fixes |

---

## Project Structure

```
werkflow/
  services/
    engine/        -- Flowable BPMN engine (:8081)
    admin/         -- Admin service (:8083)
    business/      -- Consolidated domain service (:8084)
  frontends/
    portal/        -- Unified Next.js app (:4000)
  shared/
    delegates/     -- Generic BPMN delegates
  infrastructure/
    docker/        -- Docker Compose files
    keycloak/      -- Realm export/import
```

---

## Archive

Historical docs (phase reports, migration summaries, feasibility analyses) are in `docs/archive/`.
