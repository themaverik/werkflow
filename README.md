# whrkflow - HR Management Platform

A comprehensive HR management platform with visual BPMN workflow designer and dynamic form builder.

## 🏗️ Monorepo Structure

```
whrkflow/
├── backend/              # Spring Boot REST API + Flowable BPM
├── frontend/             # Next.js React UI (Coming Soon)
├── docker-compose.yml    # Infrastructure services
├── README.md            # This file
└── docs/                # Documentation
```

## 🚀 Features

### Core HR Management
- **Department Management**: Hierarchical department structure
- **Employee Management**: Complete employee lifecycle with relationships
- **Leave Management**: Leave requests with approval workflows
- **Attendance Tracking**: Daily attendance with worked hours calculation
- **Performance Reviews**: Employee evaluations and feedback
- **Payroll Management**: Salary calculations with deductions and bonuses

### Workflow Automation (Flowable BPM) ✅
- **Leave Approval Process**: Manager review and approval workflow
- **Employee Onboarding**: Parallel task execution (IT, HR, Manager)
- **Performance Review Cycle**: Self-assessment, manager evaluation, HR approval
- **Custom BPMN Workflows**: Extensible workflow engine

### Visual Workflow Designer (In Progress)
- **BPMN Designer**: Visual process modeling with bpmn-js
- **Form Builder**: Drag-drop dynamic form creation
- **Task Portal**: User-friendly task management interface
- **Process Deployment**: One-click deployment from UI

## 🛠️ Technology Stack

### Backend
- **Java 21**
- **Spring Boot 3.3.2**
- **PostgreSQL 15**
- **Flowable BPM 7.0.1**
- **Keycloak OAuth2/JWT**
- **Flyway** (Database migrations)
- **Maven** (Build tool)

### Frontend (Coming Soon)
- **Next.js 14** (App Router)
- **React 18**
- **TypeScript 5**
- **Tailwind CSS**
- **shadcn/ui** components
- **bpmn-js** (BPMN designer)
- **Form.io** (Form builder)
- **NextAuth** (Authentication)

### Infrastructure
- **Docker & Docker Compose**
- **PostgreSQL 15**
- **Keycloak 23**
- **pgAdmin 4**

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Node.js 20+** (for frontend)
- **Docker & Docker Compose**
- **PostgreSQL 15+** (via Docker)

## 🏃 Quick Start

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL, Keycloak, and pgAdmin
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Run Backend

```bash
cd backend

# Build and run
mvn clean spring-boot:run
```

### 3. Run Frontend (Coming Soon)

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

### 4. Access the Applications

**Backend:**
- API Base URL: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- API Docs: http://localhost:8080/api/v3/api-docs

**Frontend:**
- Application: http://localhost:3000
- Process Designer: http://localhost:3000/studio/processes
- Form Builder: http://localhost:3000/studio/forms
- Task Portal: http://localhost:3000/portal/tasks

**Infrastructure:**
- Keycloak: http://localhost:8090 (admin/admin123)
- pgAdmin: http://localhost:5050 (admin@whrkflow.com/admin123)

## 📚 Documentation

- [Backend README](./backend/README.md) - Backend service documentation
- [Frontend README](./frontend/README.md) - Frontend application documentation (Coming Soon)
- [Workflow Guide](./WORKFLOW_GUIDE.md) - Workflow usage and API reference
- [Testing Guide](./TESTING.md) - API testing instructions
- [Keycloak Setup](./KEYCLOAK_SETUP.md) - Authentication configuration
- [Quick Start](./QUICK_START.md) - Getting started guide
- [Development Roadmap](./ROADMAP.md) - Frontend implementation plan (Coming Soon)

## 🏗️ Project Structure

```
whrkflow/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/whrkflow/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/      # Spring Data repositories
│   │   │   │   ├── service/         # Business logic
│   │   │   │   └── workflow/        # Flowable BPM
│   │   │   └── resources/
│   │   │       ├── application.yml  # Configuration
│   │   │       ├── db/migration/    # Flyway migrations
│   │   │       └── processes/       # BPMN files
│   │   └── test/                    # Tests
│   ├── pom.xml                      # Maven config
│   └── README.md                    # Backend docs
│
├── frontend/                        # (Coming Soon)
│   ├── app/                         # Next.js App Router
│   │   ├── (auth)/                  # Authentication pages
│   │   ├── (studio)/                # Process & Form Designer
│   │   └── (portal)/                # Task Management
│   ├── components/                  # React components
│   ├── lib/                         # Utilities & API
│   ├── package.json
│   └── README.md
│
├── docker-compose.yml               # Infrastructure services
├── .gitignore
└── README.md                        # This file
```

## 🔐 Authentication

The platform uses Keycloak for OAuth2/JWT authentication with role-based access control:

- **HR_ADMIN**: Full platform access, workflow design
- **HR_MANAGER**: HR operations, workflow management
- **MANAGER**: Team management, task approval
- **EMPLOYEE**: Self-service access, task completion

See [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md) for configuration details.

## 🔄 Workflow Processes

Three production-ready BPMN workflows are included:

1. **Leave Approval** - Automated leave request processing
2. **Employee Onboarding** - Multi-team onboarding coordination
3. **Performance Review** - Comprehensive review cycle management

See [WORKFLOW_GUIDE.md](./WORKFLOW_GUIDE.md) for usage details.

## 🧪 Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests (Coming Soon)
cd frontend
npm test
```

See [TESTING.md](./TESTING.md) for API testing with Postman.

## 🚀 Deployment

### Backend (Spring Boot)

```bash
cd backend
mvn clean package
java -jar target/whrkflow-1.0.0.jar
```

### Frontend (Next.js)

```bash
cd frontend
npm run build
npm start
```

### Docker (Full Stack)

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🛣️ Roadmap

### ✅ Completed
- [x] Core HR CRUD APIs
- [x] Flowable BPM integration
- [x] Three production workflows
- [x] Keycloak authentication
- [x] Comprehensive API documentation
- [x] Monorepo structure

### 🚧 In Progress
- [ ] Next.js frontend with BPMN designer
- [ ] Dynamic form builder
- [ ] Task management portal

### 📋 Planned
- [ ] Real-time notifications
- [ ] Mobile app (React Native)
- [ ] Analytics dashboard
- [ ] Multi-tenancy support
- [ ] API rate limiting

See [ROADMAP.md](./ROADMAP.md) for detailed frontend implementation plan.

## 🤝 Contributing

This is a proprietary project. For questions or issues, contact the development team.

## 📄 License

Proprietary - All rights reserved

---

**Note**: This is a monorepo structure. Backend is production-ready. Frontend development is in progress.
