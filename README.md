# W[ER]kflow Enterprise Platform

A comprehensive enterprise workflow platform with self-service workflow creation, visual BPMN designer, dynamic form builder, and multi-department orchestration.

## Overview

Werkflow is an enterprise workflow platform that enables departments to create and manage their own workflows without code, while maintaining centralized governance and orchestration through Flowable BPM.

**Key Capabilities:**
- 90%+ no-code workflow creation
- Visual BPMN designer
- Dynamic form builder (Form.io)
- Generic reusable delegates
- Multi-department support (HR, Finance, Procurement, Legal, etc.)
- Centralized BPM orchestration
- OAuth2/JWT authentication with Keycloak

## Monorepo Structure

```
werkflow/
├── services/                    # Backend microservices
│   ├── engine/                  # Flowable BPM orchestration (8081) [Implemented]
│   ├── hr/                      # HR domain service (8082) [Implemented]
│   ├── admin/                   # User/org/dept management (8083) [Phase 1]
│   ├── finance/                 # Finance service (8084) [Implemented]
│   ├── procurement/             # Procurement service (8085) [Implemented]
│   ├── inventory/               # Inventory service (8086) [Implemented]
│   └── ...                      # Additional services (Legal, etc.)
├── frontends/                   # Frontend applications
│   ├── admin-portal/            # Workflow designer (4000) [Implemented]
│   ├── hr-portal/               # HR portal (4001) [Phase 2]
│   └── shared/                  # Shared UI components
├── shared/                      # Shared libraries
│   ├── common/                  # Common utilities [Phase 1]
│   └── delegates/               # Generic Flowable delegates [Phase 1]
├── infrastructure/
│   └── docker/                  # Docker configs
│       ├── Dockerfile           # Multi-stage build [Phase 0]
│       └── docker-compose.yml   # Local development [Implemented]
├── docs/                        # Documentation
│   ├── Enterprise_Workflow_Roadmap.md
│   ├── KEYCLOAK_SETUP.md
│   ├── QUICK_START.md
│   ├── TESTING.md
│   └── WORKFLOW_GUIDE.md        # Workflow integration guide [Implemented]
├── ROADMAP-DRAFT.md             # Enterprise platform roadmap
├── ROADMAP.md                   # Development tracking [Implemented]
├── CLAUDE.md                    # Development guidelines
└── README.md                    # This file
```

## Current Features

### Phase 3: Backend Services (Completed)

**Department Services:**
- **HR Management**: Complete employee lifecycle, leave, attendance, performance reviews, payroll
- **Finance Service**: CapEx request management, budget tracking, multi-level approvals
- **Procurement Service**: Purchase requests, vendor management, purchase order generation
- **Inventory Service**: Asset management, custody tracking, transfer workflows

**Workflow Engine:**
- **Engine Service**: Centralized Flowable BPM orchestration (port 8081)
- **BPMN Workflows**:
  - Leave approval, employee onboarding, performance review (HR)
  - CapEx approval with budget verification (Finance)
  - Procurement approval with vendor selection (Procurement)
  - Asset transfer with custody tracking (Inventory)

### Phase 3.5: Frontend Orchestration (Completed)

**Admin Portal Features:**
- **Visual BPMN Designer**: Create workflows with bpmn-js (no code required)
- **Dynamic Form Builder**: Form.io integration for workflow forms
- **Multi-Department Workflow Dashboard**: Centralized view across HR, Finance, Procurement, Inventory
- **Monitoring Dashboard**: Real-time process statistics with 30-second polling
- **Analytics Dashboard**: Performance metrics, completion rates, bottleneck analysis
- **Task Portal**: My Tasks and Group Tasks with claiming and completion
- **Process Management**: Deploy, version, and manage workflows via UI

**Platform Features:**
- **Authentication**: Keycloak OAuth2/JWT with role-based access control
- **90%+ No-Code**: Generic delegates enable workflow creation without coding
- **Real-Time Updates**: React Query polling for live dashboard updates
- **Multi-Department**: HR, Finance, Procurement, Inventory with centralized orchestration
- **Microservices Architecture**: Schema-separated PostgreSQL, independent services

### Recently Completed (Phase 5-6)
- Task Management APIs with pagination and filtering
- Async Notification Service with email templates
- Process Monitoring APIs for visibility and audit trails
- Form-js Backend Integration with 8 API endpoints
- CapEx Integration Test Suite (58 test cases)
- All build errors resolved (100+ DTO errors + 22 API compatibility issues)

### In Progress (Phase 7)
- End-to-End Integration Testing (CapEx, Procurement, Inventory workflows)
- Form-js Frontend-Backend Integration
- Admin Service (User/Org/Dept Management)
- HR Portal (employee-facing)
- Performance testing and optimization

