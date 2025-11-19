# Workflow Architecture Design

## Overview

This document describes the workflow architecture and deployment strategy for the Werkflow enterprise platform. It clarifies the distinction between centralized and distributed workflow management, explains the rationale for the current architecture, and provides guidance for future workflow implementations.

---

## Architectural Decision: Hybrid Workflow Deployment Model

### Current State

Werkflow uses a **hybrid deployment model** for BPMN workflows:

1. **Centralized Workflows** (Engine Service)
   - Cross-domain processes that span multiple departments
   - Process definitions stored in `/services/engine/src/main/resources/processes/`
   - Examples: Asset Transfer Approval, Generic workflow templates

2. **Distributed Workflows** (Department Services)
   - Domain-specific processes managed by individual departments
   - Process definitions stored in service-specific directories
   - HR: `/services/hr/src/main/resources/processes/`
   - Finance: `/services/finance/src/main/resources/processes/`
   - Procurement: `/services/procurement/src/main/resources/processes/`
   - Inventory: `/services/inventory/src/main/resources/processes/`

### Rationale

This hybrid approach provides several benefits:

#### 1. Separation of Concerns
- Each department owns and manages its core workflows
- Reduces complexity of centralized engine configuration
- Allows department teams to evolve processes independently

#### 2. Scalability
- Workflows scale with their respective services
- Engine Service remains focused on cross-domain orchestration
- No single point of bottleneck for all workflow operations

#### 3. Domain Autonomy
- HR team can update leave and onboarding workflows without affecting Finance
- Finance can modify CapEx approvals without coordinating with Procurement
- Reduces coordination overhead and deployment complexity

#### 4. Process Lifecycle Management
- Department services can maintain process versions separately
- Easier rollback if a specific workflow has issues
- Faster iteration on domain-specific improvements

---

## Workflow Categorization

### Type 1: Department-Owned Workflows

**Location**: Service-specific process directories
**Owner**: Department team
**Deployment**: Automatic on service startup via Flowable

**Examples**:
- Employee Onboarding (HR)
- Leave Approval (HR)
- Performance Review (HR)
- CapEx Approval (Finance) - *also in Engine for cross-reference*
- Purchase Requisition (Procurement)
- Stock Requisition (Inventory)

**Characteristics**:
- Single-service data access
- Service-specific form definitions
- Department-level business logic
- Independent versioning

---

### Type 2: Cross-Domain Orchestration Workflows

**Location**: Engine Service process directory
**Owner**: Platform team
**Deployment**: Automatic on service startup via Flowable

**Examples**:
- Asset Transfer Approval (involves HR and Inventory)
- Procurement Approval (involves Finance and Procurement)
- Generic delegate-based workflows (REST calls, notifications)

**Characteristics**:
- Multi-service communication via REST/Kafka
- Generic delegates for cross-service interaction
- Engine Service acts as orchestrator
- Standardized patterns for inter-service workflows

---

### Type 3: Future - Event-Driven Workflows

**Location**: Kafka event topics (planned for Phase 5+)
**Owner**: Platform team
**Deployment**: Via Kafka topic subscriptions

**Examples**:
- Cascade workflows triggered by events (e.g., "CapExApproved")
- Real-time inter-service communication
- Event sourcing for audit trail

---

## Deployment Strategy

### Automatic Deployment on Service Startup

All BPMN files in the service's `resources/processes/` directory are automatically deployed:

```java
// Flowable auto-discovery configuration
spring:
  application:
    name: hr-service
flowable:
  process:
    definition-cache-limit: 100
    async-executor-activate: true
```

Process files are scanned at startup and deployed to the shared Flowable database.

### Multi-Service, Single Database

**Important**: All services share a single PostgreSQL database with schema separation:

```
PostgreSQL Database (werkflow_db)
├── flowable (Engine Service)
│   ├── act_re_deployment
│   ├── act_re_procdef
│   ├── act_ru_execution
│   └── ...
├── hr_service
├── finance_service
├── procurement_service
└── inventory_service
```

**Implications**:
- All process definitions visible to Flowable engine regardless of source
- HR processes can be started from any service via Engine API
- Forms and process definitions are centrally managed in Flowable schema
- Queries can join across process and service schemas

---

## Inter-Service Communication Patterns

### Pattern 1: REST-Based Service Delegation

**Use Case**: One service needs data from another during workflow execution

