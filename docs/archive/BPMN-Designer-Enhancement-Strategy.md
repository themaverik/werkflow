# BPMN Designer Enhancement Strategy

## Executive Summary

This document outlines a practical, incremental approach to enhance the BPMN visual designer with:
- Prepopulated process templates and metadata
- Approver/role mappings with clear BPMN constructs
- Gateway and decision type templates
- Service integration patterns

**Key Constraint**: Keep it SIMPLE - minimize backend changes, work with existing Camunda/Flowable modeler, make it intuitive for non-technical users.

---

## 1. Data Model

### 1.1 Process Template Metadata

**Entity**: `ProcessTemplate` (stored in engine service database)

```json
{
  "id": "capex-approval-v1",
  "name": "Capital Expenditure Approval",
  "description": "Multi-tier approval workflow for CapEx requests based on amount thresholds",
  "service": "FINANCE",
  "category": "APPROVAL",
  "version": 1,
  "bpmnKey": "capexApproval",
  "tags": ["finance", "approval", "capex"],
  "variables": [
    {
      "name": "requestAmount",
      "type": "double",
      "required": true,
      "description": "Total capital expenditure amount"
    },
    {
      "name": "departmentId",
      "type": "long",
      "required": true,
      "description": "Requesting department ID"
    },
    {
      "name": "projectDescription",
      "type": "string",
      "required": true,
      "description": "Description of the capital project"
    }
  ],
  "approvalLevels": ["DEPARTMENT_HEAD", "FINANCE_MANAGER", "FINANCE_VP", "CFO", "CEO"],
  "integrations": ["FINANCE_SERVICE", "ADMIN_SERVICE"],
  "active": true,
  "createdAt": "2025-11-01T00:00:00Z"
}
```

### 1.2 Approver Role Mapping

**Entity**: `ApprovalRole` (stored in admin service, exposed via engine)

```json
{
  "id": 1,
  "roleName": "DEPARTMENT_HEAD",
  "displayName": "Department Head",
  "description": "Head of department with approval authority up to $50K",
  "service": "ADMIN",
  "candidateGroup": "DEPARTMENT_HEAD_{departmentId}",
  "emailPattern": "dept-head-{departmentId}@werkflow.local",
  "approvalThreshold": {
    "minAmount": 0,
    "maxAmount": 50000,
    "currency": "USD"
  },
  "taskListenerClass": "com.werkflow.engine.listener.ApprovalTaskListener",
  "executionListenerClass": "com.werkflow.engine.listener.ApprovalExecutionListener",
  "notificationTemplate": "APPROVAL_REQUEST",
  "active": true
}
```

### 1.3 Gateway Templates

**Entity**: `GatewayTemplate` (stored in engine metadata)

```json
{
  "id": "budget-check-gateway",
  "name": "Budget Check Gateway",
  "type": "EXCLUSIVE",
  "description": "Routes based on budget availability",
  "category": "DECISION",
  "conditions": [
    {
      "name": "Budget Available",
      "expression": "${budgetCheckResponse.available == true}",
      "description": "Budget is available for the request",
      "sequenceFlowName": "budgetAvailable"
    },
    {
      "name": "Budget Not Available",
      "expression": "${budgetCheckResponse.available == false}",
      "description": "Insufficient budget",
      "sequenceFlowName": "budgetNotAvailable",
      "default": true
    }
  ],
  "variables": [
    {
      "name": "budgetCheckResponse",
      "type": "object",
      "description": "Response from budget check service task"
    }
  ],
  "icon": "decision-diamond",
  "usageExample": "Use after budget validation service task"
}
```

**Common Gateway Templates**:
1. **Amount-Based Routing**: Routes based on threshold amounts
2. **Approval Status Gateway**: Routes based on approved/rejected status
3. **Vendor Selection Gateway**: Routes to RFQ process or direct PO
4. **Parallel Approvals Gateway**: Forks for parallel approval paths
5. **Merge Gateway**: Joins parallel paths

### 1.4 Service Integration Mapping

**Entity**: `ServiceIntegration` (stored in engine metadata)

