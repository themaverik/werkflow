# Deployment Configuration Guide

This guide explains how to customize your Werkflow deployment to include only specific departments/services based on your needs.

## Architecture Overview

### Service Dependencies

```
Infrastructure Layer (Always Required)
├── postgres (database server)
├── keycloak-postgres (Keycloak database)
└── keycloak (authentication & authorization)

Core Services Layer (Required for any department)
├── engine-service (BPM workflow orchestration)
└── admin-service (users, organizations, roles, departments)

Department Services Layer (Optional - Deploy as needed)
├── hr-service (employee management, onboarding, time-off)
├── finance-service (budgets, expenses, approvals)
├── procurement-service (purchase requests, vendor management)
└── inventory-service (asset tracking, custody transfers)

Frontend Layer (Optional - Deploy as needed)
├── admin-portal (workflow designer, monitoring, analytics, task management)
└── hr-portal (employee-specific features)

Utilities (Optional)
└── pgadmin (database management UI)
```

### Dependency Matrix

| Service | Depends On |
|---------|-----------|
| **Infrastructure** | |
| postgres | None |
| keycloak-postgres | None |
| keycloak | keycloak-postgres |
| pgadmin | postgres |
| **Core Services** | |
| engine-service | postgres, keycloak |
| admin-service | postgres, keycloak |
| **Department Services** | |
| hr-service | postgres, keycloak |
| finance-service | postgres, keycloak, admin-service, engine-service |
| procurement-service | postgres, keycloak, admin-service, engine-service, finance-service |
| inventory-service | postgres, keycloak, admin-service, engine-service |
| **Frontends** | |
| admin-portal | All backend services you want to manage |
| hr-portal | hr-service, engine-service, admin-service |

---

## Deployment Scenarios

### Scenario 1: HR + Finance Only (Your Case)

#### Required Services

**Infrastructure:**
- `postgres`
- `keycloak-postgres`
- `keycloak`

**Core Services:**
- `engine-service`
- `admin-service`

**Department Services:**
- `hr-service`
- `finance-service`

**Frontends (Optional):**
- `admin-portal` (recommended for workflow management)
- `hr-portal` (optional, for employee self-service)

#### Docker Compose Configuration

Create a new file `docker-compose.hr-finance.yml`:

