# Testing Guide

## Service Health Checks

```bash
# Backend services
curl -f http://localhost:8081/actuator/health   # Engine
curl -f http://localhost:8083/actuator/health   # Admin
curl -f http://localhost:8084/actuator/health   # Business
curl -f http://localhost:8090/health/ready       # Keycloak

# Frontend
curl -f http://localhost:4000                    # Portal
```

## Sanity Testing Checklist

### Authentication
- [ ] Login page loads at `http://localhost:4000`
- [ ] Keycloak login redirects correctly
- [ ] User redirected to dashboard after login
- [ ] Roles displayed correctly in sidebar

### Process Designer (Platform Module)
- [ ] Process list page loads (`/processes`)
- [ ] Can create new BPMN process
- [ ] BPMN designer loads with drag-drop
- [ ] Can save and deploy process

### Form Builder
- [ ] Form list page loads (`/forms`)
- [ ] Can create new form
- [ ] form-js builder loads
- [ ] Can save form definition

### Task Management
- [ ] My Tasks tab shows assigned tasks
- [ ] Group Tasks tab shows claimable tasks
- [ ] Can claim and complete a task
- [ ] Form renders in task dialog

### Monitoring and Analytics
- [ ] Monitoring page shows live stats (`/monitoring`)
- [ ] Analytics page shows metrics (`/analytics`)

### HR Module
- [ ] HR dashboard loads (`/hr`)
- [ ] Leave management page loads (`/hr/leave`)

## End-to-End Workflow Test

1. Create a simple BPMN process (start -> user task -> end)
2. Deploy to engine
3. Start process instance via API:
   ```bash
   curl -X POST http://localhost:8081/api/workflows/processes/test-process/start \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{"businessKey":"TEST-001"}'
   ```
4. Verify task appears in Portal -> My Tasks
5. Complete task via form submission
6. Verify process completed in Monitoring dashboard

## API Testing

### Swagger UI

- Engine: `http://localhost:8081/swagger-ui.html`
- Admin: `http://localhost:8083/swagger-ui.html`
- Business: `http://localhost:8084/swagger-ui.html`

### Get Auth Token

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/werkflow-platform/protocol/openid-connect/token \
  -d "client_id=werkflow-portal" \
  -d "client_secret=<SECRET>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# Use token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/repository/process-definitions
```

## Common Issues

| Issue | Fix |
|-------|-----|
| Service won't start | Check port conflicts: `lsof -i :8081` |
| Database errors | Verify PostgreSQL running, check connection string |
| Frontend build fails | `rm -rf .next && npm run build` |
| Flyway migration error | Check `flyway_schema_history` table |