```json
{
  "id": "finance-budget-check",
  "serviceName": "FINANCE",
  "integrationName": "Budget Availability Check",
  "description": "Validates budget availability for a department",
  "taskType": "SERVICE_TASK",
  "delegateExpression": "${restServiceDelegate}",
  "endpoint": {
    "path": "/budget/check",
    "method": "POST",
    "baseUrlVariable": "financeServiceUrl"
  },
  "inputVariables": [
    { "name": "departmentId", "type": "long", "required": true },
    { "name": "amount", "type": "double", "required": true },
    { "name": "costCenter", "type": "string", "required": false },
    { "name": "fiscalYear", "type": "integer", "required": false }
  ],
  "outputVariable": "budgetCheckResponse",
  "timeout": 15,
  "retryConfig": {
    "maxAttempts": 3,
    "backoffMs": 1000
  },
  "tags": ["finance", "validation", "budget"]
}
```

---

## 2. Clear Mapping Table

### 2.1 Approval Concepts → BPMN Constructs

| Approval Concept | BPMN Element | Configuration | Example |
|-----------------|--------------|---------------|---------|
| **Approval Role** | `userTask` | `flowable:candidateGroups="ROLE_NAME"` | `candidateGroups="DEPARTMENT_HEAD_${departmentId}"` |
| **Approver Email** | `taskListener` (assignment) | `com.werkflow.engine.listener.ApprovalTaskListener` | Sends notification on task creation |
| **Approval Decision** | `exclusiveGateway` | Sequence flow conditions | `${approvalStatus == 'APPROVED'}` |
| **Parallel Approvals** | `parallelGateway` | Fork + Join pattern | CFO + CEO approve simultaneously |
| **Escalation** | `boundaryEvent` (timer) | `timeDuration="PT24H"` | Escalate if not completed in 24h |
| **Auto-Approval** | `serviceTask` | `delegateExpression="${autoApprovalDelegate}"` | Auto-approve if < threshold |

### 2.2 Process Flow → BPMN Tasks

| Process Step | BPMN Task Type | When to Use | Configuration |
|--------------|----------------|-------------|---------------|
| **Human Review** | `userTask` | Requires manual approval/input | `candidateGroups`, `formKey` |
| **System Validation** | `serviceTask` | API call to validate data | `delegateExpression="${restServiceDelegate}"` |
| **Send Notification** | `serviceTask` | Email/SMS notification | `delegateExpression="${notificationDelegate}"` |
| **Database Update** | `serviceTask` | Update external system | `delegateExpression="${dataUpdateDelegate}"` |
| **Decision Logic** | `exclusiveGateway` | Single path selection | Conditional sequence flows |
| **Parallel Work** | `parallelGateway` | Multiple paths simultaneously | Fork/Join pattern |
| **Wait for Event** | `intermediateCatchEvent` | Wait for external signal | Timer, Message, Signal events |

### 2.3 Variables → Form Fields → Process Variables

| Layer | Scope | Example | Mapping |
|-------|-------|---------|---------|
| **Form Field** | User input | `<input name="requestAmount">` | Submitted via `TaskService.complete(taskId, variables)` |
| **Task Variable** | Single task | `taskService.setVariableLocal(taskId, "taskNote", note)` | Not shared with process |
| **Process Variable** | Entire process | `execution.setVariable("totalAmount", amount)` | Available to all tasks/gateways |
| **Global Variable** | All processes | `runtimeService.getVariable(executionId, "companyName")` | Via execution context |

**Variable Naming Convention**:
- **Input Variables**: `request*` (e.g., `requestAmount`, `requestDate`)
- **Response Variables**: `*Response` (e.g., `budgetCheckResponse`, `approvalResponse`)
- **Status Variables**: `*Status` (e.g., `approvalStatus`, `budgetStatus`)
- **ID Variables**: `*Id` (e.g., `requesterId`, `departmentId`)

### 2.4 Approval Roles → candidateGroups + Listeners

