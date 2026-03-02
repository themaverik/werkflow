# Deployment Configuration Guide

## Overview

Werkflow runs as 3 backend services + 1 frontend + supporting infrastructure. This guide covers local development, Docker Compose, and environment configuration.

---

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Engine | 8081 | Flowable BPMN engine, REST APIs |
| Admin | 8083 | Admin configuration APIs |
| Business | 8084 | Consolidated domain service (HR, Finance, Procurement, Inventory) |
| Portal | 4000 | Unified Next.js frontend |
| Keycloak | 8090 | OAuth2/OIDC identity provider |
| PostgreSQL | 5432 | Shared database |

---

## Local Development Setup

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Docker (for Keycloak)

### 1. Database

```bash
createdb werkflow_db
```

### 2. Keycloak

```bash
docker compose -f infrastructure/docker/docker-compose.yml up keycloak -d
```

### 3. Backend Services

```bash
# Terminal 1: Engine
cd services/engine && mvn spring-boot:run

# Terminal 2: Admin
cd services/admin && mvn spring-boot:run

# Terminal 3: Business
cd services/business && mvn spring-boot:run
```

### 4. Frontend

```bash
cd frontends/portal
npm install
npm run dev   # Starts on port 4000
```

---

## Environment Variables

### Engine Service (`services/engine`)

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/werkflow_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Keycloak
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow-platform
KEYCLOAK_AUTH_SERVER_URL=http://localhost:8090

# Cross-service URLs
BUSINESS_SERVICE_URL=http://localhost:8084
```

### Admin Service (`services/admin`)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/werkflow_db
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow-platform
SERVER_PORT=8083
```

### Business Service (`services/business`)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/werkflow_db
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow-platform
SERVER_PORT=8084
```

### Portal (`frontends/portal`)

```properties
# NextAuth
NEXTAUTH_URL=http://localhost:4000
NEXTAUTH_SECRET=<random-secret>

# Keycloak (three-URL strategy for Docker compatibility)
KEYCLOAK_ISSUER_INTERNAL=http://localhost:8090/realms/werkflow-platform
KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow-platform
KEYCLOAK_ISSUER_PUBLIC=http://localhost:8090/realms/werkflow-platform
KEYCLOAK_CLIENT_ID=werkflow-portal
KEYCLOAK_CLIENT_SECRET=<from-keycloak>

# Backend API URLs (via next.config.mjs rewrites)
NEXT_PUBLIC_ENGINE_API_URL=http://localhost:8081
```

---

## Docker Compose

### Full Stack

```yaml
services:
  postgres:
    image: postgres:15
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: werkflow_db
      POSTGRES_PASSWORD: postgres
    volumes:
      - pgdata:/var/lib/postgresql/data

  keycloak:
    image: quay.io/keycloak/keycloak:24.0.4
    ports: ["8090:8080"]
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/werkflow_db
      KC_DB_USERNAME: postgres
      KC_DB_PASSWORD: postgres
    command: start-dev
    depends_on: [postgres]

  engine-service:
    build: ./services/engine
    ports: ["8081:8081"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow_db
      KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow-platform
      BUSINESS_SERVICE_URL: http://business-service:8084
    depends_on: [postgres, keycloak]

  admin-service:
    build: ./services/admin
    ports: ["8083:8083"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow_db
      KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow-platform
    depends_on: [postgres, keycloak]

  business-service:
    build: ./services/business
    ports: ["8084:8084"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow_db
      KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow-platform
    depends_on: [postgres, keycloak]

  portal:
    build: ./frontends/portal
    ports: ["4000:4000"]
    environment:
      NEXTAUTH_URL: http://localhost:4000
      KEYCLOAK_ISSUER_INTERNAL: http://keycloak:8080/realms/werkflow-platform
      KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow-platform
      KEYCLOAK_CLIENT_ID: werkflow-portal
    depends_on: [engine-service, admin-service, business-service]

volumes:
  pgdata:
```

---

## Next.js API Rewrites

The Portal proxies API calls to backend services via `next.config.mjs`:

```javascript
async rewrites() {
  return [
    { source: '/api/engine/:path*', destination: 'http://localhost:8081/api/:path*' },
    { source: '/api/admin/:path*', destination: 'http://localhost:8083/api/:path*' },
    { source: '/api/business/:path*', destination: 'http://localhost:8084/api/:path*' },
  ]
}
```

---

## Keycloak Three-URL Strategy

The Portal uses three Keycloak URLs to handle Docker networking:

| Variable | Purpose | Example (local) | Example (Docker) |
|----------|---------|-----------------|------------------|
| `KEYCLOAK_ISSUER_INTERNAL` | Server-side token validation | `http://localhost:8090/realms/werkflow-platform` | `http://keycloak:8080/realms/werkflow-platform` |
| `KEYCLOAK_ISSUER_BROWSER` | Browser redirect for login | `http://localhost:8090/realms/werkflow-platform` | `http://localhost:8090/realms/werkflow-platform` |
| `KEYCLOAK_ISSUER_PUBLIC` | Public-facing issuer claim | `http://localhost:8090/realms/werkflow-platform` | `http://localhost:8090/realms/werkflow-platform` |

This is necessary because:
- Server-side code (Next.js) accesses Keycloak via Docker network (`keycloak:8080`)
- Browser redirects must use the host-accessible URL (`localhost:8090`)

---

## Database Migrations

Each service manages its own Flyway migrations:

```
services/engine/src/main/resources/db/migration/    -- Flowable + engine tables
services/business/src/main/resources/db/migration/   -- HR, Finance, Procurement, Inventory tables
services/admin/src/main/resources/db/migration/      -- Admin config tables
```

Run migrations: services auto-run Flyway on startup via Spring Boot.

---

## Health Checks

| Service | Endpoint |
|---------|----------|
| Engine | `http://localhost:8081/actuator/health` |
| Admin | `http://localhost:8083/actuator/health` |
| Business | `http://localhost:8084/actuator/health` |
| Keycloak | `http://localhost:8090/health/ready` |

---

## Related Documentation

- [Workflow Architecture Design](Architecture/Workflow-Architecture-Design.md)
- [OAuth2 Setup Guide](OAuth2-Setup-Guide.md)
- [API Path Structure](API-Path-Structure.md)
