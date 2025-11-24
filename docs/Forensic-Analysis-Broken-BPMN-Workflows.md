# Forensic Analysis: Broken BPMN Workflows

**Analysis Date**: 2025-11-23
**Analyst**: Staff Engineer
**Scope**: Three BPMN workflows in Engine Service referencing non-existent beans

---

## Executive Summary

Three BPMN workflow files deployed in the Engine Service (`capex-approval-process.bpmn20.xml`, `procurement-approval-process.bpmn20.xml`, and `asset-transfer-approval-process.bpmn20.xml`) are using **direct Spring bean expressions** that reference service beans which **DO NOT EXIST** in the Engine Service. These workflows are currently deployed but will **FAIL AT RUNTIME** when any service task attempts to execute.

**Critical Finding**: The workflows were designed with the wrong architectural pattern. They use `flowable:expression="${capexService.method()}"` expecting service beans to exist locally in the Engine Service, but these beans are never defined. The working example (`pr-to-po.bpmn20.xml` in Procurement Service) uses the correct pattern with `RestServiceDelegate` for inter-service communication.

---

## File 1: capex-approval-process.bpmn20.xml

### Location
`/Users/lamteiwahlang/Projects/werkflow/services/engine/src/main/resources/processes/capex-approval-process.bpmn20.xml`

### Bean Expression References

| Line | Task ID | Bean Reference | Method Called | Status |
|------|---------|----------------|---------------|--------|
| 23 | createCapExRequest | `capexService` | `createRequest(execution.getVariables())` | **MISSING** |
| 29 | checkBudget | `capexService` | `checkBudget(requestAmount, departmentId)` | **MISSING** |
| 65 | updateApproved | `capexService` | `updateStatus(capexId, 'APPROVED', ...)` | **MISSING** |
| 71 | reserveBudget | `capexService` | `reserveBudget(capexId, requestAmount, departmentId)` | **MISSING** |
| 77 | sendApprovalNotification | `notificationService` | `sendEmail(requesterEmail, ...)` | **MISSING** |
| 83 | updateRejected | `capexService` | `updateStatus(capexId, 'REJECTED', ...)` | **MISSING** |
| 89 | sendRejectionNotification | `notificationService` | `sendEmail(requesterEmail, ...)` | **MISSING** |

### Total Service Task Expressions: 7
- **capexService** referenced: 5 times
- **notificationService** referenced: 2 times

### Bean Existence Check

**Engine Service** (`/Users/lamteiwahlang/Projects/werkflow/services/engine/src/main/java`):
```
No directories: delegate/
No files matching: *CapexService*, *FinanceService*, *NotificationService*
No @Component or @Bean definitions for: capexService, notificationService
```

**Finance Service** (`/Users/lamteiwahlang/Projects/werkflow/services/finance`):
- Has controller: `CapExController.java` - but NOT a bean named `capexService`
- Has delegate: `NotificationDelegate.java` - but scoped to Finance Service only (component name: `notificationDelegate`)

### Deployment Status
**DEPLOYED**: Yes - Auto-deployed on engine startup
```
2025-11-23 14:11:36 - Deploying resources [...capex-approval-process.bpmn20.xml...]
for engine org.flowable.engine.impl.ProcessEngineImpl deployment name hint SpringBootAutoDeployment
```

### Runtime Impact
**Severity**: CRITICAL - Process will fail immediately on first service task execution

When a user starts this process and it reaches the first service task (`createCapExRequest`):
```
org.flowable.common.engine.api.FlowableException: Unknown property used in expression: ${execution.setVariable('capexId', capexService.createRequest(execution.getVariables()))}
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No bean named 'capexService' available
```

---

## File 2: procurement-approval-process.bpmn20.xml

### Location
`/Users/lamteiwahlang/Projects/werkflow/services/engine/src/main/resources/processes/procurement-approval-process.bpmn20.xml`

### Bean Expression References