| Role | candidateGroup | Task Listener | Execution Listener | Notification Template |
|------|----------------|---------------|-------------------|----------------------|
| `DEPARTMENT_HEAD` | `DEPARTMENT_HEAD_${departmentId}` | `ApprovalTaskListener` | `ApprovalExecutionListener` | `APPROVAL_REQUEST` |
| `FINANCE_MANAGER` | `FINANCE_MANAGER` | `ApprovalTaskListener` | `ApprovalExecutionListener` | `APPROVAL_REQUEST` |
| `FINANCE_VP` | `FINANCE_VP` | `ApprovalTaskListener` | `ApprovalExecutionListener` | `APPROVAL_REQUEST` |
| `CFO` | `CFO` | `ApprovalTaskListener` | `ApprovalExecutionListener` | `APPROVAL_REQUEST` |
| `CEO` | `CEO` | `ApprovalTaskListener` | `ApprovalExecutionListener` | `APPROVAL_REQUEST` |
| `PROCUREMENT_SPECIALIST` | `PROCUREMENT_SPECIALIST` | `TaskAssignmentListener` | N/A | `TASK_ASSIGNED` |
| `HR_MANAGER` | `HR_MANAGER_${departmentId}` | `TaskAssignmentListener` | N/A | `TASK_ASSIGNED` |

**Listener Behavior**:
- **ApprovalTaskListener** (create event): Sends approval request email to candidate group
- **ApprovalExecutionListener** (end event): Logs approval decision to audit trail
- **TaskAssignmentListener** (assignment event): Sends notification to assigned user

---

## 3. Backend API Design (Minimal REST)

### 3.1 Process Template Metadata API

```java
@RestController
@RequestMapping("/api/process-metadata")
public class ProcessMetadataController {

    /**
     * GET /api/process-metadata/templates
     * Returns available process templates to extend/clone
     */
    @GetMapping("/templates")
    public ResponseEntity<List<ProcessTemplateResponse>> getProcessTemplates(
        @RequestParam(required = false) String service,
        @RequestParam(required = false) String category
    ) {
        // Returns: HR processes, Finance processes, Procurement, Inventory
        // Each template includes: id, name, description, service, variables, approval levels
    }

    /**
     * GET /api/process-metadata/templates/{templateId}
     * Returns detailed template with BPMN XML
     */
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<ProcessTemplateDetailResponse> getProcessTemplateDetail(
        @PathVariable String templateId
    ) {
        // Returns: Full template + BPMN XML for cloning
    }
}
```

**Response Structure**:
```json
{
  "templates": [
    {
      "id": "capex-approval-v1",
      "name": "Capital Expenditure Approval",
      "description": "Multi-tier approval workflow for CapEx requests",
      "service": "FINANCE",
      "category": "APPROVAL",
      "version": 1,
      "bpmnKey": "capexApproval",
      "tags": ["finance", "approval", "capex"],
      "variableCount": 5,
      "approvalLevelCount": 5,
      "integrationCount": 2,
      "thumbnailUrl": "/api/process-metadata/templates/capex-approval-v1/thumbnail",
      "canClone": true
    }
  ]
}
```

### 3.2 Approver Role Metadata API

```java
@RestController
@RequestMapping("/api/process-metadata")
public class ProcessMetadataController {

    /**
     * GET /api/process-metadata/approvers
     * Returns available approver roles with BPMN configuration
     */
    @GetMapping("/approvers")
    public ResponseEntity<List<ApproverRoleResponse>> getApproverRoles(
        @RequestParam(required = false) String service
    ) {
        // Returns: All approval roles with candidateGroup patterns, thresholds
    }
}
```

**Response Structure**:
```json
{
  "approvers": [
    {
      "id": 1,
      "roleName": "DEPARTMENT_HEAD",
      "displayName": "Department Head",
      "description": "Department head with approval authority up to $50K",
      "service": "ADMIN",
      "candidateGroup": "DEPARTMENT_HEAD_{departmentId}",
      "approvalThreshold": {
        "minAmount": 0,
        "maxAmount": 50000,
        "currency": "USD"
      },
      "bpmnConfig": {
        "taskListenerClass": "com.werkflow.engine.listener.ApprovalTaskListener",
        "executionListenerClass": "com.werkflow.engine.listener.ApprovalExecutionListener",
        "notificationTemplate": "APPROVAL_REQUEST"
      },
      "usageExample": "flowable:candidateGroups=\"DEPARTMENT_HEAD_${departmentId}\""
    }
  ]
}
```