### Planned (Future Phases 8+)
- Legal service (Contract review, compliance workflows)
- Event-driven architecture (Kafka)
- CQRS pattern implementation
- Advanced analytics and reporting
- Production deployment (Kubernetes, CI/CD)

## Technology Stack

### Backend Services
- **Java 21**
- **Spring Boot 3.3.x**
- **Flowable BPM 7.0.x**
- **PostgreSQL 15** (schema separation per service)
- **Keycloak** (OAuth2/JWT)
- **Flyway** (Database migrations)
- **Maven** (Build tool)

### Frontend Applications
- **Next.js 14** (App Router)
- **React 18**
- **TypeScript 5**
- **Tailwind CSS**
- **shadcn/ui** components
- **bpmn-js 17** (BPMN designer)
- **Form.io 5** (Form builder)
- **NextAuth v5** (Authentication)

### Infrastructure
- **Docker & Docker Compose**
- **PostgreSQL 15** (port 5433)
- **Keycloak 23** (port 8090)
- Future: **Apache Kafka**, **Redis**, **Elasticsearch**

## Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Node.js 20+**
- **Docker & Docker Compose**
- **Git**

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/themaverik/werkflow.git
cd werkflow
```

### 2. Start Infrastructure Services

```bash
cd infrastructure/docker
docker-compose up -d

# Verify services
docker-compose ps
```

### 3. Configure Environment

```bash
# Copy example files
cp .env.shared.example .env.shared
cp .env.hr.example .env.hr

# Update with your values
vi .env.shared
vi .env.hr
```

### 4. Run HR Service

```bash
cd services/hr

# Build and run
mvn clean spring-boot:run
```

### 5. Run Admin Portal

```bash
cd frontends/admin-portal

# Install dependencies
npm install

# Copy environment file
cp .env.local.example .env.local

# Update environment variables
vi .env.local

# Run development server (port 4000)
npm run dev
```

### 6. Access Applications

**Backend Services:**
- Engine Service: http://localhost:8081/api
- HR Service: http://localhost:8082/api
- Admin Service: http://localhost:8083/api
- Finance Service: http://localhost:8084/api
- Procurement Service: http://localhost:8085/api
- Inventory Service: http://localhost:8086/api
- Swagger UI: http://localhost:808X/api/swagger-ui.html (replace X with service port)

**Admin Portal (http://localhost:4000):**
- Studio → Processes: BPMN workflow designer
- Studio → Forms: Form.io form builder
- Studio → Workflows: Multi-department workflow dashboard
- Portal → Monitoring: Real-time process monitoring
- Portal → Analytics: Performance metrics and analytics
- Portal → Tasks: My Tasks and Group Tasks management

**Infrastructure:**
- Keycloak: http://localhost:8090 (admin/admin123)
- PostgreSQL: localhost:5433 (werkflow_admin/secure_password)
- pgAdmin: http://localhost:5050 (admin@werkflow.com/admin)

## Authentication and Roles

Keycloak-based OAuth2/JWT with role-based access control:

**Roles:**
- `SUPER_ADMIN` - Platform administration
- `ORG_ADMIN` - Organization management
- `DEPT_MANAGER` - Department management, workflow design
- `HR_ADMIN` - HR operations
- `HR_MANAGER` - HR workflow management
- `MANAGER` - Team management, approvals
- `EMPLOYEE` - Self-service access

See [docs/KEYCLOAK_SETUP.md](./docs/KEYCLOAK_SETUP.md) for configuration.

## Multi-Department Support

Werkflow supports multiple departments with centralized orchestration:

### HR Department (Implemented)
**Service Port**: 8082
- Employee lifecycle management
- Leave approval workflows
- Performance review processes
- Employee onboarding workflows
- Attendance and payroll integration

### Finance Department (Phase 3 - Implemented)
**Service Port**: 8084
- **CapEx Approval Workflow**: Multi-level approval based on amount thresholds
  - Manager approval ($0-50k)
  - VP approval ($50k-250k)
  - CFO approval ($250k+)
- Budget verification and reservation
- ROI and payback period tracking
- Supporting documents management

### Procurement Department (Phase 3 - Implemented)
**Service Port**: 8085
- **Purchase Request Workflow**: Vendor selection and approval
  - Supervisor approval (all requests)
  - Manager approval (>$10k)
  - Director approval (>$50k)
- Vendor master data management
- Quotation comparison
- Purchase order generation

### Inventory Department (Phase 3 - Implemented)
**Service Port**: 8086
- **Asset Transfer Workflow**: Custody tracking and approval
  - Current custodian release
  - Manager approval for high-value assets (>$5k)
  - New custodian acceptance
- Asset category and definition management
- Inter-department custody model
- Maintenance record tracking

### Legal Department (Planned)
**Service Port**: 8087
- Contract review workflows
- Compliance approval processes
- Document approval workflows
- Legal risk assessment

**Architecture**: Each department maintains autonomy while using shared Engine Service for workflow orchestration and generic delegates for cross-service communication.

## Generic Delegates (Phase 1)

Reusable workflow components enable no-code workflow creation:

- **RestServiceDelegate** - HTTP API calls
- **EmailDelegate** - Email notifications
- **NotificationDelegate** - Multi-channel notifications
- **ValidationDelegate** - Form/data validation
- **ApprovalDelegate** - Standard approvals with escalation
- **FormRequestDelegate** - Cross-department form requests

## Documentation

- [Services](#services-documentation)
  - [HR Service](./services/hr/README.md)
  - [Engine Service](./services/engine/README.md) (Phase 1)
  - [Admin Service](./services/admin/README.md) (Phase 1)
- [Frontends](#frontend-documentation)
  - [Admin Portal](./frontends/admin-portal/README.md)
  - [HR Portal](./frontends/hr-portal/README.md) (Phase 2)
- [Guides](#guides)
  - [Workflow Guide](./docs/WORKFLOW_GUIDE.md)
  - [Testing Guide](./docs/TESTING.md)
  - [Keycloak Setup](./docs/KEYCLOAK_SETUP.md)
  - [Quick Start](./docs/QUICK_START.md)
- [Roadmaps](#roadmaps)
  - [Enterprise Roadmap](./docs/Enterprise_Workflow_Roadmap.md)
  - [Implementation Roadmap](./ROADMAP-DRAFT.md)
  - [Development Tracking](./ROADMAP.md)
- [Development](#development)
  - [Guidelines](./CLAUDE.md)

## Testing

### Backend Services

```bash
# HR Service
cd services/hr
mvn test

