# BPMN JSON Expression Architecture Solution

**Problem Statement**: Flowable BPMN engine fails to parse `#{{}}` syntax for JSON objects in service task expressions, causing engine startup failures and workflow execution errors.

**Date**: 2025-12-17
**Severity**: Critical - Engine in restart loop
**Status**: Comprehensive Solution Provided

---

## Executive Summary

The root cause is **attempting to use invalid Flowable Expression Language (EL) syntax** to construct JSON payloads. The disabled BPMN file (`capex-approval-process-v2.bpmn20.xml.disabled`) attempts to use non-existent `restServiceDelegate` beans with manually constructed JSON strings, which is:

1. **Syntactically invalid** - `#{{}}` is not valid Flowable EL
2. **Architecturally flawed** - String concatenation for JSON is error-prone and unmaintainable
3. **Missing implementations** - The `restServiceDelegate` and `notificationDelegate` beans don't exist

**Recommended Solution**: Implement custom Java delegates that accept process variables directly and handle JSON serialization internally using Jackson/Gson.

---

## Root Cause Analysis

### 1. The Invalid Expression Syntax

**Problematic Code (Line 33 of disabled file):**
```xml
<flowable:expression>${'{"title":"' + title + '","description":"' + description + '","category":"' + category + '","amount":' + requestAmount + ',"priority":"' + priority + '","approvalLevel":' + approvalLevel + ',"businessJustification":"' + businessJustification + '","expectedBenefits":"' + expectedBenefits + '","expectedCompletionDate":"' + expectedCompletionDate + '","budgetYear":' + budgetYear + ',"departmentName":"' + departmentName + '","requestedBy":"' + requestedBy + '"}'}</flowable:expression>
```

**Why This Fails:**
- **JSON Escaping Issues**: String values may contain quotes, newlines, or special characters that break JSON syntax
- **No Type Safety**: Numbers and booleans are concatenated as strings
- **Null Handling**: If any variable is null, expression becomes `"null"` string instead of JSON `null`
- **Unmaintainable**: 13 fields in one expression line
- **Error-Prone**: Easy to miss commas, quotes, or closing braces

### 2. Flowable Expression Language (EL) Capabilities

Flowable uses **Unified Expression Language (UEL)** which supports:
- ✅ Variable access: `${variableName}`
- ✅ Method calls: `${serviceBean.methodName(param1, param2)}`
- ✅ Arithmetic/logical operations: `${amount > 1000}`
- ✅ String concatenation: `${'Hello ' + name}`
- ❌ **NOT** object literal syntax: `#{{}}`
- ❌ **NOT** JSON construction

### 3. Missing Bean Implementations

The BPMN files reference beans that don't exist:
```xml
flowable:delegateExpression="${restServiceDelegate}"
flowable:delegateExpression="${notificationDelegate}"
```

Current delegate implementations found:
- ✅ `TaskAssignmentDelegate` - Exists
- ✅ `ApprovalTaskCompletionListener` - Exists
- ❌ `RestServiceDelegate` - **MISSING**
- ❌ `NotificationDelegate` - **MISSING**

---

## Architectural Solutions (4 Options)

### Option 1: Custom Java Delegates with Auto-Serialization (RECOMMENDED)

**Approach**: Create service-specific delegates that accept process variables and handle REST calls internally.

**Pros:**
- Type-safe and maintainable
- Built-in error handling and retries
- Centralized HTTP client configuration
- Easy to test and mock
- Clean BPMN files

**Cons:**
- Requires Java code for each service integration
- Slightly more boilerplate

**Implementation:**

#### Step 1: Create CapExServiceDelegate