**Implementation**:
```java
public class FinanceBudgetCheckDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // Call Finance Service from Procurement workflow
        RestTemplate restTemplate = new RestTemplate();
        Budget budget = restTemplate.getForObject(
            "http://finance-service:8084/api/budgets/{id}",
            Budget.class
        );
    }
}
```

**Workflows Using This Pattern**:
- Procurement Approval (checks Finance budget)
- CapEx Approval (integrates with Procurement for PO creation)

---

### Pattern 2: Form-Based Task Integration

**Use Case**: Workflow task form submission triggers service updates

**Implementation**:
```
Workflow Task (with form key: "capex-request")
  ↓
FormRenderer displays Form.io JSON
  ↓
User submits form
  ↓
FormData converted to process variables
  ↓
Service-specific handler processes variables
```

**Workflows Using This Pattern**:
- All department workflows with user tasks
- Leave Approval (captures leave dates, type)
- CapEx Request (captures amount, ROI, payback period)

---

### Pattern 3: Notification Delegates

**Use Case**: Workflow sends notifications to external systems

**Implementation**:
```java
public class NotificationDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        String recipientEmail = execution.getVariable("approverEmail");
        String subject = "Action Required: " + execution.getProcessDefinitionKey();

        // Send email via delegate
        emailService.sendNotification(recipientEmail, subject);
    }
}
```

**Workflows Using This Pattern**:
- All workflows with manager approval tasks
- Send welcome email for new employees
- Notify teams of absence (leave approved)

---

## Process Instance Routing

### How Processes Are Started

1. **From Admin Portal UI**:
   ```
   Admin Portal → Engine Service /api/workflows/start
   → Flowable Engine
   → Routes to appropriate process definition (HR, Finance, etc.)
   ```

2. **Programmatically from Service**:
   ```java
   // In HR Service
   ProcessInstance instance = runtimeService.startProcessInstanceByKey(
       "leaveApproval",
       new HashMap<>() {{ put("employeeId", "EMP001"); }}
   );
   ```

3. **Via Flowable Task REST API** (external systems):
   ```bash
   POST http://engine-service:8081/api/workflows/processes
   {
       "processDefinitionKey": "leaveApproval",
       "variables": { "employeeId": "EMP001" }
   }
   ```

---

## Form Management Strategy

### Form Definition Locations

1. **Hardcoded Templates** (current approach):
   ```typescript
   // frontends/admin-portal/lib/form-templates.ts
   export const FORMS = {
       'employee-onboarding': { ... },
       'leave-request': { ... },
       'capex-request': { ... }
   }
   ```

2. **Flowable Form Service** (planned for Phase 5):
   ```java
   // Form definitions stored in Flowable database
   FormRepository.createFormDefinition()
       .key("leave-request")
       .resourceName("leave-request.form")
       .deploy();
   ```

### Form-to-Task Linking

Forms are linked to BPMN tasks via `flowable:formKey` property:

```xml
<userTask id="managerApproval" name="Manager Approval">
    <extensionElements>
        <flowable:formKey>leave-approval</flowable:formKey>
    </extensionElements>
</userTask>
```

Task completion includes form data:
```javascript
// Frontend: submit form data with task completion
await completeTask(taskId, {
    formData: formSubmissionData,
    comment: "Approved",
    variables: {}
});
```

---

## Process Variable Management

### Variable Scoping

Variables are scoped to process instances and can be:

1. **Process Variables** (execution level)
   ```java
   execution.setVariable("approverEmail", "manager@company.com");
   ```

2. **Task Variables** (task level)
   ```java
   task.setVariable("taskSpecificData", data);
   ```

3. **Business Key** (unique identifier)
   ```java
   runtimeService.startProcessInstanceByKey(
       "leaveApproval",
       "LEAVE-001", // business key
       variables
   );
   ```

### Variable Propagation Across Services

When a delegate calls another service:

```
Process Instance (HR Service - leaveApproval)
  ↓ [process variable: employeeId = "EMP001"]
  ↓
Delegate calls Finance Service API
  ↓ (passes employeeId as request parameter)
  ↓
Finance Service processes and returns result
  ↓
Delegate stores result in process variable
  ↓
Next task in workflow uses the result
```

---

## Error Handling and Compensation

### Task Failure Handling

