# Delegate Architecture Analysis - Generic vs Service-Specific

**Date**: 2025-12-17
**Author**: Architecture Review
**Purpose**: Analyze delegate coupling patterns and provide architectural recommendations aligned with Werkflow's microservices philosophy

---

## Executive Summary

**User's Concern**: "Ideally we have delegates for cross department/service workflows that are **reusable but not tightly coupled to a specific implementation**."

**Finding**: The user is **ABSOLUTELY CORRECT**. The proposed service-specific delegates (`CapExServiceDelegate`, `ProcurementServiceDelegate`) would create tight coupling and violate Werkflow's architectural principles.

**Recommendation**: Use the **EXISTING generic `RestServiceDelegate`** pattern which is already production-ready and aligns perfectly with the architecture.

---

## 1. Werkflow Architectural Principles

Based on analysis of the codebase and documentation:

### Core Architectural Philosophy

1. **90%+ No-Code Platform**
   - Workflows should be created without writing Java code
   - Generic, reusable delegates enable this vision
   - Service-specific delegates would require code changes for each workflow

2. **Microservices Architecture**
   - Engine Service orchestrates, does not implement business logic
   - Domain services (Finance, HR, Procurement) own their business logic
   - Loose coupling between services via REST APIs

3. **Separation of Concerns**
   - Engine Service: Orchestration layer
   - Domain Services: Business logic layer
   - Delegates: Generic integration layer

4. **Reusability Across Departments**
   - Workflows should be templates that work across departments
   - Finance workflow patterns should be reusable in HR or Procurement
   - Domain-specific logic stays in domain services, not Engine

### What the Documentation Says

From `/docs/README.md`:
```
**Key Capabilities:**
- 90%+ no-code workflow creation
- Generic reusable delegates
- Multi-department support
- Centralized BPM orchestration
```

From `/docs/Architecture/Workflow-Architecture-Design.md`:
```
### Pattern 1: REST-Based Service Delegation
Use Case: One service needs data from another during workflow execution
```

---

## 2. Current Delegate Patterns in Werkflow

### Pattern Analysis from Audit

The architectural audit found **three distinct patterns**:

| Pattern | Usage | Coupling | Reusability | Status |
|---------|-------|----------|-------------|--------|
| **Tight SpEL** | 21% (Engine workflows) | HIGH | LOW | BROKEN |
| **Generic RestServiceDelegate** | 7% (1 working example) | LOW | HIGH | PRODUCTION-READY |
| **Domain-Specific Delegates** | 72% (Service-internal) | MEDIUM | MEDIUM | WORKING |

### Pattern 1: Tight SpEL Coupling (Anti-Pattern in Engine)

**Example from `capex-approval-process.bpmn20.xml`:**
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:expression="${execution.setVariable('capexId',
                 capexService.createRequest(execution.getVariables()))}">
</serviceTask>
```

**Problems**:
- **Assumes Spring bean exists**: `capexService` must be in Engine Service
- **Tight coupling**: Engine knows about Finance service implementation
- **Not reusable**: Can't use same pattern for different services
- **Breaks at runtime**: Beans don't actually exist (verified by audit)
- **No flexibility**: Can't change service URLs without redeploying

**Status**: CRITICAL - These workflows fail at runtime

---

### Pattern 2: Generic RestServiceDelegate (Recommended Pattern)

**Example from `pr-to-po.bpmn20.xml` (working production code):**
```xml
<serviceTask id="budgetCheck" name="Check Budget Availability"
             flowable:delegateExpression="${restServiceDelegate}">
  <documentation>Verify budget availability via Finance Service REST API</documentation>
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'departmentId': departmentId, 'amount': totalAmount}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>budgetCheckResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Benefits**:
- **Generic and reusable**: Same delegate works for ANY REST service
- **Loosely coupled**: Only knows about HTTP, not service implementation
- **Configurable**: URL from environment variables
- **Production-ready**: Fully implemented in `/shared/delegates/`
- **No code changes**: Add new services without Java coding
- **Testable**: Easy to mock HTTP calls

**Implementation**:
- File: `/shared/delegates/src/main/java/com/werkflow/delegates/rest/RestServiceDelegate.java`
- Spring Bean: `@Component("restServiceDelegate")`
- Status: **VERIFIED - 100% PRODUCTION READY**

---

### Pattern 3: Domain-Specific Delegates (Service-Internal)