### 3.3 Gateway Templates API

```java
@RestController
@RequestMapping("/api/process-metadata")
public class ProcessMetadataController {

    /**
     * GET /api/process-metadata/gateways
     * Returns pre-configured gateway templates
     */
    @GetMapping("/gateways")
    public ResponseEntity<List<GatewayTemplateResponse>> getGatewayTemplates(
        @RequestParam(required = false) String category
    ) {
        // Returns: Budget check, approval status, amount threshold, parallel gateways
    }
}
```

**Response Structure**:
```json
{
  "gateways": [
    {
      "id": "budget-check-gateway",
      "name": "Budget Check Gateway",
      "type": "EXCLUSIVE",
      "category": "DECISION",
      "description": "Routes based on budget availability",
      "conditions": [
        {
          "name": "Budget Available",
          "expression": "${budgetCheckResponse.available == true}",
          "description": "Budget is available",
          "sequenceFlowName": "budgetAvailable"
        },
        {
          "name": "Budget Not Available",
          "expression": "${budgetCheckResponse.available == false}",
          "description": "Insufficient budget",
          "sequenceFlowName": "budgetNotAvailable",
          "default": true
        }
      ],
      "requiredVariables": ["budgetCheckResponse"],
      "usageExample": "Use after budget validation service task"
    }
  ]
}
```

### 3.4 Service Integration Catalog API

```java
@RestController
@RequestMapping("/api/process-metadata")
public class ProcessMetadataController {

    /**
     * GET /api/process-metadata/services
     * Returns available service integrations
     */
    @GetMapping("/services")
    public ResponseEntity<List<ServiceIntegrationResponse>> getServiceIntegrations(
        @RequestParam(required = false) String service
    ) {
        // Returns: Finance budget check, HR employee lookup, Procurement vendor search, etc.
    }
}
```

**Response Structure**:
```json
{
  "integrations": [
    {
      "id": "finance-budget-check",
      "serviceName": "FINANCE",
      "integrationName": "Budget Availability Check",
      "description": "Validates budget availability for a department",
      "taskType": "SERVICE_TASK",
      "bpmnConfig": {
        "delegateExpression": "${restServiceDelegate}",
        "fields": [
          {
            "name": "url",
            "expression": "${financeServiceUrl}/budget/check"
          },
          {
            "name": "method",
            "value": "POST"
          },
          {
            "name": "responseVariable",
            "value": "budgetCheckResponse"
          },
          {
            "name": "timeoutSeconds",
            "value": "15"
          }
        ]
      },
      "inputVariables": [
        { "name": "departmentId", "type": "long", "required": true },
        { "name": "amount", "type": "double", "required": true }
      ],
      "outputVariable": "budgetCheckResponse",
      "tags": ["finance", "validation", "budget"]
    }
  ]
}
```

### 3.5 BPMN Validation API

```java
@RestController
@RequestMapping("/api/process-definitions")
public class ProcessDefinitionController {

    /**
     * POST /api/process-definitions/validate
     * Validates BPMN XML before deployment
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateBpmn(
        @RequestParam("file") MultipartFile file
    ) {
        // Returns: validation errors, warnings, suggestions
        // Checks: BPMN structure, candidateGroups exist, variables defined, etc.
    }
}
```

**Response Structure**:
```json
{
  "valid": false,
  "errors": [
    {
      "severity": "ERROR",
      "elementId": "managerApproval",
      "elementType": "userTask",
      "message": "candidateGroup 'INVALID_ROLE' does not exist in system",
      "suggestion": "Use one of: DEPARTMENT_HEAD, FINANCE_MANAGER, CFO"
    }
  ],
  "warnings": [
    {
      "severity": "WARNING",
      "elementId": "budgetCheck",
      "elementType": "serviceTask",
      "message": "Variable 'departmentId' is not defined in process",
      "suggestion": "Add departmentId to process start form or start event"
    }
  ],
  "suggestions": [
    {
      "severity": "INFO",
      "elementId": "approvalDecision",
      "elementType": "exclusiveGateway",
      "message": "Consider using template 'Approval Status Gateway'",
      "templateId": "approval-status-gateway"
    }
  ]
}
```

