# Werkflow Docker Infrastructure

Docker configuration for the werkflow enterprise platform.

## Files

- **docker-compose.yml** - Full production-like environment (all services in Docker)
- **docker-compose.dev.yml** - Development environment (infrastructure only)
- **init-db.sql** - Database initialization script (creates schemas)
- **Dockerfile** - Multi-stage build (located in project root)

## Quick Start

### Development Mode (Recommended)

Run only infrastructure in Docker, services on host:

```bash
# Start infrastructure
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml up -d

# Verify
docker-compose -f docker-compose.dev.yml ps

# Run a backend service locally (engine or admin)
cd ../../services/engine
mvn spring-boot:run

# Run the portal locally
cd ../../frontends/portal
npm run dev
```

**Access:**
- PostgreSQL: localhost:5433
- Keycloak: http://localhost:8090

### Full Docker Mode

Run everything in Docker:

```bash
# Start all services
cd infrastructure/docker
docker-compose up -d --build

# Verify
docker-compose ps

# View logs
docker-compose logs -f engine-service
docker-compose logs -f admin-service
docker-compose logs -f portal
```

**Access:**
- Engine: http://localhost:8081
- Admin: http://localhost:8083
- Portal: http://localhost:4000
- Keycloak: http://localhost:8090
- Mailpit (email sandbox): http://localhost:8025
- PostgreSQL: localhost:5433

## Services

### Infrastructure

| Service | Port | Container Name | Description |
|---------|------|----------------|-------------|
| PostgreSQL | 5433 | werkflow-postgres | Main database |
| Keycloak | 8090 | werkflow-keycloak | OAuth2/JWT auth |
| Mailpit | 8025 | werkflow-mailpit | Email sandbox (dev only) |

### Backend Services

| Service | Port | Container Name | Status |
|---------|------|----------------|--------|
| Engine | 8081 | werkflow-engine | Ready |
| Admin | 8083 | werkflow-admin | Ready |

### Frontend Services

| Service | Port | Container Name | Status |
|---------|------|----------------|--------|
| Portal | 4000 | werkflow-portal | Ready |

## Database Schemas

Single PostgreSQL instance with schema separation:

- **flowable** - Flowable BPM engine tables
- **hr_service** - HR domain tables
- **admin_service** - User/org/dept management
- **finance_service** - Finance domain (future)
- **procurement_service** - Procurement domain (future)
- **inventory_service** - Inventory domain (future)
- **legal_service** - Legal domain (future)

## Environment Variables

### Required Files

Create these files from examples:

```bash
# Backend services
cp ../../config/env/.env.shared.example ../../config/env/.env.shared
cp ../../config/env/.env.engine.example ../../config/env/.env.engine
cp ../../config/env/.env.admin.example ../../config/env/.env.admin

# Frontend (unified portal)
cp ../../frontends/portal/.env.local.example ../../frontends/portal/.env.local
```

### Key Variables

**Shared (config/env/.env.shared):**
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `KEYCLOAK_URL`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`
- `JWT_SECRET`, `ENCRYPTION_KEY`
- `SMTP_*` for email

**Service-specific (config/env/.env.{service}):**
- `SERVER_PORT`
- `SPRING_DATASOURCE_SCHEMA`
- Service-specific configuration

## Common Commands

### Development Mode

```bash
# Start infrastructure
docker-compose -f docker-compose.dev.yml up -d

# Stop infrastructure
docker-compose -f docker-compose.dev.yml down

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Reset database
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

### Full Docker Mode

```bash
# Build and start all services
docker-compose up -d --build

# Stop all services
docker-compose down

# Rebuild specific service
docker-compose build engine-service
docker-compose up -d engine-service

# View logs for specific service
docker-compose logs -f engine-service

# Access service shell
docker-compose exec engine-service sh

# Reset everything (including volumes)
docker-compose down -v
docker-compose up -d --build
```

### Database Access

```bash
# Connect via psql
docker-compose exec postgres psql -U werkflow_admin -d werkflow

# Connect to specific schema
docker-compose exec postgres psql -U werkflow_admin -d werkflow -c "SET search_path TO hr_service;"

# View schemas
docker-compose exec postgres psql -U werkflow_admin -d werkflow -c "\dn"
```

## Health Checks

All services include health checks:

```bash
# Check service health
docker-compose ps

# View health check logs
docker inspect --format='{{json .State.Health}}' werkflow-postgres
docker inspect --format='{{json .State.Health}}' werkflow-engine
```

## Volumes

### Development Mode
- `postgres_data_dev` - PostgreSQL data
- `keycloak_postgres_data_dev` - Keycloak database

### Full Docker Mode
- `postgres_data` - PostgreSQL data
- `keycloak_postgres_data` - Keycloak database
- `engine_data` - Engine BPMN process-definition storage
- `engine_logs` - Engine service logs
- `admin_logs` - Admin service logs

## Network

All services communicate via `werkflow-network` bridge network.

**Internal DNS:**
- Services resolve by container name
- Example: the engine connects to `postgres:5432` internally

**External Access:**
- Services exposed via port mapping
- Example: Access postgres at `localhost:5433` from host

## Troubleshooting

### PostgreSQL Connection Issues

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U werkflow_admin -d werkflow -c "SELECT 1;"
```

### Service Won't Start

```bash
# Check build logs
docker-compose build engine-service

# Check runtime logs
docker-compose logs engine-service

# Check environment variables
docker-compose config
```

### Port Already in Use

```bash
# Find process using port
lsof -i :5433

# Change port in docker-compose.yml
ports:
  - "5434:5432"  # Use 5434 instead
```

### Clean Slate

```bash
# Stop everything
docker-compose down

# Remove all volumes (WARNING: deletes all data)
docker-compose down -v

# Remove all werkflow images
docker images | grep werkflow | awk '{print $3}' | xargs docker rmi -f

# Start fresh
docker-compose up -d --build
```

## Production Deployment

See ROADMAP-DRAFT.md Phase 3 for production deployment with:
- Kubernetes orchestration
- Helm charts
- CI/CD pipelines
- Terraform infrastructure

Current Docker Compose is for development and testing only.