**Example from HR Service `leave-approval-process.bpmn20.xml`:**
```xml
<serviceTask id="approveLeaveTask" name="Approve Leave"
             flowable:delegateExpression="${leaveApprovalDelegate}">
  <documentation>Updates the leave status to APPROVED in the database.</documentation>
</serviceTask>
```

**When to Use**:
- **Service-internal operations**: Workflow deployed in same service
- **Direct database access**: No REST call needed
- **Business logic encapsulation**: Complex domain logic
- **Performance critical**: Avoid HTTP overhead

**Examples in Codebase**:
- HR Service: `${leaveApprovalDelegate}`, `${notifyEmployeeDelegate}`
- Finance Service: `${budgetAvailabilityDelegate}`
- Inventory Service: `${inventoryAvailabilityDelegate}`

**Characteristics**:
- Deployed WITH the service (not in Engine)
- Access service's database directly
- Single responsibility
- NO cross-service coupling

---

## 3. The Problem with Service-Specific Delegates

### What Was Proposed (Anti-Pattern)

```java
// CapExServiceDelegate.java in Engine Service
@Component("capexServiceDelegate")
public class CapExServiceDelegate implements JavaDelegate {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${finance.service.url}")
    private String financeServiceUrl;

    @Override
    public void execute(DelegateExecution execution) {
        // Hardcoded knowledge of Finance service APIs
        String url = financeServiceUrl + "/api/capex-requests";

        // Hardcoded request structure
        CapExRequest request = new CapExRequest();
        request.setAmount((BigDecimal) execution.getVariable("amount"));
        request.setDepartment((String) execution.getVariable("department"));

        // Make REST call
        CapExResponse response = restTemplate.postForObject(url, request, CapExResponse.class);

        execution.setVariable("capexId", response.getId());
    }
}
```

### Why This is an Anti-Pattern

1. **Tight Coupling to Finance Service**
   - Delegate knows about `CapExRequest` and `CapExResponse` classes
   - Changes to Finance API require Engine code changes
   - Can't reuse for Procurement or HR

2. **Not Reusable**
   - Need separate delegates for each service: `ProcurementServiceDelegate`, `HRServiceDelegate`
   - Similar code duplicated across delegates
   - Violates DRY principle

3. **Violates No-Code Vision**
   - Adding new service requires Java coding
   - Need to rebuild and redeploy Engine Service
   - Business users can't create workflows

4. **Maintenance Nightmare**
   - N services = N delegates to maintain
   - API changes require delegate updates
   - Testing requires mocking each service

5. **Defeats Microservices Architecture**
   - Engine Service couples to all domain services
   - Creates compile-time dependencies
   - Shared DTOs across service boundaries

---

## 4. The Generic RestServiceDelegate Solution

### Why It's Superior

1. **Zero Coupling**
   - No knowledge of service-specific APIs
   - Works with ANY REST endpoint
   - No compile-time dependencies

2. **100% Reusable**
   - Same delegate for Finance, HR, Procurement, Inventory
   - One implementation serves all workflows
   - Follows DRY principle

3. **Enables No-Code**
   - Configure via BPMN visual designer
   - No Java coding required
   - Business users can create workflows

4. **Flexible and Configurable**
   - Service URLs from environment variables
   - Easy to switch between dev/staging/prod
   - Request/response mapping via expressions

5. **Production-Ready**
   - Already implemented and tested
   - Used in working workflows (`pr-to-po.bpmn20.xml`)
   - Handles timeouts, errors, headers

### Real-World Example: Budget Check

**ANY service can check budget using the SAME delegate:**

```xml
<!-- Finance workflow checking budget -->
<serviceTask flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'amount': requestAmount, 'dept': 'FINANCE'}}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- HR workflow checking budget (SAME delegate) -->
<serviceTask flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'amount': trainingCost, 'dept': 'HR'}}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- Procurement workflow checking budget (SAME delegate) -->
<serviceTask flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'amount': poAmount, 'dept': 'PROCUREMENT'}}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**One delegate, three departments, zero code changes.**

---

## 5. Solving the JSON Expression Problem

### The Original Problem

BPMN v2 broken syntax:
```xml
<flowable:field name="body">
  <!-- INVALID: JSON syntax in BPMN expression -->
  <flowable:expression>{"amount": ${amount}, "dept": "${dept}"}</flowable:expression>
</flowable:field>
```

### The Solution: SpEL Map Syntax

**Correct syntax using SpEL hash-map literal:**
```xml
<flowable:field name="body">
  <flowable:expression>#{{'amount': amount, 'dept': dept, 'year': fiscalYear}}</flowable:expression>
