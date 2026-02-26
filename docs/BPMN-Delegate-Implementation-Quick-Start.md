# BPMN Delegate Implementation Quick Start Guide

**Quick reference for implementing custom Java delegates for Flowable BPMN workflows**

---

## TL;DR - The Problem

```xml
<!-- WRONG - This will fail -->
<flowable:expression>#{{'title': title, 'description': description}}</flowable:expression>

<!-- WRONG - String concatenation is error-prone -->
<flowable:expression>${'{"title":"' + title + '","description":"' + description + '"}'}</flowable:expression>

<!-- RIGHT - Use Java delegates -->
<serviceTask id="createRequest" flowable:delegateExpression="${capexServiceDelegate}">
  <!-- All logic is in Java, BPMN stays clean -->
</serviceTask>
```

---

## Quick Implementation (15 Minutes)

### 1. Create Service Delegate Class

**Location**: `/services/engine/src/main/java/com/werkflow/engine/delegate/CapExServiceDelegate.java`

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

@Slf4j
@Component("capexServiceDelegate") // Bean name for BPMN reference
public class CapExServiceDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${app.services.finance-url}")
    private String financeServiceUrl;

    public CapExServiceDelegate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String operation = (String) execution.getVariable("operation");

        try {
            switch (operation) {
                case "CREATE_REQUEST" -> createRequest(execution);
                case "CHECK_BUDGET" -> checkBudget(execution);
                case "UPDATE_STATUS" -> updateStatus(execution);
                case "ALLOCATE_BUDGET" -> allocateBudget(execution);
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            log.error("Error in CapEx operation: {}", operation, e);
            execution.setVariable("serviceError", e.getMessage());
            throw new RuntimeException("CapEx service call failed", e);
        }
    }

    private void createRequest(DelegateExecution execution) {
        // Extract variables from workflow
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("title", execution.getVariable("title"));
        requestData.put("description", execution.getVariable("description"));
        requestData.put("category", execution.getVariable("category"));
        requestData.put("amount", execution.getVariable("requestAmount"));
        requestData.put("departmentName", execution.getVariable("departmentName"));
        requestData.put("requestedBy", execution.getVariable("requestedBy"));

        // Make REST call
        String url = financeServiceUrl + "/api/workflow/capex/create-request";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Store response in workflow variables
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("capexId", response.getBody().get("capexId"));
            execution.setVariable("requestNumber", response.getBody().get("requestNumber"));
            log.info("Created CapEx request: {}", response.getBody().get("requestNumber"));
        } else {
            throw new RuntimeException("Failed to create CapEx request");
        }
    }

    private void checkBudget(DelegateExecution execution) {
        Map<String, Object> requestData = Map.of(
            "capexId", execution.getVariable("capexId"),
            "requestAmount", execution.getVariable("requestAmount"),
            "departmentName", execution.getVariable("departmentName")
        );

        String url = financeServiceUrl + "/api/workflow/capex/check-budget";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("budgetAvailable", response.getBody().get("budgetAvailable"));
            execution.setVariable("availableBudget", response.getBody().get("availableBudget"));
        }
    }

    private void updateStatus(DelegateExecution execution) {
        Map<String, Object> requestData = Map.of(
            "capexId", execution.getVariable("capexId"),
            "status", execution.getVariable("newStatus"),
            "comments", execution.getVariable("statusComments")
        );

        String url = financeServiceUrl + "/api/workflow/capex/update-status";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
        log.info("Updated CapEx status to: {}", execution.getVariable("newStatus"));
    }

    private void allocateBudget(DelegateExecution execution) {
        Map<String, Object> requestData = Map.of(
            "capexId", execution.getVariable("capexId"),
            "requestAmount", execution.getVariable("requestAmount"),
            "departmentName", execution.getVariable("departmentName")
        );

        String url = financeServiceUrl + "/api/workflow/capex/allocate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            execution.setVariable("budgetAllocationId", response.getBody().get("allocationId"));
        }
    }
}
```

### 2. Update BPMN File

**Before (BROKEN):**
```xml
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:expression>${financeServiceUrl}/api/workflow/capex/create-request</flowable:expression>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <!-- THIS IS BROKEN -->
      <flowable:expression>${'{"title":"' + title + '","description":"' + description + '"}'}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>

<!-- Additional script task needed to parse response -->
<scriptTask id="extractCapExId" name="Extract CapEx ID" scriptFormat="groovy">
  <script><![CDATA[
    def response = execution.getVariable('createRequestResponse')
    execution.setVariable('capexId', response.capexId)
  ]]></script>
</scriptTask>
```

**After (FIXED):**
```xml
<!-- Service Task: Create CapEx Request -->
<serviceTask id="createCapExRequest" name="Create CapEx Request"
             flowable:delegateExpression="${capexServiceDelegate}">
  <documentation>Creates CapEx request record in Finance Service via REST API</documentation>
  <extensionElements>
    <!-- Simply specify which operation to perform -->
    <flowable:executionListener event="start"
      expression="${execution.setVariable('operation', 'CREATE_REQUEST')}"/>
  </extensionElements>
</serviceTask>

<!-- No script task needed! Delegate handles everything -->
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

<!-- Service Task: Update Status to Approved -->
<serviceTask id="updateApproved" name="Update Status: Approved"
             flowable:delegateExpression="${capexServiceDelegate}">
  <documentation>Updates CapEx request status to APPROVED in Finance Service</documentation>
  <extensionElements>
    <flowable:executionListener event="start"
      expression="${execution.setVariable('operation', 'UPDATE_STATUS')}"/>
    <flowable:executionListener event="start"
      expression="${execution.setVariable('newStatus', 'APPROVED')}"/>
    <flowable:executionListener event="start"
      expression="${execution.setVariable('statusComments', execution.getVariable('approvalComments'))}"/>
  </extensionElements>
