# Werkflow Documentation Hub

Enterprise Workflow Automation Platform -- consolidated documentation index.

**Last Updated**: 2026-03-02

---

## Platform Architecture

Werkflow runs 3 backend services, 1 unified frontend, Keycloak for auth, and PostgreSQL.

| Component | Port | Description |
|-----------|------|-------------|
| Engine | 8081 | Flowable BPMN engine, workflow orchestration |
| Admin | 8083 | Admin APIs, route access control |
| Business | 8084 | All domain logic (HR, Finance, Procurement, Inventory) |
| Portal | 4000 | Unified Next.js frontend with module-based route groups |
| Keycloak | 8090 | OAuth2/OIDC identity provider |
| PostgreSQL | 5432 | Shared database |

---

## Core Documentation

### Architecture

| Document | Description |
|----------|-------------|
| [Workflow Architecture Design](Architecture/Workflow-Architecture-Design.md) | Service topology, deployment model, inter-service patterns |
| [Delegate Architecture Analysis](Architecture/Delegate-Architecture-Analysis.md) | Generic vs specific delegates, RestServiceDelegate usage |
| [BPMN Workflows](Architecture/BPMN-Workflows.md) | Process definition reference |
| [Keycloak Authentication Flow](Architecture/Keycloak-Authentication-Flow.md) | Auth flow diagrams |

### Deployment

| Document | Description |
|----------|-------------|
| [Deployment Configuration Guide](Deployment/Deployment-Configuration-Guide.md) | Local dev, Docker Compose, env vars, health checks |

### Development

| Document | Description |
|----------|-------------|
| [API Path Structure](Development/API-Path-Structure.md) | `/api/*` vs `/werkflow/api/*` conventions |
| [BPMN Quick Reference](BPMN-Quick-Reference-Guide.md) | Common BPMN patterns and delegate examples |
| [Service Registry User Guide](Service-Registry-User-Guide.md) | Service registry for delegate URL management |

### Authentication and Security

| Document | Description |
|----------|-------------|
| [OAuth2 Setup Guide](OAuth2/OAuth2-Setup-Guide.md) | Keycloak realm, client, user setup |
| [Keycloak Implementation Guide](Security/Keycloak-Implementation-Guide.md) | Backend + frontend Keycloak integration |
| [Keycloak RBAC Design](Security/Keycloak-RBAC-Design.md) | Role matrix and permission model |
| [OAuth2 Docker Configuration](OAuth2/OAuth2-Docker-Configuration.md) | Three-URL strategy for Docker networking |
| [OAuth2 Troubleshooting](OAuth2/OAuth2-Troubleshooting.md) | Common auth errors and fixes |

### Testing

| Document | Description |
|----------|-------------|
| [Testing Guide](Testing/Testing.md) | Test strategy and methodology |
| [Sanity Testing](Testing/Sanity-Testing.md) | Pre-deployment validation |

### Troubleshooting

| Document | Description |
|----------|-------------|
| [Authentication Issues](Troubleshooting/Authentication-Issues.md) | Auth error resolution |
| [Frontend Route Issues](Troubleshooting/Frontend-Route-Issues.md) | Routing and navigation fixes |
| [Flowable REST API Conflict](Troubleshooting/Flowable-REST-API-Conflict.md) | API path conflict resolution |

---

## Project Structure

```
werkflow/
  services/
    engine/          -- Flowable BPMN engine (port 8081)
    admin/           -- Admin service (port 8083)
    business/        -- Consolidated domain service (port 8084)
  frontends/
    portal/          -- Unified Next.js app (port 4000)
      app/
        (platform)/  -- Process Designer, Forms, Workflows
        (hr)/        -- HR module
        (finance)/   -- Finance module
        (procurement)/ -- Procurement module
        (inventory)/ -- Inventory module
        (auth)/      -- Login
  shared/
    delegates/       -- Generic BPMN delegates (RestServiceDelegate)
  infrastructure/
    docker/          -- Docker Compose files
    keycloak/        -- Realm export/import
  docs/              -- This documentation
    archive/         -- Historical phase docs (preserved for reference)
```

---

## Quick Start

1. Start Keycloak: `docker compose -f infrastructure/docker/docker-compose.yml up keycloak -d`
2. Start backend: `cd services/engine && mvn spring-boot:run` (repeat for admin, business)
3. Start frontend: `cd frontends/portal && npm install && npm run dev`
4. Open http://localhost:4000

See [Deployment Configuration Guide](Deployment/Deployment-Configuration-Guide.md) for full details.

---

## Archive

Historical phase-specific docs (Phase 3-7, Phase 4-6, migration summaries, etc.) are preserved in `docs/archive/` for reference. These reflect the pre-consolidation architecture and are no longer actively maintained.