</flowable:field>
```

**How It Works**:
- `#{}` = SpEL expression evaluator
- `{'key': value}` = Map literal syntax
- `amount`, `dept`, `fiscalYear` = Process variables (auto-resolved)
- Result: Java `Map<String, Object>` passed to delegate

**RestServiceDelegate Handling**:
```java
// In RestServiceDelegate.execute()
Object body = execution.getVariable("body"); // Gets the Map
if (body != null) {
    requestSpec.bodyValue(body); // WebClient serializes Map to JSON
}
```

**JSON Output**:
```json
{
  "amount": 150000.00,
  "dept": "FINANCE",
  "year": 2025
}
```

### Complex Example: Nested Objects

```xml
<flowable:field name="body">
  <flowable:expression>#{{'request': #{{'amount': amount, 'description': description}}, 'approver': approverEmail, 'metadata': #{{'createdBy': requestorId, 'priority': priority}}}}</flowable:expression>
</flowable:field>
```

**Result**:
```json
{
  "request": {
    "amount": 150000.00,
    "description": "New server infrastructure"
  },
  "approver": "manager@company.com",
  "metadata": {
    "createdBy": "EMP001",
    "priority": "HIGH"
  }
}
```

---

## 6. Architectural Decision: Generic vs Specific

### Decision Matrix

| Factor | Generic RestServiceDelegate | Service-Specific Delegates |
|--------|----------------------------|----------------------------|
| **Coupling** | LOW - No service knowledge | HIGH - Knows service APIs |
| **Reusability** | HIGH - Works everywhere | LOW - One service only |
| **Maintainability** | HIGH - One implementation | LOW - N implementations |
| **No-Code Vision** | ✅ Enables no-code | ❌ Requires coding |
| **Microservices** | ✅ Loosely coupled | ❌ Tightly coupled |
| **Flexibility** | ✅ Config-driven | ❌ Code-driven |
| **Testing** | ✅ Easy to mock | ❌ Complex mocking |
| **Production Ready** | ✅ Yes (verified) | ❌ Needs implementation |

### Recommendation: GENERIC DELEGATES ONLY

**For Cross-Service Communication in Engine Service:**
- ✅ Use `RestServiceDelegate` for ALL external service calls
- ✅ Configure via BPMN extension elements
- ✅ Use SpEL map literals for JSON bodies
- ❌ DO NOT create service-specific delegates

**For Service-Internal Operations:**
- ✅ Use domain-specific delegates (e.g., `leaveApprovalDelegate` in HR Service)
- ✅ Deploy delegates WITH the service
- ✅ Direct database access, no REST calls
- ❌ DO NOT use these delegates from Engine Service

---

## 7. Migration Strategy

### Step 1: Fix Broken BPMN Files (CRITICAL)

**Files to Migrate**:
1. `/services/engine/src/main/resources/processes/capex-approval-process.bpmn20.xml`
2. `/services/engine/src/main/resources/processes/procurement-approval-process.bpmn20.xml`
3. `/services/engine/src/main/resources/processes/asset-transfer-approval-process.bpmn20.xml`

**Reference**: `/services/procurement/src/main/resources/processes/pr-to-po.bpmn20.xml`

### Migration Template

**BEFORE (Broken)**:
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:expression="${execution.setVariable('capexId',
                 capexService.createRequest(execution.getVariables()))}">
</serviceTask>
```

**AFTER (Fixed)**:
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${restServiceDelegate}">
  <documentation>Creates CapEx request in Finance Service</documentation>
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/api/capex-requests</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'amount': requestAmount, 'description': description, 'departmentId': departmentId, 'requestorId': requestorId, 'justification': justification, 'expectedROI': expectedROI, 'paybackPeriod': paybackPeriod}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>capexResponse</flowable:string>
    </flowable:field>
    <flowable:field name="timeoutSeconds">
      <flowable:string>30</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- Extract capexId from response -->
<sequenceFlow sourceRef="createCapExRequest" targetRef="extractCapExId" />

<scriptTask id="extractCapExId" name="Extract CapEx ID" scriptFormat="groovy">
  <script>
    execution.setVariable('capexId', capexResponse.id)
  </script>
</scriptTask>
```

### Step 2: Environment Configuration