---

## 4. Frontend Enhancement (bpmn-js Modeler)

### 4.1 Custom Palette Provider

Add custom palette items for quick task creation:

```typescript
// frontends/admin-portal/lib/bpmn/WerkflowPaletteProvider.ts
export class WerkflowPaletteProvider {
  constructor(
    private palette: any,
    private create: any,
    private elementFactory: any,
    private spaceTool: any,
    private lassoTool: any,
    private handTool: any,
    private globalConnect: any,
    private translate: any
  ) {
    palette.registerProvider(this);
  }

  getPaletteEntries(element: any) {
    return {
      'werkflow-separator': {
        group: 'werkflow',
        separator: true
      },
      'create.approval-task': {
        group: 'werkflow',
        className: 'bpmn-icon-user-task approval',
        title: this.translate('Create Approval Task'),
        action: {
          dragstart: this.createApprovalTask(),
          click: this.createApprovalTask()
        }
      },
      'create.service-integration': {
        group: 'werkflow',
        className: 'bpmn-icon-service-task integration',
        title: this.translate('Create Service Integration'),
        action: {
          dragstart: this.createServiceIntegration(),
          click: this.createServiceIntegration()
        }
      },
      'create.budget-gateway': {
        group: 'werkflow',
        className: 'bpmn-icon-gateway-xor budget',
        title: this.translate('Create Budget Check Gateway'),
        action: {
          dragstart: this.createBudgetGateway(),
          click: this.createBudgetGateway()
        }
      },
      'create.notification-task': {
        group: 'werkflow',
        className: 'bpmn-icon-service-task notification',
        title: this.translate('Create Notification Task'),
        action: {
          dragstart: this.createNotificationTask(),
          click: this.createNotificationTask()
        }
      }
    };
  }

  createApprovalTask() {
    return {
      type: 'bpmn:UserTask',
      businessObject: {
        name: 'Approval Task',
        candidateGroups: '', // Will be set via properties panel
        formKey: ''
      }
    };
  }
}
```

### 4.2 Custom Context Pad Provider

Add quick actions to existing elements:

```typescript
// frontends/admin-portal/lib/bpmn/WerkflowContextPadProvider.ts
export class WerkflowContextPadProvider {
  getContextPadEntries(element: any) {
    const actions: any = {};

    if (is(element, 'bpmn:UserTask')) {
      actions['configure-approver'] = {
        group: 'werkflow',
        className: 'bpmn-icon-user',
        title: this.translate('Configure Approver'),
        action: {
          click: (event: any, element: any) => {
            // Open approver configuration dialog
            this.openApproverDialog(element);
          }
        }
      };
    }

    if (is(element, 'bpmn:ServiceTask')) {
      actions['configure-service'] = {
        group: 'werkflow',
        className: 'bpmn-icon-service',
        title: this.translate('Configure Service Integration'),
        action: {
          click: (event: any, element: any) => {
            // Open service integration dialog
            this.openServiceIntegrationDialog(element);
          }
        }
      };
    }

    if (is(element, 'bpmn:ExclusiveGateway')) {
      actions['configure-gateway'] = {
        group: 'werkflow',
        className: 'bpmn-icon-gateway-xor',
        title: this.translate('Apply Gateway Template'),
        action: {
          click: (event: any, element: any) => {
            // Open gateway template dialog
            this.openGatewayTemplateDialog(element);
          }
        }
      };
    }

    return actions;
  }
}
```

### 4.3 Custom Properties Panel Provider

Enhance properties panel with Werkflow-specific properties:

```typescript
// frontends/admin-portal/lib/bpmn/WerkflowPropertiesProvider.ts
export class WerkflowPropertiesProvider {
  getTabs(element: any) {
    return [
      {
        id: 'general',
        label: 'General',
        groups: this.createGeneralGroups(element)
      },
      {
        id: 'werkflow',
        label: 'Werkflow',
        groups: this.createWerkflowGroups(element)
      },
      {
        id: 'variables',
        label: 'Variables',
        groups: this.createVariablesGroups(element)
      }
    ];
  }

  createWerkflowGroups(element: any) {
    if (is(element, 'bpmn:UserTask')) {
      return [
        {
          id: 'approver-config',
          label: 'Approver Configuration',
          entries: [
            {
              id: 'approver-role',
              label: 'Approver Role',
              modelProperty: 'candidateGroups',
              widget: 'select',
              selectOptions: this.getApproverRoles() // Fetched from API
            },
            {
              id: 'approval-threshold',
              label: 'Approval Threshold',
              widget: 'text',
              description: 'Amount threshold for this approval level'
            },
            {
              id: 'notification-template',
              label: 'Notification Template',
              modelProperty: 'extensionElements.notificationTemplate',
              widget: 'select',
              selectOptions: ['APPROVAL_REQUEST', 'TASK_ASSIGNED', 'TASK_REMINDER']
            }
          ]
        }
      ];
    }

    if (is(element, 'bpmn:ServiceTask')) {
      return [
        {
          id: 'service-integration',
          label: 'Service Integration',
          entries: [
            {
              id: 'integration-template',
              label: 'Integration Template',
              widget: 'select',
              selectOptions: this.getServiceIntegrations(), // Fetched from API
              onChange: (value: string) => {
                // Auto-populate service task fields
                this.applyServiceIntegrationTemplate(element, value);
              }
            },
            {
              id: 'service-url',
              label: 'Service URL',
              modelProperty: 'extensionElements.field[url]',
              widget: 'text'
            },
            {
              id: 'response-variable',
              label: 'Response Variable',
              modelProperty: 'extensionElements.field[responseVariable]',
              widget: 'text'
            }
          ]
        }
      ];
    }

    if (is(element, 'bpmn:ExclusiveGateway')) {
      return [
        {
          id: 'gateway-template',
          label: 'Gateway Template',
          entries: [
            {
              id: 'template-selector',
              label: 'Apply Template',
              widget: 'select',
              selectOptions: this.getGatewayTemplates(), // Fetched from API
              onChange: (value: string) => {
                // Auto-populate gateway conditions
                this.applyGatewayTemplate(element, value);
              }
            }
          ]
        }
      ];
    }

    return [];
  }
}
```

### 4.4 Variable Helper Component

Show available variables at each point in the process:

```tsx
// frontends/admin-portal/components/bpmn/VariableHelper.tsx
'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface Variable {
  name: string
  type: string
  scope: 'process' | 'task' | 'global'
  description: string
}

export function VariableHelper({ elementId, modeler }: { elementId: string, modeler: any }) {
  const [variables, setVariables] = useState<Variable[]>([])

  useEffect(() => {
    // Analyze BPMN and extract available variables at this point
    const availableVariables = analyzeAvailableVariables(modeler, elementId)
    setVariables(availableVariables)
  }, [elementId, modeler])

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Available Variables</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          {variables.map((variable) => (
            <div key={variable.name} className="flex items-center justify-between text-xs">
              <code className="bg-muted px-2 py-1 rounded">{variable.name}</code>
              <Badge variant="outline">{variable.type}</Badge>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

function analyzeAvailableVariables(modeler: any, elementId: string): Variable[] {
  // Walk backwards from current element to find all variables set by previous tasks
  // Return list of available variables
  return []
}
```

### 4.5 Template Gallery Component

Allow users to clone existing processes:

