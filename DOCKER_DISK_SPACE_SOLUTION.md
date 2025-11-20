# Docker Disk Space Issue - Resolution Guide

**Issue**: `initdb: error: could not create directory "/var/lib/postgresql/data/pg_wal": No space left on device`

**Root Cause**: Docker host system is running out of available disk space for PostgreSQL database initialization.

**Error Context**:
- Service: `keycloak-postgres` (or main `postgres` service)
- Stage: Database initialization during Docker Compose startup
- Image: `postgres:15-alpine`
- Volume: `keycloak_postgres_data:/var/lib/postgresql/data`

---

## Diagnosis

### Check Current Disk Usage

```bash
# Check overall disk space
df -h

# Check Docker-specific disk usage
docker system df

# Check Docker volumes directory
du -sh /var/lib/docker/volumes/*
```

### Expected Output
- Look for mounted filesystems showing 100% usage or very high usage (>95%)
- `docker system df` shows space used by images, containers, and volumes

---

## Solution 1: Clean Up Existing Docker Resources (Recommended First)

### Step 1: Stop All Running Containers
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
docker-compose down
# OR for dev environment:
docker-compose -f docker-compose.dev.yml down
```

### Step 2: Remove Dangling Volumes and Images

**WARNING**: This removes unused Docker resources. Be sure you don't need existing data.

```bash
# Remove unused volumes (frees most space)
docker volume prune -f

# Remove dangling images
docker image prune -f

# Remove build cache
docker builder prune -f

# Full cleanup (removes all unused resources)
docker system prune -a -f
```

### Step 3: Restart Docker Services
```bash
# After cleanup, restart the compose stack
docker-compose up -d
# OR for dev:
docker-compose -f docker-compose.dev.yml up -d
```

---

## Solution 2: Increase Docker Desktop Disk Space Allocation

If you're using Docker Desktop (Mac/Windows):

1. **Open Docker Desktop Settings**
   - Mac: Docker menu → Preferences
   - Windows: Settings → Docker Engine

2. **Navigate to Resources/Disk**
   - Increase the disk image size limit
   - Recommendation: At least 50-100GB for development with all services

3. **Apply Changes**
   - May require Docker daemon restart

### Alternative: Configure Docker via CLI (macOS)

```bash
# Check current Docker Desktop settings
docker info | grep -i "Root Dir"

# The config is typically at:
# Mac: ~/.docker/config.json
# Linux: /etc/docker/daemon.json
```

---

## Solution 3: Move Docker Data to Larger Partition (Linux Only)

### Step 1: Create New Docker Data Directory
```bash
# Create on a partition with more space
sudo mkdir -p /mnt/large-disk/docker-data

# Ensure proper permissions
sudo chmod 700 /mnt/large-disk/docker-data
```

### Step 2: Configure Docker to Use New Location

Edit or create `/etc/docker/daemon.json`:
```json
{
  "data-root": "/mnt/large-disk/docker-data"
}
```

### Step 3: Migrate Existing Data (Optional)
```bash
# Stop Docker
sudo systemctl stop docker

# Copy existing data
sudo rsync -av /var/lib/docker/ /mnt/large-disk/docker-data/

# Start Docker
sudo systemctl start docker

# Verify new location
docker info | grep "Root Dir"
```

---

## Solution 4: Reduce PostgreSQL Initial Allocation

If disk space is severely limited, you can reduce PostgreSQL's initial memory allocation:

### Modify docker-compose.yml

Add memory limit to PostgreSQL services:

```yaml
keycloak-postgres:
  image: postgres:15-alpine
  # ... existing config ...
  environment:
    POSTGRES_DB: keycloak_db
    POSTGRES_USER: keycloak_user
    POSTGRES_PASSWORD: keycloak_pass
    # Add memory parameters
    POSTGRES_INITDB_ARGS: "-c shared_buffers=128MB -c max_connections=50"
  deploy:
    resources:
      limits:
        memory: 512M
```

---

## Solution 5: Configure Named Volume Driver Options (Advanced)

Add size limits to named volumes in docker-compose.yml:

```yaml
volumes:
  keycloak_postgres_data:
    driver: local
    driver_opts:
      type: tmpfs
      device: tmpfs
      o: size=2gb  # Adjust size as needed
```

**Note**: This option is experimental and not recommended for production.

---

## Verification Steps

After applying a solution, verify the setup works:

### 1. Check Disk Space Again
```bash
df -h
docker system df
```

### 2. Start Services
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
docker-compose up -d
```