```java
package com.werkflow.engine.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service delegate for CapEx workflow operations via Finance Service REST API.
 *
 * Handles JSON serialization, HTTP communication, and error handling.
 * Process variables are automatically extracted and sent as JSON payload.
 */
@Slf4j
@Component("capexServiceDelegate")
public class CapExServiceDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.finance-url}")
    private String financeServiceUrl;

    public CapExServiceDelegate(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String operation = (String) execution.getVariable("operation");

        try {
            switch (operation) {
                case "CREATE_REQUEST":
                    createRequest(execution);
                    break;
                case "CHECK_BUDGET":
                    checkBudget(execution);
                    break;
                case "UPDATE_STATUS":
                    updateStatus(execution);
                    break;
                case "ALLOCATE_BUDGET":
                    allocateBudget(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            log.error("Error executing CapEx service operation: {}", operation, e);
            execution.setVariable("serviceError", e.getMessage());
            throw new RuntimeException("CapEx service call failed", e);
        }
    }

    private void createRequest(DelegateExecution execution) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("title", execution.getVariable("title"));
        requestData.put("description", execution.getVariable("description"));
        requestData.put("category", execution.getVariable("category"));
        requestData.put("amount", execution.getVariable("requestAmount"));
        requestData.put("priority", execution.getVariable("priority"));
        requestData.put("approvalLevel", execution.getVariable("approvalLevel"));
        requestData.put("businessJustification", execution.getVariable("businessJustification"));
        requestData.put("expectedBenefits", execution.getVariable("expectedBenefits"));
        requestData.put("expectedCompletionDate", execution.getVariable("expectedCompletionDate"));
        requestData.put("budgetYear", execution.getVariable("budgetYear"));
        requestData.put("departmentName", execution.getVariable("departmentName"));
        requestData.put("requestedBy", execution.getVariable("requestedBy"));

        String url = financeServiceUrl + "/api/workflow/capex/create-request";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("capexId", response.getBody().get("capexId"));
            execution.setVariable("requestNumber", response.getBody().get("requestNumber"));
            log.info("Created CapEx request: {}", response.getBody().get("requestNumber"));
        } else {
            throw new RuntimeException("Failed to create CapEx request");
        }
    }

    private void checkBudget(DelegateExecution execution) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("capexId", execution.getVariable("capexId"));
        requestData.put("requestAmount", execution.getVariable("requestAmount"));
        requestData.put("departmentName", execution.getVariable("departmentName"));

        String url = financeServiceUrl + "/api/workflow/capex/check-budget";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("budgetAvailable", response.getBody().get("budgetAvailable"));
            execution.setVariable("availableBudget", response.getBody().get("availableBudget"));
            log.info("Budget check result: {}", response.getBody().get("budgetAvailable"));
        } else {
            throw new RuntimeException("Failed to check budget");
        }
    }

    private void updateStatus(DelegateExecution execution) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("capexId", execution.getVariable("capexId"));
        requestData.put("status", execution.getVariable("newStatus"));
        requestData.put("comments", execution.getVariable("statusComments"));

        String url = financeServiceUrl + "/api/workflow/capex/update-status";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
        log.info("Updated CapEx status to: {}", execution.getVariable("newStatus"));
    }

    private void allocateBudget(DelegateExecution execution) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("capexId", execution.getVariable("capexId"));
        requestData.put("requestAmount", execution.getVariable("requestAmount"));
        requestData.put("departmentName", execution.getVariable("departmentName"));

        String url = financeServiceUrl + "/api/workflow/capex/allocate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("budgetAllocationId", response.getBody().get("allocationId"));
            log.info("Allocated budget for CapEx: {}", execution.getVariable("capexId"));
        } else {
            throw new RuntimeException("Failed to allocate budget");
        }
    }
}
```

#### Step 2: Update BPMN to Use Delegate

```xml
<!-- Service Task: Create CapEx Request via Delegate -->
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${capexServiceDelegate}">
  <documentation>Creates CapEx request record in Finance Service via REST API</documentation>
  <extensionElements>
    <!-- Simply set the operation type -->
    <flowable:executionListener event="start"
      expression="${execution.setVariable('operation', 'CREATE_REQUEST')}"/>
  </extensionElements>
</serviceTask>

<!-- No more script tasks needed! -->
<sequenceFlow id="flow2" sourceRef="createCapExRequest" targetRef="checkBudget" />

<!-- Service Task: Check Budget -->
<serviceTask id="checkBudget" name="Check Budget Availability"
             flowable:delegateExpression="${capexServiceDelegate}">
  <documentation>Verifies if budget is available for the requested amount</documentation>
  <extensionElements>
    <flowable:executionListener event="start"
      expression="${execution.setVariable('operation', 'CHECK_BUDGET')}"/>
  </extensionElements>
</serviceTask>
```

**Benefits:**
- Clean BPMN file (no complex expressions)
- All business logic in testable Java code
- Automatic JSON serialization via Jackson
- Built-in error handling and logging
- Easy to extend with retry logic, circuit breakers, etc.

