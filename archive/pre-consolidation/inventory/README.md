# werkflow Inventory Service

Comprehensive asset and inventory management service for the werkflow Enterprise Platform with asset tracking, inter-department custody management, transfer workflows, and maintenance scheduling.

## Overview

The Inventory Service (Port 8086) handles:
- Asset category management (hierarchical)
- Asset definition catalog (templates)
- Asset instance tracking (physical items)
- Inter-department asset custody and assignment
- Asset transfer requests with approval workflows
- Maintenance record tracking and scheduling
- Order batching for bulk asset operations
- Workflow orchestration via Flowable BPM

## Architecture

### Service Components

**Entities:**
- `AssetCategory` - Hierarchical asset categories (with parent-child relationships)
- `AssetDefinition` - Asset templates/catalog items (SKU-based)
- `AssetInstance` - Physical asset instances (with barcode/QR tagging)
- `CustodyRecord` - Inter-department asset assignments and custody
- `TransferRequest` - Asset transfer requests (workflow-driven)
- `MaintenanceRecord` - Maintenance history and scheduling

**Enums:**
- `AssetCondition` - Asset state (NEW, GOOD, FAIR, POOR, DAMAGED, NEEDS_REPAIR)
- `AssetStatus` - Asset availability (AVAILABLE, IN_USE, MAINTENANCE, RETIRED, DISPOSED, LOST)
- `CustodyType` - Custody model (PERMANENT, TEMPORARY, LOAN)
- `TransferType` - Transfer type (INTER_DEPARTMENT, RETURN_TO_OWNER, DISPOSAL, LOAN)
- `TransferStatus` - Request status (PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED)
- `MaintenanceType` - Maintenance category (SCHEDULED, REPAIR, INSPECTION, CALIBRATION, UPGRADE)

**Repositories:**
- `AssetCategoryRepository` - CRUD and query operations for asset categories
- `AssetDefinitionRepository` - CRUD and query operations for asset definitions
- `AssetInstanceRepository` - CRUD and query operations for asset instances
- `CustodyRecordRepository` - CRUD and query operations for custody records
- `TransferRequestRepository` - CRUD and query operations for transfer requests
- `MaintenanceRecordRepository` - CRUD and query operations for maintenance records

**Services:**
- `AssetCategoryService` - Business logic for asset category management
- `AssetDefinitionService` - Business logic for asset definitions
- `AssetInstanceService` - Business logic for asset instances
- `CustodyRecordService` - Business logic for custody management
- `TransferRequestService` - Business logic for asset transfers
- `MaintenanceRecordService` - Business logic for maintenance tracking

**Controllers:**
- `AssetCategoryController` - REST API for asset categories
- `AssetDefinitionController` - REST API for asset definitions
- `AssetInstanceController` - REST API for asset instances
- `CustodyRecordController` - REST API for custody management
- `TransferRequestController` - REST API for asset transfers
- `MaintenanceRecordController` - REST API for maintenance records

## API Endpoints

### Asset Categories

**Create Asset Category**
```
POST /asset-categories
Content-Type: application/json

{
  "name": "IT Equipment",
  "code": "IT",
  "description": "Information Technology equipment",
  "parentCategoryId": null,
  "primaryCustodianDeptId": 1,
  "requiresApproval": true,
  "active": true
}
```

**Get Asset Category by ID**
```
GET /asset-categories/{id}
```

**Get All Asset Categories**
```
GET /asset-categories
```

**Get Active Categories**
```
GET /asset-categories/active
```

**Get Root Categories (No Parent)**
```
GET /asset-categories/root
```

**Get Child Categories**
```
GET /asset-categories/{parentId}/children
```

**Search Categories**
```
GET /asset-categories/search?searchTerm=equipment
```

**Update Asset Category**
```
PUT /asset-categories/{id}
```

**Deactivate/Activate Category**
```
PUT /asset-categories/{id}/deactivate
PUT /asset-categories/{id}/activate
```

### Asset Definitions

