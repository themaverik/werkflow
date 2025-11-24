# Keycloak RBAC Role Matrix Design for Werkflow Enterprise Platform

**Version:** 1.0
**Date:** 2025-11-24
**Status:** Production-Ready Design

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Keycloak Configuration Strategy](#keycloak-configuration-strategy)
3. [Role Hierarchy](#role-hierarchy)
4. [Department Role Matrices](#department-role-matrices)
5. [Workflow-to-Role Mapping](#workflow-to-role-mapping)
6. [DOA (Delegation of Authority) System](#doa-system)
7. [Custom Attributes & Claims](#custom-attributes--claims)
8. [Keycloak Configuration (JSON/YAML)](#keycloak-configuration)
9. [Application Integration](#application-integration)
10. [Task Routing Logic](#task-routing-logic)
11. [Operational Procedures](#operational-procedures)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      KEYCLOAK IAM LAYER (Port 8090)             │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Realm: werkflow-platform                                 │  │
│  │                                                          │  │
│  │  Users          Groups              Roles               │  │
│  │  ├─ john.doe    ├─ /HR Dept        ├─ admin            │  │
│  │  ├─ jane.smith  ├─ /IT Dept        ├─ employee         │  │
│  │  ├─ mike.chen   ├─ /Finance        ├─ asset_requester  │  │
│  │  └─ ...         ├─ /Procurement    ├─ asset_approver   │  │
│  │                 ├─ /Transport      ├─ doa_approver_*   │  │
│  │                 └─ /Inventory      └─ ...              │  │
│  │                                                          │  │
│  │  Custom Attributes (per user):                           │  │
│  │  ├─ department: "HR", "IT", "Finance", etc.             │  │
│  │  ├─ manager_id: UUID of direct manager                  │  │
│  │  ├─ cost_center: "HR-001", "IT-002", etc.               │  │
│  │  ├─ doa_level: 1, 2, 3, 4 (approval authority)          │  │
│  │  ├─ is_poc: true/false (Point of Contact)               │  │
│  │  ├─ hub_id: For warehouse managers                       │  │
│  │  └─ business_unit: For org structure                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Clients:                                                       │
│  ├─ admin-portal (Workflow UI)                                 │
│  ├─ hr-portal (HR UI)                                          │
│  ├─ workflow-engine (Backend Service)                          │
│  └─ ...                                                        │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ JWT Token with roles/groups/attributes
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼─────┐        ┌────▼──────┐      ┌────▼────┐
   │  Admin   │        │  Workflow │      │   HR    │
   │  Portal  │        │  Engine   │      │  Portal │
   │(Port 4000)        │(Port 8081)│      │(Port4001)
   └──────────┘        └───────────┘      └─────────┘
```

---

## Keycloak Configuration Strategy

### Configuration Approach

We use a **Hybrid Approach** combining:
- **Groups** for organizational hierarchy (departments, teams)
- **Roles** for functional authorities (permissions, approvals)
- **Custom Attributes** for workflow-specific data

### Why This Approach?

| Aspect | Groups | Roles | Custom Attributes |
|--------|--------|-------|-------------------|
| **Org Hierarchy** | ✅ Natural fit | ❌ Not ideal | ❌ Not ideal |
| **Permissions** | ❌ Not ideal | ✅ Perfect | ❌ Not ideal |
| **Workflow Data** | ❌ Not ideal | ❌ Not ideal | ✅ Perfect |
| **Scalability** | ✅ Scales well | ✅ Scales well | ✅ Scales well |
| **Token Size** | Medium | Small | Medium (only custom attrs) |

---

## Role Hierarchy

### Global Role Hierarchy

```
┌─────────────────────────────────────┐
│         super_admin (C-Suite)       │
│  - CEO, CFO, CTO, President         │
│  - Access: Everything               │
│  - Approval: Unlimited authority    │
└──────────┬──────────────────────────┘
           │
┌──────────▼──────────────────────────┐
│        admin (IT Admin)             │
│  - System administrator             │
│  - Keycloak admin access            │
│  - User & role management           │
└──────────┬──────────────────────────┘
           │
           ├─────────────────────────────┐
           │                             │
┌──────────▼──────────────┐  ┌──────────▼──────────────┐
│  department_head        │  │  department_poc        │
│  (HR, Finance, etc.)    │  │  (Point of Contact)    │
│  - Department authority │  │  - Manage dept flows   │
│  - All dept approvals   │  │  - Delegate tasks      │
└──────────┬──────────────┘  └──────────┬──────────────┘
           │                             │
           ├─────────┬───────────────────┤
           │         │                   │
    ┌──────▼──┐  ┌───▼────────┐  ┌──────▼──────┐
    │ Manager │  │ Specialist │  │  Approver   │
    │ (Team)  │  │ (Domain)   │  │ (DOA-based) │
    └──────┬──┘  └────┬───────┘  └──────┬──────┘
           │          │                 │
    ┌──────▼──┐  ┌────▼───────┐  ┌──────▼──────┐
    │Employee │  │   Requester│  │  Viewer    │
    │(base)   │  │  (submit)  │  │ (read-only)│
    └─────────┘  └────────────┘  └────────────┘
```

### Functional Roles (Cross-Department)

```
Approval Authorities (DOA-Based):
├─ doa_approver_level1    ($0 - $1K)
├─ doa_approver_level2    ($1K - $10K)
├─ doa_approver_level3    ($10K - $100K)
└─ doa_approver_level4    (>$100K)

Task Roles:
├─ asset_request_requester     (Can submit asset requests)
├─ asset_request_approver      (Can approve in department)
├─ asset_assignment_requester  (Can request asset assignment)
├─ inventory_manager           (Can manage inventory)
├─ procurement_approver        (Can approve POs)
├─ hub_manager                 (Manage warehouse hub)
├─ central_hub_manager         (Manage central hub)
├─ transport_manager           (Manage transport)
└─ leave_request_approver      (Approve leave requests)

Viewer Roles:
├─ request_viewer              (View own requests)
├─ department_viewer           (View department requests)
├─ finance_viewer              (View financial data)
└─ admin_viewer                (View all data)
```

---

## Department Role Matrices

### 1. HR Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **HR Officer** | Employee | Process HR requests, data entry | None | 5-10 |
| **HR Manager** | Manager | Manage HR team, approve requests | Department-level | 2-3 |
| **HR Head** | Department Head | All HR decisions, DOA Level 1 | Up to $1K | 1 |
| **HR POC** | Specialist | Coordinate with other departments | Department-level | 1-2 |
| **HR Admin** | Admin | User management, system config | All HR functions | 1 |

**Keycloak Roles Assigned:**
- HR Officer: `employee`, `asset_request_requester`, `leave_request_requester`
- HR Manager: `hr_manager`, `asset_request_approver`, `leave_request_approver`, `doa_approver_level1`
- HR Head: `hr_head`, `asset_request_approver`, `doa_approver_level2`
- HR POC: `department_poc`, `asset_request_approver`, `hr_coordinator`
- HR Admin: `admin`, `hr_admin`

**Keycloak Groups:**
- /HR Department
  - /HR Department/Managers
  - /HR Department/Specialists
  - /HR Department/POC

---

### 2. IT Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **IT Technician** | Employee | Install/maintain assets | None | 10-15 |
| **IT Manager** | Manager | Manage IT team, hardware | Department-level | 2-3 |
| **IT Head** | Department Head | IT strategy, all approvals | DOA Level 2 | 1 |
| **Inventory Manager** | Specialist | Manage asset inventory | Department-level | 2 |
| **Asset Manager** | Specialist | Track asset lifecycle | Department-level | 2 |
| **IT POC** | Specialist | Liaison with other depts | Department-level | 1-2 |
| **IT Admin** | Admin | System access, permissions | All IT functions | 1 |

**Keycloak Roles Assigned:**
- IT Technician: `employee`, `asset_request_requester`, `asset_assignment_requester`
- IT Manager: `it_manager`, `asset_request_approver`, `inventory_manager`, `doa_approver_level1`
- IT Head: `it_head`, `asset_request_approver`, `inventory_manager`, `doa_approver_level2`
- Inventory Manager: `inventory_manager`, `asset_viewer`, `inventory_admin`
- Asset Manager: `asset_manager`, `asset_viewer`
- IT POC: `department_poc`, `it_coordinator`, `asset_request_approver`
- IT Admin: `admin`, `it_admin`

**Keycloak Groups:**
- /IT Department
  - /IT Department/Managers
  - /IT Department/Inventory
  - /IT Department/Field Technicians
  - /IT Department/POC

---

### 3. Finance Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **Finance Officer** | Employee | Data entry, invoicing | None | 5-8 |
| **Finance Manager** | Manager | Budget management, approvals | DOA Level 1-2 | 2-3 |
| **Finance Head (CFO)** | Department Head | Financial strategy | DOA Level 3-4 | 1 |
| **DOA Approver Lvl 1** | Specialist | Approve up to $1K | $0 - $1K | 2-3 |
| **DOA Approver Lvl 2** | Specialist | Approve up to $10K | $1K - $10K | 2 |
| **DOA Approver Lvl 3** | Specialist | Approve up to $100K | $10K - $100K | 1 |
| **DOA Approver Lvl 4** | Specialist | Approve >$100K | Unlimited | 1 (CFO) |
| **Finance POC** | Specialist | Coordinate with other depts | Department-level | 1-2 |
| **Finance Admin** | Admin | User/role management | All Finance functions | 1 |

**Keycloak Roles Assigned:**
- Finance Officer: `employee`, `finance_viewer`
- Finance Manager: `finance_manager`, `doa_approver_level1`, `doa_approver_level2`
- Finance Head: `finance_head`, `doa_approver_level3`, `doa_approver_level4`
- DOA Approver: `doa_approver_level1` (or 2, 3, 4 as appropriate)
- Finance POC: `department_poc`, `finance_coordinator`
- Finance Admin: `admin`, `finance_admin`

**Keycloak Groups:**
- /Finance Department
  - /Finance Department/Approvers (contains DOA approvers)
  - /Finance Department/POC
  - /Finance Department/Analysts

---

### 4. Procurement Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **Procurement Specialist** | Employee | Create RFQs, vendor mgmt | None | 5-8 |
| **Procurement Manager** | Manager | Approve POs, contracts | Department-level | 2 |
| **Procurement Head** | Department Head | Vendor strategy | DOA Level 2 | 1 |
| **Vendor Manager** | Specialist | Manage vendor relationships | Department-level | 2 |
| **Procurement POC** | Specialist | Coordinate with others | Department-level | 1 |
| **Procurement Admin** | Admin | User/config management | All Procurement functions | 1 |

**Keycloak Roles Assigned:**
- Procurement Specialist: `employee`, `procurement_viewer`
- Procurement Manager: `procurement_manager`, `procurement_approver`, `doa_approver_level1`
- Procurement Head: `procurement_head`, `procurement_approver`, `doa_approver_level2`
- Vendor Manager: `vendor_manager`, `procurement_viewer`
- Procurement POC: `department_poc`, `procurement_coordinator`
- Procurement Admin: `admin`, `procurement_admin`

**Keycloak Groups:**
- /Procurement Department
  - /Procurement Department/Managers
  - /Procurement Department/Vendors
  - /Procurement Department/POC

---

### 5. Transport / Logistics Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **Driver** | Employee | Pick up, deliver assets | None | 15-20 |
| **Transport Coordinator** | Employee | Schedule shipments | None | 3-5 |
| **Logistics Manager** | Manager | Route planning, approvals | Department-level | 2 |
| **Transport Head** | Department Head | Transport strategy | DOA Level 1 | 1 |
| **Transport POC** | Specialist | Cross-dept coordination | Department-level | 1-2 |
| **Transport Admin** | Admin | User/config management | All Transport functions | 1 |

**Keycloak Roles Assigned:**
- Driver: `employee`, `driver`
- Transport Coordinator: `employee`, `transport_coordinator`
- Logistics Manager: `logistics_manager`, `transport_approver`, `doa_approver_level1`
- Transport Head: `transport_head`, `transport_approver`
- Transport POC: `department_poc`, `transport_coordinator`
- Transport Admin: `admin`, `transport_admin`

**Keycloak Groups:**
- /Transport Department
  - /Transport Department/Drivers
  - /Transport Department/Coordinators
  - /Transport Department/Management

---

### 6. Inventory / Warehouse Department

| Role | Hierarchy | Responsibilities | Approval Authority | User Count |
|------|-----------|------------------|-------------------|-----------|
| **Warehouse Staff** | Employee | Stock picking, packing | None | 10-15 |
| **Warehouse Manager** | Manager | Manage warehouse ops | Department-level | 2-3 |
| **Hub Manager** | Specialist | Manage specific hub | Hub-specific | 5-10 (one per hub) |
| **Central Hub Manager** | Specialist | Manage central hub | Department-level | 1-2 |
| **Inventory Manager** | Manager | System inventory mgmt | Department-level | 2-3 |
| **Inventory POC** | Specialist | Cross-dept coordination | Department-level | 1 |
| **Inventory Admin** | Admin | User/config management | All Inventory functions | 1 |

**Keycloak Roles Assigned:**
- Warehouse Staff: `employee`, `warehouse_staff`
- Warehouse Manager: `warehouse_manager`, `inventory_manager`
- Hub Manager: `hub_manager`, `hub_request_approver`
- Central Hub Manager: `central_hub_manager`, `hub_request_approver`
- Inventory Manager: `inventory_manager`, `asset_manager`
- Inventory POC: `department_poc`, `inventory_coordinator`
- Inventory Admin: `admin`, `inventory_admin`

**Keycloak Groups:**
- /Inventory Warehouse
  - /Inventory Warehouse/Hub A (for each hub)
  - /Inventory Warehouse/Hub B
  - /Inventory Warehouse/Central
  - /Inventory Warehouse/Management

---

## Workflow-to-Role Mapping

### Workflow 1: Asset Request (Multi-Department)

**Task Flow & Role Requirements:**

| Task | Assigned To | Required Role(s) | Group Requirement | Attribute Check | Auto-Route |
|------|-------------|------------------|-------------------|-----------------|-----------|
| **Submit Request** | Requester | `asset_request_requester` | Any (employee) | None | User submits |
| **Line Manager Approval** | User's Manager | `asset_request_approver` | /HR Dept/Managers | `manager_id` matches | `${submitter.manager_id}` |
| **IT Approval** | IT Manager/POC | `asset_request_approver` | /IT Dept/Managers OR /IT Dept/POC | `department=IT` | Any IT approver |
| **Stock Check** | System (Auto) | N/A | N/A | N/A | Auto-execute |
| **Procurement (if needed)** | Procurement POC | `procurement_approver` | /Procurement Dept | `department=Procurement` | Procurement POC |
| **Finance Approval (DOA)** | Finance Approver | `doa_approver_level_*` | /Finance Dept/Approvers | `doa_level >= required` | DOA level match |
| **PO Creation** | System (Auto) | N/A | N/A | N/A | Auto-execute |
| **IT Update Inventory** | Inventory Manager | `inventory_manager` | /IT Dept/Inventory | `department=IT` | IT Inventory Mgr |
| **Complete** | Requester | None | N/A | N/A | Auto-complete |

**Keycloak Token Claims Needed:**
```json
{
  "sub": "user-uuid",
  "preferred_username": "john.doe",
  "email": "john.doe@company.com",
  "groups": ["/HR Department", "/HR Department/Managers"],
  "realm_access": {
    "roles": ["employee", "asset_request_requester", "asset_request_approver"]
  },
  "department": "HR",
  "manager_id": "jane-smith-uuid",
  "cost_center": "HR-001",
  "doa_level": 1,
  "is_poc": false
}
```

---

### Workflow 2: Asset Assignment Request

| Task | Assigned To | Required Role(s) | Group Requirement | Attribute Check |
|------|-------------|------------------|-------------------|-----------------|
| **Submit Request** | User | `asset_assignment_requester` | Any | None |
| **Manager Approval** | User's Manager | `asset_assignment_approver` | User's dept managers | `manager_id` matches |
| **Inventory Approval** | Inventory Manager | `inventory_manager` | /Inventory Warehouse | `department=Inventory` |
| **Asset Assignment** | System | N/A | N/A | N/A |
| **Confirmation** | Requester | None | N/A | N/A |

---

### Workflow 3: Bulk Asset Type (Hub-Based)

| Task | Assigned To | Required Role(s) | Group Requirement | Attribute Check | Auto-Route |
|------|-------------|------------------|-------------------|-----------------|-----------|
| **User Submits** | Requester | `asset_request_requester` | Any | None | User |
| **Hub Manager Approval** | Hub Manager | `hub_request_approver` | /Inventory/Hub X | `hub_id` (if assigned) | Hub manager for region |
| **Central Hub Assigns** | Central Hub Mgr | `central_hub_manager` | /Inventory/Central | `department=Inventory` | Central hub mgr |
| **Hub Mgr Approve/Reject** | Assigned Hub Mgr | `hub_request_approver` | /Inventory/Hub X | `hub_id` matches | Assigned hub mgr |
| **Transport Request (if approved)** | System | N/A | N/A | N/A | Auto-create |
| **Logistics Approval** | Logistics Manager | `transport_approver` | /Transport/Management | `department=Transport` | Transport mgr |
| **Driver Assignment** | Driver | `driver` | /Transport/Drivers | `department=Transport` | Available driver |
| **Pickup & Delivery** | Driver | None | N/A | N/A | Driver |
| **User Verification** | Requester | None | N/A | N/A | User |

---

## DOA System

### DOA Level Matrix

```
Amount Range      | Approver Role           | Approver Level  | User Type
──────────────────┼────────────────────────┼─────────────────┼──────────────────
$0 - $1,000       | doa_approver_level1    | Team Manager    | Finance Manager
$1,000 - $10,000  | doa_approver_level2    | Department Head | Finance Head
$10,000 - $100K   | doa_approver_level3    | Senior Manager  | Finance Head/CFO
>$100,000         | doa_approver_level4    | C-Suite         | CFO/CEO
```

### Custom Attribute: `doa_level`

**Keycloak Configuration:**
```
Attribute Name: doa_level
Attribute Type: Integer (1, 2, 3, or 4)
User Attribute: doa_level
Mapper: oidc-usermodel-attribute-mapper
Include in: ID Token, Access Token

Example User:
  john.doe@company.com: doa_level = 1
  finance_head@company.com: doa_level = 3
  cfo@company.com: doa_level = 4
```

### DOA Determination Logic (Application Code)

```java
public int calculateRequiredDOALevel(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) {
        return 1;  // doa_approver_level1
    } else if (amount.compareTo(BigDecimal.valueOf(10000)) <= 0) {
        return 2;  // doa_approver_level2
    } else if (amount.compareTo(BigDecimal.valueOf(100000)) <= 0) {
        return 3;  // doa_approver_level3
    } else {
        return 4;  // doa_approver_level4
    }
}

public boolean isUserAuthorizedForDOA(int userDOALevel, int requiredDOALevel) {
    return userDOALevel >= requiredDOALevel;
}
```

---

## Custom Attributes & Claims

### User Custom Attributes

**Defined in Keycloak:**

| Attribute | Type | Example | Purpose |
|-----------|------|---------|---------|
| `department` | String | "HR", "IT", "Finance" | Department assignment |
| `manager_id` | String | "jane-smith-uuid" | Direct manager (for routing) |
| `cost_center` | String | "HR-001", "IT-002" | Financial tracking |
| `doa_level` | Integer | 1, 2, 3, 4 | Approval authority |
| `is_poc` | Boolean | true/false | Point of Contact flag |
| `hub_id` | String | "HUB-A", "HUB-B" | Assigned hub (for warehouse mgrs) |
| `business_unit` | String | "Operations", "Support" | Org structure |
| `phone` | String | "+1-555-0100" | Contact info |
| `location` | String | "New York", "Remote" | Work location |

### JWT Token Mapper Configuration

**In Keycloak Admin Console:**

```
For admin-portal Client:
  Mappers:
    1. Name: "department"
       Mapper Type: User Attribute
       User Attribute: department
       Token Claim Name: department
       Claim JSON Type: String

    2. Name: "manager_id"
       Mapper Type: User Attribute
       User Attribute: manager_id
       Token Claim Name: manager_id
       Claim JSON Type: String

    3. Name: "doa_level"
       Mapper Type: User Attribute
       User Attribute: doa_level
       Token Claim Name: doa_level
       Claim JSON Type: int

    4. Name: "is_poc"
       Mapper Type: User Attribute
       User Attribute: is_poc
       Token Claim Name: is_poc
       Claim JSON Type: boolean

    5. Name: "groups"
       Mapper Type: Group Membership
       Token Claim Name: groups
       Full group path: ON
```

### Sample JWT Token

```json
{
  "iss": "http://localhost:8090/realms/werkflow",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "aud": "admin-portal",
  "exp": 1700000000,
  "iat": 1699996400,
  "auth_time": 1699996400,
  "jti": "token-id-123",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "email": "john.doe@company.com",
  "email_verified": true,
  "preferred_username": "john.doe",

  "realm_access": {
    "roles": ["employee", "asset_request_requester", "asset_request_approver"]
  },

  "resource_access": {
    "admin-portal": {
      "roles": ["manager", "approver"]
    },
    "workflow-engine": {
      "roles": ["task_processor"]
    }
  },

  "groups": ["/HR Department", "/HR Department/Managers"],

  "department": "HR",
  "manager_id": "660f9511-f30c-52e5-b827-557766551111",
  "cost_center": "HR-001",
  "doa_level": 1,
  "is_poc": false,
  "hub_id": null,
  "business_unit": "Human Resources",
  "phone": "+1-555-0100",
  "location": "New York"
}
```

---

## Keycloak Configuration

### Keycloak Realm Export (keycloak-realm.json)

Save as: `/infrastructure/keycloak/realm-export.json`

```json
{
  "realm": "werkflow",
  "enabled": true,
  "sslRequired": "none",
  "accessTokenLifespan": 3600,
  "accessCodeLifespan": 600,
  "refreshTokenLifespan": 86400,
  "changePasswordAllowed": true,

  "roles": {
    "realm": [
      {
        "name": "admin",
        "description": "System Administrator",
        "composite": false
      },
      {
        "name": "super_admin",
        "description": "Super Administrator (C-Suite)",
        "composite": false
      },
      {
        "name": "employee",
        "description": "Base Employee Role",
        "composite": false
      },

      {
        "name": "asset_request_requester",
        "description": "Can submit asset requests",
        "composite": false
      },
      {
        "name": "asset_request_approver",
        "description": "Can approve asset requests",
        "composite": false
      },
      {
        "name": "asset_assignment_requester",
        "description": "Can request asset assignment",
        "composite": false
      },
      {
        "name": "asset_assignment_approver",
        "description": "Can approve asset assignments",
        "composite": false
      },

      {
        "name": "doa_approver_level1",
        "description": "Approve up to $1,000",
        "composite": false
      },
      {
        "name": "doa_approver_level2",
        "description": "Approve $1,000 to $10,000",
        "composite": false
      },
      {
        "name": "doa_approver_level3",
        "description": "Approve $10,000 to $100,000",
        "composite": false
      },
      {
        "name": "doa_approver_level4",
        "description": "Approve over $100,000",
        "composite": false
      },

      {
        "name": "hr_manager",
        "description": "HR Manager",
        "composite": true,
        "composites": ["asset_request_approver", "doa_approver_level1"]
      },
      {
        "name": "hr_head",
        "description": "HR Department Head",
        "composite": true,
        "composites": ["asset_request_approver", "doa_approver_level2", "department_head"]
      },

      {
        "name": "it_manager",
        "description": "IT Manager",
        "composite": true,
        "composites": ["asset_request_approver", "inventory_manager", "doa_approver_level1"]
      },
      {
        "name": "it_head",
        "description": "IT Department Head",
        "composite": true,
        "composites": ["asset_request_approver", "inventory_manager", "doa_approver_level2", "department_head"]
      },

      {
        "name": "finance_manager",
        "description": "Finance Manager",
        "composite": true,
        "composites": ["doa_approver_level1", "doa_approver_level2"]
      },
      {
        "name": "finance_head",
        "description": "Finance Department Head (CFO)",
        "composite": true,
        "composites": ["doa_approver_level3", "doa_approver_level4", "department_head"]
      },

      {
        "name": "procurement_manager",
        "description": "Procurement Manager",
        "composite": true,
        "composites": ["procurement_approver", "doa_approver_level1"]
      },
      {
        "name": "procurement_head",
        "description": "Procurement Department Head",
        "composite": true,
        "composites": ["procurement_approver", "doa_approver_level2", "department_head"]
      },

      {
        "name": "logistics_manager",
        "description": "Logistics Manager",
        "composite": true,
        "composites": ["transport_approver", "doa_approver_level1"]
      },
      {
        "name": "transport_head",
        "description": "Transport Department Head",
        "composite": true,
        "composites": ["transport_approver", "department_head"]
      },

      {
        "name": "inventory_manager",
        "description": "Inventory Manager",
        "composite": false
      },
      {
        "name": "hub_manager",
        "description": "Warehouse Hub Manager",
        "composite": false
      },
      {
        "name": "central_hub_manager",
        "description": "Central Hub Manager",
        "composite": true,
        "composites": ["hub_manager"]
      },

      {
        "name": "procurement_approver",
        "description": "Procurement Approver",
        "composite": false
      },
      {
        "name": "transport_approver",
        "description": "Transport Approver",
        "composite": false
      },
      {
        "name": "hub_request_approver",
        "description": "Hub Request Approver",
        "composite": false
      },
      {
        "name": "leave_request_approver",
        "description": "Leave Request Approver",
        "composite": false
      },

      {
        "name": "department_poc",
        "description": "Department Point of Contact",
        "composite": false
      },
      {
        "name": "department_head",
        "description": "Department Head Authority",
        "composite": false
      },

      {
        "name": "driver",
        "description": "Transport Driver",
        "composite": false
      },
      {
        "name": "warehouse_staff",
        "description": "Warehouse Staff",
        "composite": false
      }
    ]
  },

  "groups": [
    {
      "name": "HR Department",
      "path": "/HR Department",
      "attributes": {
        "department": ["HR"]
      },
      "subGroups": [
        {
          "name": "Managers",
          "path": "/HR Department/Managers",
          "attributes": {}
        },
        {
          "name": "Specialists",
          "path": "/HR Department/Specialists",
          "attributes": {}
        },
        {
          "name": "POC",
          "path": "/HR Department/POC",
          "attributes": {}
        }
      ]
    },
    {
      "name": "IT Department",
      "path": "/IT Department",
      "attributes": {
        "department": ["IT"]
      },
      "subGroups": [
        {
          "name": "Managers",
          "path": "/IT Department/Managers",
          "attributes": {}
        },
        {
          "name": "Inventory",
          "path": "/IT Department/Inventory",
          "attributes": {}
        },
        {
          "name": "Field Technicians",
          "path": "/IT Department/Field Technicians",
          "attributes": {}
        },
        {
          "name": "POC",
          "path": "/IT Department/POC",
          "attributes": {}
        }
      ]
    },
    {
      "name": "Finance Department",
      "path": "/Finance Department",
      "attributes": {
        "department": ["Finance"]
      },
      "subGroups": [
        {
          "name": "Approvers",
          "path": "/Finance Department/Approvers",
          "attributes": {}
        },
        {
          "name": "Analysts",
          "path": "/Finance Department/Analysts",
          "attributes": {}
        },
        {
          "name": "POC",
          "path": "/Finance Department/POC",
          "attributes": {}
        }
      ]
    },
    {
      "name": "Procurement Department",
      "path": "/Procurement Department",
      "attributes": {
        "department": ["Procurement"]
      },
      "subGroups": [
        {
          "name": "Managers",
          "path": "/Procurement Department/Managers",
          "attributes": {}
        },
        {
          "name": "Vendors",
          "path": "/Procurement Department/Vendors",
          "attributes": {}
        },
        {
          "name": "POC",
          "path": "/Procurement Department/POC",
          "attributes": {}
        }
      ]
    },
    {
      "name": "Transport Department",
      "path": "/Transport Department",
      "attributes": {
        "department": ["Transport"]
      },
      "subGroups": [
        {
          "name": "Drivers",
          "path": "/Transport Department/Drivers",
          "attributes": {}
        },
        {
          "name": "Coordinators",
          "path": "/Transport Department/Coordinators",
          "attributes": {}
        },
        {
          "name": "Management",
          "path": "/Transport Department/Management",
          "attributes": {}
        }
      ]
    },
    {
      "name": "Inventory Warehouse",
      "path": "/Inventory Warehouse",
      "attributes": {
        "department": ["Inventory"]
      },
      "subGroups": [
        {
          "name": "Hub A",
          "path": "/Inventory Warehouse/Hub A",
          "attributes": {}
        },
        {
          "name": "Hub B",
          "path": "/Inventory Warehouse/Hub B",
          "attributes": {}
        },
        {
          "name": "Hub C",
          "path": "/Inventory Warehouse/Hub C",
          "attributes": {}
        },
        {
          "name": "Central",
          "path": "/Inventory Warehouse/Central",
          "attributes": {}
        },
        {
          "name": "Management",
          "path": "/Inventory Warehouse/Management",
          "attributes": {}
        }
      ]
    }
  ],

  "clients": [
    {
      "clientId": "admin-portal",
      "name": "Workflow Admin Portal",
      "rootUrl": "http://localhost:4000",
      "redirectUris": ["http://localhost:4000/*"],
      "webOrigins": ["http://localhost:4000"],
      "publicClient": false,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": false,
      "clientSecret": "4uohM7y1sGkOcR2gTR1APo4JDmkwRxSv"
    },
    {
      "clientId": "hr-portal",
      "name": "HR Portal",
      "rootUrl": "http://localhost:4001",
      "redirectUris": ["http://localhost:4001/*"],
      "webOrigins": ["http://localhost:4001"],
      "publicClient": false,
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": true,
      "clientSecret": "HR_PORTAL_SECRET_2024_SECURE"
    },
    {
      "clientId": "workflow-engine",
      "name": "Workflow Engine Service",
      "standardFlowEnabled": false,
      "serviceAccountsEnabled": true,
      "directAccessGrantsEnabled": true,
      "clientSecret": "workflow-engine-secret-key"
    }
  ]
}
```

---

## Application Integration

### Spring Security Configuration

**File:** `/services/engine/src/main/java/com/werkflow/engine/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/health/**", "/metrics/**").permitAll()
                .antMatchers("/api/workflows/**").authenticated()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter());

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Extract realm roles
            List<String> realmRoles = jwt.getClaimAsStringList("realm_access.roles");
            if (realmRoles != null) {
                realmRoles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
            }

            // Extract client-specific roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null && resourceAccess.containsKey("admin-portal")) {
                Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get("admin-portal");
                List<String> roles = (List<String>) clientRoles.get("roles");
                if (roles != null) {
                    roles.forEach(role ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    );
                }
            }

            return authorities;
        });

        return converter;
    }
}
```

### Authorization Annotations

**File:** `/services/engine/src/main/java/com/werkflow/engine/controller/WorkflowController.java`

```java
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowTaskRouter taskRouter;

    @PostMapping
    @PreAuthorize("hasRole('ASSET_REQUEST_REQUESTER')")
    public ResponseEntity<WorkflowResponse> submitAssetRequest(
            @RequestBody AssetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workflowService.submitRequest(request, jwt));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ASSET_REQUEST_APPROVER')")
    public ResponseEntity<WorkflowResponse> approveRequest(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workflowService.approveRequest(id, jwt));
    }

    @PutMapping("/{id}/approve-finance")
    @PreAuthorize("hasAnyRole('DOA_APPROVER_LEVEL1', 'DOA_APPROVER_LEVEL2', 'DOA_APPROVER_LEVEL3', 'DOA_APPROVER_LEVEL4')")
    public ResponseEntity<WorkflowResponse> approveFinance(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        // Get request amount and validate user's DOA level
        WorkflowRequest request = workflowService.getRequest(id);
        int requiredDOALevel = calculateDOALevel(request.getAmount());
        int userDOALevel = Integer.parseInt(jwt.getClaimAsString("doa_level"));

        if (userDOALevel < requiredDOALevel) {
            throw new AccessDeniedException("Insufficient delegation of authority");
        }

        return ResponseEntity.ok(workflowService.approveFinance(id, jwt));
    }
}
```

---

## Task Routing Logic

### Task Router Service

**File:** `/services/engine/src/main/java/com/werkflow/engine/service/WorkflowTaskRouter.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTaskRouter {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserRepository userRepository;

    /**
     * Route line manager approval task
     */
    public String routeLineManagerApproval(String submitterId) {
        User submitter = userRepository.findByKeycloakId(submitterId);
        String managerId = submitter.getManagerId();

        if (managerId == null) {
            log.error("No manager found for user: {}", submitterId);
            throw new RuntimeException("User has no assigned manager");
        }

        log.info("Routing line manager approval to: {}", managerId);
        return managerId;
    }

    /**
     * Route to department team (all managers in department)
     */
    public List<String> routeToDepartmentTeam(String department, String roleRequired) {
        String groupPath = "/" + department.toUpperCase() + " Department/Managers";
        List<String> groupMembers = keycloakAdminClient.getGroupMembers(groupPath);

        // Filter by required role
        return groupMembers.stream()
            .filter(userId -> userHasRole(userId, roleRequired))
            .collect(Collectors.toList());
    }

    /**
     * Route to DOA approver based on amount
     */
    public String routeToDoaApprover(BigDecimal amount) {
        int requiredDOALevel = calculateDOALevel(amount);
        String requiredRole = "doa_approver_level" + requiredDOALevel;

        // Find Finance Approver with matching DOA level
        List<String> financeApprovers = keycloakAdminClient.getGroupMembers(
            "/Finance Department/Approvers"
        );

        String approver = financeApprovers.stream()
            .filter(userId -> {
                String userDOALevel = keycloakAdminClient.getUserAttribute(userId, "doa_level");
                int doa = Integer.parseInt(userDOALevel);
                return doa >= requiredDOALevel;
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No approver with DOA level " + requiredDOALevel));

        log.info("Routing DOA approval (${}) to approver with DOA level {}", amount, requiredDOALevel);
        return approver;
    }

    /**
     * Route to hub manager
     */
    public String routeToHubManager(String hubId) {
        String groupPath = "/Inventory Warehouse/Hub " + hubId;
        List<String> hubMembers = keycloakAdminClient.getGroupMembers(groupPath);

        // Return hub manager (should be one per hub)
        String hubManager = hubMembers.stream()
            .filter(userId -> userHasRole(userId, "hub_manager"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hub manager found for hub: " + hubId));

        return hubManager;
    }

    private int calculateDOALevel(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) return 1;
        if (amount.compareTo(BigDecimal.valueOf(10000)) <= 0) return 2;
        if (amount.compareTo(BigDecimal.valueOf(100000)) <= 0) return 3;
        return 4;
    }

    private boolean userHasRole(String userId, String role) {
        List<String> userRoles = keycloakAdminClient.getUserRoles(userId);
        return userRoles.contains(role);
    }
}
```

---

## Operational Procedures

### Creating a New User

**Steps:**

1. **Create User in Keycloak**
   ```
   Keycloak Admin Console
   → Realm Users
   → Create User

   Details:
   - Username: john.doe
   - Email: john.doe@company.com
   - First Name: John
   - Last Name: Doe
   - Email Verified: ON
   - Enabled: ON
   ```

2. **Set Password**
   ```
   Credentials Tab
   → Set Password
   → Value: [temporary password]
   → Temporary: ON
   ```

3. **Assign Groups**
   ```
   Groups Tab
   → Available Groups
   → Select: /HR Department/Managers
   → Join
   ```

4. **Assign Roles**
   ```
   Role Mappings Tab
   → Assign Role
   → Select: employee, asset_request_requester, asset_request_approver
   ```

5. **Set Custom Attributes**
   ```
   Attributes Tab
   → Add Attribute

   - Key: department, Value: HR
   - Key: manager_id, Value: [UUID of manager]
   - Key: cost_center, Value: HR-001
   - Key: doa_level, Value: 1
   - Key: is_poc, Value: false
   ```

### Promoting User to Manager

**Steps:**

1. **Add to Manager Group**
   ```
   Groups Tab
   → Remove from: /HR Department/Specialists
   → Add to: /HR Department/Managers
   ```

2. **Assign Manager Roles**
   ```
   Role Mappings Tab
   → Add Composite Role: hr_manager
   (This automatically includes: asset_request_approver, doa_approver_level1)
   ```

3. **Update DOA Level**
   ```
   Attributes Tab
   → Update: doa_level = 2
   ```

### Updating DOA Level (Delegation of Authority)

**Example: Temporary increase for acting authority**

```
User: finance_mgr@company.com
Current doa_level: 2
New doa_level: 3 (Acting Finance Head during leave)
Duration: 2025-12-01 to 2025-12-31

Steps:
1. Go to User Details
2. Attributes Tab
3. Update doa_level: 2 → 3
4. Add note in realm (audit trail)
5. Remove after period expires
```

### Assigning POC (Point of Contact)

**Steps:**

1. **Add to POC Group**
   ```
   Groups Tab
   → Add to: /HR Department/POC
   ```

2. **Assign POC Role**
   ```
   Role Mappings Tab
   → Add Role: department_poc
   ```

3. **Mark as POC**
   ```
   Attributes Tab
   → Add: is_poc = true
   ```

### Audit Trail

**Keycloak Audit Events:**
```
Events Tab
- Login success/failure
- User create/update/delete
- Group add/remove
- Role assign/revoke
```

**Application-Level Audit:**
```
Log workflow decisions:
- Task assignments
- Approvals
- DOA changes
- Role changes
```

---

## Summary

This Keycloak RBAC design provides:

✅ **Organizational Clarity**
- Department hierarchy via Groups
- Clear role definitions
- User-to-role mapping

✅ **Workflow Automation**
- Automatic task routing
- Manager assignment via attributes
- DOA-based approvals

✅ **Scalability**
- Support multiple departments
- Extensible group structure
- Custom attribute framework

✅ **Security**
- Token-based authentication
- Role-based authorization
- Audit trail for compliance

✅ **Operations**
- Simple user onboarding
- Clear promotion path
- Easy DOA management

**Next Steps:**
1. Import realm-export.json to Keycloak
2. Create sample users and test workflows
3. Implement application security config
4. Configure task routing logic
5. Set up audit logging