</serviceTask>
```

### 3. Verify RestTemplate Bean Configuration

**Location**: `/services/engine/src/main/java/com/werkflow/engine/config/RestTemplateConfig.java`

```java
package com.werkflow.engine.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
}
```

---

## Testing Your Delegate

### Unit Test Template

```java
package com.werkflow.engine.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        delegate = new CapExServiceDelegate(restTemplate);
    }

    @Test
    void testCreateRequest_Success() {
        // Setup mock execution variables
        when(execution.getVariable("operation")).thenReturn("CREATE_REQUEST");
        when(execution.getVariable("title")).thenReturn("Test CapEx");
        when(execution.getVariable("requestAmount")).thenReturn(50000.0);

        // Mock REST response
        Map<String, Object> mockResponse = Map.of(
            "capexId", 123L,
            "requestNumber", "CAPEX-2025-001"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(responseEntity);

        // Execute
        delegate.execute(execution);

        // Verify results stored in execution
        verify(execution).setVariable("capexId", 123L);
        verify(execution).setVariable("requestNumber", "CAPEX-2025-001");
    }
}
```

---

## Common Patterns

### Pattern 1: Simple REST Call (No Response Needed)

```java
private void sendNotification(DelegateExecution execution) {
    Map<String, Object> notification = Map.of(
        "recipient", execution.getVariable("userEmail"),
        "subject", "Approval Required",
        "template", "approval-request"
    );

    String url = notificationServiceUrl + "/api/notifications/send";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(notification, headers);

    restTemplate.postForEntity(url, request, Void.class);
    log.info("Notification sent to: {}", execution.getVariable("userEmail"));
}
```

### Pattern 2: Error Handling with Fallback

```java
private void checkBudget(DelegateExecution execution) {
    try {
        // Attempt REST call
        String url = financeServiceUrl + "/api/budget/check";
        // ... REST call code ...

        execution.setVariable("budgetAvailable", true);
    } catch (Exception e) {
        log.warn("Budget check failed, using fallback", e);
        execution.setVariable("budgetAvailable", false);
        execution.setVariable("budgetCheckError", e.getMessage());
    }
}
```

### Pattern 3: Conditional Logic Based on Variable

```java
@Override
public void execute(DelegateExecution execution) {
    Double amount = (Double) execution.getVariable("requestAmount");

    if (amount > 100000) {
        // High-value request - additional checks
        performComplianceCheck(execution);
    }

    createRequest(execution);
}
```

### Pattern 4: Response Mapping with Custom DTO

```java
private void createRequest(DelegateExecution execution) {
    // Build request DTO
    CapExRequest request = CapExRequest.builder()
        .title((String) execution.getVariable("title"))
        .amount((Double) execution.getVariable("requestAmount"))
        .department((String) execution.getVariable("departmentName"))
        .build();

    String url = financeServiceUrl + "/api/capex/requests";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CapExRequest> httpRequest = new HttpEntity<>(request, headers);

    ResponseEntity<CapExResponse> response = restTemplate.postForEntity(
        url, httpRequest, CapExResponse.class);

    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        CapExResponse capexResponse = response.getBody();
        execution.setVariable("capexId", capexResponse.getId());
        execution.setVariable("requestNumber", capexResponse.getRequestNumber());
    }
}
```

---

## Checklist for Each Delegate

- [ ] Component annotation with unique bean name
- [ ] Constructor injection of RestTemplate
- [ ] @Value injection for service URLs
- [ ] Error handling with try-catch
- [ ] Logging for debugging
- [ ] Store results in execution variables
- [ ] Unit tests with mocked RestTemplate
- [ ] Integration test with actual service

---

## Troubleshooting

### Issue: "No bean named 'capexServiceDelegate' found"

**Solution**: Ensure class has `@Component("capexServiceDelegate")` annotation and is in scanned package.

### Issue: "Cannot resolve variable 'operation'"

**Solution**: Add execution listener to set operation variable before delegate executes:
```xml
<flowable:executionListener event="start"
  expression="${execution.setVariable('operation', 'CREATE_REQUEST')}"/>
```

### Issue: RestTemplate connection timeout

**Solution**: Configure timeouts in RestTemplateConfig:
```java
.setConnectTimeout(Duration.ofSeconds(10))
.setReadTimeout(Duration.ofSeconds(30))
```

### Issue: Variables are null in delegate

**Solution**: Check variable names match exactly between BPMN start event form and delegate code. Use `execution.getVariables()` to debug:
```java
log.debug("Available variables: {}", execution.getVariables().keySet());
```

---

## Migration Checklist

For each BPMN file with REST calls:

1. [ ] Identify all `<serviceTask>` elements with REST logic
2. [ ] Create delegate class or add operation to existing delegate
3. [ ] Replace `flowable:delegateExpression="${restServiceDelegate}"` with specific delegate
4. [ ] Remove `<flowable:field name="body">` expressions
5. [ ] Add operation type via execution listener
6. [ ] Remove script tasks for response parsing
7. [ ] Test delegate with unit tests
8. [ ] Test workflow end-to-end
9. [ ] Commit changes with descriptive message
10. [ ] Deploy and verify in dev environment

---

## Need Help?

- **Full architectural guide**: See `/docs/BPMN-JSON-Expression-Architecture-Solution.md`
- **Flowable docs**: https://www.flowable.com/open-source/docs/bpmn/ch07b-BPMN-Constructs
- **Example implementation**: `TaskAssignmentDelegate.java` (existing working delegate)

---

**Last Updated**: 2025-12-17