**Add to `/services/engine/src/main/resources/application.yml`:**
```yaml
# Service URLs for cross-service communication
services:
  finance:
    url: ${FINANCE_SERVICE_URL:http://localhost:8084/api}
  hr:
    url: ${HR_SERVICE_URL:http://localhost:8082/api}
  procurement:
    url: ${PROCUREMENT_SERVICE_URL:http://localhost:8085/api}
  inventory:
    url: ${INVENTORY_SERVICE_URL:http://localhost:8086/api}
```

**Docker Compose Environment Variables**:
```yaml
engine-service:
  environment:
    - FINANCE_SERVICE_URL=http://finance-service:8084/api
    - HR_SERVICE_URL=http://hr-service:8082/api
    - PROCUREMENT_SERVICE_URL=http://procurement-service:8085/api
    - INVENTORY_SERVICE_URL=http://inventory-service:8086/api
```

**BPMN Variable Access**:
```xml
<flowable:field name="url">
  <flowable:expression>${financeServiceUrl}</flowable:expression>
</flowable:field>
```

This requires registering the property in Spring:
```java
@Configuration
public class ServiceUrlConfig {

    @Value("${services.finance.url}")
    private String financeServiceUrl;

    @Bean
    public String financeServiceUrl() {
        return financeServiceUrl;
    }
}
```

---

## 8. Complete CapEx Workflow Migration Example

### Original (Broken) BPMN

```xml
<serviceTask id="createCapExRequest"
             flowable:expression="${capexService.createRequest(execution.getVariables())}"/>

<serviceTask id="checkBudget"
             flowable:expression="${capexService.checkBudget(requestAmount, departmentId)}"/>

<serviceTask id="updateApproved"
             flowable:expression="${capexService.updateStatus(capexId, 'APPROVED', approvalComments)}"/>

<serviceTask id="sendNotification"
             flowable:expression="${notificationService.sendEmail(requesterEmail, 'Approved', message)}"/>
```

### Migrated (Working) BPMN

```xml
<!-- Service Task: Create CapEx Request -->
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/capex-requests</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'amount': requestAmount, 'description': description, 'departmentId': departmentId, 'requestorId': requestorId, 'justification': justification, 'expectedROI': expectedROI, 'paybackPeriod': paybackPeriod}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>capexResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>

<scriptTask id="extractCapExId" scriptFormat="groovy">
  <script>execution.setVariable('capexId', capexResponse.id)</script>
</scriptTask>

<!-- Service Task: Check Budget -->
<serviceTask id="checkBudget" name="Check Budget Availability"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'departmentId': departmentId, 'amount': requestAmount, 'fiscalYear': fiscalYear}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>budgetCheckResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>

<scriptTask id="extractBudgetStatus" scriptFormat="groovy">
  <script>execution.setVariable('budgetAvailable', budgetCheckResponse.available)</script>
</scriptTask>

<!-- Service Task: Update Status to Approved -->
<serviceTask id="updateApproved" name="Update Status: Approved"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/capex-requests/${capexId}/status</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>PUT</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'status': 'APPROVED', 'comments': approvalComments, 'approvedBy': approverEmail, 'approvedAt': T(java.time.Instant).now().toString()}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>updateResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- Service Task: Send Email Notification -->
<serviceTask id="sendNotification" name="Send Approval Notification"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${engineServiceUrl}/notifications/email</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'to': requesterEmail, 'subject': 'CapEx Request Approved', 'templateName': 'capex-approval', 'variables': #{{'requestId': capexId, 'amount': requestAmount, 'approver': approverEmail}}}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>notificationResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

---

## 9. Advanced Patterns

### Pattern: Conditional Service Calls

```xml
<!-- Only call procurement if amount > 100k -->
<exclusiveGateway id="needsProcurement" />

<sequenceFlow sourceRef="needsProcurement" targetRef="createPO">
  <conditionExpression>${requestAmount > 100000}</conditionExpression>
</sequenceFlow>

<serviceTask id="createPO" flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${procurementServiceUrl}/purchase-orders</flowable:expression>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'capexId': capexId, 'amount': requestAmount}}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

### Pattern: Parallel Service Calls

```xml
<parallelGateway id="fork" />

<!-- Call Finance and HR in parallel -->
<serviceTask id="checkFinance" flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/budget/check</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>

<serviceTask id="checkHR" flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${hrServiceUrl}/employees/${requestorId}/eligibility</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>

<parallelGateway id="join" />
```

### Pattern: Error Handling

```xml
<serviceTask id="callService" flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/capex-requests</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>capexResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- Handle errors -->
<boundaryEvent id="serviceError" attachedToRef="callService">
  <errorEventDefinition />
</boundaryEvent>

<sequenceFlow sourceRef="serviceError" targetRef="handleError" />

<userTask id="handleError" name="Manual Intervention Required">
  <documentation>Service call failed: ${capexResponseError}</documentation>
</userTask>
```

