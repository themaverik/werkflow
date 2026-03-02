# Sanity Testing Guide

This document provides sanity tests for verifying basic functionality of all Werkflow services in local deployment.

## Prerequisites

- Docker and Docker Compose running
- All services started via `docker-compose up -d` or running locally
- Keycloak configured with werkflow realm
- PostgreSQL running on port 5433

## Infrastructure Services

### 1. PostgreSQL Database

**Test**: Verify database connectivity and schemas

```bash
# Connect to PostgreSQL
psql -h localhost -p 5433 -U werkflow_admin -d werkflow

# Verify schemas exist
\dn

# Expected output should include:
# - public
# - flowable
# - hr_service
# - admin_service
# - finance_service
# - procurement_service
# - inventory_service

# List tables in each schema
\dt flowable.*
\dt hr_service.*
\dt finance_service.*
\dt procurement_service.*
\dt inventory_service.*

# Exit
\q
```

**Expected Result**: All schemas present, tables created via Flyway migrations

### 2. Keycloak

**Test**: Verify Keycloak is accessible

```bash
# Check Keycloak health
curl -f http://localhost:8090/health || echo "Keycloak not ready"

# Access admin console
open http://localhost:8090
# Login: admin / admin123
```

**Expected Result**:
- Keycloak admin console loads
- werkflow realm exists
- Clients configured (werkflow-engine, werkflow-admin-portal)

## Backend Services

### 3. Engine Service (Port 8081)

**Test**: Verify Engine Service is running and Flowable is operational

```bash
# Health check
curl -f http://localhost:8081/actuator/health || echo "Engine Service not ready"

# Check Flowable REST API
curl -u admin:test http://localhost:8081/flowable-rest/service/repository/process-definitions | jq '.'

# Expected: JSON array of process definitions (may be empty initially)

# Swagger UI
open http://localhost:8081/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- Flowable REST API accessible
- Swagger UI displays Engine Service endpoints

**Key Endpoints to Verify**:
- `GET /api/workflows/processes` - List process definitions
- `POST /api/workflows/processes/{processKey}/start` - Start process instance
- `GET /api/workflows/tasks` - List tasks

### 4. HR Service (Port 8082)

**Test**: Verify HR Service APIs

```bash
# Health check
curl -f http://localhost:8082/actuator/health || echo "HR Service not ready"

# List employees (may require auth)
curl http://localhost:8082/api/employees | jq '.'

# Swagger UI
open http://localhost:8082/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- API endpoints accessible
- Swagger UI displays HR Service endpoints

**Key Endpoints to Verify**:
- `GET /api/employees` - List employees
- `GET /api/departments` - List departments
- `GET /api/leave-requests` - List leave requests
- `GET /api/attendance` - Attendance records

### 5. Admin Service (Port 8083)

**Test**: Verify Admin Service (if implemented)

```bash
# Health check
curl -f http://localhost:8083/actuator/health || echo "Admin Service not ready"

# Swagger UI
open http://localhost:8083/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- Swagger UI accessible

**Status**: Admin Service may be partially implemented

### 6. Finance Service (Port 8084)

**Test**: Verify Finance Service APIs

```bash
# Health check
curl -f http://localhost:8084/actuator/health || echo "Finance Service not ready"

# List budget categories
curl http://localhost:8084/api/finance/budget-categories | jq '.'

# List expenses
curl http://localhost:8084/api/finance/expenses | jq '.'

# Swagger UI
open http://localhost:8084/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- API endpoints accessible
- Swagger UI displays Finance Service endpoints

**Key Endpoints to Verify**:
- `GET /api/finance/budget-categories` - Budget categories
- `GET /api/finance/budget-plans` - Budget plans
- `GET /api/finance/expenses` - Expense records
- `GET /api/finance/approval-thresholds` - Approval thresholds

### 7. Procurement Service (Port 8085)

**Test**: Verify Procurement Service APIs

```bash
# Health check
curl -f http://localhost:8085/actuator/health || echo "Procurement Service not ready"

# List vendors
curl http://localhost:8085/api/procurement/vendors | jq '.'

# List purchase requests
curl http://localhost:8085/api/procurement/purchase-requests | jq '.'

# Swagger UI
open http://localhost:8085/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- API endpoints accessible
- Swagger UI displays Procurement Service endpoints

**Key Endpoints to Verify**:
- `GET /api/procurement/vendors` - Vendor master data
- `GET /api/procurement/purchase-requests` - Purchase requests
- `GET /api/procurement/purchase-orders` - Purchase orders
- `GET /api/procurement/receipts` - Goods receipts

### 8. Inventory Service (Port 8086)

**Test**: Verify Inventory Service APIs

```bash
# Health check
curl -f http://localhost:8086/actuator/health || echo "Inventory Service not ready"