1. **Automatic Retries**:
   ```xml
   <serviceTask id="budgetCheck">
       <extensionElements>
           <flowable:failedJobRetryTimeCycle>R5:PT10S</flowable:failedJobRetryTimeCycle>
       </extensionElements>
   </serviceTask>
   ```

2. **Boundary Error Events**:
   ```xml
   <userTask id="managerApproval">
       <boundaryEvent id="timeoutError" attachedToRef="managerApproval">
           <timerEventDefinition>
               <timeDuration>P3D</timeDuration>
           </timerEventDefinition>
       </boundaryEvent>
   </userTask>
   ```

### Compensation Workflows

For critical processes requiring rollback:

```xml
<process id="capexApproval">
    <subProcess id="approval" triggerByEvent="false">
        <serviceTask id="reserveBudget" />
        <boundaryEvent id="compensate" attachedToRef="approval">
            <compensateEventDefinition />
            <compensationHandler>
                <serviceTask id="releaseBudgetReservation" />
            </compensationHandler>
        </boundaryEvent>
    </subProcess>
</process>
```

---

## Future Improvements (Phase 5+)

### 1. Event-Driven Orchestration

**Current State**: Synchronous REST calls
**Future**: Kafka event topics for asynchronous workflows

```
HR Service (Leave Approved)
  ↓ publishes event
  → Kafka Topic: "leave.approved"
  ↓
Finance Service subscribes
  ↓ receives event
  ↓ updates payroll in workflow
```

### 2. Centralized Form Repository

**Current State**: Hardcoded form templates
**Future**: Form definitions in Flowable Form Service

```
Admin Portal → Form Builder
  ↓
Save to Flowable Form Service
  ↓
Task renderer fetches form dynamically
  ↓
Form deployed to multiple services
```

### 3. Process Template Library

**Current State**: Each service maintains own processes
**Future**: Shared process templates with customization

```
Process Template (Generic Leave Approval)
  ↓ customize for HR department
  ↓
HR-specific Leave Approval Process
  ↓ deploy to HR service
```

### 4. Real-Time Process Monitoring

**Current State**: Polling-based task lists
**Future**: WebSocket for real-time updates

```
Flowable Engine publishes execution events
  ↓ via WebSocket
  ↓
Admin Portal receives real-time task updates
  ↓
Dashboard updates automatically
```

---

## Scaling Considerations

### Horizontal Scaling

1. **Department Services**: Scale independently
   ```bash
   # Scale HR to 3 instances
   docker-compose up -d --scale hr=3

   # Flowable engine handles multiple instances
   ```

2. **Engine Service**: Can be load-balanced
   ```
   Load Balancer
   ├── Engine-1 (8081)
   ├── Engine-2 (8081)
   └── Engine-3 (8081)
   ```

### Database Scaling

Current single-database approach suitable for:
- Up to ~10M process instances
- Up to ~100k concurrent processes
- Distributed deployment requires:
  - Read replicas for query scaling
  - Connection pooling
  - Database sharding by customer/tenant

---

## Migration Path: Centralized to Distributed

If migrating from centralized approach:

1. **Phase 1**: Identify cross-cutting workflows (centralize to Engine)
2. **Phase 2**: Move department workflows to services
3. **Phase 3**: Update REST calls for inter-service communication
4. **Phase 4**: Implement event-driven async patterns
5. **Phase 5**: Decouple shared Flowable database (if needed)

---

## Recommendations for New Workflows

### When to Centralize (Engine Service)

- Cross-domain processes (multi-service)
- Standardized templates (used by multiple services)
- Strategic processes requiring audit trail
- Processes requiring governance/approval

### When to Distribute (Department Service)

- Single-service workflows
- Department-specific business rules
- Frequently changing processes
- Department team owns requirements

### When to Make Asynchronous (Kafka)

- Non-critical notifications
- Batch processing workflows
- Real-time event reactions
- High-volume scenarios

---

## Conclusion

The hybrid workflow deployment model balances:
- **Centralization**: Cross-domain orchestration, governance
- **Autonomy**: Department control, faster iteration
- **Scalability**: Independent scaling per domain
- **Simplicity**: Shared database, centralized monitoring

This architecture supports the 90%+ no-code vision while maintaining flexibility for enterprise requirements.

For questions or improvements to this architecture, refer to the implementation guides in the respective service READMEs and the main project CLAUDE.md development guidelines.