# Integration tests
mvn verify
```

### Frontend Applications

```bash
# Admin Portal
cd frontends/admin-portal
npm test

# Type checking
npm run type-check

# Linting
npm run lint
```

See [docs/TESTING.md](./docs/TESTING.md) for API testing with Postman.

## Deployment

### Local Development (Docker Compose)

```bash
cd infrastructure/docker
docker-compose up -d
```

### Production Deployment

See ROADMAP-DRAFT.md for production deployment strategy (Phase 3):
- Kubernetes orchestration
- Helm charts
- CI/CD pipelines
- Terraform infrastructure

## Development Roadmap

### Phase 0: Foundation (Completed)
- [x] Monorepo restructure
- [x] Environment configuration
- [x] Docker Compose setup
- [x] PostgreSQL schema separation

### Phase 1: Core Platform (Completed)
- [x] Flowable Engine Service
- [x] HR Service integration
- [x] Basic BPMN workflows

### Phase 3: Multi-Department Services (Completed)
- [x] **Finance Service** (port 8084)
  - CapEx approval workflow with multi-level approvals
  - Budget verification and reservation
- [x] **Procurement Service** (port 8085)
  - Purchase request workflow with vendor selection
  - Multi-level approval and PO generation
- [x] **Inventory Service** (port 8086)
  - Asset transfer workflow with custody tracking
  - Inter-department asset management
- [x] **BPMN Workflows** in Engine Service
- [x] **Infrastructure** updates (Docker, DB schemas)

### Phase 3.5: Frontend Orchestration (Completed)
- [x] **Monitoring Dashboard** - Real-time process statistics
- [x] **Analytics Dashboard** - Performance metrics and bottlenecks
- [x] **Multi-Department Workflow Dashboard** - Centralized orchestration view
- [x] **Task Portal** - My Tasks, Group Tasks, claiming, completion
- [x] **Real-time updates** - 30-second polling with React Query
- [x] **Zero mock data** - All dashboards connected to Engine Service APIs

### Phase 5: Task Management, Notifications, Process Monitoring (Completed)
- [x] Task APIs with pagination, filtering, HATEOAS
- [x] Async notification service with email templates (3 types)
- [x] Process monitoring APIs (details, tasks, history, business key)
- [x] CapEx integration test suite (10 files, 58 tests)

### Phase 6: Form-js Backend Integration (Completed)
- [x] FormSchemaService with caching and versioning
- [x] TaskFormService for form submission
- [x] FormSchemaValidator with comprehensive validation
- [x] 8 API endpoints for form management
- [x] 5 ready-to-use form schemas (CapEx, Leave, Procurement)
- [x] Database migration for form storage

### Phase 7: End-to-End Integration Testing & Polish (In Progress)
- [ ] End-to-end testing (CapEx, Procurement, Inventory workflows)
- [ ] Form-js frontend-backend integration
- [ ] Performance testing and optimization
- [ ] Regression testing across all workflows
- [ ] Admin Service (User/Org/Dept Management)
- [ ] HR Portal (employee-facing)

### Phase 8: Future Enhancements
- [ ] Legal service
- [ ] Event-driven architecture (Kafka)
- [ ] CQRS implementation
- [ ] Advanced analytics

See [ROADMAP.md](./ROADMAP.md) for detailed development tracking and [docs/TESTING.md](./docs/TESTING.md) for testing procedures.

## Configuration

### Port Allocation

**Backend Services:**
- Engine Service: 8081 (Implemented)
- HR Service: 8082 (Implemented)
- Admin Service: 8083 (In Progress)
- Finance Service: 8084 (Implemented)
- Procurement Service: 8085 (Implemented)
- Inventory Service: 8086 (Implemented)
- Legal Service: 8087 (Planned)

**Frontend Applications:**
- Admin Portal: 4000 (Implemented)
- HR Portal: 4001 (In Progress)
- Finance Portal: 4002 (Planned)

**Infrastructure:**
- PostgreSQL: 5433 (Implemented)
- Keycloak: 8090 (Implemented)
- pgAdmin: 5050 (Implemented)
- Kafka: 9092 (planned)
- Redis: 6379 (planned)

### Environment Configuration

Hybrid approach:
- `.env.shared` - Common infrastructure (Postgres, Keycloak, JWT)
- `.env.{service}` - Service-specific config
- Override hierarchy: Central → Service → Runtime

## Contributing

See [CLAUDE.md](./CLAUDE.md) for development guidelines including:
- Branch naming conventions
- Commit message format
- Code style standards
- Testing requirements

## License

Proprietary - All rights reserved

---

## Current Status Summary

**Overall Status**: Phase 6 Complete - Backend API Layer Ready for Integration Testing

**Completion Level**: 80-85% of feature roadmap

**Backend Services**: 100% Complete
- All 5 department services implemented (Engine, HR, Finance, Procurement, Inventory)
- Multi-level approval workflows for each domain
- Cross-service REST API integration
- PostgreSQL with schema separation

**Frontend Admin Portal**: 95% Complete
- BPMN visual designer with bpmn-js
- Form.io dynamic form builder and renderer
- Process management and versioning
- Real-time monitoring and analytics dashboards
- Multi-department workflow orchestration view
- Task portal with claiming and completion

**Architecture**: Microservices with centralized BPM orchestration
- Hybrid workflow deployment (centralized + distributed)
- OAuth2/JWT authentication via Keycloak
- Real-time updates with React Query polling

**Approach**: 90%+ no-code through visual tools
- BPMN Designer: Create workflows without XML knowledge
- Form Builder: Design forms without coding
- Generic Delegates: Pre-built workflow components
- One-click deployment from UI

**Key Implemented Features**:
- Multi-Department Support: HR, Finance, Procurement, Inventory (4 of 5 planned)
- Visual Workflow Designer: Full BPMN 2.0 support with properties panel
- Dynamic Form Builder: Form.io integration with 8+ pre-built templates
- Real-Time Dashboards: Monitoring, Analytics, Multi-Department Workflows
- Task Management: My Tasks, Group Tasks, claiming, and completion
- Authentication & Authorization: Keycloak-based RBAC with role hierarchy
- Process Versioning: Automatic version tracking for all deployments

**Recently Completed** (Phase 5-6):
- Task Management APIs (GET /workflows/tasks/my-tasks, /group-tasks)
- Async Notification Service (Task Assigned, Completed, Delegated with email templates)
- Process Monitoring APIs (details, tasks, history, business key queries)
- Form-js Backend Integration (8 endpoints, 5 form schemas, full validation)
- CapEx Integration Test Suite (10 files, 58 test cases, all API compatible)
- Build System: Resolved 100+ compilation errors and 22 Flowable API compatibility issues

**In Progress**:
- End-to-End Integration Testing (CapEx, Procurement, Inventory workflows)
- Form-js Frontend Integration (React components + backend APIs)
- Performance Testing and Optimization
- Admin Service (User/Organization/Department Management)
- HR Portal (Employee-facing application)

**Planned Future Phases** (Phase 8+):
- Event-driven architecture (Apache Kafka)
- Advanced analytics and reporting
- Legal department workflows
- Kubernetes production deployment
- CI/CD pipeline automation

**Latest Update**: 2025-11-30 - Phase 6 backend complete, all build errors resolved, ready for integration testing