```yaml
version: '3.8'

services:
  # ================================================================
  # INFRASTRUCTURE SERVICES
  # ================================================================

  postgres:
    image: postgres:15-alpine
    container_name: werkflow-postgres
    environment:
      POSTGRES_DB: werkflow
      POSTGRES_USER: werkflow_admin
      POSTGRES_PASSWORD: werkflow_secure_pass
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql:ro
    networks:
      - werkflow-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U werkflow_admin -d werkflow"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  keycloak-postgres:
    image: postgres:15-alpine
    container_name: werkflow-keycloak-db
    environment:
      POSTGRES_DB: keycloak_db
      POSTGRES_USER: keycloak_user
      POSTGRES_PASSWORD: keycloak_pass
    volumes:
      - keycloak_postgres_data:/var/lib/postgresql/data
    networks:
      - werkflow-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak_user -d keycloak_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: werkflow-keycloak
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-postgres:5432/keycloak_db
      KC_DB_USERNAME: keycloak_user
      KC_DB_PASSWORD: keycloak_pass
      KC_HOSTNAME: localhost
      KC_HOSTNAME_PORT: 8090
      KC_HOSTNAME_STRICT: false
      KC_HOSTNAME_STRICT_HTTPS: false
      KC_LOG_LEVEL: info
      KC_METRICS_ENABLED: true
      KC_HEALTH_ENABLED: true
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
    command: start-dev
    ports:
      - "8090:8080"
    depends_on:
      keycloak-postgres:
        condition: service_healthy
    networks:
      - werkflow-network
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/127.0.0.1/8080;echo -e 'GET /health/ready HTTP/1.1\r\nhost: http://localhost\r\nConnection: close\r\n\r\n' >&3;if [ $? -eq 0 ]; then echo 'Healthcheck Successful';exit 0;else echo 'Healthcheck Failed';exit 1;fi;"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped

  # ================================================================
  # CORE BACKEND SERVICES
  # ================================================================

  engine-service:
    build:
      context: ../..
      dockerfile: Dockerfile
      target: engine-service
    container_name: werkflow-engine
    env_file:
      - ../../.env.shared
      - ../../.env.engine
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow
      SPRING_DATASOURCE_USERNAME: werkflow_admin
      SPRING_DATASOURCE_PASSWORD: werkflow_secure_pass
      SPRING_DATASOURCE_SCHEMA: flowable
      SERVER_PORT: 8081
    ports:
      - "8081:8081"
    volumes:
      - engine_data:/app/process-definitions
      - engine_logs:/app/logs
    networks:
      - werkflow-network
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    restart: unless-stopped

  admin-service:
    build:
      context: ../..
      dockerfile: Dockerfile
      target: admin-service
    container_name: werkflow-admin
    env_file:
      - ../../.env.shared
      - ../../.env.admin
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow
      SPRING_DATASOURCE_USERNAME: werkflow_admin
      SPRING_DATASOURCE_PASSWORD: werkflow_secure_pass
      SPRING_DATASOURCE_SCHEMA: admin_service
      SERVER_PORT: 8083
    ports:
      - "8083:8083"
    volumes:
      - admin_logs:/app/logs
    networks:
      - werkflow-network
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    restart: unless-stopped

  # ================================================================
  # DEPARTMENT SERVICES
  # ================================================================

  hr-service:
    build:
      context: ../..
      dockerfile: Dockerfile
      target: hr-service
    container_name: werkflow-hr
    env_file:
      - ../../.env.shared
      - ../../.env.hr
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow
      SPRING_DATASOURCE_USERNAME: werkflow_admin
      SPRING_DATASOURCE_PASSWORD: werkflow_secure_pass
      SPRING_DATASOURCE_SCHEMA: hr_service
      SERVER_PORT: 8082
    ports:
      - "8082:8082"
    volumes:
      - hr_documents:/app/hr-documents
      - hr_logs:/app/logs
    networks:
      - werkflow-network
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    restart: unless-stopped

  finance-service:
    build:
      context: ../..
      dockerfile: Dockerfile
      target: finance-service
    container_name: werkflow-finance
    env_file:
      - ../../.env.shared
      - ../../.env.finance
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/werkflow
      SPRING_DATASOURCE_USERNAME: werkflow_admin
      SPRING_DATASOURCE_PASSWORD: werkflow_secure_pass
      SPRING_DATASOURCE_SCHEMA: finance_service
      SERVER_PORT: 8084
      ENGINE_SERVICE_URL: http://engine-service:8081
      ADMIN_SERVICE_URL: http://admin-service:8083
    ports:
      - "8084:8084"
    volumes:
      - finance_logs:/app/logs
    networks:
      - werkflow-network
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
      admin-service:
        condition: service_started
      engine-service:
        condition: service_started
    restart: unless-stopped

  # ================================================================
  # FRONTEND SERVICES (Optional)
  # ================================================================

  admin-portal:
    build:
      context: ../..
      dockerfile: Dockerfile
      target: admin-portal
    container_name: werkflow-admin-portal
    env_file:
      - ../../.env.shared
    environment:
      NEXT_PUBLIC_ENGINE_API_URL: http://localhost:8081/api
      NEXT_PUBLIC_HR_API_URL: http://localhost:8082/api
      NEXT_PUBLIC_ADMIN_API_URL: http://localhost:8083/api
      NEXT_PUBLIC_FINANCE_API_URL: http://localhost:8084/api
      NEXT_PUBLIC_KEYCLOAK_URL: http://localhost:8090
      NEXT_PUBLIC_KEYCLOAK_REALM: werkflow
      NEXT_PUBLIC_KEYCLOAK_CLIENT_ID: werkflow-admin-portal
      NODE_ENV: production
    ports:
      - "4000:4000"
    networks:
      - werkflow-network
    depends_on:
      - hr-service
      - engine-service
      - admin-service
      - finance-service
    restart: unless-stopped

networks:
  werkflow-network:
    driver: bridge

volumes:
  postgres_data:
  keycloak_postgres_data:
  engine_data:
  engine_logs:
  admin_logs:
  hr_documents:
  hr_logs:
  finance_logs:
```

#### Running the Deployment

```bash
# Navigate to infrastructure directory
cd infrastructure/docker

# Start HR + Finance deployment
docker-compose -f docker-compose.hr-finance.yml up -d

# Check service status
docker-compose -f docker-compose.hr-finance.yml ps

# View logs
docker-compose -f docker-compose.hr-finance.yml logs -f

# Stop services
docker-compose -f docker-compose.hr-finance.yml down
```

---

### Scenario 2: Single Department (HR Only)

For an even more minimal deployment with just HR:

**Required Services:**
- postgres
- keycloak-postgres
- keycloak
- engine-service
- admin-service
- hr-service

Simply remove the `finance-service` section from the docker-compose file above.

---

### Scenario 3: Add Services Incrementally

Start with HR only, then add Finance later without rebuilding:

```bash
# Initial deployment (HR only)
docker-compose -f docker-compose.hr-only.yml up -d

# Later, add Finance service
docker-compose -f docker-compose.hr-finance.yml up -d finance-service
```

---

## Important Considerations

### 1. Database Schemas

Each service uses its own PostgreSQL schema:
- `flowable` - Engine Service
- `admin_service` - Admin Service
- `hr_service` - HR Service
- `finance_service` - Finance Service
- `procurement_service` - Procurement Service
- `inventory_service` - Inventory Service

The `init-db.sql` script creates ALL schemas. This is fine - unused schemas don't consume significant resources. If you want to minimize:

