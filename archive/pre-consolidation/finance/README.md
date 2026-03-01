# werkflow Finance Service

Finance Management Service for the werkflow Enterprise Platform with Capital Expenditure (CapEx) request management, budget tracking, and approval workflows.

## Overview

The Finance Service (Port 8084) handles:
- Capital Expenditure (CapEx) requests and tracking
- CapEx approval workflows with multi-level authorization
- Budget management and allocation
- Financial accounting data
- Invoice approvals
- Workflow orchestration via Flowable BPM

## Architecture

### Service Components

**Entities:**
- `CapExRequest` - Capital Expenditure requests
- `CapExApproval` - Approval records for CapEx requests
- `Budget` - Budget allocations by category and department
- `BaseEntity` - Abstract base class with audit fields

**Enums:**
- `CapExStatus` - Request status (SUBMITTED, UNDER_REVIEW, PENDING_APPROVAL, APPROVED, REJECTED, BUDGETED, IN_PROCUREMENT, COMPLETED, CANCELLED)
- `CapExCategory` - Request categories (INFRASTRUCTURE, IT, MACHINERY_EQUIPMENT, VEHICLES, etc.)
- `Priority` - Request priority (LOW, NORMAL, HIGH, CRITICAL)
- `ApprovalLevel` - Authority levels (DEPARTMENT_HEAD, FINANCE_MANAGER, CFO, CEO, BOARD_EXECUTIVE)

**Repositories:**
- `CapExRequestRepository` - CRUD and query operations for CapEx requests
- `CapExApprovalRepository` - CRUD and query operations for approvals

**Services:**
- `CapExService` - Business logic for CapEx requests

**Controllers:**
- `CapExController` - REST API endpoints

## API Endpoints

### CapEx Request Management

**Create CapEx Request**
```
POST /api/capex
Content-Type: application/json

{
  "title": "New Server Infrastructure",
  "description": "Purchase of new database servers",
  "category": "INFRASTRUCTURE",
  "amount": 150000.00,
  "priority": "HIGH",
  "approvalLevel": "CFO",
  "expectedCompletionDate": "2025-12-31",
  "businessJustification": "Improve system performance",
  "expectedBenefits": "20% increase in throughput",
  "budgetYear": 2025,
  "departmentName": "IT"
}
```

**Get CapEx Request by ID**
```
GET /api/capex/{id}
```

**Get CapEx Request by Request Number**
```
GET /api/capex/number/{requestNumber}
```

**List All CapEx Requests with Pagination**
```
GET /api/capex?page=0&size=20
```

**Filter by Status**
```
GET /api/capex/status/{status}
```

**Filter by Category**
```
GET /api/capex/category/{category}
```

**Filter by Priority**
```
GET /api/capex/priority/{priority}
```

**Filter by Department**
```
GET /api/capex/department/{departmentName}
```

**Filter by Budget Year**
```
GET /api/capex/budget-year/{budgetYear}
```

**Filter by Requested User**
```
GET /api/capex/requested-by/{requestedBy}
```

**Search CapEx Requests**
```
GET /api/capex/search?searchTerm=server&page=0&size=20
```

**Get Pending Approvals**
```
GET /api/capex/pending-approvals
```

**Update CapEx Request**
```
PUT /api/capex/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description",
  ...
}
```

**Approve CapEx Request**
```
PUT /api/capex/{id}/approve?remarks=Approved%20by%20CFO
```

**Reject CapEx Request**
```
PUT /api/capex/{id}/reject?rejectionReason=Exceeds%20budget%20allocation
```

**Update Request Status**
```
PUT /api/capex/{id}/status?status=BUDGETED
```

**Get Statistics Summary**
```
GET /api/capex/summary/statistics
```

## Database Schema

### capex_requests Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| request_number | VARCHAR(50) | Unique request identifier |
| title | VARCHAR(255) | Request title |
| category | VARCHAR(30) | CapEx category |
| amount | NUMERIC(15,2) | Requested amount |
| status | VARCHAR(30) | Current status |
| priority | VARCHAR(20) | Request priority |
| approval_level | VARCHAR(30) | Required approval authority |
| requested_by | VARCHAR(255) | Employee who submitted request |
| request_date | DATE | Request submission date |
| expected_completion_date | DATE | Expected completion date |
| approved_amount | NUMERIC(15,2) | Amount approved |
| approved_by | VARCHAR(255) | Approver |
| approved_at | DATE | Approval date |
| budget_year | INTEGER | Fiscal year |
| department_name | VARCHAR(100) | Department name |

### capex_approvals Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| capex_request_id | BIGINT | Reference to CapEx request |
| approval_level | VARCHAR(30) | Required approval level |
| approver | VARCHAR(255) | Person approving |
| approval_status | VARCHAR(20) | PENDING, APPROVED, REJECTED |
| approved_at | DATE | Approval date |
| approval_order | INTEGER | Sequential approval order |