---

### Option 2: Generic REST Delegate with Field Injection

**Approach**: Create a single reusable delegate that accepts URL, method, and variable names via field injection.

**Pros:**
- Single delegate for all REST calls
- Configurable from BPMN
- Less Java boilerplate

**Cons:**
- BPMN files become more complex
- Less type safety
- Harder to add custom logic per endpoint

**Implementation:**

```java
package com.werkflow.engine.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic REST service delegate that accepts variable names and constructs JSON automatically.
 */
@Slf4j
@Component("genericRestDelegate")
public class GenericRestDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Injected from BPMN
    private Expression url;
    private Expression method;
    private Expression variableNames; // Comma-separated list
    private Expression responseVariable;

    public GenericRestDelegate(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String urlValue = (String) url.getValue(execution);
        String methodValue = (String) method.getValue(execution);
        String variableNamesValue = (String) variableNames.getValue(execution);
        String responseVarName = responseVariable != null ?
            (String) responseVariable.getValue(execution) : null;

        // Build request body from variable names
        Map<String, Object> requestBody = new HashMap<>();
        for (String varName : variableNamesValue.split(",")) {
            String trimmedName = varName.trim();
            Object value = execution.getVariable(trimmedName);
            requestBody.put(trimmedName, value);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        HttpMethod httpMethod = HttpMethod.valueOf(methodValue.toUpperCase());

        ResponseEntity<Map> response = restTemplate.exchange(
            urlValue, httpMethod, request, Map.class);

        if (responseVarName != null && response.getBody() != null) {
            // Store entire response or extract specific fields
            response.getBody().forEach((key, value) ->
                execution.setVariable(key.toString(), value));
        }

        log.info("REST call to {} completed with status: {}", urlValue, response.getStatusCode());
    }
}
```

**BPMN Usage:**

```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${genericRestDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/create-request</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="variableNames">
      <flowable:string>title,description,category,requestAmount,priority,approvalLevel,businessJustification,expectedBenefits,expectedCompletionDate,budgetYear,departmentName,requestedBy</flowable:string>
    </flowable:field>
    <flowable:field name="responseVariable">
      <flowable:string>createRequestResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

---

### Option 3: Flowable HTTP Task (Built-in)

**Approach**: Use Flowable's built-in HTTP Service Task instead of custom delegates.

**Pros:**
- No custom Java code needed
- Standard Flowable feature
- Well-documented

**Cons:**
- Still requires manual JSON construction or Groovy scripts
- Less flexible than custom delegates
- HTTP task configuration can be verbose

**Implementation:**

```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:type="http">
  <extensionElements>
    <flowable:field name="requestMethod">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="requestUrl">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/create-request</flowable:expression>
    </flowable:field>
    <flowable:field name="requestHeaders">
      <flowable:string>Content-Type:application/json</flowable:string>
    </flowable:field>
    <flowable:field name="requestBody">
      <!-- Still need Groovy to build JSON properly -->
      <flowable:expression>
        <![CDATA[
        ${groovy(
          def mapper = new groovy.json.JsonBuilder()
          mapper {
            title execution.getVariable('title')
            description execution.getVariable('description')
            category execution.getVariable('category')
            amount execution.getVariable('requestAmount')
            // ... etc
          }
          return mapper.toString()
        )}
        ]]>
      </flowable:expression>
    </flowable:field>
    <flowable:field name="saveResponseVariableName">
      <flowable:string>createRequestResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

**Note**: Requires Groovy dependency and script engine configuration.

---

### Option 4: Combine Script Tasks with HTTP Tasks

**Approach**: Use Groovy script tasks to build JSON, then pass to HTTP tasks.

**Pros:**
- Separation of data transformation and communication
- Groovy has native JSON support

**Cons:**
- Two tasks per operation
- Requires Groovy scripting knowledge
- More complex BPMN diagrams

**Implementation:**

