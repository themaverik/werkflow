# BPMN Visual Delegate Editor - Technical Architecture

**Date**: 2025-11-23
**Related**: BPMN-Visual-Delegate-Editor-Feasibility-Analysis.md

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         BpmnDesigner.tsx                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    BPMN-js Modeler                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │  │
│  │  │   Canvas     │  │  Modeling    │  │   Moddle     │    │  │
│  │  │   (Visual)   │  │  (Updates)   │  │  (XML Gen)   │    │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ├──────────────────┐
                              ▼                  ▼
┌──────────────────────────────────────┐  ┌─────────────────────┐
│  FlowablePropertiesProvider.ts       │  │  Properties Panel   │
│  ┌────────────────────────────────┐  │  │  (BPMN-js UI)      │
│  │  getGroups(element)            │  │  │                     │
│  │  ├─ UserTask properties        │  │  │  ┌───────────────┐ │
│  │  ├─ ServiceTask properties ◄───┼──┼──┼──│   Rendered    │ │
│  │  │   └─ Pattern-based UI       │  │  │  │   Properties  │ │
│  │  └─ Extension elements         │  │  │  └───────────────┘ │
│  └────────────────────────────────┘  │  └─────────────────────┘
└──────────────────────────────────────┘
                │
                ├─────────────────────────────────────────┐
                ▼                                         ▼
┌───────────────────────────────────┐    ┌──────────────────────────────┐
│ ServiceTaskPropertiesPanel.tsx    │    │  ExtensionElementsEditor.tsx │
│ ┌───────────────────────────────┐ │    │ ┌──────────────────────────┐ │
│ │ Pattern Detection             │ │    │ │ Add/Edit/Delete Fields   │ │
│ │ └─ detectDelegatePattern()    │ │    │ │ ├─ flowable:field name   │ │
│ │                               │ │    │ │ ├─ string/expression     │ │
│ │ Pattern Router                │ │    │ │ └─ XML preview           │ │
│ │ ├─ REST_SERVICE     ──────────┼─┼────┼─┤                          │ │
│ │ ├─ LOCAL_SERVICE    ──────────┼─┼────┼─┤ Moddle API               │ │
│ │ └─ NOTIFICATION     ──────────┼─┼────┼─┤ └─ moddle.create()       │ │
│ └───────────────────────────────┘ │    │ └──────────────────────────┘ │
└───────────────────────────────────┘    └──────────────────────────────┘
                │                                          │
                ├──────────────┬───────────────┬──────────┘
                ▼              ▼               ▼
┌─────────────────────┐ ┌──────────────┐ ┌─────────────────────┐
│RestServicePatternUI │ │LocalService  │ │NotificationPatternUI│
│                     │ │PatternUI     │ │                     │
│ ┌─────────────────┐ │ │              │ │ ┌─────────────────┐ │
│ │Service Selector │ │ │ ┌──────────┐ │ │ │Type Selector    │ │
│ │  ↓ useServices()│ │ │ │Bean Name │ │ │ │  ├─ EMAIL       │ │
│ │Endpoint Selector│ │ │ │Selector  │ │ │ │  ├─ SMS         │ │
│ │  ↓ endpoints[]  │ │ │ └──────────┘ │ │ │  └─ PUSH        │ │
│ │HTTP Method      │ │ │              │ │ │Template Selector│ │
│ │Request Body     │ │ │              │ │ └─────────────────┘ │
│ │Response Variable│ │ │              │ │                     │
│ └─────────────────┘ │ └──────────────┘ └─────────────────────┘
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────┐
│   Service Registry Integration       │
│  ┌────────────────────────────────┐ │
│  │ useServiceRegistry.ts          │ │
│  │ ├─ useServices()               │ │
│  │ ├─ useServiceEndpoints()       │ │
│  │ └─ useServiceHealth()          │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ services.ts (API Client)       │ │
│  │ ├─ getServices()               │ │
│  │ ├─ Mock Data Fallback ✅       │ │
│  │ └─ Backend API (optional)      │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## Data Flow: Creating a REST Service Task

### Step 1: User Creates Service Task

```
User clicks ServiceTask in BPMN palette
    ↓
BpmnDesigner creates element on canvas
    ↓
Element selected, properties panel opens
    ↓
FlowablePropertiesProvider.getGroups(element) called
```

### Step 2: Pattern Detection

```
ServiceTaskPropertiesPanel mounted
    ↓
detectDelegatePattern(element) executed
    ↓
Returns: { type: 'UNKNOWN', delegateExpression: '', fields: Map() }
    ↓
Show pattern selector (REST/Local/Notification)
```

### Step 3: User Selects REST Pattern

```
User selects "REST Service Delegate"
    ↓
handleDelegateChange('${restServiceDelegate}')
    ↓
modeling.updateProperties(element, {
  'flowable:delegateExpression': '${restServiceDelegate}'
})
    ↓
RestServicePatternUI component rendered
```

