# Werkflow - Workflow Management Guide

## Overview

Phase 3 has implemented Flowable BPM integration for automated HR workflow processes. This guide explains how to use the workflow features.

## Available Workflows

### 1. Leave Approval Process (`leaveApprovalProcess`)

Automates the employee leave request approval workflow.

**Process Flow:**
1. Employee submits leave request → System starts workflow
2. Manager reviews request
3. Manager approves or rejects
4. If approved: HR is notified → Employee is notified
5. If rejected: Employee is notified
6. Process ends

**Process Variables:**
- `leaveId`: Leave request ID
- `employeeId`: Employee ID
- `employeeName`: Employee name
- `leaveType`: VACATION, SICK, PERSONAL, UNPAID
- `startDate`: Leave start date
- `endDate`: Leave end date
- `reason`: Leave reason
- `managerId`: Manager ID
- `approved`: Boolean (set by manager)
- `managerComments`: Manager's comments

### 2. Employee Onboarding Process (`employeeOnboardingProcess`)

Automates the new employee onboarding workflow with parallel task execution.

**Process Flow:**
1. New employee hired → System starts workflow
2. **Parallel execution:**
   - IT team: Set up accounts, laptop, system access
   - HR team: Collect documents, contracts, benefits enrollment
   - Manager: Assign buddy, prepare workspace, schedule first week
3. Send welcome email to employee
4. First day orientation by HR
5. Update employee status to ACTIVE
6. Notify all stakeholders
7. Process ends

**Process Variables:**
- `employeeId`: Employee ID
- `employeeName`: Employee name
- `employeeEmail`: Employee email
- `departmentId`: Department ID
- `position`: Job position
- `startDate`: Start date
- `managerId`: Manager ID
- Plus task-specific variables (IT setup, HR docs, manager prep)

### 3. Performance Review Process (`performanceReviewProcess`)

Automates the comprehensive performance review cycle.

**Process Flow:**
1. Review cycle started → System starts workflow
2. Employee completes self-assessment
3. Manager completes evaluation
4. Manager schedules review meeting
5. Conduct review meeting
6. Decision point: Employee agrees?
   - Yes → Proceed to HR approval
   - No → Address concerns → Proceed to HR approval
7. HR reviews and approves
8. Update review record
9. Notify all stakeholders
10. Process ends

**Process Variables:**
- `reviewId`: Performance review ID
- `employeeId`: Employee ID
- `employeeName`: Employee name
- `managerId`: Manager ID
- `reviewPeriodStart`: Review period start date
- `reviewPeriodEnd`: Review period end date
- `reviewType`: ANNUAL, QUARTERLY, PROBATION
- Plus assessment and evaluation details

## REST API Endpoints

All workflow endpoints are under `/api/workflows` and require authentication.

### Process Management

**Start a Process:**
```http
POST /api/workflows/processes/start
Authorization: Bearer {token}
Content-Type: application/json

{
  "processDefinitionKey": "leaveApprovalProcess",
  "businessKey": "LEAVE-2024-001",
  "variables": {
    "leaveId": 1,
    "employeeId": 5,
    "employeeName": "John Doe",
    "leaveType": "VACATION",
    "startDate": "2024-06-01",
    "endDate": "2024-06-10",
    "reason": "Summer vacation",
    "managerId": 2
  }
}
```

**Get Process Instance:**
```http
GET /api/workflows/processes/{processInstanceId}
Authorization: Bearer {token}
```

**Get All Process Instances:**
```http
GET /api/workflows/processes?processDefinitionKey=leaveApprovalProcess
Authorization: Bearer {token}
```

**Delete Process Instance:**
```http
DELETE /api/workflows/processes/{processInstanceId}?deleteReason=Cancelled
Authorization: Bearer {token}
```

**Get Process Variables:**
```http
GET /api/workflows/processes/{processInstanceId}/variables
Authorization: Bearer {token}
```

**Set Process Variables:**
```http
PUT /api/workflows/processes/{processInstanceId}/variables
Authorization: Bearer {token}
Content-Type: application/json

{
  "customVariable": "value",
  "anotherVariable": 123
}
```

### Task Management

**Get Tasks for Assignee:**
```http
GET /api/workflows/tasks/assignee/{userId}
Authorization: Bearer {token}
```

**Get Tasks for Group:**
```http
GET /api/workflows/tasks/group/MANAGER
Authorization: Bearer {token}
```

**Get Tasks for Process:**
```http
GET /api/workflows/tasks/process/{processInstanceId}
Authorization: Bearer {token}
```

**Get Task Details:**
```http
GET /api/workflows/tasks/{taskId}
Authorization: Bearer {token}
```

**Claim a Task:**
```http
POST /api/workflows/tasks/{taskId}/claim?userId={userId}
Authorization: Bearer {token}
```

**Complete a Task:**
```http
POST /api/workflows/tasks/complete
Authorization: Bearer {token}
Content-Type: application/json

{
  "taskId": "12345",
  "variables": {
    "approved": true,
    "managerComments": "Approved. Enjoy your vacation!"
  },
  "comment": "Looks good, approved."
}
```

## Role-Based Access Control