| Line | Task ID | Bean Reference | Method Called | Status |
|------|---------|----------------|---------------|--------|
| 23 | createPurchaseRequest | `procurementService` | `createRequest(execution.getVariables())` | **MISSING** |
| 29 | fetchVendors | `procurementService` | `getApprovedVendors(itemCategory)` | **MISSING** |
| 40 | requestQuotations | `procurementService` | `requestQuotations(requestId, selectedVendorIds)` | **MISSING** |
| 51 | calculateTotalCost | `procurementService` | `calculateTotalCost(requestId, selectedQuotationId)` | **MISSING** |
| 84 | createPurchaseOrder | `procurementService` | `createPurchaseOrder(requestId, selectedQuotationId)` | **MISSING** |
| 90 | sendPOToVendor | `procurementService` | `sendPurchaseOrder(poNumber, vendorEmail)` | **MISSING** |
| 96 | updateApproved | `procurementService` | `updateRequestStatus(requestId, 'APPROVED', poNumber)` | **MISSING** |
| 102 | sendApprovalNotification | `notificationService` | `sendEmail(requesterEmail, ...)` | **MISSING** |
| 108 | updateRejected | `procurementService` | `updateRequestStatus(requestId, 'REJECTED', ...)` | **MISSING** |
| 114 | sendRejectionNotification | `notificationService` | `sendEmail(requesterEmail, ...)` | **MISSING** |

### Total Service Task Expressions: 10
- **procurementService** referenced: 8 times
- **notificationService** referenced: 2 times

### Bean Existence Check

**Engine Service**: None found

**Procurement Service** (`/Users/lamteiwahlang/Projects/werkflow/services/procurement`):
- Has controller: `ProcurementController.java` - but NOT a bean named `procurementService`
- Has delegate: `PurchaseOrderCreationDelegate.java` - different pattern entirely

### Deployment Status
**DEPLOYED**: Yes - Auto-deployed on engine startup (same log entry as above)

### Runtime Impact
**Severity**: CRITICAL - Same as File 1
```
org.springframework.beans.factory.NoSuchBeanDefinitionException: No bean named 'procurementService' available
```

---

## File 3: asset-transfer-approval-process.bpmn20.xml

### Location
`/Users/lamteiwahlang/Projects/werkflow/services/engine/src/main/resources/processes/asset-transfer-approval-process.bpmn20.xml`

### Bean Expression References

| Line | Task ID | Bean Reference | Method Called | Status |
|------|---------|----------------|---------------|--------|
| 23 | createTransferRequest | `inventoryService` | `createTransferRequest(execution.getVariables())` | **MISSING** |
| 29 | verifyAsset | `inventoryService` | `verifyAsset(assetId)` | **MISSING** |
| 38 | checkCustody | `inventoryService` | `getCurrentCustodian(assetId)` | **MISSING** |
| 71 | updateCustody | `inventoryService` | `updateCustody(assetId, newCustodian, transferId)` | **MISSING** |
| 77 | updateLocation | `inventoryService` | `updateAssetLocation(assetId, newLocation, newDepartment)` | **MISSING** |
| 83 | createTransferRecord | `inventoryService` | `createTransferRecord(transferId, assetId, ...)` | **MISSING** |
| 89 | updateCompleted | `inventoryService` | `updateTransferStatus(transferId, 'COMPLETED')` | **MISSING** |
| 95 | sendCompletionNotification | `notificationService` | `sendTransferNotification(requesterEmail, ...)` | **MISSING** |
| 101 | updateRejected | `inventoryService` | `updateTransferStatus(transferId, 'REJECTED', ...)` | **MISSING** |
| 107 | sendRejectionNotification | `notificationService` | `sendEmail(requesterEmail, ...)` | **MISSING** |

### Total Service Task Expressions: 10
- **inventoryService** referenced: 8 times
- **notificationService** referenced: 2 times

### Bean Existence Check

**Engine Service**: None found

**Inventory Service** (`/Users/lamteiwahlang/Projects/werkflow/services/inventory`):
- Has controller structure but NO bean named `inventoryService`
- Has delegates: `InventoryAvailabilityDelegate.java`, `ReservationDelegate.java` - different use case

### Deployment Status
**DEPLOYED**: Yes - Auto-deployed on engine startup

### Runtime Impact
**Severity**: CRITICAL - Same pattern as Files 1 and 2
```
org.springframework.beans.factory.NoSuchBeanDefinitionException: No bean named 'inventoryService' available
```

---

## Service Bean Location Analysis

### Actual Bean Definitions Across Services