### Step 4: Configure REST Service

```
RestServicePatternUI loads
    ↓
useServices() hook fetches service list
    ├─ Backend API call (if available)
    └─ Falls back to mock data
    ↓
User selects service: "finance"
    ↓
useServiceEndpoints('finance') fetches endpoints
    ↓
User selects endpoint: "/budget/check" (POST)
    ↓
Auto-populate extension fields:
  - url: "http://finance-service:8084/api/budget/check"
  - method: "POST"
```

### Step 5: Configure Request Body

```
User enters JSON body:
{
  "departmentId": "${departmentId}",
  "amount": "${totalAmount}"
}
    ↓
JSON validation (JSON.parse())
    ↓
Create extension field:
  - name: "body"
  - value: "#{...}"
  - type: "expression"
```

### Step 6: Save to XML

```
ExtensionElementsEditor.updateExtensionElements() called
    ↓
moddle.create('bpmn:ExtensionElements', {
  values: [
    moddle.create('flowable:Field', { name: 'url', string: '...' }),
    moddle.create('flowable:Field', { name: 'method', string: 'POST' }),
    moddle.create('flowable:Field', { name: 'body', expression: '#{...}' })
  ]
})
    ↓
modeling.updateProperties(element, { extensionElements })
    ↓
BPMN XML updated ✅
```

### Step 7: Deploy

```
User clicks "Deploy to Flowable"
    ↓
modeler.saveXML({ format: true })
    ↓
Generated XML:

<serviceTask id="checkBudget" name="Check Budget"
             flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:string>http://finance-service:8084/api/budget/check</flowable:string>
    </flowable:field>
    <flowable:field name="method">
      <flowable:string>POST</flowable:string>
    </flowable:field>
    <flowable:field name="body">
      <flowable:expression>#{{'departmentId': departmentId, 'amount': totalAmount}}</flowable:expression>
    </flowable:field>
  </extensionElements>
</serviceTask>
    ↓
POST /api/process-definitions/deploy
    ↓
Success ✅
```

---

## Component Hierarchy

```
BpmnDesigner
├── BpmnModeler (bpmn-js)
│   ├── Canvas
│   ├── Modeling
│   └── Moddle
└── Properties Panel
    └── FlowablePropertiesProvider
        ├── UserTask Properties
        │   ├── AssigneeEntry
        │   ├── CandidateUsersEntry
        │   └── FormKeyEntry
        └── ServiceTask Properties ◄── NEW ENHANCEMENT
            ├── ServiceTaskPropertiesPanel
            │   ├── Pattern Detector
            │   ├── Delegate Selector
            │   └── Pattern Router
            │       ├── RestServicePatternUI
            │       │   ├── Service Selector
            │       │   ├── Endpoint Selector
            │       │   ├── HTTP Method Selector
            │       │   ├── Request Body Editor
            │       │   └── Response Variable Input
            │       ├── LocalServicePatternUI
            │       │   └── Bean Name Selector
            │       └── NotificationPatternUI
            │           ├── Notification Type Selector
            │           └── Template Selector
            └── ExtensionElementsEditor
                ├── Field List
                ├── Add Field Form
                ├── Field Editor (name, value, type)
                └── XML Preview
```

---

## State Management

### Component State

```typescript
// ServiceTaskPropertiesPanel state
{
  delegateExpression: string              // '${restServiceDelegate}'
  pattern: DelegatePattern               // 'REST_SERVICE'
  selectedService: string                // 'finance'
  selectedEndpoint: string               // '/budget/check'
  extensionFields: Map<string, Field>    // url, method, body, etc.
}

// ExtensionElementsEditor state
{
  fields: ExtensionField[]               // Array of flowable:field
  newField: ExtensionField               // Form state for new field
  isAdding: boolean                      // Show/hide add form
}

// RestServicePatternUI state
{
  selectedService: Service               // Full service object
  selectedEndpoint: Endpoint             // Full endpoint object
  httpMethod: HTTPMethod                 // GET, POST, etc.
  requestBody: string                    // JSON string
  responseVariable: string               // Variable name
}
```

### Global State (React Query)

```typescript
// Cached service registry data
services: Service[]                     // From useServices()
serviceEndpoints: Map<string, Endpoint[]> // Keyed by service name
serviceHealth: Map<string, HealthStatus>  // Keyed by service name
```

### BPMN Element State (XML)

```xml
<!-- Stored in BPMN businessObject -->
<serviceTask
  id="task123"
  name="Check Budget"
  flowable:delegateExpression="${restServiceDelegate}">
  <extensionElements>
    <flowable:field name="url">
      <flowable:string>...</flowable:string>
    </flowable:field>
    <!-- ... more fields -->
  </extensionElements>
</serviceTask>
```