Workflow endpoints are protected by Keycloak roles:

| Role | Permissions |
|------|-------------|
| **HR_ADMIN** | Full access to all workflow operations |
| **HR_MANAGER** | Manage processes, delete instances, handle escalations |
| **MANAGER** | Start processes, complete tasks, view team workflows |
| **EMPLOYEE** | View assigned tasks, complete own tasks |

## Example Workflow Scenarios

### Scenario 1: Employee Requests Leave

1. **Employee submits leave via Leave API**
   ```http
   POST /api/leaves
   {
     "employeeId": 5,
     "type": "VACATION",
     "startDate": "2024-06-01",
     "endDate": "2024-06-10",
     "reason": "Family vacation"
   }
   ```

2. **System starts workflow automatically (in service layer)**
   ```http
   POST /api/workflows/processes/start
   {
     "processDefinitionKey": "leaveApprovalProcess",
     "businessKey": "LEAVE-123",
     "variables": { ... }
   }
   ```

3. **Manager checks pending tasks**
   ```http
   GET /api/workflows/tasks/group/MANAGER
   ```

4. **Manager completes review task**
   ```http
   POST /api/workflows/tasks/complete
   {
     "taskId": "task-456",
     "variables": {
       "approved": true,
       "managerComments": "Approved!"
     }
   }
   ```

5. **System automatically:**
   - Updates leave status to APPROVED
   - Notifies HR
   - Notifies employee
   - Ends process

### Scenario 2: New Employee Onboarding

1. **HR creates employee record**
   ```http
   POST /api/employees
   ```

2. **HR starts onboarding workflow**
   ```http
   POST /api/workflows/processes/start
   {
     "processDefinitionKey": "employeeOnboardingProcess",
     "variables": { ... }
   }
   ```

3. **Parallel tasks created for IT, HR, Manager**
   ```http
   GET /api/workflows/tasks/group/IT
   GET /api/workflows/tasks/group/HR_ADMIN
   GET /api/workflows/tasks/group/MANAGER
   ```

4. **Each team completes their tasks**
   - IT completes account setup
   - HR completes documentation
   - Manager prepares workspace

5. **System automatically:**
   - Sends welcome email
   - Schedules orientation
   - Updates employee status
   - Notifies stakeholders

## Testing Workflows

### Using Swagger UI

1. Start the application and navigate to: `http://localhost:8080/api/swagger-ui.html`
2. Authenticate with Keycloak token
3. Navigate to "Workflow Management" section
4. Test workflow endpoints interactively

### Using Postman

Import the provided `postman_collection.json` and use the Workflow folder for pre-configured requests.

### Flowable UI (Optional)

Flowable provides admin UIs for workflow management:
- Process definitions: `http://localhost:8080/api/flowable-ui/`
- Task management: `http://localhost:8080/api/flowable-task/`

(Note: May require additional configuration)

## Monitoring and Debugging

### Logging

Workflow operations are logged at INFO and DEBUG levels:

```yaml
logging:
  level:
    org.flowable: INFO
    com.werkflow.workflow: DEBUG
```

### Check Process Status

```http
GET /api/workflows/processes/{processInstanceId}
```

Response shows:
- Current state
- Start/end times
- Process variables
- Suspension status

### Check Active Tasks

```http
GET /api/workflows/tasks/process/{processInstanceId}
```

Shows all tasks in the process, their assignees, and status.

## Integration with Existing CRUD APIs

Workflow processes integrate seamlessly with existing HR CRUD operations:

1. **Leave Management:**
   - Create leave → Start `leaveApprovalProcess`
   - Manager approves via workflow task
   - Leave status updated automatically by workflow delegate

2. **Employee Management:**
   - Create employee → Start `employeeOnboardingProcess`
   - Teams complete onboarding tasks
   - Employee status updated to ACTIVE by workflow delegate

3. **Performance Reviews:**
   - Create review → Start `performanceReviewProcess`
   - Employee and manager complete assessments
   - Review record updated by workflow delegate

## Best Practices

1. **Always use business keys** when starting processes for easier tracking
2. **Include all required variables** when starting processes
3. **Add comments when completing tasks** for audit trail
4. **Check task assignee** before attempting to complete
5. **Use appropriate roles** - don't give EMPLOYEE role access to management tasks
6. **Monitor long-running processes** using the process instance endpoints
7. **Handle errors gracefully** - workflow errors return standard error responses

## Troubleshooting

### Process doesn't start
- Check all required variables are provided
- Verify user has appropriate role
- Check logs for validation errors

### Task cannot be completed
- Verify task exists and is not already completed
- Check user is assignee or in candidate group
- Ensure all required variables for task completion are provided

### Delegate errors
- Check entity IDs exist in database
- Verify foreign key relationships
- Review delegate logs for specific error messages

## Next Steps

Phase 4 will add comprehensive testing:
- Unit tests for workflow services
- Integration tests for workflow APIs
- Process definition tests

---

For more information, see:
- [README.md](README.md) - General project setup
- [TESTING.md](TESTING.md) - API testing guide
- [KEYCLOAK_SETUP.md](KEYCLOAK_SETUP.md) - Authentication setup