### budgets Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| budget_year | INTEGER | Fiscal year |
| category | VARCHAR(30) | CapEx category |
| department_name | VARCHAR(100) | Department name |
| allocated_amount | NUMERIC(15,2) | Budget allocation |
| utilized_amount | NUMERIC(15,2) | Amount spent |
| remaining_amount | NUMERIC(15,2) | Available budget |

## Configuration

### application.yml

**Key Settings:**
```yaml
server:
  port: 8084
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/werkflow_db
  jpa:
    properties:
      hibernate:
        default_schema: finance_service

  flyway:
    schemas: finance_service
```

### Environment Variables

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=werkflow_user
POSTGRES_PASSWORD=werkflow_pass
POSTGRES_DB=werkflow_db

# Keycloak
KEYCLOAK_AUTH_SERVER_URL=http://localhost:8090
KEYCLOAK_REALM=werkflow

# Flowable
FLOWABLE_ASYNC_EXECUTOR_ACTIVATE=true
FLOWABLE_DATABASE_SCHEMA_UPDATE=true
```

## Security

The Finance Service uses Keycloak OAuth2/JWT authentication with role-based access control.

### Required Roles

- `FINANCE_ADMIN` - Full access to all finance operations
- `FINANCE_MANAGER` - Approve/reject CapEx, manage budgets
- `FINANCE_ANALYST` - Create and review CapEx requests
- `DEPARTMENT_MANAGER` - Create CapEx for their department
- `EMPLOYEE` - View own CapEx requests

### Access Control

- **POST /capex** - FINANCE_ADMIN, FINANCE_ANALYST, DEPARTMENT_MANAGER
- **PUT /capex/{id}** - FINANCE_ADMIN, FINANCE_MANAGER
- **PUT /capex/{id}/approve** - FINANCE_ADMIN, FINANCE_MANAGER
- **PUT /capex/{id}/reject** - FINANCE_ADMIN, FINANCE_MANAGER
- **GET /capex/** - All authenticated users

## Workflow Integration

The Finance Service integrates with Flowable BPM Engine for CapEx approval workflows:

**Workflow Stages:**
1. Submit Request → CapEx Service creates request
2. Review → Finance team reviews request
3. Route to Approver → Based on approval level
4. Approval/Rejection → Appropriate authority approves or rejects
5. Budget Allocation → Approved requests allocated from budget
6. Procurement → Send to Procurement Service
7. Completion → Mark as completed

**BPMN Integration:**
- Generic `ApprovalDelegate` for multi-level approvals
- Configurable approval hierarchy via process variables
- Email notifications via `EmailDelegate`
- Budget update via `RestServiceDelegate`

## Building and Running

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Flowable Engine Service (8081)
- Keycloak (8090)

### Build

```bash
cd services/finance
mvn clean package
```

### Run

```bash
# Development mode
mvn spring-boot:run

# With specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"
```

### Docker

```bash
# Build Docker image
docker build -t werkflow-finance:latest .

# Run container
docker run -p 8084:8084 \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_USER=werkflow_user \
  -e POSTGRES_PASSWORD=werkflow_pass \
  werkflow-finance:latest
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CapExServiceTest

# Run with coverage
mvn test jacoco:report
```

## Swagger/OpenAPI Documentation

Access API documentation at: `http://localhost:8084/api/swagger-ui.html`

API Spec: `http://localhost:8084/api/v3/api-docs`

## Example Workflows

### Simple CapEx Request Workflow

1. Employee submits CapEx request (SUBMITTED)
2. Finance analyst reviews (UNDER_REVIEW)
3. System routes to appropriate approver (PENDING_APPROVAL)
4. CFO approves (APPROVED)
5. Request allocated from budget (BUDGETED)
6. Procurement team processes (IN_PROCUREMENT)
7. Asset received and recorded (COMPLETED)

### Multi-Level Approval Workflow

1. Department head submits $50,000 request
2. Finance Manager reviews (not required)
3. CFO approves
4. System routes to Board for amounts > $100,000

## Audit and Tracking

All entities track:
- `createdAt` - When record was created
- `createdBy` - User who created record
- `updatedAt` - Last modification time
- `updatedBy` - User who last modified
- `version` - Optimistic locking version

## Performance Optimization

- Database indexes on frequently queried columns
- Connection pooling (HikariCP) with 10 max connections
- Lazy loading for related entities
- JPA query optimization
- Future: Redis caching for budgets

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running on port 5432
   - Verify credentials in application.yml
   - Ensure `finance_service` schema exists

2. **Keycloak Authentication Failed**
   - Verify Keycloak is running on port 8090
   - Check realm and client configuration
   - Verify JWT issuer-uri is correct

3. **Migration Failures**
   - Check Flyway migration files in db/migration
   - Verify schema exists: `CREATE SCHEMA finance_service;`
   - Check database permissions

## Contributing

See [CLAUDE.md](../../CLAUDE.md) for development guidelines.

## Future Enhancements

- Procurement Service integration
- Budget forecasting and analytics
- Invoice approval workflow
- Multi-currency support
- CQRS implementation
- Event-driven notifications
- Advanced reporting and dashboards
