# Werkflow Backend (Spring Boot)

This is the backend service for the Werkflow HR Management Platform.

## Technology Stack

- **Java 21**
- **Spring Boot 3.3.2**
- **PostgreSQL 15**
- **Flowable BPM 7.0.1**
- **Keycloak OAuth2/JWT Authentication**
- **Flyway** for database migrations
- **Maven** for build management

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/werkflow/
│   │   │   ├── config/          # Spring configuration
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── enums/           # Enumerations
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── workflow/        # Flowable BPM components
│   │   │       ├── controller/  # Workflow REST controllers
│   │   │       ├── delegate/    # BPMN service tasks
│   │   │       ├── dto/         # Workflow DTOs
│   │   │       └── service/     # Workflow services
│   │   └── resources/
│   │       ├── application.yml  # Application configuration
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       └── processes/       # BPMN workflow definitions
│   └── test/                    # Unit and integration tests
└── pom.xml                      # Maven dependencies
```

## Running the Backend

### Prerequisites

Make sure Docker is running with PostgreSQL and Keycloak:

```bash
# From project root
docker-compose up -d
```

### Build and Run

```bash
# Navigate to backend folder
cd backend

# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

### Verify

- **API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Actuator**: http://localhost:8080/api/actuator/health

## API Endpoints

### HR Management APIs
- `/api/employees` - Employee CRUD
- `/api/departments` - Department management
- `/api/leaves` - Leave management
- `/api/attendance` - Attendance tracking
- `/api/performance-reviews` - Performance reviews
- `/api/payroll` - Payroll management

### Workflow APIs
- `/api/workflows/processes/*` - Process management
- `/api/workflows/tasks/*` - Task management

### Flowable Deployment APIs
- `/api/flowable/deployments` - Deploy BPMN processes
- `/api/flowable/forms` - Deploy form definitions
- `/api/flowable/process-definitions` - List processes
- `/api/flowable/forms/{formKey}` - Get form definition

## Authentication

All endpoints require JWT authentication via Keycloak. See `../KEYCLOAK_SETUP.md` for setup instructions.

### Roles
- **HR_ADMIN** - Full access
- **HR_MANAGER** - Management access
- **MANAGER** - Team management
- **EMPLOYEE** - Self-service access

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`:
- `V1__create_hr_tables.sql` - Initial schema
- `V2__seed_initial_data.sql` - Sample data

Migrations run automatically on application startup.

## Workflows

Three BPMN workflows are included:
1. **Leave Approval Process** (`leave-approval-process.bpmn20.xml`)
2. **Employee Onboarding Process** (`employee-onboarding-process.bpmn20.xml`)
3. **Performance Review Process** (`performance-review-process.bpmn20.xml`)

See `../WORKFLOW_GUIDE.md` for details.

## Development

### Hot Reload

```bash
mvn spring-boot:run -Dspring-boot.run.fork=false
```

### Run Tests

```bash
mvn test
```

## Configuration

Key configuration in `src/main/resources/application.yml`:
- Database connection
- Keycloak OAuth2 settings
- Flowable engine configuration
- Logging levels

## Troubleshooting

**Port 8080 already in use:**
```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9
```

**Database connection error:**
```bash
# Restart PostgreSQL container
docker-compose restart postgres
```

**Keycloak authentication fails:**
- Verify Keycloak is running: http://localhost:8090
- Check realm and client configuration
- See `../KEYCLOAK_SETUP.md`

## Documentation

- [Main README](../README.md) - Project overview
- [Workflow Guide](../WORKFLOW_GUIDE.md) - Workflow usage
- [Testing Guide](../TESTING.md) - API testing
- [Keycloak Setup](../KEYCLOAK_SETUP.md) - Authentication setup
- [Quick Start](../QUICK_START.md) - Getting started

## License

Proprietary - All rights reserved
