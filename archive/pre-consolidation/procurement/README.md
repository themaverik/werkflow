# werkflow Procurement Service

Procurement Management Service for the werkflow Enterprise Platform with vendor management, purchase request management, and purchase order capabilities.

## Overview

The Procurement Service (Port 8085) handles:
- Purchase Request creation and tracking
- Vendor master data management
- Purchase Order (PO) creation and management
- Request for Quotations (RFQs) - basic implementation
- Workflow orchestration via Flowable BPM

## Architecture

### Service Components

**Entities:**
- `Vendor` - Vendor/supplier master data
- `PurchaseRequest` - Purchase requests from departments
- `PurchaseOrder` - Purchase orders to vendors
- `BaseEntity` - Abstract base class with audit fields

**Enums:**
- `RequestStatus` - Request status (SUBMITTED, UNDER_REVIEW, PENDING_APPROVAL, APPROVED, REJECTED, RFQ_SENT, QUOTES_RECEIVED, VENDOR_SELECTED, PO_CREATED, etc.)
- `VendorStatus` - Vendor status (ACTIVE, INACTIVE, PENDING_APPROVAL, BLACKLISTED, SUSPENDED, PROBATION)
- `Priority` - Request priority (LOW, NORMAL, HIGH, CRITICAL)
- `PurchaseOrderStatus` - PO status (DRAFT, SUBMITTED, SENT, ACKNOWLEDGED, IN_FULFILLMENT, IN_DELIVERY, RECEIVED, INVOICED, PAID, CLOSED, CANCELLED)

**Repositories:**
- `VendorRepository` - CRUD and query operations for vendors
- `PurchaseRequestRepository` - CRUD and query operations for purchase requests
- `PurchaseOrderRepository` - CRUD and query operations for purchase orders

**Services:**
- `ProcurementService` - Business logic for purchase requests
- `VendorService` - Business logic for vendors

**Controllers:**
- `ProcurementController` - REST API endpoints for purchase requests
- `VendorController` - REST API endpoints for vendors

## API Endpoints

### Purchase Request Management

**Create Purchase Request**
```
POST /api/procurement/requests
Content-Type: application/json

{
  "title": "Office Supplies",
  "description": "Monthly office supplies",
  "quantity": 100,
  "unit": "units",
  "estimatedUnitPrice": 50.00,
  "priority": "NORMAL",
  "requiredByDate": "2025-12-31",
  "departmentName": "IT",
  "businessJustification": "Regular supplies",
  "preferredVendorId": 1
}
```

**Get Purchase Request by ID**
```
GET /api/procurement/requests/{id}
```

**Get Purchase Request by Number**
```
GET /api/procurement/requests/number/{requestNumber}
```

**List All Purchase Requests with Pagination**
```
GET /api/procurement/requests?page=0&size=20
```

**Filter by Status**
```
GET /api/procurement/requests/status/{status}
```

**Filter by Priority**
```
GET /api/procurement/requests/priority/{priority}
```

**Filter by Department**
```
GET /api/procurement/requests/department/{departmentName}
```

**Filter by Requested User**
```
GET /api/procurement/requests/requested-by/{requestedBy}
```

**Search Purchase Requests**
```
GET /api/procurement/requests/search?searchTerm=office&page=0&size=20
```

**Get Pending Approvals**
```
GET /api/procurement/requests/pending-approvals
```

**Update Purchase Request**
```
PUT /api/procurement/requests/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description",
  ...
}
```

**Approve Purchase Request**
```
PUT /api/procurement/requests/{id}/approve
```

**Reject Purchase Request**
```
PUT /api/procurement/requests/{id}/reject?rejectionReason=Exceeds+budget
```

**Update Request Status**
```
PUT /api/procurement/requests/{id}/status?status=RFQ_SENT
```

**Get Statistics Summary**
```
GET /api/procurement/requests/summary/statistics
```

### Vendor Management

**Create Vendor**
```
POST /api/procurement/vendors
Content-Type: application/json

{
  "vendorCode": "VENDOR-001",
  "vendorName": "Acme Supplies",
  "description": "Office supplies vendor",
  "contactPerson": "John Doe",
  "email": "john@acme.com",
  "phone": "+1-555-1234",
  "website": "www.acme.com",
  "address": "123 Main St",
  "city": "Springfield",
  "state": "IL",
  "postalCode": "62701",
  "country": "USA",
  "taxId": "12-3456789",
  "paymentTerms": "Net 30",
  "deliveryLeadTimeDays": 5,
  "minimumOrderAmount": 100.00
}
```