```xml
<!-- Script Task: Prepare Request Data -->
<scriptTask id="prepareCapExRequest" name="Prepare CapEx Request" scriptFormat="groovy">
  <script>
    <![CDATA[
    import groovy.json.JsonBuilder

    def requestData = [
      title: execution.getVariable('title'),
      description: execution.getVariable('description'),
      category: execution.getVariable('category'),
      amount: execution.getVariable('requestAmount'),
      priority: execution.getVariable('priority'),
      approvalLevel: execution.getVariable('approvalLevel'),
      businessJustification: execution.getVariable('businessJustification'),
      expectedBenefits: execution.getVariable('expectedBenefits'),
      expectedCompletionDate: execution.getVariable('expectedCompletionDate'),
      budgetYear: execution.getVariable('budgetYear'),
      departmentName: execution.getVariable('departmentName'),
      requestedBy: execution.getVariable('requestedBy')
    ]

    def builder = new JsonBuilder(requestData)
    execution.setVariable('capexRequestJson', builder.toString())
    ]]>
  </script>
</scriptTask>

<!-- HTTP Task: Send Request -->
<serviceTask id="createCapExRequest" name="Create CapEx Request" flowable:type="http">
  <extensionElements>
    <flowable:field name="requestMethod">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="requestUrl">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/create-request</flowable:expression>
    </flowable:field>
    <flowable:field name="requestHeaders">
      <flowable:string>Content-Type:application/json</flowable:string>
    </flowable:field>
    <flowable:field name="requestBody">
      <flowable:expression>${capexRequestJson}</flowable:expression>
    </flowable:field>
    <flowable:field name="saveResponseVariableName">
      <flowable:string>createRequestResponse</flowable:string>
    </flowable:field>
  </extensionElements>
</serviceTask>
```

---

## Recommended Implementation Plan

### Phase 1: Immediate Fix (Option 1 - Custom Delegates)

**Priority: Critical**

1. **Create Service Delegates** (2-3 hours)
   - Implement `CapExServiceDelegate`
   - Implement `ProcurementServiceDelegate`
   - Implement `InventoryServiceDelegate`
   - Implement `NotificationServiceDelegate`

2. **Update BPMN Files** (1-2 hours)
   - Replace all REST expressions with delegate calls
   - Remove script tasks for JSON extraction
   - Update variable names to match delegate expectations

3. **Testing** (2 hours)
   - Unit tests for each delegate
   - Integration test with Finance service
   - End-to-end workflow test

4. **Deployment** (1 hour)
   - Build Docker images
   - Deploy to Docker environment
   - Verify engine starts successfully
   - Run smoke test workflows

**Files to Create:**
```
/services/engine/src/main/java/com/werkflow/engine/delegate/
├── CapExServiceDelegate.java
├── ProcurementServiceDelegate.java
├── InventoryServiceDelegate.java
└── NotificationServiceDelegate.java
```

**Files to Update:**
```
/services/engine/src/main/resources/processes/
├── capex-approval-process.bpmn20.xml (keep as reference - uses method call pattern)
├── capex-approval-process-v2.bpmn20.xml.disabled → rename to .bpmn20.xml after fixes
├── procurement-approval-process.bpmn20.xml
└── asset-transfer-approval-process.bpmn20.xml
```

### Phase 2: Enhanced Features (Week 2)

1. **Add Resilience Patterns**
   - Implement retry logic with exponential backoff
   - Add circuit breaker for external services
   - Implement timeout handling

2. **Improve Error Handling**
   - Create custom exception types
   - Add compensation logic for failed transactions
   - Implement error boundary events in BPMN

3. **Monitoring & Observability**
   - Add metrics for service call duration
   - Implement distributed tracing
   - Create dashboards for workflow health

### Phase 3: Refactoring (Month 2)

1. **Consider Generic Delegate** (Option 2)
   - Evaluate if service-specific logic is minimal
   - Refactor to generic delegate if appropriate
   - Update BPMN files to use field injection

2. **Optimize for Maintainability**
   - Extract common HTTP client configuration
   - Create base delegate class for shared logic
   - Implement delegate factory pattern

---

## Validation & Testing Strategy

### 1. BPMN Syntax Validation

**Pre-Deployment Checks:**

