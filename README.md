# W[ER]kflow Enterprise Platform

A comprehensive enterprise workflow platform with self-service workflow creation, visual BPMN designer, dynamic form builder, and multi-department orchestration.

## 🎯 Overview

Werkflow is an enterprise workflow platform that enables departments to create and manage their own workflows without code, while maintaining centralized governance and orchestration through Flowable BPM.

**Key Capabilities:**
- 90%+ no-code workflow creation
- Visual BPMN designer
- Dynamic form builder (Form.io)
- Generic reusable delegates
- Multi-department support (HR, Finance, Procurement, Legal, etc.)
- Centralized BPM orchestration
- OAuth2/JWT authentication with Keycloak

## 🏗️ Monorepo Structure

```
werkflow/
├── services/                    # Backend microservices
│   ├── engine/                  # Flowable BPM orchestration (8081) [Phase 1]
│   ├── hr/                      # HR domain service (8082) ✅
│   ├── admin/                   # User/org/dept management (8083) [Phase 1]
│   ├── finance/                 # Finance service (8084) [Phase 3]
│   ├── procurement/             # Procurement service (8085) [Phase 3]
│   └── ...                      # Additional services
├── frontends/                   # Frontend applications
│   ├── admin-portal/            # Workflow designer (4000) ✅
│   ├── hr-portal/               # HR portal (4001) [Phase 2]
│   └── shared/                  # Shared UI components
├── shared/                      # Shared libraries
│   ├── common/                  # Common utilities [Phase 1]
│   └── delegates/               # Generic Flowable delegates [Phase 1]
├── infrastructure/
│   └── docker/                  # Docker configs
│       ├── Dockerfile           # Multi-stage build [Phase 0]
│       └── docker-compose.yml   # Local development ✅
├── docs/                        # Documentation
│   ├── Enterprise_Workflow_Roadmap.md
│   ├── KEYCLOAK_SETUP.md
│   ├── QUICK_START.md
│   ├── TESTING.md
│   └── WORKFLOW_GUIDE.md
├── ROADMAP-DRAFT.md             # Enterprise platform roadmap
├── ROADMAP.md                   # Development tracking
├── CLAUDE.md                    # Development guidelines
└── README.md                    # This file
```

## 🚀 Current Features

### ✅ Implemented
- **HR Management**: Complete employee lifecycle, leave, attendance, performance reviews, payroll
- **BPMN Workflows**: Leave approval, employee onboarding, performance review processes
- **Visual Designer**: BPMN process designer with bpmn-js
- **Form Builder**: Dynamic form creation with Form.io
- **Authentication**: Keycloak OAuth2/JWT with role-based access control
- **Process Management**: Deploy, version, and manage workflows via UI

### 🚧 In Progress (Phase 0-1)
- Monorepo restructuring ✅
- Environment configuration ✅
- Flowable Engine Service (Week 3-4)
- Generic Delegates Library (Week 5-6)
- Admin Service (Week 7-8)

### 📋 Planned (Phase 2-3)
- HR Portal (employee-facing)
- Additional department services (Finance, Procurement, Legal)
- Event-driven architecture (Kafka)
- CQRS pattern
- Real-time notifications
- Advanced analytics

## 🛠️ Technology Stack

### Backend Services
- **Java 17**
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

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.9+**
- **Node.js 20+**
- **Docker & Docker Compose**
- **Git**

## 🏃 Quick Start

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

**Services:**
- HR Service API: http://localhost:8082/api
- HR Swagger UI: http://localhost:8082/api/swagger-ui.html

**Frontends:**
- Admin Portal: http://localhost:4000
- Process Designer: http://localhost:4000/processes
- Form Builder: http://localhost:4000/forms
- Task Portal: http://localhost:4000/tasks

**Infrastructure:**
- Keycloak: http://localhost:8090 (admin/admin123)
- PostgreSQL: localhost:5433 (werkflow_admin/secure_password)

## 🔐 Authentication & Roles

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

## 🏢 Multi-Department Support

Werkflow supports multiple departments out of the box:

### Current: HR (✅ Implemented)
- Employee management
- Leave management
- Performance reviews
- Onboarding workflows

### Planned: Finance (Phase 3)
- Invoice approval
- Budget requests
- Expense management

### Planned: Procurement (Phase 3)
- Purchase requests
- Vendor management
- Contract workflows

### Planned: Legal (Phase 3)
- Contract review
- Compliance workflows
- Document approvals

Each department maintains autonomy while using shared infrastructure and generic delegates.

## 🔄 Generic Delegates (Phase 1)

Reusable workflow components enable no-code workflow creation:

- **RestServiceDelegate** - HTTP API calls
- **EmailDelegate** - Email notifications
- **NotificationDelegate** - Multi-channel notifications
- **ValidationDelegate** - Form/data validation
- **ApprovalDelegate** - Standard approvals with escalation
- **FormRequestDelegate** - Cross-department form requests

## 📚 Documentation

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

## 🧪 Testing

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

## 🚀 Deployment

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

## 🛣️ Development Roadmap

### ✅ Phase 0: Foundation (Week 1-2)
- [x] Rename whrkflow → werkflow
- [x] Restructure to monorepo
- [x] Environment configuration
- [x] Git repository setup
- [ ] Docker multi-stage build
- [ ] Updated docker-compose

### 🚧 Phase 1: Core Platform (Week 3-8)
- [ ] Flowable Engine Service (Week 3-4)
- [ ] Generic Delegates Library (Week 5-6)
- [ ] Admin Service (Week 7-8)
- [ ] HR Service migration

### 📋 Phase 2: Department Portals (Week 9-12)
- [ ] HR Portal (employee-facing)
- [ ] Department-specific workflows
- [ ] Enhanced monitoring

### 📋 Phase 3: Future Enhancements
- [ ] Finance, Procurement, Legal services
- [ ] Event-driven architecture (Kafka)
- [ ] CQRS implementation
- [ ] Kubernetes deployment
- [ ] CI/CD automation
- [ ] Advanced analytics

See [ROADMAP-DRAFT.md](./ROADMAP-DRAFT.md) for detailed implementation plan.

## 🔧 Configuration

### Port Allocation

**Backend Services:**
- Engine Service: 8081
- HR Service: 8082
- Admin Service: 8083
- Finance Service: 8084 (future)
- Procurement Service: 8085 (future)

**Frontend Applications:**
- Admin Portal: 4000
- HR Portal: 4001 (future)
- Finance Portal: 4002 (future)

**Infrastructure:**
- PostgreSQL: 5433
- Keycloak: 8090
- Kafka: 9092 (future)
- Redis: 6379 (future)

### Environment Configuration

Hybrid approach:
- `.env.shared` - Common infrastructure (Postgres, Keycloak, JWT)
- `.env.{service}` - Service-specific config
- Override hierarchy: Central → Service → Runtime

## 🤝 Contributing

See [CLAUDE.md](./CLAUDE.md) for development guidelines including:
- Branch naming conventions
- Commit message format
- Code style standards
- Testing requirements

## 📄 License

Proprietary - All rights reserved

---

**Status**: Phase 0 in progress - Transforming from HR-only platform to enterprise workflow platform

**Architecture**: Microservices with centralized BPM orchestration

**Approach**: 90%+ no-code through generic delegates and visual designers
