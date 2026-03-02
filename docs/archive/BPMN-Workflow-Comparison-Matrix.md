# BPMN Workflow Comparison Matrix

**Quick Reference**: Broken vs Working Workflow Patterns

---

## Pattern Comparison

| Aspect | Broken Workflows (3 files) | Working Example (pr-to-po) |
|--------|---------------------------|---------------------------|
| **Location** | Engine Service `/processes/` | Procurement Service `/processes/` |
| **Bean Pattern** | Direct expression: `${capexService.method()}` | Delegate expression: `${restServiceDelegate}` |
| **Service Calls** | Assumes local beans exist | Uses HTTP REST calls via WebClient |
| **Inter-Service** | Expects beans to exist locally | Uses RestServiceDelegate with URL injection |
| **Configuration** | Hardcoded in expression | Field injection via BPMN extensionElements |
| **Deployment** | Deployed successfully | Deployed and WORKING |
| **Runtime** | Will fail immediately | Executes successfully |
| **Bean Existence** | 0 out of 27 references exist | All delegates exist |

---

## File-by-File Breakdown

### 1. capex-approval-process.bpmn20.xml (BROKEN)

```
Location: services/engine/src/main/resources/processes/
Status: DEPLOYED but BROKEN
Service Tasks: 7
Missing Beans: 2 (capexService, notificationService)
Total Failed References: 7

Service Task Pattern:
<serviceTask flowable:expression="${capexService.createRequest(...)}">

What It Needs:
@Service or @Component bean named "capexService" in Engine Service

What Actually Exists:
NOTHING - Engine Service has NO capexService bean
```

**Expected Beans**:
- `capexService` → Finance Service operations (create, update, check budget, reserve)
- `notificationService` → Email/notification operations

**Where They Should Be**:
- Finance Service has REST controllers but NO workflow beans
- Engine Service has NO finance-related beans at all

---

### 2. procurement-approval-process.bpmn20.xml (BROKEN)

```
Location: services/engine/src/main/resources/processes/
Status: DEPLOYED but BROKEN
Service Tasks: 10
Missing Beans: 2 (procurementService, notificationService)
Total Failed References: 10

Service Task Pattern:
<serviceTask flowable:expression="${procurementService.createRequest(...)}">

What It Needs:
@Service or @Component bean named "procurementService" in Engine Service

What Actually Exists:
NOTHING - Engine Service has NO procurementService bean
```

**Expected Beans**:
- `procurementService` → Procurement operations (create request, fetch vendors, create PO, etc.)
- `notificationService` → Email/notification operations

**Where They Should Be**:
- Procurement Service has controllers and delegates but different pattern
- Engine Service has NO procurement-related beans

---

### 3. asset-transfer-approval-process.bpmn20.xml (BROKEN)

```
Location: services/engine/src/main/resources/processes/
Status: DEPLOYED but BROKEN
Service Tasks: 10
Missing Beans: 2 (inventoryService, notificationService)
Total Failed References: 10

Service Task Pattern:
<serviceTask flowable:expression="${inventoryService.createTransferRequest(...)}">

What It Needs:
@Service or @Component bean named "inventoryService" in Engine Service

What Actually Exists:
NOTHING - Engine Service has NO inventoryService bean
```

**Expected Beans**:
- `inventoryService` → Asset/inventory operations (create transfer, verify asset, update custody, etc.)
- `notificationService` → Email/notification operations

**Where They Should Be**:
- Inventory Service has controllers but NO workflow beans
- Engine Service has NO inventory-related beans

---

### 4. pr-to-po.bpmn20.xml (WORKING)

```
Location: services/procurement/src/main/resources/processes/
Status: DEPLOYED and WORKING
Service Tasks: Multiple (budget check, PO creation, notifications, etc.)
Missing Beans: 0
Total Failed References: 0

Service Task Patterns:

1. External Service Call (Finance Service):
<serviceTask flowable:delegateExpression="${restServiceDelegate}">
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

2. Local Service Delegate:
<serviceTask flowable:delegateExpression="${purchaseOrderCreationDelegate}">
  <documentation>Create PO in the system</documentation>
</serviceTask>

3. Local Notification:
<serviceTask flowable:delegateExpression="${notificationDelegate}">
  <extensionElements>
    <flowable:field name="notificationType">
      <flowable:string>PR_BUDGET_SHORTFALL</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Beans Used**:
- `restServiceDelegate` → Shared delegate from werkflow-delegates module (HTTP client)
- `purchaseOrderCreationDelegate` → Local bean in Procurement Service
- `notificationDelegate` → Local bean in Procurement Service
- `processVariableInjector` → Injects service URLs at process start

**Why It Works**:
1. All beans exist in the Procurement Service classpath
2. External calls use RestServiceDelegate with proper configuration
3. Service URLs injected via ProcessVariableInjector at start event
4. Response handling via process variables

---

## Architecture Pattern Analysis

### Broken Pattern (Don't Use)

```
┌─────────────────────┐
│   Engine Service    │
│                     │
│  BPMN Workflow:     │
│  ${capexService...} │ ──┐
│                     │   │
└─────────────────────┘   │
                          │ LOOKS FOR BEAN LOCALLY
                          ▼
                     ❌ NO BEAN FOUND
                     ❌ NoSuchBeanDefinitionException