**Engine Service** (`/services/engine/src/main/java/com/werkflow/engine/`):
```
config/
  - FlowableConfig.java
  - JwtDecoderConfig.java
  - OpenApiConfig.java
  - SecurityConfig.java
controller/
  - FormController.java
  - ProcessDefinitionController.java
  - ProcessInstanceController.java
  - TaskController.java
service/
  - FormService.java (@Service)
  - ProcessDefinitionService.java (@Service)
  - ProcessInstanceService.java (@Service)
  - TaskService.java (@Service)
```
**NO DELEGATES DIRECTORY**

**Procurement Service** (WORKING EXAMPLE):
```
delegate/
  - PurchaseOrderCreationDelegate.java (@Component("purchaseOrderCreationDelegate"))
config/
  - ProcessVariableInjector.java (@Component("processVariableInjector"))
  - ServiceUrlConfiguration.java (@Configuration)
```

**Finance Service**:
```
delegate/
  - BudgetAvailabilityDelegate.java (@Component("budgetAvailabilityDelegate"))
  - NotificationDelegate.java (@Component("notificationDelegate"))
```

**Inventory Service**:
```
delegate/
  - InventoryAvailabilityDelegate.java (@Component("inventoryAvailabilityDelegate"))
  - ReservationDelegate.java (@Component("reservationDelegate"))
```

**HR Service**:
```
delegate/
  - Multiple notification and process delegates (@Component)
```

### Shared Delegates Module

**Location**: `/shared/delegates/src/main/java/com/werkflow/delegates/`

**RestServiceDelegate.java** (@Component("restServiceDelegate")):
- Generic HTTP client delegate
- Used for inter-service REST API calls
- Configurable via BPMN field injection
- **THIS IS THE CORRECT PATTERN**

**Engine Service pom.xml** (line 111):
```xml
<dependency>
    <groupId>com.werkflow</groupId>
    <artifactId>werkflow-delegates</artifactId>
</dependency>
```
**CONFIRMED**: Engine Service HAS access to `restServiceDelegate`

---

## Working Example Analysis: pr-to-po.bpmn20.xml

### Location
`/Users/lamteiwahlang/Projects/werkflow/services/procurement/src/main/resources/processes/pr-to-po.bpmn20.xml`

**Process**: Purchase Requisition to Purchase Order
**Owner**: Procurement Service
**Status**: WORKING

### How RestServiceDelegate is Used Correctly

#### Pattern 1: External Service Call (Finance Service Budget Check)
```xml
<serviceTask id="budgetCheck" name="Check Budget Availability"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'departmentId': departmentId, 'amount': totalAmount, 'costCenter': costCenter, 'fiscalYear': fiscalYear}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>budgetCheckResponse</flowable:string>
    </flowable:field>
    <flowable:field name="timeoutSeconds">
      <flowable:string>15</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Key Differences from Broken Workflows**:
1. Uses `flowable:delegateExpression="${restServiceDelegate}"` instead of `flowable:expression="${serviceBean.method()}"`
2. Service URL injected via process variables from `ProcessVariableInjector`
3. HTTP method, body, response variable all configured via field injection
4. Works across service boundaries (Procurement calling Finance)

#### Pattern 2: Local Service Delegate (PO Creation)
```xml
<serviceTask id="createPO" name="Create Purchase Order"
             flowable:delegateExpression="${purchaseOrderCreationDelegate}">
  <documentation>Create PO in the system with approved vendor and pricing</documentation>
</serviceTask>
```

**Why This Works**:
- `purchaseOrderCreationDelegate` is a @Component bean in the SAME service (Procurement)
- Delegate implements JavaDelegate interface
- Has direct access to local repositories and services

#### Pattern 3: Local Notification Delegate
```xml
<serviceTask id="notifyBudgetShortfall" name="Notify Budget Shortfall"
             flowable:delegateExpression="${notificationDelegate}">
  <extensionElements>
    <flowable:field name="notificationType">
      <flowable:string><![CDATA[PR_BUDGET_SHORTFALL]]></flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Why This Works**:
- Uses delegate pattern with field injection
- `notificationDelegate` bean exists locally
- Configured via BPMN extension elements

### Service URL Injection Mechanism

**ProcessVariableInjector.java** (Procurement Service):
```java
@Component("processVariableInjector")
public class ProcessVariableInjector implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        Map<String, String> serviceUrls = serviceUrlConfiguration.getServiceUrlMap();
        // Injects: financeServiceUrl, inventoryServiceUrl, hrServiceUrl, engineServiceUrl
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            execution.setVariable(entry.getKey(), entry.getValue());
        }
    }
}
```