### 3. Monitor PostgreSQL Initialization
```bash
# Check logs
docker logs werkflow-keycloak-db

# Wait for healthy status
docker ps | grep postgres

# Test database connection
docker exec werkflow-postgres pg_isready -U werkflow_admin -d werkflow
docker exec werkflow-keycloak-db pg_isready -U keycloak_user -d keycloak_db
```

### 4. Verify All Services Started
```bash
# Check all containers are running
docker ps | grep werkflow

# Monitor logs for errors
docker-compose logs -f
```

---

## Prevention Tips

1. **Monitor Disk Usage Regularly**
   ```bash
   watch -n 60 'df -h && docker system df'
   ```

2. **Implement Log Rotation**
   - Configure Docker daemon to limit container logs
   - Add to `/etc/docker/daemon.json`:
   ```json
   {
     "log-driver": "json-file",
     "log-opts": {
       "max-size": "10m",
       "max-file": "3"
     }
   }
   ```

3. **Schedule Regular Cleanup**
   - Weekly: `docker system prune -f`
   - Monthly: Remove old images and volumes

4. **Environment-Specific Strategies**
   - **Development**: Use `-f docker-compose.dev.yml` (infrastructure only)
   - **Testing**: Use main `docker-compose.yml` with cleanup intervals
   - **Production**: Allocate adequate disk space (100GB+ recommended)

---

## Docker System Disk Usage Breakdown

Typical space requirements for full Werkflow deployment:

| Component | Size | Notes |
|-----------|------|-------|
| PostgreSQL Data (main) | 500MB - 2GB | Grows with data |
| PostgreSQL Data (Keycloak) | 100MB - 500MB | Keycloak metadata |
| pgAdmin Data | 50MB | Usually small |
| Service Logs | 100MB - 500MB | Depends on logging level |
| Service Binaries | 500MB - 1GB | JAR files and dependencies |
| Frontend Build Output | 200MB - 500MB | Next.js build artifacts |
| Docker images (cached) | 2GB - 5GB | Base images, dependencies |
| **Total Recommended** | **10GB minimum** | **20GB+ recommended** |

---

## Troubleshooting Commands

### If Services Don't Start After Cleanup

```bash
# Full reset (destructive - removes all werkflow containers/volumes)
docker-compose down -v

# Or for dev:
docker-compose -f docker-compose.dev.yml down -v

# Rebuild from scratch
docker-compose build --no-cache

# Start fresh
docker-compose up -d
```

### Check Service Health
```bash
# All services
docker ps --format "table {{.Names}}\t{{.Status}}"

# Specific service logs
docker logs -f werkflow-keycloak-db --tail 50

# PostgreSQL status
docker exec werkflow-postgres psql -U werkflow_admin -d werkflow -c "SELECT version();"
```

### Verify Database Initialization
```bash
# Check if Keycloak database exists
docker exec werkflow-keycloak-db psql -U keycloak_user -d keycloak_db -c "\dt"

# Check Werkflow database schema
docker exec werkflow-postgres psql -U werkflow_admin -d werkflow -c "\dn"
```

---

## Common Issues and Fixes

### Issue: "docker: Error response from daemon: OCI runtime create failed"
**Solution**: Usually low disk space. Run cleanup and check `df -h`.

### Issue: "No space left on device" even after cleanup
**Solution**:
- Check for docker build artifacts: `docker builder prune -af`
- Check for old docker images: `docker images | grep none | awk '{print $3}' | xargs docker rmi`
- Check host filesystem for large files: `du -sh /*`

### Issue: PostgreSQL slow to initialize or hangs
**Solution**:
- Not necessarily a disk issue. Check logs: `docker logs werkflow-postgres`
- Wait longer for initialization (can take 1-2 minutes on slow systems)
- Check available memory: `docker inspect werkflow-postgres`

---

## Next Steps

1. **Immediate**: Run `docker system df` to assess current usage
2. **Apply Solution 1** (cleanup) first - usually resolves the issue
3. If cleanup insufficient:
   - **Mac/Windows**: Apply Solution 2 (increase Docker Desktop allocation)
   - **Linux**: Apply Solution 3 (move to larger partition)
4. **Verify**: Run verification steps after each solution
5. **Monitor**: Implement prevention tips to avoid future issues

---

## Reference

- Docker Disk Usage: https://docs.docker.com/config/containers/resource_constraints/
- PostgreSQL Container: https://hub.docker.com/_/postgres
- Keycloak PostgreSQL: https://www.keycloak.org/server/containers
- Docker Compose Volumes: https://docs.docker.com/compose/compose-file/compose-file-v3/#volumes