**Create Asset Definition**
```
POST /asset-definitions
Content-Type: application/json

{
  "categoryId": 1,
  "sku": "LAP-MBP16-01",
  "name": "MacBook Pro 16\"",
  "manufacturer": "Apple",
  "model": "MacBook Pro 2023",
  "specifications": {"cpu": "M3 Pro", "ram": "32GB", "storage": "1TB SSD"},
  "unitCost": 2999.00,
  "expectedLifespanMonths": 48,
  "requiresMaintenance": false,
  "active": true
}
```

**Get Asset Definition by ID**
```
GET /asset-definitions/{id}
```

**Get Definition by SKU**
```
GET /asset-definitions/sku/{sku}
```

**Get All Asset Definitions**
```
GET /asset-definitions
```

**Get Active Definitions**
```
GET /asset-definitions/active
```

**Get Definitions by Category**
```
GET /asset-definitions/category/{categoryId}
```

**Get Definitions Requiring Maintenance**
```
GET /asset-definitions/maintenance
```

**Get Definitions by Manufacturer**
```
GET /asset-definitions/manufacturer/{manufacturer}
```

**Get Definitions by Price Range**
```
GET /asset-definitions/price-range?minPrice=1000&maxPrice=5000
```

**Search Definitions**
```
GET /asset-definitions/search?searchTerm=laptop
```

### Asset Instances

**Create Asset Instance**
```
POST /asset-instances
Content-Type: application/json

{
  "assetDefinitionId": 1,
  "assetTag": "IT-LAP-001",
  "serialNumber": "ABC123XYZ",
  "purchaseDate": "2023-01-15",
  "purchaseCost": 2999.00,
  "warrantyExpiryDate": "2024-01-15",
  "condition": "NEW",
  "status": "AVAILABLE",
  "currentLocation": "Office Building A",
  "notes": "Brand new MacBook Pro"
}
```

**Get Asset Instance by ID**
```
GET /asset-instances/{id}
```

**Get Instance by Asset Tag**
```
GET /asset-instances/tag/{assetTag}
```

**Get All Asset Instances**
```
GET /asset-instances
```

**Get Instances by Definition**
```
GET /asset-instances/definition/{definitionId}
```

**Get Instances by Status**
```
GET /asset-instances/status/{status}
```

**Get Available Assets**
```
GET /asset-instances/available
```

**Get Assets in Use**
```
GET /asset-instances/in-use
```

**Get Assets Requiring Maintenance**
```
GET /asset-instances/maintenance
```

**Get Assets with Expiring Warranty**
```
GET /asset-instances/expiring-warranty?daysFromNow=30
```

**Get Assets Needing Attention**
```
GET /asset-instances/attention
```

**Search Asset Instances**
```
GET /asset-instances/search?searchTerm=LAP
```

**Update Asset Status**
```
PUT /asset-instances/{id}/status?status=IN_USE
```

### Custody Records

**Create Custody Record**
```
POST /custody-records
Content-Type: application/json

{
  "assetInstanceId": 1,
  "custodianDeptId": 2,
  "custodianUserId": 5,
  "physicalLocation": "Office Room 201",
  "custodyType": "PERMANENT",
  "startDate": "2023-01-15T10:00:00",
  "endDate": null,
  "assignedByUserId": 3,
  "notes": "Assigned to John Doe"
}
```

**Get Custody Record by ID**
```
GET /custody-records/{id}
```

**Get Current Custody for Asset**
```
GET /custody-records/current/asset/{assetId}
```

**Get Custody History**
```
GET /custody-records/history/asset/{assetId}
```

**Get All Custody Records**
```
GET /custody-records
```

**Get Active Custody Records**
```
GET /custody-records/active
```

**Get Custody by Department**
```
GET /custody-records/department/{deptId}
```

**Get Custody by User**
```
GET /custody-records/user/{userId}
```

**Get Custody by Type**
```
GET /custody-records/type/{custodyType}
```

**Get Overdue Temporary Custody**
```
GET /custody-records/overdue-temporary
```

**End Custody (Return Asset)**
```
PUT /custody-records/{id}/end?returnCondition=GOOD
```

### Transfer Requests