**Used in BPMN Start Event**:
```xml
<startEvent id="startEvent" name="PR Created">
  <extensionElements>
    <flowable:executionListener event="start" delegateExpression="${processVariableInjector}" />
  </extensionElements>
</startEvent>
```

**Result**: All service URLs available as process variables for RestServiceDelegate to use

---

## Why the System Hasn't Crashed Yet

### Deployment Success

The broken BPMN files deploy successfully because:
1. **Flowable validates BPMN XML structure** - all 3 files are syntactically valid
2. **Bean references are NOT validated at deployment time** - Flowable doesn't check if `${capexService}` exists
3. **Lazy evaluation** - Expressions are only evaluated when the process instance reaches that specific task

### Current System Status (from docker ps)
```
werkflow-engine         Up 3 hours (healthy)
werkflow-finance        Restarting (1) 19 seconds ago    <- FLYWAY ISSUE, NOT BPMN
werkflow-procurement    Restarting (1) 17 seconds ago    <- FLYWAY ISSUE, NOT BPMN
werkflow-inventory      Restarting (1) 48 seconds ago    <- FLYWAY ISSUE, NOT BPMN
```

The Finance, Procurement, and Inventory services are failing due to **Flyway migration conflicts**, NOT BPMN issues:
```
org.flywaydb.core.api.FlywayException: Found more than one migration with version 1
```

### Runtime Failure Scenario

The broken workflows WILL FAIL when:
1. User navigates to Process Definitions in Admin Portal
2. User clicks "Start Process" on `capex-approval-process`
3. User fills out the start form and submits
4. Process instance is created successfully (start event has no service task)
5. **FAILURE OCCURS** when execution reaches first service task:
```
Task ID: createCapExRequest (line 22-25)
Expression: ${execution.setVariable('capexId', capexService.createRequest(execution.getVariables()))}

ERROR: org.springframework.beans.factory.NoSuchBeanDefinitionException:
No bean named 'capexService' available
```

### Why No Errors in Logs Yet

Checking current logs:
```bash
docker logs werkflow-engine 2>&1 | grep -i "capex\|procurement-approval\|asset-transfer"
```

**Result**: Only deployment messages, NO runtime execution attempts

**Conclusion**: No user has attempted to start these processes yet. The workflows are "armed bombs" waiting to explode on first use.

---

## Frontend Service Reference Analysis

### Admin Portal Process Designer

**File**: `/frontends/admin-portal/lib/form-templates.ts`

This file contains form templates for HR workflows only:
- leave-request
- employee-onboarding
- performance-review

**NO REFERENCES** to:
- capex-approval-process
- procurement-approval-process
- asset-transfer-approval-process

### Process Definition API

The Admin Portal can still **display** these processes via:
```
GET /api/repository/process-definitions
```

The Engine Service `ProcessDefinitionController.java` will return them in the list, but:
- No custom forms are configured in the Admin Portal
- Users would see generic Flowable forms if they tried to start them
- **Starting the process would immediately fail** at the first service task

### User Impact Assessment

**Current Impact**: NONE - No users are using these workflows yet

**Potential Impact** (if users try to use them):
1. User sees "CapEx Approval Process" in process list
2. User clicks "Start Process"
3. Generic form appears (no custom template)
4. User fills form and submits
5. **Process starts but immediately fails with NoSuchBeanDefinitionException**
6. User sees cryptic error message in UI
7. Process instance is stuck in failed state
8. Manual cleanup required in database

---

## Migration Path: Broken Workflows to Working Pattern

### Architecture Decision Required

**Option 1: Keep Workflows in Engine Service** (Recommended for cross-cutting processes)
- Create wrapper beans in Engine Service that call department services via REST
- Use RestServiceDelegate pattern internally
- Pros: Centralized workflow management, works across service boundaries
- Cons: Extra layer of abstraction, slight latency increase

**Option 2: Move Workflows to Department Services**
- Move `capex-approval-process.bpmn20.xml` to Finance Service
- Move `procurement-approval-process.bpmn20.xml` to Procurement Service
- Move `asset-transfer-approval-process.bpmn20.xml` to Inventory Service
- Create local service beans in each department service
- Pros: Direct database access, no REST overhead, follows pr-to-po pattern
- Cons: Workflows scattered across services, harder to manage centrally