```bash
# Script: validate-bpmn.sh
#!/bin/bash

BPMN_DIR="/services/engine/src/main/resources/processes"

echo "Validating BPMN files..."

for file in $BPMN_DIR/*.bpmn20.xml; do
  if [ "$(basename "$file")" != "*.disabled" ]; then
    echo "Checking: $file"

    # Check for invalid expression syntax
    if grep -q '#{' "$file"; then
      echo "ERROR: Found invalid #{} expression in $file"
      exit 1
    fi

    # Check for delegate beans that exist
    delegates=$(grep -o 'delegateExpression="\${[^}]*}"' "$file" |
                sed 's/delegateExpression="\${//g' | sed 's/}"//g')

    for delegate in $delegates; do
      echo "  - Delegate: $delegate"
      # TODO: Check if bean exists in Spring context
    done

    echo "✓ $file is valid"
  fi
done

echo "All BPMN files validated successfully!"
```

### 2. Unit Tests for Delegates

```java
package com.werkflow.engine.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CapExServiceDelegateTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DelegateExecution execution;

    private CapExServiceDelegate delegate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new CapExServiceDelegate(restTemplate, new ObjectMapper());
    }

    @Test
    void testCreateRequest_Success() {
        // Given
        when(execution.getVariable("operation")).thenReturn("CREATE_REQUEST");
        when(execution.getVariable("title")).thenReturn("New Asset");
        when(execution.getVariable("requestAmount")).thenReturn(50000.0);
        // ... other variables

        Map<String, Object> mockResponse = Map.of(
            "capexId", 123L,
            "requestNumber", "CAPEX-2025-001"
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(responseEntity);

        // When
        delegate.execute(execution);

        // Then
        verify(execution).setVariable("capexId", 123L);
        verify(execution).setVariable("requestNumber", "CAPEX-2025-001");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), requestCaptor.capture(), eq(Map.class));

        HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
        assertEquals(MediaType.APPLICATION_JSON, capturedRequest.getHeaders().getContentType());
        assertNotNull(capturedRequest.getBody());
    }

    @Test
    void testCreateRequest_ServiceError() {
        // Given
        when(execution.getVariable("operation")).thenReturn("CREATE_REQUEST");
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When/Then
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
        verify(execution).setVariable(eq("serviceError"), anyString());
    }
}
```

### 3. Integration Test with Finance Service

```java
@SpringBootTest
@AutoConfigureMockMvc
class CapExWorkflowIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testCapExWorkflow_EndToEnd() {
        // Mock Finance Service responses
        Map<String, Object> createResponse = Map.of(
            "capexId", 1L,
            "requestNumber", "CAPEX-2025-001"
        );

        when(restTemplate.postForEntity(
            contains("/api/workflow/capex/create-request"),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(createResponse, HttpStatus.OK));

        // Start workflow
        Map<String, Object> variables = Map.of(
            "title", "Server Upgrade",
            "description", "Upgrade production servers",
            "requestAmount", 75000.0,
            "departmentName", "IT"
        );

        ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("capex-approval-process-v2", variables);

        assertNotNull(processInstance);
        assertEquals(1L, runtimeService.getVariable(processInstance.getId(), "capexId"));

        // Verify Manager Approval task was created
        Task managerTask = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskDefinitionKey("managerApproval")
            .singleResult();

        assertNotNull(managerTask);
        assertEquals("Manager Review", managerTask.getName());
    }
}
```

---

## Migration Strategy for Existing BPMN Files

### Step-by-Step Process

1. **Backup All BPMN Files**
   ```bash
   cp -r /services/engine/src/main/resources/processes \
        /services/engine/src/main/resources/processes.backup
   ```

2. **Fix One Workflow at a Time**
   - Start with `capex-approval-process-v2.bpmn20.xml`
   - Keep original working version (`capex-approval-process.bpmn20.xml`) as fallback
   - Test thoroughly before moving to next workflow

3. **Migration Checklist for Each BPMN File**
   - [ ] Create/update service delegate Java class
   - [ ] Replace `flowable:delegateExpression="${restServiceDelegate}"` with specific delegate
   - [ ] Remove `<flowable:field name="body">` expressions
   - [ ] Add operation type via execution listener or variable
   - [ ] Remove intermediate script tasks for JSON parsing
   - [ ] Update variable names to match delegate expectations
   - [ ] Test with unit tests
   - [ ] Test with integration tests
   - [ ] Deploy to dev environment
   - [ ] Validate in QA environment

4. **Rollback Plan**
   - Keep `.disabled` suffix on new BPMN files until fully tested
   - Rename working version to `.v1.bpmn20.xml` as fallback
   - Only remove old version after 2 weeks of stable operation

---