**Create Transfer Request**
```
POST /transfer-requests
Content-Type: application/json

{
  "assetInstanceId": 1,
  "fromDeptId": 1,
  "fromUserId": 5,
  "toDeptId": 2,
  "toUserId": 6,
  "transferType": "INTER_DEPARTMENT",
  "transferReason": "Equipment reassignment due to departmental restructuring",
  "expectedReturnDate": "2024-12-31",
  "initiatedByUserId": 3
}
```

**Get Transfer Request by ID**
```
GET /transfer-requests/{id}
```

**Get All Transfer Requests**
```
GET /transfer-requests
```

**Get Transfers by Asset**
```
GET /transfer-requests/asset/{assetId}
```

**Get Transfers by Status**
```
GET /transfer-requests/status/{status}
```

**Get Pending Transfers**
```
GET /transfer-requests/pending
```

**Get Transfers from Department**
```
GET /transfer-requests/from-dept/{deptId}
```

**Get Transfers to Department**
```
GET /transfer-requests/to-dept/{deptId}
```

**Get Active Inter-Department Transfers**
```
GET /transfer-requests/inter-department/active
```

**Get Active Loans**
```
GET /transfer-requests/loans/active
```

**Get Overdue Loans**
```
GET /transfer-requests/loans/overdue
```

**Search Transfer Requests**
```
GET /transfer-requests/search?searchTerm=reassignment
```

**Approve Transfer Request**
```
PUT /transfer-requests/{id}/approve?approverUserId=3
```

**Reject Transfer Request**
```
PUT /transfer-requests/{id}/reject?rejectionReason=Insufficient budget
```

**Complete Transfer Request**
```
PUT /transfer-requests/{id}/complete
```

### Maintenance Records

**Create Maintenance Record**
```
POST /maintenance-records
Content-Type: application/json

{
  "assetInstanceId": 1,
  "maintenanceType": "SCHEDULED",
  "scheduledDate": "2024-02-01",
  "completedDate": null,
  "performedBy": "Tech Services",
  "cost": 150.00,
  "description": "Annual preventive maintenance",
  "nextMaintenanceDate": "2025-02-01"
}
```

**Get Maintenance Record by ID**
```
GET /maintenance-records/{id}
```

**Get All Maintenance Records**
```
GET /maintenance-records
```

**Get Maintenance History**
```
GET /maintenance-records/asset/{assetId}
```

**Get Maintenance by Type**
```
GET /maintenance-records/type/{maintenanceType}
```

**Get Incomplete Maintenance**
```
GET /maintenance-records/incomplete
```

**Get Overdue Maintenance**
```
GET /maintenance-records/overdue
```

**Get Completed Maintenance**
```
GET /maintenance-records/completed
```

**Get Scheduled Maintenance Due**
```
GET /maintenance-records/scheduled-due?daysFromNow=30
```

**Get Expensive Maintenance**
```
GET /maintenance-records/expensive?minCost=500
```

**Complete Maintenance Record**
```
PUT /maintenance-records/{id}/complete?completedDate=2024-02-01&nextMaintenanceDate=2025-02-01
```

## Database Schema

### asset_categories Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| parent_category_id | BIGINT | Parent category (for hierarchy) |
| name | VARCHAR(100) | Category name |
| code | VARCHAR(50) | Unique code |
| description | VARCHAR(1000) | Category description |
| primary_custodian_dept_id | BIGINT | Default custodian department |
| requires_approval | BOOLEAN | Requires transfer approval |
| active | BOOLEAN | Active status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Update timestamp |

### asset_definitions Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| category_id | BIGINT | Category reference |
| sku | VARCHAR(100) | Stock keeping unit |
| name | VARCHAR(200) | Asset name |
| manufacturer | VARCHAR(100) | Manufacturer name |
| model | VARCHAR(100) | Model number |
| specifications | JSONB | Technical specifications |
| unit_cost | DECIMAL(10,2) | Unit cost |
| expected_lifespan_months | INTEGER | Expected lifespan |
| requires_maintenance | BOOLEAN | Requires maintenance |
| maintenance_interval_months | INTEGER | Maintenance interval |
| active | BOOLEAN | Active status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Update timestamp |