**Get Vendor by ID**
```
GET /api/procurement/vendors/{id}
```

**Get Vendor by Code**
```
GET /api/procurement/vendors/code/{vendorCode}
```

**List All Vendors with Pagination**
```
GET /api/procurement/vendors?page=0&size=20
```

**Get Vendors by Status**
```
GET /api/procurement/vendors/status/{status}
```

**Get Active Vendors**
```
GET /api/procurement/vendors/active
```

**Get Vendors by City**
```
GET /api/procurement/vendors/city/{city}
```

**Get Vendors by Country**
```
GET /api/procurement/vendors/country/{country}
```

**Search Vendors**
```
GET /api/procurement/vendors/search?searchTerm=Acme&page=0&size=20
```

**Get Vendors by Minimum Rating**
```
GET /api/procurement/vendors/rating/{minRating}
```

**Update Vendor**
```
PUT /api/procurement/vendors/{id}
Content-Type: application/json

{
  "vendorName": "Updated Name",
  ...
}
```

**Approve Vendor**
```
PUT /api/procurement/vendors/{id}/approve
```

**Reject Vendor**
```
PUT /api/procurement/vendors/{id}/reject
```

**Deactivate Vendor**
```
PUT /api/procurement/vendors/{id}/deactivate
```

## Database Schema

### vendors Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| vendor_code | VARCHAR(50) | Unique vendor identifier |
| vendor_name | VARCHAR(255) | Vendor name |
| contact_person | VARCHAR(255) | Primary contact name |
| email | VARCHAR(255) | Contact email |
| phone | VARCHAR(20) | Contact phone |
| status | VARCHAR(20) | Vendor status |
| rating | NUMERIC(3,2) | Vendor rating (0-5) |
| total_purchases | NUMERIC(15,2) | Total purchase amount |
| payment_terms | VARCHAR(255) | Standard payment terms |
| delivery_lead_time_days | INTEGER | Typical delivery lead time |
| approved_by | VARCHAR(255) | Approver name |
| approved_at | DATE | Approval date |

### purchase_requests Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| request_number | VARCHAR(50) | Unique request identifier |
| title | VARCHAR(255) | Request title |
| quantity | NUMERIC(15,2) | Quantity to purchase |
| estimated_unit_price | NUMERIC(15,2) | Estimated price per unit |
| total_amount | NUMERIC(15,2) | Total request amount |
| status | VARCHAR(30) | Current request status |
| priority | VARCHAR(20) | Request priority |
| requested_by | VARCHAR(255) | Employee who submitted |
| request_date | DATE | Submission date |
| required_by_date | DATE | Required delivery date |
| department_name | VARCHAR(255) | Requesting department |
| approved_by | VARCHAR(255) | Approver name |
| approved_at | DATE | Approval date |

### purchase_orders Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| po_number | VARCHAR(50) | Unique PO identifier |
| purchase_request_id | BIGINT | Reference to purchase request |
| vendor_id | BIGINT | Reference to vendor |
| vendor_name | VARCHAR(255) | Vendor name (denormalized) |
| quantity | NUMERIC(15,2) | Order quantity |
| unit_price | NUMERIC(15,2) | Price per unit |
| po_amount | NUMERIC(15,2) | Total PO amount |
| status | VARCHAR(30) | Current PO status |
| po_date | DATE | PO creation date |
| delivery_date | DATE | Expected delivery date |
| payment_terms | VARCHAR(255) | Payment terms |

## Configuration

### application.yml