```sql
-- Edit infrastructure/docker/init-db.sql
-- Comment out schemas for services you won't deploy
-- CREATE SCHEMA IF NOT EXISTS procurement_service;
-- CREATE SCHEMA IF NOT EXISTS inventory_service;
```

### 2. Environment Files

Ensure you have the required .env files:

**Always Required:**
- `.env.shared` (common configuration)
- `.env.engine`
- `.env.admin`

**For HR + Finance:**
- `.env.hr`
- `.env.finance`

**Not Needed (can delete or ignore):**
- `.env.procurement`
- `.env.inventory`

### 3. Frontend Configuration

If using `admin-portal`, update environment variables to only include active services:

```env
# Remove URLs for services you're not deploying
NEXT_PUBLIC_ENGINE_API_URL=http://localhost:8081/api
NEXT_PUBLIC_HR_API_URL=http://localhost:8082/api
NEXT_PUBLIC_ADMIN_API_URL=http://localhost:8083/api
NEXT_PUBLIC_FINANCE_API_URL=http://localhost:8084/api
# DON'T include these if not deployed:
# NEXT_PUBLIC_PROCUREMENT_API_URL=http://localhost:8085/api
# NEXT_PUBLIC_INVENTORY_API_URL=http://localhost:8086/api
```

### 4. Workflow Definitions

The Engine Service can run workflows that span multiple departments. If you deploy a workflow that references a service task for a non-deployed department (e.g., Procurement), those tasks will fail.

**Recommendation:** Only deploy workflow definitions that use the services you have running.

### 5. Resource Requirements

Approximate memory requirements per service:

| Service | Memory |
|---------|--------|
| postgres | 512MB |
| keycloak-postgres | 256MB |
| keycloak | 512MB |
| engine-service | 512MB |
| admin-service | 256MB |
| hr-service | 256MB |
| finance-service | 256MB |
| admin-portal | 256MB |

**HR + Finance deployment:** ~2.5GB RAM minimum

---

## Modifying Existing docker-compose.yml

If you prefer to modify the existing `docker-compose.yml` instead of creating a new file:

### Option 1: Comment Out Services

```yaml
# Procurement Service - DISABLED
# procurement-service:
#   build:
#     context: ../..
#     dockerfile: Dockerfile
#     target: procurement-service
#   ...
```

### Option 2: Use Docker Compose Profiles

Add profiles to services in `docker-compose.yml`:

```yaml
services:
  hr-service:
    profiles: ["hr", "full"]
    # ... rest of config

  finance-service:
    profiles: ["finance", "full"]
    # ... rest of config

  procurement-service:
    profiles: ["procurement", "full"]
    # ... rest of config
```

Then start specific profiles:

```bash
# HR + Finance only
docker-compose --profile hr --profile finance up -d

# Full deployment
docker-compose --profile full up -d

# HR only
docker-compose --profile hr up -d
```

### Option 3: Selective Service Start

With the existing `docker-compose.yml`, specify which services to start:

```bash
# Start infrastructure + core + HR + Finance
docker-compose up -d postgres keycloak-postgres keycloak \
  engine-service admin-service hr-service finance-service admin-portal
```

**Note:** This is tedious for multiple services but works without file modification.

---

## Testing Your Deployment

After starting your custom deployment:

```bash
# Run health checks
./scripts/health-check.sh

# Test specific services
curl http://localhost:8081/actuator/health  # Engine
curl http://localhost:8083/actuator/health  # Admin
curl http://localhost:8082/actuator/health  # HR
curl http://localhost:8084/actuator/health  # Finance

# Access admin portal
open http://localhost:4000
```

---

## Scaling Up Later

When you need to add more departments:

1. **Update docker-compose file** - Add new service sections
2. **Create .env file** - Add `.env.procurement`, etc.
3. **Start new services**:
   ```bash
   docker-compose up -d procurement-service
   ```
4. **Update frontends** - Add new API URLs to environment variables
5. **Restart frontends**:
   ```bash
   docker-compose restart admin-portal
   ```

The beauty of the microservices architecture is that you can add services without affecting existing ones!

---

## Summary

**Yes, you can deploy only HR + Finance by:**

1. **Easiest:** Create `docker-compose.hr-finance.yml` with only needed services
2. **Alternative:** Use `--profile` flags with profiles in docker-compose.yml
3. **Alternative:** Explicitly list services when running `docker-compose up`

**What you need beyond docker-compose.yml:**

- ✅ Ensure required .env files exist
- ✅ Keep `init-db.sql` (or edit to remove unused schemas)
- ✅ Update frontend environment variables (if using admin-portal)
- ⚠️ Be aware of service dependencies (don't deploy Finance without Engine + Admin)
- ⚠️ Only deploy workflows that use your active services

The infrastructure (PostgreSQL, Keycloak) and core services (Engine, Admin) are **always required** for any department deployment.