# List asset categories
curl http://localhost:8086/api/inventory/categories | jq '.'

# List asset definitions
curl http://localhost:8086/api/inventory/definitions | jq '.'

# Swagger UI
open http://localhost:8086/api/swagger-ui.html
```

**Expected Result**:
- Health check returns `{"status":"UP"}`
- API endpoints accessible
- Swagger UI displays Inventory Service endpoints

**Key Endpoints to Verify**:
- `GET /api/inventory/categories` - Asset categories
- `GET /api/inventory/definitions` - Asset definitions
- `GET /api/inventory/instances` - Asset instances
- `GET /api/inventory/transfer-requests` - Transfer requests

## Frontend Applications

### 9. Admin Portal (Port 4000)

**Test**: Verify Admin Portal is accessible and functional

```bash
# Check if Next.js server is running
curl -f http://localhost:4000 || echo "Admin Portal not ready"

# Open in browser
open http://localhost:4000
```

**Manual Testing Checklist**:

#### Authentication
- [ ] Login page loads
- [ ] Can authenticate with Keycloak
- [ ] Redirected to dashboard after login
- [ ] User roles displayed correctly

#### Studio → Processes (BPMN Designer)
- [ ] Process list page loads
- [ ] Can create new process
- [ ] BPMN designer interface loads
- [ ] Can drag-drop BPMN elements
- [ ] Properties panel works
- [ ] Can save process definition
- [ ] Can deploy process to Engine Service

#### Studio → Forms (Form Builder)
- [ ] Form list page loads
- [ ] Can create new form
- [ ] Form.io builder interface loads
- [ ] Can drag-drop form fields
- [ ] Can configure field validation
- [ ] Can save form definition

#### Studio → Workflows (Multi-Department Dashboard)
- [ ] Workflows page loads
- [ ] Department tabs visible (All, HR, Finance, Procurement, Inventory)
- [ ] Statistics cards show workflow counts
- [ ] Can switch between departments
- [ ] Workflow instance table displays data
- [ ] Status filtering works
- [ ] "View Details" links work

#### Portal → Monitoring
- [ ] Monitoring page loads
- [ ] Statistics cards show process metrics (Active, Completed, Failed, etc.)
- [ ] Running process instances table displays data
- [ ] Recent activity feed shows events
- [ ] Manual refresh button works
- [ ] Auto-refresh occurs every 30 seconds

#### Portal → Analytics
- [ ] Analytics page loads
- [ ] Overview cards show metrics (Total Processes, Completion Rate, Avg Duration, Active Users)
- [ ] Process Performance table displays metrics
- [ ] Activity Analysis section shows bottlenecks
- [ ] Time range filter works (24h, 7d, 30d, 90d)
- [ ] Period-over-period comparisons visible

#### Portal → Tasks
- [ ] Tasks page loads
- [ ] My Tasks tab shows assigned tasks
- [ ] Group Tasks tab shows available tasks
- [ ] Task count badges update
- [ ] Can claim a group task
- [ ] Task form dialog opens
- [ ] Form.io form renders correctly
- [ ] Can complete task with form submission
- [ ] Task lists refresh after completion
- [ ] Can release (unclaim) a task

**Expected Result**:
- All pages load without errors
- Navigation works smoothly
- Real-time data displayed (no mock data)
- Forms are interactive and responsive

## Integration Testing

### 10. End-to-End Workflow Test

**Test**: Complete workflow from creation to completion

1. **Create BPMN Process**:
   - Navigate to Studio → Processes → New Process
   - Design a simple approval workflow
   - Add start event → user task → end event
   - Configure user task with form key
   - Save and deploy

2. **Create Form**:
   - Navigate to Studio → Forms → New Form
   - Add text field, select field, textarea
   - Save form with matching form key

3. **Start Process Instance**:
   ```bash
   curl -X POST http://localhost:8081/api/workflows/processes/test-process/start \
     -H "Content-Type: application/json" \
     -d '{"businessKey":"TEST-001","variables":{}}'
   ```

4. **Verify in Multi-Department Dashboard**:
   - Navigate to Studio → Workflows
   - Verify new process instance appears
   - Check status is "active"

5. **Verify in Monitoring Dashboard**:
   - Navigate to Portal → Monitoring
   - Check "Active Processes" count increased
   - Verify process instance in "Running Process Instances" table

6. **Complete Task**:
   - Navigate to Portal → Tasks
   - Find task in My Tasks or Group Tasks
   - Claim task (if in Group Tasks)
   - Click "Work on Task"
   - Fill out form
   - Submit

7. **Verify Completion**:
   - Check process instance status changed to "completed"
   - Verify in Analytics Dashboard completion metrics updated

**Expected Result**: Complete workflow execution without errors

## Service Health Check Script

Create a script to automate basic health checks:

```bash
#!/bin/bash
# save as: scripts/health-check.sh