### asset_instances Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| asset_definition_id | BIGINT | Definition reference |
| asset_tag | VARCHAR(100) | Barcode/QR tag (unique) |
| serial_number | VARCHAR(100) | Serial number |
| purchase_date | DATE | Purchase date |
| purchase_cost | DECIMAL(10,2) | Purchase cost |
| warranty_expiry_date | DATE | Warranty expiration |
| condition | VARCHAR(50) | Asset condition |
| status | VARCHAR(50) | Asset status |
| current_location | VARCHAR(200) | Physical location |
| notes | VARCHAR(2000) | Additional notes |
| metadata | JSONB | Custom metadata |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Update timestamp |

### custody_records Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| asset_instance_id | BIGINT | Asset reference |
| custodian_dept_id | BIGINT | Custodian department |
| custodian_user_id | BIGINT | Custodian user (optional) |
| physical_location | VARCHAR(200) | Physical location |
| custody_type | VARCHAR(50) | Custody type |
| start_date | TIMESTAMP | Custody start |
| end_date | TIMESTAMP | Custody end (null=current) |
| assigned_by_user_id | BIGINT | Assigner user ID |
| return_condition | VARCHAR(50) | Return condition |
| notes | VARCHAR(1000) | Notes |
| created_at | TIMESTAMP | Creation timestamp |

### transfer_requests Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| asset_instance_id | BIGINT | Asset reference |
| from_dept_id | BIGINT | Source department |
| from_user_id | BIGINT | Source user |
| to_dept_id | BIGINT | Destination department |
| to_user_id | BIGINT | Destination user |
| transfer_type | VARCHAR(50) | Transfer type |
| transfer_reason | VARCHAR(1000) | Transfer reason |
| expected_return_date | DATE | Expected return (for loans) |
| initiated_by_user_id | BIGINT | Initiator user ID |
| initiated_date | TIMESTAMP | Request date |
| approved_by_user_id | BIGINT | Approver user ID |
| approved_date | TIMESTAMP | Approval date |
| completed_date | TIMESTAMP | Completion date |
| status | VARCHAR(50) | Request status |
| process_instance_id | VARCHAR(255) | Flowable workflow ID |
| rejection_reason | VARCHAR(1000) | Rejection reason |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Update timestamp |

### maintenance_records Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| asset_instance_id | BIGINT | Asset reference |
| maintenance_type | VARCHAR(50) | Maintenance type |
| scheduled_date | DATE | Scheduled date |
| completed_date | DATE | Completion date |
| performed_by | VARCHAR(200) | Performer name |
| cost | DECIMAL(10,2) | Maintenance cost |
| description | VARCHAR(2000) | Description |
| next_maintenance_date | DATE | Next maintenance date |
| created_at | TIMESTAMP | Creation timestamp |

## Configuration

### application.yml

**Key Settings:**
```yaml
server:
  port: 8086

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/werkflow
  jpa:
    properties:
      hibernate:
        default_schema: inventory_service

  flyway:
    schemas: inventory_service
```

### Environment Variables

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5433
POSTGRES_USER=werkflow_admin
POSTGRES_PASSWORD=secure_password
POSTGRES_DB=werkflow

# Keycloak
KEYCLOAK_URL=http://localhost:8090
KEYCLOAK_REALM=werkflow

# Services
ADMIN_SERVICE_URL=http://localhost:8083
ENGINE_SERVICE_URL=http://localhost:8081

