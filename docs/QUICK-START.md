# 🚀 Quick Start Guide

## Start Testing in 3 Steps!

### Step 1: Start PostgreSQL (30 seconds)
```bash
docker-compose up -d
```

Wait for PostgreSQL to be ready:
```bash
docker-compose logs -f postgres
```

Look for: `database system is ready to accept connections`

### Step 2: Start the Application (2 minutes)
```bash
mvn spring-boot:run
```

Wait for the application to start. Look for:
```
Started WerkflowApplication in X.XXX seconds
```

### Step 3: Test the APIs!

**Option A: Swagger UI (Easiest)**
1. Open browser: http://localhost:8080/api/swagger-ui.html
2. Try any endpoint with "Try it out" button

**Option B: Quick cURL Test**
```bash
# Get all employees (should return 8 sample employees)
curl http://localhost:8080/api/employees | json_pp

# Get all departments (should return 7 sample departments)
curl http://localhost:8080/api/departments | json_pp
```

**Option C: Postman**
1. Import `postman_collection.json`
2. Run any request

## 🎯 What's Included

### Sample Data (Pre-loaded)
- ✅ 7 Departments (Engineering, HR, Finance, Sales, Marketing + sub-departments)
- ✅ 8 Employees with complete profiles
- ✅ 4 Leave requests (including approved ones)
- ✅ 8 Attendance records
- ✅ 3 Performance reviews
- ✅ 4 Payroll records

### API Endpoints (49 total)
- ✅ Departments: 8 endpoints
- ✅ Employees: 10 endpoints
- ✅ Leaves: 9 endpoints
- ✅ Attendance: 7 endpoints
- ✅ Performance Reviews: 7 endpoints
- ✅ Payroll: 8 endpoints

## 🧪 Quick Tests

### Test 1: View All Employees
```bash
curl http://localhost:8080/api/employees | json_pp
```

### Test 2: Search for "John"
```bash
curl "http://localhost:8080/api/employees/search?searchTerm=john" | json_pp
```

### Test 3: Create New Department
```bash
curl -X POST http://localhost:8080/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "IT Operations",
    "code": "IT-OPS",
    "isActive": true
  }' | json_pp
```

### Test 4: Create New Employee
```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "employeeCode": "EMP999",
    "firstName": "Test",
    "lastName": "User",
    "email": "test.user@werkflow.com",
    "dateOfBirth": "1990-01-01",
    "joinDate": "2024-11-11",
    "jobTitle": "Test Engineer",
    "employmentStatus": "ACTIVE",
    "salary": 75000,
    "departmentId": 1
  }' | json_pp
```

### Test 5: Create Leave Request
```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 2,
    "leaveType": "ANNUAL",
    "startDate": "2024-12-20",
    "endDate": "2024-12-24",
    "numberOfDays": 4,
    "reason": "Holiday vacation"
  }' | json_pp
```

## 📊 Database Access

### pgAdmin (Web UI)
- URL: http://localhost:5050
- Email: admin@werkflow.com
- Password: admin123

Add Server Connection:
- Host: postgres (or localhost)
- Port: 5432
- Database: werkflow_db
- Username: werkflow_user
- Password: werkflow_pass

## ⚠️ Troubleshooting

### Issue: Port 8080 in use
```bash
# Change port in src/main/resources/application.yml
server:
  port: 8081  # Change to any available port
```

### Issue: PostgreSQL connection failed
```bash
# Check PostgreSQL is running
docker-compose ps

# Restart PostgreSQL
docker-compose restart postgres

# View logs
docker-compose logs postgres
```

### Issue: Flyway migration error
```bash
# Clean everything and restart
docker-compose down -v
docker-compose up -d
mvn clean spring-boot:run
```

## 📖 More Information

- **Complete Documentation**: See `README.md`
- **Detailed Testing Guide**: See `TESTING.md`
- **API Documentation**: http://localhost:8080/api/swagger-ui.html

## 🎉 You're Ready!

The application is now running with:
- ✅ 6 HR modules fully functional
- ✅ 49 REST API endpoints
- ✅ Sample data loaded
- ✅ Swagger documentation
- ✅ Database ready

**Next Steps:**
1. Test the APIs using Swagger UI or Postman
2. Explore the sample data
3. Try creating new records
4. Report any issues found
5. Proceed to Phase 3: Flowable BPM Integration

---

**Have fun testing! 🚀**