---

## 10. Complete Generic Delegate Library

Werkflow already has these generic delegates in `/shared/delegates/`:

### Available Generic Delegates

1. **RestServiceDelegate** (`${restServiceDelegate}`)
   - HTTP calls to any REST API
   - Configurable: url, method, headers, body, timeout
   - Status: PRODUCTION READY

2. **EmailDelegate** (likely in `/shared/delegates/email/`)
   - Send emails via SMTP
   - Configurable: to, subject, body, template
   - Status: To verify

3. **NotificationDelegate** (`${notificationDelegate}`)
   - Multi-channel notifications
   - Configurable: channels, recipients, message
   - Status: To verify

4. **ApprovalDelegate** (likely in `/shared/delegates/approval/`)
   - Standard approval patterns
   - Configurable: approvers, escalation, timeout
   - Status: To verify

5. **ValidationDelegate** (likely in `/shared/delegates/validation/`)
   - Form and data validation
   - Configurable: rules, schema
   - Status: To verify

6. **FormRequestDelegate** (likely in `/shared/delegates/form/`)
   - Cross-department form requests
   - Configurable: form type, recipients
   - Status: To verify

---

## 11. Final Architectural Recommendation

### DO: Use Generic Delegates

✅ **For Cross-Service Communication (Engine Service)**:
- Use `RestServiceDelegate` for ALL external service calls
- Configure via BPMN extension elements
- Use SpEL map literals for request bodies
- Store service URLs in environment variables
- Extract response data via script tasks

✅ **For Service-Internal Operations (Domain Services)**:
- Use domain-specific delegates (e.g., `leaveApprovalDelegate`)
- Deploy delegates WITH the owning service
- Direct database access within service boundary
- Encapsulate complex business logic

### DON'T: Create Service-Specific Delegates

❌ **DO NOT create these in Engine Service**:
- ~~`CapExServiceDelegate`~~ - Use `RestServiceDelegate` instead
- ~~`ProcurementServiceDelegate`~~ - Use `RestServiceDelegate` instead
- ~~`InventoryServiceDelegate`~~ - Use `RestServiceDelegate` instead
- ~~`HRServiceDelegate`~~ - Use `RestServiceDelegate` instead

**Why?** They violate:
- No-code vision (require Java coding)
- Loose coupling (create tight dependencies)
- Reusability (one delegate per service)
- Microservices architecture (Engine knows service internals)

---

## 12. Implementation Checklist

### Immediate Actions (Critical)

- [ ] Migrate `capex-approval-process.bpmn20.xml` to `RestServiceDelegate`
- [ ] Migrate `procurement-approval-process.bpmn20.xml` to `RestServiceDelegate`
- [ ] Migrate `asset-transfer-approval-process.bpmn20.xml` to `RestServiceDelegate`
- [ ] Add service URL configuration to `application.yml`
- [ ] Create Spring beans for service URLs
- [ ] Test migrated workflows end-to-end

### Documentation

- [ ] Update BPMN developer guide with `RestServiceDelegate` examples
- [ ] Document SpEL map literal syntax
- [ ] Create migration guide for existing workflows
- [ ] Add architecture decision record (ADR)

### Future Enhancements

- [ ] Implement Service Registry backend for dynamic URL management
- [ ] Add visual helper in BPMN designer for RestServiceDelegate configuration
- [ ] Create BPMN templates with pre-configured RestServiceDelegate tasks
- [ ] Add request/response schema validation

---

## 13. Conclusion

**User's Concern**: Absolutely valid. Service-specific delegates would create tight coupling.

**Solution**: Use the existing `RestServiceDelegate` pattern, which is:
- ✅ Already implemented and production-ready
- ✅ Generic and reusable across all services
- ✅ Loosely coupled (no service dependencies)
- ✅ Aligns with 90%+ no-code vision
- ✅ Follows microservices best practices
- ✅ Proven in working workflows (`pr-to-po.bpmn20.xml`)

**Next Steps**: Migrate the 3 broken Engine Service BPMN files to use `RestServiceDelegate` instead of creating service-specific delegates.

---

**Architecture Review Status**: APPROVED - Generic delegate approach
**Service-Specific Delegate Approach**: REJECTED - Violates architectural principles
**Migration Priority**: CRITICAL - Workflows currently broken at runtime