**Key Settings:**
```yaml
server:
  port: 8085
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/werkflow_db
  jpa:
    properties:
      hibernate:
        default_schema: procurement_service

  flyway:
    schemas: procurement_service
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

The Procurement Service uses Keycloak OAuth2/JWT authentication with role-based access control.

### Required Roles

- `PROCUREMENT_ADMIN` - Full access to all procurement operations
- `PROCUREMENT_MANAGER` - Approve/reject requests, manage POs
- `PROCUREMENT_ANALYST` - Create and review purchase requests
- `VENDOR_MANAGER` - Manage vendor master data
- `DEPARTMENT_USER` - Create purchase requests for their department
- `EMPLOYEE` - View own purchase requests

### Access Control

- **POST /procurement/requests** - PROCUREMENT_ADMIN, PROCUREMENT_ANALYST, DEPARTMENT_USER
- **PUT /procurement/requests/{id}** - PROCUREMENT_ADMIN, PROCUREMENT_MANAGER
- **PUT /procurement/requests/{id}/approve** - PROCUREMENT_ADMIN, PROCUREMENT_MANAGER
- **GET /procurement/requests/** - All authenticated users
- **POST /procurement/vendors** - PROCUREMENT_ADMIN, VENDOR_MANAGER
- **PUT /procurement/vendors/{id}** - PROCUREMENT_ADMIN, VENDOR_MANAGER
- **GET /procurement/vendors/** - All authenticated users

## Workflow Integration

The Procurement Service integrates with Flowable BPM Engine for purchase workflows:

**Workflow Stages:**
1. Submit Request → Procurement Service creates request
2. Review → Procurement team reviews request
3. RFQ (optional) → Send RFQ to vendors
4. Vendor Selection → Select vendor from quotations
5. PO Creation → Create and send PO to vendor
6. Acknowledgment → Vendor acknowledges PO
7. Delivery → Goods received and verified
8. Invoice → Invoice received and matched
9. Payment → Payment processed
10. Closure → PO closed and archived

**BPMN Integration:**
- Generic `ApprovalDelegate` for purchase approvals
- `RestServiceDelegate` for vendor validation
- `EmailDelegate` for vendor notifications
- Budget checks via `RestServiceDelegate` to Finance Service

## Building and Running

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Flowable Engine Service (8081)
- Keycloak (8090)

### Build

```bash
cd services/procurement
mvn clean package
```

### Run

```bash
# Development mode
mvn spring-boot:run

# With specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"
```

### Docker

```bash
# Build Docker image
docker build -t werkflow-procurement:latest .

# Run container
docker run -p 8085:8085 \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_USER=werkflow_user \
  -e POSTGRES_PASSWORD=werkflow_pass \
  werkflow-procurement:latest
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProcurementServiceTest

# Run with coverage
mvn test jacoco:report
```

## Swagger/OpenAPI Documentation

Access API documentation at: `http://localhost:8085/api/swagger-ui.html`

API Spec: `http://localhost:8085/api/v3/api-docs`

## Example Workflows

### Simple Purchase Request Workflow

1. Department submits purchase request (SUBMITTED)
2. Procurement analyst reviews request (UNDER_REVIEW)
3. System routes to approver (PENDING_APPROVAL)
4. Manager approves request (APPROVED)
5. Procurement team sends RFQ to vendors (RFQ_SENT)
6. Vendors submit quotes (QUOTES_RECEIVED)
7. Procurement selects vendor (VENDOR_SELECTED)
8. Purchase order created (PO_CREATED)
9. PO sent to vendor (PO_SENT)
10. Vendor acknowledges PO (PO_ACKNOWLEDGED)
11. Goods delivered (IN_DELIVERY)
12. Goods received and verified (RECEIVED)
13. Invoice matched and payment processed (COMPLETED)

### Vendor Onboarding Workflow

1. New vendor registered (PENDING_APPROVAL)
2. Procurement team reviews vendor (under review)
3. Documents verified
4. Vendor approved (ACTIVE)
5. Vendor added to approved vendor list
6. Can now be used for purchases

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
- Future: Redis caching for vendor master data

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running on port 5432
   - Verify credentials in application.yml
   - Ensure `procurement_service` schema exists

2. **Keycloak Authentication Failed**
   - Verify Keycloak is running on port 8090
   - Check realm and client configuration
   - Verify JWT issuer-uri is correct

3. **Migration Failures**
   - Check Flyway migration files in db/migration
   - Verify schema exists: `CREATE SCHEMA procurement_service;`
   - Check database permissions

## Contributing

See [CLAUDE.md](../../CLAUDE.md) for development guidelines.

## Future Enhancements

- Advanced RFQ management
- Vendor rating and performance analytics
- Multi-currency support
- Contract management integration
- Supplier performance dashboard
- CQRS implementation
- Event-driven notifications
- Advanced reporting and analytics