---

## Pattern Detection Algorithm

```typescript
┌─────────────────────────────────────────────────────────────────┐
│ detectDelegatePattern(element)                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
            Extract delegateExpression from element
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
        delegateExpression exists?    NO → Return 'UNKNOWN'
                    │
                   YES
                    │
                    ▼
    ┌───────────────────────────────────────────────┐
    │ Check against known patterns                  │
    ├───────────────────────────────────────────────┤
    │ 1. ${restServiceDelegate}     → REST_SERVICE  │
    │ 2. ${notificationDelegate}    → NOTIFICATION  │
    │ 3. ${emailDelegate}            → NOTIFICATION │
    │ 4. ${[a-zA-Z][a-zA-Z0-9_]*}   → LOCAL_SERVICE │
    │ 5. Default                     → UNKNOWN       │
    └───────────────────────────────────────────────┘
                    │
                    ▼
    Extract extension fields from element
                    │
                    ▼
    Return { type, delegateExpression, fields }
```

---

## Extension Elements XML Generation

```typescript
┌─────────────────────────────────────────────────────────────────┐
│ Input: ExtensionField[]                                          │
│ [                                                                │
│   { name: 'url', value: 'http://...', type: 'string' },        │
│   { name: 'method', value: 'POST', type: 'string' },           │
│   { name: 'body', value: '#{...}', type: 'expression' }        │
│ ]                                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ moddle.create('bpmn:ExtensionElements')                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ For each field:                                                  │
│   flowableField = moddle.create('flowable:Field', {             │
│     name: field.name                                             │
│   })                                                             │
│                                                                  │
│   if (field.type === 'expression'):                             │
│     flowableField.expression = field.value                       │
│   else:                                                          │
│     flowableField.string = field.value                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ modeling.updateProperties(element, {                             │
│   extensionElements: extensionElements                           │
│ })                                                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Generated XML:                                                   │
│ <extensionElements>                                              │
│   <flowable:field name="url">                                   │
│     <flowable:string>http://...</flowable:string>               │
│   </flowable:field>                                             │
│   <flowable:field name="method">                                │
│     <flowable:string>POST</flowable:string>                     │
│   </flowable:field>                                             │
│   <flowable:field name="body">                                  │
│     <flowable:expression>#{...}</flowable:expression>           │
│   </flowable:field>                                             │
│ </extensionElements>                                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## File Organization

```
frontends/admin-portal/
├── components/
│   └── bpmn/
│       ├── BpmnDesigner.tsx                    ✅ Exists
│       ├── ServiceTaskPropertiesPanel.tsx      ✅ Exists (90% ready)
│       ├── ExtensionElementsEditor.tsx         ✅ Exists (100% ready)
│       └── patterns/                           ❌ NEW
│           ├── RestServicePatternUI.tsx        ❌ NEW
│           ├── LocalServicePatternUI.tsx       ❌ NEW
│           ├── NotificationPatternUI.tsx       ❌ NEW
│           └── PatternSelector.tsx             ❌ NEW
│
├── lib/
│   ├── bpmn/
│   │   ├── flowable-properties-provider.ts     ✅ Exists (needs update)
│   │   ├── pattern-detector.ts                 ❌ NEW
│   │   ├── extension-field-mapper.ts           ❌ NEW
│   │   └── utils.ts                            ✅ Exists
│   │
│   ├── api/
│   │   ├── services.ts                         ✅ Exists (100% ready)
│   │   └── client.ts                           ✅ Exists
│   │
│   └── hooks/
│       └── useServiceRegistry.ts               ✅ Exists (100% ready)
│
└── types/
    ├── bpmn-js.d.ts                            ✅ Exists
    ├── bpmn-js-properties-panel.d.ts           ✅ Exists
    └── delegate-patterns.d.ts                  ❌ NEW
```

---

## API Integration Points

### Service Registry API

```typescript
// Frontend calls (already implemented)
GET  /api/services                     → List all services
GET  /api/services/{name}              → Get service details
GET  /api/services/{name}/endpoints    → Get service endpoints
GET  /api/services/{name}/health       → Check service health
POST /api/services/test-connectivity   → Test URL connectivity

// Backend status: Optional (mock data fallback exists)
// Risk: ZERO (UI works standalone)
```

### BPMN Deployment API

```typescript
// Flowable REST API (already working)
POST /api/process-definitions/deploy
{
  name: string,
  resourceName: string,
  bpmnXml: string
}

// Status: Production-ready ✅
```

---

## Testing Strategy

### Unit Tests

```typescript
// Pattern detection
describe('Pattern Detector', () => {
  test('detects REST service pattern', () => {})
  test('detects local service pattern', () => {})
  test('detects notification pattern', () => {})
  test('handles unknown patterns', () => {})
})