# Business Rules
HIGH_VALUE_THRESHOLD=5000.00
AUTO_RETURN_REMINDER_DAYS=7
MAINTENANCE_ALERT_DAYS=30
```

## Security

The Inventory Service uses Keycloak OAuth2/JWT authentication with role-based access control.

### Required Roles

- `INVENTORY_ADMIN` - Full access to all inventory operations
- `INVENTORY_MANAGER` - Approve/reject transfers, manage assets
- `ASSET_CUSTODIAN` - Manage assets under their custody
- `DEPARTMENT_MANAGER` - Request asset transfers for their department
- `EMPLOYEE` - View own asset assignments

### Access Control

- **POST /asset-*** - INVENTORY_ADMIN, INVENTORY_MANAGER
- **PUT /asset-*** - INVENTORY_ADMIN, INVENTORY_MANAGER
- **GET /asset-*** - All authenticated users
- **POST /custody-records** - INVENTORY_ADMIN, ASSET_CUSTODIAN
- **POST /transfer-requests** - All authenticated users
- **PUT /transfer-requests/*/approve** - INVENTORY_ADMIN, INVENTORY_MANAGER
- **GET /maintenance-records** - All authenticated users
- **POST /maintenance-records** - INVENTORY_ADMIN, INVENTORY_MANAGER

## Workflow Integration

The Inventory Service integrates with Flowable BPM Engine for asset transfer approval workflows:

**Workflow Stages:**
1. Request Transfer → Inventory Service creates transfer request
2. Route to Approver → Based on asset value and transfer type
3. Approval/Rejection → Appropriate authority approves or rejects
4. Update Custody → Custody records updated upon approval
5. Complete Transfer → Mark as completed upon physical transfer

**BPMN Integration:**
- Generic `ApprovalDelegate` for multi-level approvals
- `RestServiceDelegate` for inter-service communication
- Email notifications via `EmailDelegate`
- Custody updates via service calls

## Building and Running

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Flowable Engine Service (8081)
- Keycloak (8090)

### Build

```bash
cd services/inventory
mvn clean package
```

### Run

```bash
# Development mode
mvn spring-boot:run

# With specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8086"
```

### Docker

```bash
# Build Docker image
docker build -t werkflow-inventory:latest .

# Run container
docker run -p 8086:8086 \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_USER=werkflow_admin \
  -e POSTGRES_PASSWORD=secure_password \
  werkflow-inventory:latest
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AssetCategoryServiceTest

# Run with coverage
mvn test jacoco:report
```

## Swagger/OpenAPI Documentation

Access API documentation at: `http://localhost:8086/api/swagger-ui.html`

API Spec: `http://localhost:8086/api/v3/api-docs`

## Example Workflows

### Asset Assignment Workflow

1. Create asset in catalog (AssetDefinition)
2. Create physical asset instance (AssetInstance)
3. Create initial custody record
4. Request transfer to new department
5. Approve transfer request
6. Create new custody record for destination
7. Mark transfer as completed

### Order Batching Workflow

1. Multiple departments request assets
2. System batches compatible transfer requests
3. Batch approval by inventory manager
4. Batch fulfillment notification
5. Individual custody record updates

### Maintenance Scheduling

1. Asset definition marks requires_maintenance = true
2. Schedule initial maintenance record
3. Complete maintenance and update next date
4. Alert when next maintenance is due
5. Update asset status during maintenance

## Audit and Tracking

All entities track:
- `createdAt` - When record was created
- `updatedAt` - Last modification time
- User IDs for all operations (created_by, updated_by via audit listeners)

Custody records maintain complete history:
- Current custody (endDate = null)
- Historical records (endDate not null)
- Full audit trail for compliance

## Performance Optimization

- Database indexes on frequently queried columns
- Connection pooling (HikariCP) with configurable connections
- Lazy loading for related entities
- JPA query optimization
- Search and filter queries optimized for common operations

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running on port 5433
   - Verify credentials in application.yml
   - Ensure `inventory_service` schema exists

2. **Keycloak Authentication Failed**
   - Verify Keycloak is running on port 8090
   - Check realm and client configuration
   - Verify JWT issuer-uri is correct

3. **Migration Failures**
   - Check Flyway migration files exist
   - Verify schema exists: `CREATE SCHEMA inventory_service;`
   - Check database permissions

4. **Asset Tag Uniqueness Violation**
   - Asset tags must be globally unique
   - Check for duplicates: `SELECT asset_tag, COUNT(*) FROM asset_instances GROUP BY asset_tag HAVING COUNT(*) > 1;`

## Contributing

See [CLAUDE.md](../../CLAUDE.md) for development guidelines.

## Future Enhancements

- QR code/barcode generation and scanning
- Asset location tracking via IoT/beacons
- Predictive maintenance analytics
- Asset depreciation calculations
- Multi-site inventory management
- Bulk operations and import/export
- Advanced reporting and dashboards
- Mobile app for asset management
- Integration with procurement for automated ordering