## Long-Term Architectural Recommendations

### 1. Service Mesh Pattern

For microservices communication, consider implementing:

- **Service Registry** - Already implemented (admin-service)
- **API Gateway** - Single entry point for all workflow-to-service communication
- **Circuit Breaker** - Resilience4j for fault tolerance
- **Distributed Tracing** - Sleuth + Zipkin for request tracking

### 2. Event-Driven Architecture

Instead of synchronous REST calls from workflows:

- **Publish Events** - Workflow publishes domain events to message broker
- **Service Consumers** - Services subscribe to events and process asynchronously
- **Correlation** - Use correlation IDs to link events back to workflow instances

**Benefits:**
- Decouples workflow from service availability
- Better scalability
- Natural retry mechanism via message queues

### 3. BPMN Best Practices

- **Keep BPMN Simple** - Avoid complex expressions in XML
- **Business Logic in Code** - Delegates should contain logic, not BPMN files
- **Use External Tasks** - For truly asynchronous operations
- **Versioning** - Use semantic versioning for process definitions
- **Documentation** - Maintain up-to-date process documentation

### 4. Code Generation

For repetitive delegate patterns, consider:

```java
// Annotation-based delegate generation
@WorkflowDelegate("capexService")
public interface CapExService {

    @POST("/api/workflow/capex/create-request")
    @Variables("title", "description", "category", "requestAmount")
    CapExResponse createRequest(DelegateExecution execution);

    @POST("/api/workflow/capex/check-budget")
    @Variables("capexId", "requestAmount", "departmentName")
    BudgetCheckResponse checkBudget(DelegateExecution execution);
}

// Code generator produces implementation
```

---

## Summary & Decision Matrix

| Approach | Complexity | Maintainability | Performance | Flexibility | Recommendation |
|----------|-----------|-----------------|-------------|-------------|----------------|
| **Option 1: Custom Delegates** | Medium | High | High | High | ✅ **RECOMMENDED** |
| Option 2: Generic Delegate | Low | Medium | High | Medium | ⚠️ Consider for v2 |
| Option 3: Flowable HTTP Task | Medium | Low | Medium | Low | ❌ Not recommended |
| Option 4: Script + HTTP | High | Low | Medium | Medium | ❌ Not recommended |

### Why Option 1 is Best

1. **Type Safety** - Compile-time checks prevent runtime errors
2. **Testability** - Easy to unit test and mock
3. **Extensibility** - Can add custom logic per service/operation
4. **Debugging** - Stack traces show exact failure points
5. **Maintainability** - Clear separation of concerns
6. **Production Ready** - Built-in error handling and logging

---

## Immediate Action Items

**For Engine Service Team:**

1. **Create 4 Delegate Classes** (Priority: P0)
   - CapExServiceDelegate
   - ProcurementServiceDelegate
   - InventoryServiceDelegate
   - NotificationServiceDelegate

2. **Update capex-approval-process-v2.bpmn20.xml** (Priority: P0)
   - Replace all REST expressions with delegate calls
   - Test thoroughly

3. **Verify RestTemplate Bean Configuration** (Priority: P1)
   - Ensure RestTemplate is configured with appropriate timeouts
   - Add interceptors for logging and auth headers

4. **Write Tests** (Priority: P1)
   - Unit tests for each delegate
   - Integration test for CapEx workflow

5. **Update Documentation** (Priority: P2)
   - Document delegate pattern usage
   - Create BPMN authoring guidelines
   - Update deployment runbook

**Estimated Timeline:**
- Delegate implementation: 4 hours
- BPMN updates: 2 hours
- Testing: 3 hours
- Deployment: 1 hour
- **Total: 1-2 days**

---

## References

- [Flowable Expression Language Documentation](https://www.flowable.com/open-source/docs/bpmn/ch07b-BPMN-Constructs#expression-language)
- [Java Delegate Reference](https://www.flowable.com/open-source/docs/bpmn/ch07b-BPMN-Constructs#java-service-task)
- [Flowable HTTP Task](https://www.flowable.com/open-source/docs/bpmn/ch07b-BPMN-Constructs#http-task)
- [Spring RestTemplate Best Practices](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)

---

**Document Version**: 1.0
**Last Updated**: 2025-12-17
**Author**: Backend Architecture Team
**Review Status**: Ready for Implementation
