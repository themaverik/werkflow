# Testing Guide for werkflow

This guide will help you test the werkflow HR Management Platform APIs.

## 🚀 Quick Start

### Step 1: Start PostgreSQL Database

```bash
# Start PostgreSQL using Docker Compose
docker-compose up -d

# Verify PostgreSQL is running
docker-compose ps
```

Expected output:
```
NAME                  IMAGE                    STATUS
werkflow-postgres     postgres:15-alpine       Up
werkflow-pgadmin      dpage/pgadmin4:latest    Up
```

### Step 2: Start the Application

```bash
# Build and run the application
mvn spring-boot:run
```

Wait for the application to start. You should see:
```
Started WerkflowApplication in X.XXX seconds
```

### Step 3: Verify Application is Running

Open your browser and navigate to:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/v3/api-docs

## 🧪 Testing Methods

### Method 1: Using Swagger UI (Recommended for Beginners)

1. Open http://localhost:8080/api/swagger-ui.html
2. You'll see all API endpoints grouped by resource
3. Click on any endpoint to expand it
4. Click "Try it out"
5. Fill in the required parameters
6. Click "Execute"
7. View the response below

**Example: Get All Departments**
- Navigate to "Departments" section
- Click on `GET /api/departments`
- Click "Try it out"
- Click "Execute"
- View the list of departments

### Method 2: Using Postman

1. **Import the Collection**
   - Open Postman
   - Click "Import"
   - Select `postman_collection.json` from the project root
   - The collection will be imported with all endpoints

2. **Set the Base URL** (Already configured)
   - Variable: `baseUrl`
   - Value: `http://localhost:8080/api`

3. **Test Endpoints**
   - Browse the collection folders
   - Click on any request
   - Click "Send"
   - View the response

### Method 3: Using cURL (Command Line)

Copy and paste these commands in your terminal:

#### Test 1: Get All Departments
```bash
curl -X GET http://localhost:8080/api/departments | json_pp
```

#### Test 2: Get All Employees
```bash
curl -X GET http://localhost:8080/api/employees | json_pp
```

#### Test 3: Create a New Department
```bash
curl -X POST http://localhost:8080/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Success",
    "description": "Customer success and support team",
    "code": "CS",
    "isActive": true
  }' | json_pp
```

#### Test 4: Create a New Employee
```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "employeeCode": "EMP010",
    "firstName": "Bob",
    "lastName": "Smith",
    "email": "bob.smith@werkflow.com",
    "phoneNumber": "+1234567899",
    "dateOfBirth": "1992-03-20",
    "joinDate": "2024-11-11",
    "jobTitle": "DevOps Engineer",
    "employmentStatus": "ACTIVE",
    "salary": 90000.00,
    "address": "456 Cloud Street, City, State",
    "departmentId": 1,
    "managerId": 1
  }' | json_pp
```

#### Test 5: Search Employees
```bash
curl -X GET "http://localhost:8080/api/employees/search?searchTerm=john" | json_pp
```

#### Test 6: Create a Leave Request
```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 2,
    "leaveType": "ANNUAL",
    "startDate": "2024-12-25",
    "endDate": "2024-12-29",
    "numberOfDays": 4,
    "reason": "Christmas holiday"
  }' | json_pp
```

#### Test 7: Approve a Leave Request (Use the ID from Test 6)
```bash
curl -X PUT "http://localhost:8080/api/leaves/1/approve?approverId=1&remarks=Enjoy your holiday" | json_pp
```

#### Test 8: Create Attendance Record
```bash
curl -X POST http://localhost:8080/api/attendances \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 2,
    "attendanceDate": "2024-11-11",
    "checkInTime": "09:00:00",
    "checkOutTime": "18:00:00",
    "status": "PRESENT",
    "remarks": "On time"
  }' | json_pp
```

#### Test 9: Get Payrolls for November 2024
```bash
curl -X GET "http://localhost:8080/api/payrolls/period?month=11&year=2024" | json_pp
```

#### Test 10: Create Performance Review
```bash
curl -X POST http://localhost:8080/api/performance-reviews \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 7,
    "reviewDate": "2024-12-15",
    "reviewPeriodStart": "2024-06-01",
    "reviewPeriodEnd": "2024-12-15",
    "rating": "MEETS_EXPECTATIONS",
    "score": 80.0,
    "strengths": "Good frontend development skills, responsive to feedback",
    "areasForImprovement": "Could improve testing coverage",
    "goals": "Increase test coverage to 80%, learn React advanced patterns",
    "comments": "Solid performance, keep improving",
    "reviewerId": 3
  }' | json_pp
```

## 📊 Exploring Sample Data

The application comes pre-loaded with sample data. Here's what to explore:

### Departments
```bash
# Get all departments
curl http://localhost:8080/api/departments | json_pp

# Get Engineering department (ID: 1)
curl http://localhost:8080/api/departments/1 | json_pp

# Get sub-departments of Engineering
curl http://localhost:8080/api/departments/1/sub-departments | json_pp
```

### Employees
```bash
# Get all employees
curl http://localhost:8080/api/employees | json_pp

# Get employees in Engineering department
curl http://localhost:8080/api/employees/department/1 | json_pp

# Get employees managed by John Doe (ID: 1)
curl http://localhost:8080/api/employees/manager/1 | json_pp

# Search for "john"
curl "http://localhost:8080/api/employees/search?searchTerm=john" | json_pp
```

### Leaves
```bash
# Get all leave requests
curl http://localhost:8080/api/leaves | json_pp

# Get leaves for employee Jane Smith (ID: 2)
curl http://localhost:8080/api/leaves/employee/2 | json_pp

# Get pending leave requests
curl http://localhost:8080/api/leaves/status/PENDING | json_pp
```

### Attendance
```bash
# Get all attendance records
curl http://localhost:8080/api/attendances | json_pp

# Get attendance for employee (ID: 2)
curl http://localhost:8080/api/attendances/employee/2 | json_pp

# Get attendance for date range
curl "http://localhost:8080/api/attendances/employee/2/range?startDate=2024-11-01&endDate=2024-11-30" | json_pp
```

### Performance Reviews
```bash
# Get all performance reviews
curl http://localhost:8080/api/performance-reviews | json_pp

# Get reviews for employee (ID: 2)
curl http://localhost:8080/api/performance-reviews/employee/2 | json_pp
```

### Payroll
```bash
# Get all payrolls
curl http://localhost:8080/api/payrolls | json_pp

# Get payrolls for employee (ID: 2)
curl http://localhost:8080/api/payrolls/employee/2 | json_pp

# Get payrolls for November 2024
curl "http://localhost:8080/api/payrolls/period?month=11&year=2024" | json_pp
```

## 🔍 Testing Scenarios

### Scenario 1: New Employee Onboarding
1. Create a new department (if needed)
2. Create a new employee
3. Verify employee was created
4. Search for the new employee

### Scenario 2: Leave Request and Approval
1. Create a leave request for an employee
2. View pending leave requests
3. Approve the leave request
4. Verify the leave status changed to APPROVED

### Scenario 3: Daily Attendance Tracking
1. Create attendance record for today
2. View attendance for the employee
3. Update attendance with checkout time
4. Verify worked hours are calculated

### Scenario 4: Performance Review Cycle
1. Create a performance review
2. View reviews for the employee
3. Employee acknowledges the review
4. Verify acknowledgment

### Scenario 5: Monthly Payroll Processing
1. Create payroll for an employee
2. View payrolls for the month
3. Mark payroll as paid
4. Verify payment status

## 🐛 Troubleshooting

### Application won't start
```bash
# Check if PostgreSQL is running
docker-compose ps

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Port 8080 already in use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change the port in application.yml
```

### Database connection error
```bash
# Verify PostgreSQL credentials in application.yml match docker-compose.yml
# Default credentials:
# - Database: werkflow_db
# - Username: werkflow_user
# - Password: werkflow_pass
```

### Flyway migration errors
```bash
# Clean and restart
mvn clean
docker-compose down -v
docker-compose up -d
mvn spring-boot:run
```

## 📈 Performance Testing

### Using Apache Bench (ab)
```bash
# Install Apache Bench
sudo apt-get install apache2-utils  # Ubuntu/Debian
brew install apr-util                # macOS

# Test GET /api/employees endpoint
ab -n 1000 -c 10 http://localhost:8080/api/employees
```

### Using wrk
```bash
# Install wrk
sudo apt-get install wrk  # Ubuntu/Debian
brew install wrk          # macOS

# Test endpoint
wrk -t4 -c100 -d30s http://localhost:8080/api/employees
```

## 🎯 Next Steps

After testing the core CRUD operations:
1. ✅ Verify all endpoints work correctly
2. ✅ Test with different data scenarios
3. ✅ Check error handling with invalid data
4. 🚀 Proceed to Phase 3: Flowable BPM Integration

## 💡 Tips

1. **Use Swagger UI** for quick exploration and testing
2. **Import Postman Collection** for organized testing
3. **Check application logs** for debugging
4. **Use pgAdmin** (http://localhost:5050) for database inspection
   - Login: admin@werkflow.com / admin123
5. **Save successful test data** for future reference

## 🔗 Useful URLs

- Application Base URL: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- API Docs (JSON): http://localhost:8080/api/v3/api-docs
- pgAdmin: http://localhost:5050

---

Happy Testing! 🎉