Expected (but doesn't exist):
┌─────────────────────┐
│   Engine Service    │
│                     │
│  @Component         │
│  class CapExService │ ← Should exist but DOESN'T
│  @Component         │
│  class ProcurementService │ ← Should exist but DOESN'T
│  @Component         │
│  class InventoryService │ ← Should exist but DOESN'T
│                     │
└─────────────────────┘
```

### Working Pattern (Use This)

```
┌──────────────────────────┐
│  Procurement Service     │
│                          │
│  BPMN Workflow:          │
│  ${restServiceDelegate}  │ ──┐
│                          │   │
│  @Component              │   │ DELEGATES TO
│  RestServiceDelegate ────┼───┘ (Shared Module)
│                          │
│  @Component              │
│  ProcessVariableInjector │ ← Injects service URLs
│                          │
└──────────────────────────┘
           │
           │ HTTP POST
           ▼
┌──────────────────────────┐
│    Finance Service       │
│                          │
│  @RestController         │
│  BudgetCheckController   │ ← REST API endpoint
│    /budget/check         │
│                          │
└──────────────────────────┘

Process Flow:
1. Process starts → ProcessVariableInjector adds service URLs
2. Budget check task → RestServiceDelegate makes HTTP call
3. Response stored in process variable
4. Next tasks use response data
```

---

## Code Pattern Examples

### WRONG: Direct Bean Expression (Broken Workflows)

```xml
<!-- THIS DOES NOT WORK -->
<serviceTask id="createCapExRequest"
             name="Create CapEx Request"
             flowable:expression="${execution.setVariable('capexId', capexService.createRequest(execution.getVariables()))}">
  <documentation>Creates CapEx request</documentation>
</serviceTask>

<!-- Runtime Error: -->
<!-- NoSuchBeanDefinitionException: No bean named 'capexService' available -->
```

**Problems**:
1. Assumes `capexService` bean exists in Engine Service
2. No such bean is defined anywhere
3. Can't call Finance Service directly (different microservice)
4. No way to inject configuration
5. Breaks at runtime, not deployment time

---

### RIGHT: RestServiceDelegate Pattern (Working Example)

```xml
<!-- THIS WORKS -->
<serviceTask id="createCapExRequest"
             name="Create CapEx Request"
             flowable:delegateExpression="${restServiceDelegate}">
  <documentation>Creates CapEx request via Finance Service REST API</documentation>
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

<!-- Next task can use response -->
<serviceTask id="checkBudget"
             name="Check Budget"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/budget/check</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'capexId': capexResponse.capexId, 'amount': requestAmount}}</flowable:expression>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>budgetCheckResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Why This Works**:
1. `restServiceDelegate` bean exists (shared delegates module)
2. Service URL injected from process variables
3. Makes HTTP call to Finance Service REST API
4. Response stored in process variable for later use
5. Fully configurable via BPMN
6. Works across microservice boundaries

---

### RIGHT: Local Delegate Pattern (Working Example)

```xml
<!-- For operations within same service -->
<serviceTask id="createPO"
             name="Create Purchase Order"
             flowable:delegateExpression="${purchaseOrderCreationDelegate}">
  <documentation>Create PO in local database</documentation>
</serviceTask>
```

**Supporting Bean** (in Procurement Service):
```java
@Component("purchaseOrderCreationDelegate")
public class PurchaseOrderCreationDelegate implements JavaDelegate {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long prId = (Long) execution.getVariable("prId");
        // Direct database access - same service
        PurchaseOrder po = createPurchaseOrder(prId);
        execution.setVariable("poId", po.getId());
    }
}
```

**Why This Works**:
1. Bean is defined in the SAME service as the workflow
2. Has direct access to repositories/services
3. No HTTP overhead
4. Type-safe Java code

---

## Service URL Injection Setup

### Required Configuration

**1. Service URLs in application.yml** (Engine Service):
```yaml
app:
  services:
    finance-url: ${FINANCE_SERVICE_URL:http://localhost:8084}
    procurement-url: ${PROCUREMENT_SERVICE_URL:http://localhost:8085}
    inventory-url: ${INVENTORY_SERVICE_URL:http://localhost:8086}
    hr-url: ${HR_SERVICE_URL:http://localhost:8082}
```