// Extension fields
describe('Extension Field Mapper', () => {
  test('creates flowable:field with string value', () => {})
  test('creates flowable:field with expression value', () => {})
  test('handles empty fields', () => {})
})

// UI components
describe('RestServicePatternUI', () => {
  test('renders service selector', () => {})
  test('filters endpoints by selected service', () => {})
  test('validates JSON body', () => {})
})
```

### Integration Tests

```typescript
describe('BPMN Service Task Configuration', () => {
  test('create service task and configure REST delegate', () => {
    // 1. Create service task
    // 2. Select REST pattern
    // 3. Configure fields
    // 4. Verify XML output
  })

  test('import BPMN with REST delegate and render UI', () => {
    // 1. Import BPMN XML with delegate
    // 2. Select service task
    // 3. Verify UI shows correct pattern
    // 4. Verify fields populated correctly
  })
})
```

### E2E Tests (Playwright)

```typescript
test('end-to-end REST service configuration', async ({ page }) => {
  await page.goto('/studio/processes/new')
  await page.getByRole('button', { name: 'Service Task' }).click()
  await page.getByLabel('Delegate Expression').selectOption('restServiceDelegate')
  await page.getByLabel('Service').selectOption('finance')
  await page.getByLabel('Endpoint').selectOption('/budget/check')
  await page.getByLabel('Request Body').fill('{"departmentId": "${dept}"}')
  await page.getByRole('button', { name: 'Deploy' }).click()
  await expect(page.getByText('Deployed successfully')).toBeVisible()
})
```

---

## Performance Optimization

### React Query Caching

```typescript
// Service list cached for 1 minute
useServices() → staleTime: 30000, refetchInterval: 60000

// Endpoints cached per service
useServiceEndpoints(name) → staleTime: 60000

// Health checks every 30 seconds
useServiceHealth(name) → refetchInterval: 30000
```

### Component Memoization

```typescript
// Memoize expensive computations
const patternInfo = useMemo(
  () => detectDelegatePattern(element),
  [element]
)

// Memoize service filtering
const filteredEndpoints = useMemo(
  () => endpoints.filter(e => e.service === selectedService),
  [endpoints, selectedService]
)
```

### Lazy Loading

```typescript
// Load pattern UIs only when needed
const RestServicePatternUI = lazy(() =>
  import('./patterns/RestServicePatternUI')
)

// Render with Suspense
<Suspense fallback={<LoadingSpinner />}>
  {pattern === 'REST_SERVICE' && <RestServicePatternUI />}
</Suspense>
```

---

## Security Considerations

### Input Validation

```typescript
// JSON body validation
function validateJsonBody(input: string): boolean {
  try {
    JSON.parse(input)
    return true
  } catch {
    return false
  }
}

// Variable name validation
function validateVariableName(name: string): boolean {
  return /^[a-zA-Z][a-zA-Z0-9_]*$/.test(name)
}

// URL validation
function validateUrl(url: string): boolean {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}
```

### XSS Prevention

```typescript
// All user input sanitized by React (automatic)
// No dangerouslySetInnerHTML used
// XML generation via moddle API (safe)
```

### BPMN Validation

```typescript
// Validate BPMN before deploy
async function validateBeforeDeploy(modeler: BpmnModeler): Promise<boolean> {
  const { xml, error } = await modeler.saveXML({ format: true })

  if (error) {
    console.error('BPMN validation error:', error)
    return false
  }

  // Additional validation
  if (!xml.includes('<process')) {
    return false
  }

  return true
}
```

---

## Deployment Considerations

### Feature Flag

```typescript
// Enable/disable visual delegate editor
const FEATURE_FLAGS = {
  visualDelegateEditor: process.env.NEXT_PUBLIC_VISUAL_DELEGATE_EDITOR === 'true'
}

// Conditional rendering
{FEATURE_FLAGS.visualDelegateEditor && (
  <ServiceTaskPropertiesPanel element={element} modeler={modeler} />
)}
```

### Rollout Strategy

1. **Phase 1:** Deploy to dev environment with feature flag OFF
2. **Phase 2:** Enable for internal testing (1 week)
3. **Phase 3:** Enable for beta users (1 week)
4. **Phase 4:** Full production rollout

### Monitoring

```typescript
// Track usage metrics
analytics.track('delegate_pattern_selected', {
  pattern: 'REST_SERVICE',
  service: 'finance',
  endpoint: '/budget/check'
})

// Track errors
analytics.track('delegate_configuration_error', {
  pattern: 'REST_SERVICE',
  error: 'Invalid JSON body'
})
```

---

**Document Status:** Final
**Related Documents:**
- BPMN-Visual-Delegate-Editor-Feasibility-Analysis.md
- Frontend-No-Code-Gap-Analysis.md
