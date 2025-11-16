# 🔐 Keycloak Quick Reference

## 🚀 Quick Start

```bash
# 1. Start all services
docker-compose up -d

# 2. Wait for Keycloak (60 seconds)
docker-compose logs -f keycloak

# 3. Access Keycloak Admin
# URL: http://localhost:8090
# User: admin / admin123
```

## 🏢 Setup Checklist

- [ ] Create realm: `werkflow`
- [ ] Create client: `werkflow-api` (confidential)
- [ ] Copy client secret to `application.yml`
- [ ] Create roles: `HR_ADMIN`, `HR_MANAGER`, `MANAGER`, `EMPLOYEE`
- [ ] Create test users and assign roles

## 👤 Test Users

| Username | Password | Role | Access Level |
|----------|----------|------|--------------|
| alice.admin | alice123 | HR_ADMIN | Full access |
| bob.hrmanager | bob123 | HR_MANAGER | HR operations |
| charlie.manager | charlie123 | MANAGER | Team management |
| diana.employee | diana123 | EMPLOYEE | Basic access |

## 🔑 Get Access Token

```bash
# Replace YOUR_CLIENT_SECRET with actual secret from Keycloak
curl -X POST http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "client_id=werkflow-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=alice.admin" \
  -d "password=alice123" | jq -r '.access_token'
```

## 🧪 Test API with Token

```bash
# Save token
export TOKEN="YOUR_ACCESS_TOKEN_HERE"

# Test endpoint
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" | jq
```

## 📊 Role Permissions

```
HR_ADMIN     ✅ All operations
HR_MANAGER   ✅ Departments, Employees, Payroll, Approve Leaves
MANAGER      ✅ View Employees, Approve Leaves, Create Reviews
EMPLOYEE     ✅ View data, Create Leaves, Acknowledge Reviews
```

## 🔗 Important URLs

- **Keycloak Admin**: http://localhost:8090
- **Application API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **pgAdmin**: http://localhost:5050

## 🐛 Quick Troubleshooting

**401 Unauthorized?**
```bash
# Token expired or invalid - get new token
```

**403 Forbidden?**
```bash
# User doesn't have required role - check Keycloak role mapping
```

**Can't access Keycloak?**
```bash
docker-compose restart keycloak
docker-compose logs keycloak
```

## 📖 Full Documentation

See `KEYCLOAK_SETUP.md` for complete setup instructions.