```tsx
// frontends/admin-portal/components/bpmn/TemplateGallery.tsx
'use client'

import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

export function TemplateGallery({ onSelectTemplate }: { onSelectTemplate: (templateId: string) => void }) {
  const { data: templates } = useQuery({
    queryKey: ['processTemplates'],
    queryFn: () => fetch('/api/process-metadata/templates').then(r => r.json())
  })

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {templates?.templates.map((template: any) => (
        <Card key={template.id}>
          <CardHeader>
            <CardTitle className="text-lg">{template.name}</CardTitle>
            <CardDescription>{template.description}</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex gap-2">
                <Badge variant="secondary">{template.service}</Badge>
                <Badge variant="outline">{template.category}</Badge>
              </div>
              <div className="text-xs text-muted-foreground">
                {template.variableCount} variables • {template.approvalLevelCount} approval levels
              </div>
              <Button
                size="sm"
                className="w-full"
                onClick={() => onSelectTemplate(template.id)}
              >
                Use Template
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
```

---

## 5. Implementation Roadmap

### Phase 1: Expose Metadata APIs (2-3 days)

**Backend Work**:
1. Create `ProcessTemplate`, `ApprovalRole`, `GatewayTemplate`, `ServiceIntegration` entities
2. Seed database with existing process metadata from BPMN files
3. Implement `ProcessMetadataController` with 4 endpoints:
   - `GET /api/process-metadata/templates`
   - `GET /api/process-metadata/approvers`
   - `GET /api/process-metadata/gateways`
   - `GET /api/process-metadata/services`
4. Implement `POST /api/process-definitions/validate` endpoint

**Database Changes**:
```sql
-- In engine service schema
CREATE TABLE process_templates (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    service VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    bpmn_key VARCHAR(100) NOT NULL,
    bpmn_xml TEXT NOT NULL,
    tags TEXT[],
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gateway_templates (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    conditions JSONB NOT NULL,
    variables JSONB,
    active BOOLEAN DEFAULT true
);

CREATE TABLE service_integrations (
    id VARCHAR(100) PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL,
    integration_name VARCHAR(255) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) NOT NULL,
    delegate_expression VARCHAR(255),
    endpoint JSONB NOT NULL,
    input_variables JSONB,
    output_variable VARCHAR(100),
    timeout INTEGER DEFAULT 15,
    retry_config JSONB,
    tags TEXT[],
    active BOOLEAN DEFAULT true
);
```

**Testing**:
- Unit tests for metadata controllers
- Integration tests for API endpoints
- Verify JSON response structure

### Phase 2: Create Templates and Helpers (2-3 days)

**Data Population**:
1. Extract metadata from existing BPMN files (CapEx, PR-to-PO, Leave Approval, etc.)
2. Define 10-15 common gateway templates
3. Define 20-30 service integrations across all services
4. Define 7-10 approval roles with thresholds

**Seed Scripts**:
```java
@Component
public class ProcessMetadataSeeder {

    @PostConstruct
    public void seedMetadata() {
        seedProcessTemplates();
        seedGatewayTemplates();
        seedServiceIntegrations();
        seedApprovalRoles();
    }

    private void seedProcessTemplates() {
        // Extract from existing BPMN files
        processTemplateRepository.saveAll(Arrays.asList(
            createCapExTemplate(),
            createPrToPoTemplate(),
            createLeaveApprovalTemplate(),
            // ... more templates
        ));
    }
}
```

**Frontend Utilities**:
1. Create `BpmnTemplateService` to fetch metadata
2. Create helper functions to apply templates to BPMN elements
3. Create validation utilities using metadata

### Phase 3: Integrate with Modeler UI (2-3 days)

**bpmn-js Extensions**:
1. Implement `WerkflowPaletteProvider`
2. Implement `WerkflowContextPadProvider`
3. Implement `WerkflowPropertiesProvider`
4. Register custom modules with bpmn-js modeler

**UI Components**:
1. Template Gallery dialog
2. Approver Configuration dialog
3. Service Integration dialog
4. Gateway Template dialog
5. Variable Helper sidebar
6. Validation Results panel

**Integration Points**:
```tsx
// frontends/admin-portal/components/bpmn/BpmnModeler.tsx
import Modeler from 'bpmn-js/lib/Modeler'
import WerkflowModule from '@/lib/bpmn/werkflow-module'

const modeler = new Modeler({
  container: '#canvas',
  additionalModules: [
    WerkflowModule // Custom palette, context pad, properties
  ],
  propertiesPanel: {
    parent: '#properties'
  }
})
```