**Option 3: Hybrid - Refactor to Use RestServiceDelegate** (Recommended)
- Keep workflows in Engine Service
- Replace all `${capexService.method()}` with `${restServiceDelegate}` + field injection
- Create REST API endpoints in department services for workflow operations
- Pros: Consistent pattern, works across services, easier to maintain
- Cons: Requires REST API development in department services

### Recommended Migration: Option 3 (RestServiceDelegate Pattern)

#### Step 1: Create REST APIs in Department Services

**Finance Service** - Create `WorkflowApiController.java`:
```java
@RestController
@RequestMapping("/api/workflow/capex")
public class CapExWorkflowController {

    @PostMapping("/requests")
    public Map<String, Object> createRequest(@RequestBody Map<String, Object> variables) {
        // Create CapEx request
        return Map.of("capexId", savedRequest.getId());
    }

    @PostMapping("/budget/check")
    public Map<String, Object> checkBudget(@RequestBody Map<String, Object> params) {
        // Check budget availability
        return Map.of("available", true/false);
    }

    @PutMapping("/requests/{id}/status")
    public void updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        // Update status
    }

    @PostMapping("/budget/reserve")
    public void reserveBudget(@RequestBody Map<String, Object> params) {
        // Reserve budget
    }
}
```

**Procurement Service** - Create similar endpoints
**Inventory Service** - Create similar endpoints

#### Step 2: Add Service URL Configuration to Engine Service

**Create**: `/services/engine/src/main/java/com/werkflow/engine/config/ServiceUrlConfiguration.java`
```java
@Configuration
@ConfigurationProperties(prefix = "app.services")
@Getter
@Setter
public class ServiceUrlConfiguration {
    private String hrUrl;
    private String adminUrl;
    private String financeUrl;
    private String procurementUrl;
    private String inventoryUrl;

    public Map<String, String> getServiceUrlMap() {
        Map<String, String> urls = new HashMap<>();
        urls.put("financeServiceUrl", financeUrl);
        urls.put("procurementServiceUrl", procurementUrl);
        urls.put("inventoryServiceUrl", inventoryUrl);
        urls.put("hrServiceUrl", hrUrl);
        urls.put("adminServiceUrl", adminUrl);
        return urls;
    }
}
```

**Create**: `/services/engine/src/main/java/com/werkflow/engine/config/ProcessVariableInjector.java`
```java
@Component("processVariableInjector")
public class ProcessVariableInjector implements ExecutionListener {

    private final ServiceUrlConfiguration serviceUrlConfiguration;

    @Override
    public void notify(DelegateExecution execution) {
        Map<String, String> serviceUrls = serviceUrlConfiguration.getServiceUrlMap();
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            execution.setVariable(entry.getKey(), entry.getValue());
        }
    }
}
```

#### Step 3: Refactor BPMN Files

**Before** (capex-approval-process.bpmn20.xml line 22-25):
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:expression="${execution.setVariable('capexId', capexService.createRequest(execution.getVariables()))}">
  <documentation>Creates CapEx request record in Finance Service via REST API</documentation>
</serviceTask>
```

**After**:
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${restServiceDelegate}">
  <documentation>Creates CapEx request record in Finance Service via REST API</documentation>
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/requests</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{execution.variables}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>capexResponse</flowable:string>
    </flowable:field>
    <flowable:field name="timeoutSeconds">
      <flowable:string>30</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Add to start event**:
```xml
<startEvent id="startEvent" name="CapEx Request Submitted" flowable:formKey="capex-request">
  <extensionElements>
    <flowable:executionListener event="start" delegateExpression="${processVariableInjector}" />
  </extensionElements>
</startEvent>
```

#### Step 4: Extract Response Variables

**After RestServiceDelegate call**, response is stored in process variable:
```
capexResponse = {
  "capexId": 12345
}
```

**Next task can access**:
```xml
<sequenceFlow id="flow2" sourceRef="createCapExRequest" targetRef="checkBudget">
  <extensionElements>
    <flowable:executionListener event="take" expression="${execution.setVariable('capexId', capexResponse.capexId)}" />
  </extensionElements>