**2. Configuration Bean**:
```java
@Configuration
@ConfigurationProperties(prefix = "app.services")
public class ServiceUrlConfiguration {
    private String financeUrl;
    private String procurementUrl;
    private String inventoryUrl;
    private String hrUrl;

    public Map<String, String> getServiceUrlMap() {
        Map<String, String> urls = new HashMap<>();
        urls.put("financeServiceUrl", financeUrl);
        urls.put("procurementServiceUrl", procurementUrl);
        urls.put("inventoryServiceUrl", inventoryUrl);
        urls.put("hrServiceUrl", hrUrl);
        return urls;
    }
}
```

**3. Process Variable Injector**:
```java
@Component("processVariableInjector")
public class ProcessVariableInjector implements ExecutionListener {

    private final ServiceUrlConfiguration serviceUrlConfiguration;

    @Override
    public void notify(DelegateExecution execution) {
        Map<String, String> serviceUrls = serviceUrlConfiguration.getServiceUrlMap();
        serviceUrls.forEach(execution::setVariable);
    }
}
```

**4. BPMN Start Event**:
```xml
<startEvent id="startEvent" name="Process Started">
  <extensionElements>
    <flowable:executionListener event="start"
                                delegateExpression="${processVariableInjector}" />
  </extensionElements>
</startEvent>
```

---

## Migration Checklist

### For Each Broken Workflow

- [ ] Identify all service task expressions using `${serviceName.method()}`
- [ ] Create REST API endpoints in target service (Finance/Procurement/Inventory)
- [ ] Add ServiceUrlConfiguration to Engine Service (if not exists)
- [ ] Add ProcessVariableInjector to Engine Service (if not exists)
- [ ] Refactor each service task to use RestServiceDelegate pattern
- [ ] Add process variable injector to start event
- [ ] Update sequence flows to extract response variables
- [ ] Test service task execution individually
- [ ] Test complete process end-to-end
- [ ] Deploy and verify in dev environment

### REST API Endpoints Needed

**Finance Service** (`/api/workflow/capex/`):
- `POST /requests` - Create CapEx request
- `POST /budget/check` - Check budget availability
- `POST /budget/reserve` - Reserve budget
- `PUT /requests/{id}/status` - Update status

**Procurement Service** (`/api/workflow/procurement/`):
- `POST /requests` - Create purchase request
- `GET /vendors/approved` - Get approved vendors
- `POST /quotations/request` - Request quotations
- `POST /quotations/calculate` - Calculate total cost
- `POST /purchase-orders` - Create PO
- `POST /purchase-orders/{id}/send` - Send PO to vendor
- `PUT /requests/{id}/status` - Update status

**Inventory Service** (`/api/workflow/asset-transfer/`):
- `POST /transfers` - Create transfer request
- `GET /assets/{id}/verify` - Verify asset availability
- `GET /assets/{id}/custodian` - Get current custodian
- `PUT /assets/{id}/custody` - Update custody
- `PUT /assets/{id}/location` - Update location
- `POST /transfers/records` - Create transfer record
- `PUT /transfers/{id}/status` - Update transfer status

---

## Quick Diagnosis Guide

### Is My Workflow Broken?

**Check 1**: Does it use `flowable:expression="${someService.method()}"`?
- ✅ If YES → Likely broken (check if bean exists)
- ✅ If NO → Likely working

**Check 2**: Does the bean exist in the SERVICE where the workflow is deployed?
- ✅ If YES → Working
- ❌ If NO → Broken

**Check 3**: For inter-service calls, does it use RestServiceDelegate?
- ✅ If YES → Working
- ❌ If NO → Broken

### Common Error Messages

**Error**: `NoSuchBeanDefinitionException: No bean named 'capexService' available`
- **Cause**: BPMN references bean that doesn't exist
- **Fix**: Either create bean or refactor to use RestServiceDelegate

**Error**: `Unknown property used in expression: ${capexService}`
- **Cause**: Bean name typo or bean not registered
- **Fix**: Check @Component name matches BPMN expression exactly

**Error**: `NullPointerException` in service task
- **Cause**: Response variable not set or incorrectly accessed
- **Fix**: Check responseVariable name matches what you're trying to access

---

## Summary

| Metric | Broken Workflows | Working Example |
|--------|------------------|-----------------|
| Total Files | 3 | 1 |
| Service Tasks | 27 | ~10 |
| Missing Beans | 27 (100%) | 0 (0%) |
| Will Execute | ❌ NO | ✅ YES |
| Pattern Used | Direct expression | Delegate expression |
| Inter-Service | Broken | RestServiceDelegate |
| Deployment | ✅ Success | ✅ Success |
| Runtime | ❌ Fails | ✅ Works |
| User Impact | HIGH (immediate failure) | NONE (works perfectly) |

**Recommendation**: Migrate all 3 broken workflows to use the RestServiceDelegate pattern demonstrated in `pr-to-po.bpmn20.xml`.

---

**End of Comparison Matrix**