echo "=== Werkflow Service Health Check ==="
echo ""

services=(
  "PostgreSQL:5433:nc -z localhost 5433"
  "Keycloak:8090:curl -sf http://localhost:8090/health > /dev/null"
  "Engine:8081:curl -sf http://localhost:8081/actuator/health > /dev/null"
  "HR:8082:curl -sf http://localhost:8082/actuator/health > /dev/null"
  "Admin:8083:curl -sf http://localhost:8083/actuator/health > /dev/null"
  "Finance:8084:curl -sf http://localhost:8084/actuator/health > /dev/null"
  "Procurement:8085:curl -sf http://localhost:8085/actuator/health > /dev/null"
  "Inventory:8086:curl -sf http://localhost:8086/actuator/health > /dev/null"
  "Admin Portal:4000:curl -sf http://localhost:4000 > /dev/null"
)

for service in "${services[@]}"; do
  IFS=':' read -r name port check <<< "$service"
  printf "%-20s [Port %-5s] " "$name" "$port"
  if eval "$check"; then
    echo "✅ UP"
  else
    echo "❌ DOWN"
  fi
done

echo ""
echo "=== End of Health Check ==="
```

Run with:
```bash
chmod +x scripts/health-check.sh
./scripts/health-check.sh
```

## Common Issues

### Service Won't Start

1. **Check port conflicts**:
   ```bash
   lsof -i :8081  # Replace with service port
   ```

2. **Check logs**:
   ```bash
   # For Docker services
   docker logs werkflow-engine

   # For local services
   tail -f logs/spring.log
   ```

3. **Verify database connection**:
   ```bash
   psql -h localhost -p 5433 -U werkflow_admin -d werkflow -c "SELECT 1;"
   ```

### Frontend Build Issues

1. **Clear Next.js cache**:
   ```bash
   cd frontends/admin-portal
   rm -rf .next
   npm run build
   ```

2. **Verify environment variables**:
   ```bash
   cat .env.local
   # Should have NEXT_PUBLIC_ENGINE_API_URL, etc.
   ```

### Database Migration Errors

1. **Check Flyway migrations**:
   ```bash
   # View migration history
   SELECT * FROM flyway_schema_history;
   ```

2. **Reset database (development only)**:
   ```bash
   psql -h localhost -p 5433 -U werkflow_admin -d werkflow -c "DROP SCHEMA IF EXISTS hr_service CASCADE;"
   # Restart service to re-run migrations
   ```

## Test Report Template

```markdown
# Sanity Test Report

**Date**: YYYY-MM-DD
**Tester**: [Name]
**Environment**: Local Development

## Infrastructure
- [ ] PostgreSQL - All schemas present
- [ ] Keycloak - Accessible and configured

## Backend Services
- [ ] Engine Service (8081) - Health check passed
- [ ] HR Service (8082) - Health check passed
- [ ] Admin Service (8083) - Health check passed
- [ ] Finance Service (8084) - Health check passed
- [ ] Procurement Service (8085) - Health check passed
- [ ] Inventory Service (8086) - Health check passed

## Frontend
- [ ] Admin Portal (4000) - All pages load
- [ ] BPMN Designer - Functional
- [ ] Form Builder - Functional
- [ ] Multi-Department Dashboard - Displays data
- [ ] Monitoring Dashboard - Real-time updates
- [ ] Analytics Dashboard - Metrics displayed
- [ ] Task Portal - Task management works

## Integration
- [ ] End-to-end workflow test passed

## Issues Found
[List any issues discovered]

## Notes
[Additional observations]
```

## Next Steps

After sanity testing passes:
1. Proceed to integration testing
2. Run comprehensive test suites
3. Perform load testing
4. Security testing
5. UAT preparation

---

**Last Updated**: 2025-11-18
**Status**: Sanity testing framework ready for Phase 4 QA