**Testing**:
- E2E tests for modeler interactions
- Test template application
- Test validation workflow

### Phase 4: Documentation and Training (1 day)

**Documentation**:
1. Update BPMN Designer user guide
2. Create video tutorials for common workflows
3. Document all gateway templates with examples
4. Document all service integrations with examples

**Developer Documentation**:
1. API documentation for metadata endpoints
2. Guide for adding new templates
3. Guide for adding new service integrations

---

## 6. Example Usage Scenarios

### Scenario 1: Creating a New Approval Process

**User Journey**:
1. Click "Create New Process"
2. Select "Approval Workflow" template
3. Choose "Finance - CapEx Approval" from template gallery
4. Modeler opens with pre-populated CapEx approval process
5. Customize approval levels by adding/removing user tasks
6. Click on user task → "Configure Approver" → Select "CFO" from dropdown
7. System auto-populates `candidateGroups="CFO"` and adds task listener
8. Add gateway → Click "Apply Template" → Select "Amount Threshold Gateway"
9. System adds conditions: `${requestAmount < 100000}`, `${requestAmount >= 100000}`
10. Click "Validate" → System checks all candidateGroups exist, variables defined
11. Click "Deploy" → Process is deployed to Flowable engine

### Scenario 2: Adding Service Integration

**User Journey**:
1. Open existing process
2. Add new service task from palette
3. Click on service task → "Configure Service Integration"
4. Select "Finance - Budget Availability Check" from dropdown
5. System auto-populates:
   - `delegateExpression="${restServiceDelegate}"`
   - URL field: `${financeServiceUrl}/budget/check`
   - Method: `POST`
   - Response variable: `budgetCheckResponse`
6. User adds input variables: `departmentId`, `amount`
7. System validates that input variables exist in process
8. User saves task

### Scenario 3: Cloning and Customizing Template

**User Journey**:
1. Click "Create New Process"
2. Click "Use Template"
3. Select "Procurement - PR to PO"
4. System clones BPMN XML and opens in modeler
5. User renames process to "Custom PR Workflow"
6. User removes RFQ sub-process
7. User adds new approval level "Procurement Manager"
8. System validates modified process
9. User deploys custom process

---

## 7. Benefits and Impact

### For Non-Technical Users:
- **Reduced Learning Curve**: Template gallery and dropdowns eliminate need to understand BPMN syntax
- **Consistency**: Templates ensure processes follow organizational standards
- **Validation**: Real-time validation prevents common errors
- **Discoverability**: Context menus show available options at each step

### For Developers:
- **Faster Development**: Service integration templates eliminate boilerplate
- **Reusability**: Gateway templates can be reused across processes
- **Maintainability**: Centralized metadata makes updates easier
- **Documentation**: Self-documenting through metadata descriptions

### For System Administrators:
- **Governance**: Control available templates and integrations
- **Auditability**: Track which templates are used where
- **Standardization**: Enforce approval hierarchies and thresholds
- **Flexibility**: Add new templates without code changes

---

## 8. Future Enhancements (Post-MVP)

### AI-Powered Suggestions
- Suggest next task based on current context
- Auto-complete gateway conditions
- Detect missing error handling paths

### Visual Process Validation
- Highlight variables that are read but never set
- Show dead code paths that can never execute
- Visualize approval chains with actual role names

### Template Marketplace
- Share templates across organizations
- Import templates from community
- Rate and review templates

### Advanced Analytics
- Track which templates are most used
- Identify bottlenecks in approval chains
- Suggest optimizations based on historical data

---

## Conclusion

This strategy provides a practical, incremental approach to enhancing the BPMN designer without major infrastructure changes. By exposing process metadata through simple REST APIs and integrating with the existing bpmn-js modeler, we can significantly improve the developer and user experience while maintaining flexibility and control.

**Key Success Factors**:
1. Start with metadata extraction from existing processes
2. Build simple, focused APIs (no over-engineering)
3. Integrate progressively with modeler UI
4. Validate with real users at each phase
5. Document thoroughly for adoption

This approach balances immediate value delivery with long-term maintainability and extensibility.