</sequenceFlow>
```

#### Step 5: Testing Strategy

1. **Unit test REST endpoints** in department services
2. **Integration test** RestServiceDelegate with mocked HTTP server
3. **Process test** using Flowable's process test framework
4. **End-to-end test** in dev environment before production

---

## Impact Assessment Summary

### Current State
- **Deployment**: All 3 broken workflows successfully deployed
- **Runtime**: NOT tested - no users have started these processes
- **System Health**: Engine service healthy, department services failing (unrelated Flyway issue)
- **User Impact**: NONE (yet)

### Risk Analysis
- **Severity**: HIGH - Complete process failure on first execution
- **Probability**: MEDIUM - Depends on when users discover these processes
- **Detection**: EASY - Immediate failure with clear error message
- **Recovery**: MEDIUM - Requires process instance cleanup and BPMN fix

### Required Actions

**Immediate** (Prevent User Access):
1. Create custom process filter in Admin Portal to hide broken processes
2. Or add deployment condition to skip these 3 files until fixed

**Short-term** (Fix Workflows):
1. Implement REST APIs in Finance, Procurement, Inventory services
2. Add ServiceUrlConfiguration and ProcessVariableInjector to Engine Service
3. Refactor BPMN files to use RestServiceDelegate pattern
4. Test thoroughly in dev environment

**Long-term** (Architectural Alignment):
1. Document standard patterns for BPMN workflows
2. Create BPMN templates for common patterns
3. Add pre-deployment validation to check bean references
4. Create workflow development guide with examples

---

## Conclusion

The three broken BPMN workflows represent a **fundamental architectural mismatch** between the intended design pattern (direct bean invocation) and the actual distributed microservices architecture. They are currently deployed but dormant, waiting to fail spectacularly when first used.

The working example (`pr-to-po.bpmn20.xml`) demonstrates the correct pattern using `RestServiceDelegate` for inter-service communication and local delegates for service-specific operations. This pattern should be adopted for all workflows that need to communicate across service boundaries.

**Recommendation**: Implement Option 3 (RestServiceDelegate Pattern) migration path to align these workflows with the established architectural pattern used successfully in the Procurement Service.

**Timeline Estimate**:
- REST API development: 2-3 days
- BPMN refactoring: 1-2 days
- Testing and validation: 2-3 days
- **Total**: 5-8 days for complete migration

---

## Appendix A: Complete Bean Reference Inventory

### Beans Referenced But Missing
1. `capexService` - 5 references in capex-approval-process
2. `procurementService` - 8 references in procurement-approval-process
3. `inventoryService` - 8 references in asset-transfer-approval-process
4. `notificationService` - 6 references across all 3 files (2 per file)

**Total Missing References**: 27 service task expressions will fail

### Beans That Exist (Partial Matches)
- Finance Service has `NotificationDelegate.java` (@Component("notificationDelegate"))
  - But this is scoped to Finance Service only
  - Not available in Engine Service where workflows run
  - Different component name anyway

### Beans That Work Correctly
- `restServiceDelegate` - Available in Engine Service via werkflow-delegates dependency
- `processVariableInjector` - Exists in Procurement Service (needs to be created in Engine Service)
- `purchaseOrderCreationDelegate` - Local to Procurement Service
- `notificationDelegate` - Local to Finance Service (for Finance workflows only)

---

## Appendix B: Docker Compose Service URLs

From `docker-compose.yml`, internal service URLs available:
```yaml
FINANCE_SERVICE_URL: http://finance-service:8084
PROCUREMENT_SERVICE_URL: http://procurement-service:8085
INVENTORY_SERVICE_URL: http://inventory-service:8086
HR_SERVICE_URL: http://hr-service:8082
ENGINE_SERVICE_URL: http://engine-service:8081
ADMIN_SERVICE_URL: http://admin-service:8083
```

These URLs should be configured in Engine Service `application.yml`:
```yaml
app:
  services:
    finance-url: ${FINANCE_SERVICE_URL:http://localhost:8084}
    procurement-url: ${PROCUREMENT_SERVICE_URL:http://localhost:8085}
    inventory-url: ${INVENTORY_SERVICE_URL:http://localhost:8086}
    hr-url: ${HR_SERVICE_URL:http://localhost:8082}
    admin-url: ${ADMIN_SERVICE_URL:http://localhost:8083}
```

---

**End of Forensic Analysis**
