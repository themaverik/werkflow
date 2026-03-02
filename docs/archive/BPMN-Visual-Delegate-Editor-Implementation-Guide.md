# BPMN Visual Delegate Editor - Implementation Guide

**Date**: 2025-11-23
**Developer**: Frontend Team
**Timeline**: 4 weeks (MVP: 2 weeks)

---

## Quick Start

### Prerequisites

1. ✅ BPMN-js v17.11.1 (already installed)
2. ✅ React Query (already installed)
3. ✅ shadcn/ui components (already available)
4. ✅ Service Registry API client (already implemented)

### Development Environment

```bash
cd /Users/lamteiwahlang/Projects/werkflow/frontends/admin-portal

# Install dependencies (already done)
npm install

# Start dev server
npm run dev

# Open BPMN designer
# Navigate to: http://localhost:3000/studio/processes/new
```

---

## Implementation Phases

### Phase 1: MVP - REST Service Pattern (2 weeks)

#### Week 1: Pattern Detection & Basic UI

**Day 1-2: Pattern Detector**

Create `/lib/bpmn/pattern-detector.ts`:

```typescript
export type DelegatePattern = 'REST_SERVICE' | 'LOCAL_SERVICE' | 'NOTIFICATION' | 'UNKNOWN'

export interface PatternInfo {
  type: DelegatePattern
  delegateExpression: string
  fields: Map<string, { value: string; type: 'string' | 'expression' }>
}

export function detectDelegatePattern(element: any): PatternInfo {
  const businessObject = element.businessObject
  const delegateExpr = businessObject.get('flowable:delegateExpression') ||
                       businessObject.delegateExpression || ''

  const fields = extractExtensionFields(element)

  // Pattern matching
  if (delegateExpr === '${restServiceDelegate}') {
    return { type: 'REST_SERVICE', delegateExpression: delegateExpr, fields }
  }

  if (delegateExpr === '${notificationDelegate}') {
    return { type: 'NOTIFICATION', delegateExpression: delegateExpr, fields }
  }

  if (delegateExpr && delegateExpr.match(/^\$\{[a-zA-Z][a-zA-Z0-9_]*\}$/)) {
    return { type: 'LOCAL_SERVICE', delegateExpression: delegateExpr, fields }
  }

  return { type: 'UNKNOWN', delegateExpression: delegateExpr, fields }
}

function extractExtensionFields(element: any): Map<string, any> {
  const fields = new Map()
  const extensionElements = element.businessObject.extensionElements

  if (!extensionElements) return fields

  const flowableFields = extensionElements.values?.filter(
    (ext: any) => ext.$type === 'flowable:Field'
  ) || []

  flowableFields.forEach((field: any) => {
    const value = field.string || field.expression || ''
    const type = field.expression ? 'expression' : 'string'
    fields.set(field.name, { value, type })
  })

  return fields
}
```

**Testing:**

```typescript
// __tests__/pattern-detector.test.ts
import { describe, test, expect } from 'vitest'
import { detectDelegatePattern } from '../pattern-detector'

describe('Pattern Detector', () => {
  test('detects REST service pattern', () => {
    const element = {
      businessObject: {
        get: (key: string) => key === 'flowable:delegateExpression'
          ? '${restServiceDelegate}'
          : null,
        extensionElements: null
      }
    }

    const result = detectDelegatePattern(element)
    expect(result.type).toBe('REST_SERVICE')
    expect(result.delegateExpression).toBe('${restServiceDelegate}')
  })

  test('detects local service pattern', () => {
    const element = {
      businessObject: {
        get: (key: string) => key === 'flowable:delegateExpression'
          ? '${vendorValidationDelegate}'
          : null,
        extensionElements: null
      }
    }

    const result = detectDelegatePattern(element)
    expect(result.type).toBe('LOCAL_SERVICE')
  })
})
```

**Day 3-4: REST Service Pattern UI**

Create `/components/bpmn/patterns/RestServicePatternUI.tsx`:

```typescript
'use client'

import { useState, useEffect } from 'react'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useServices, useServiceEndpoints } from '@/lib/hooks/useServiceRegistry'
import { Badge } from '@/components/ui/badge'

interface RestServicePatternUIProps {
  element: any
  modeler: any
  onFieldsChange: (fields: Map<string, any>) => void
}

export default function RestServicePatternUI({
  element,
  modeler,
  onFieldsChange
}: RestServicePatternUIProps) {
  const { data: services } = useServices()
  const [selectedService, setSelectedService] = useState('')
  const [selectedEndpoint, setSelectedEndpoint] = useState('')
  const [requestBody, setRequestBody] = useState('')
  const [responseVariable, setResponseVariable] = useState('serviceResponse')

  const { data: endpoints } = useServiceEndpoints(selectedService)

  const handleServiceChange = (serviceName: string) => {
    setSelectedService(serviceName)
    setSelectedEndpoint('')
  }

  const handleEndpointChange = (endpointPath: string) => {
    setSelectedEndpoint(endpointPath)

    // Auto-populate URL
    const service = services?.find(s => s.name === selectedService)
    const endpoint = endpoints?.find(e => e.path === endpointPath)

    if (service && endpoint) {
      const fullUrl = `${service.baseUrl}${endpoint.path}`
      const httpMethod = endpoint.method

      // Update extension fields
      const fields = new Map([
        ['url', { value: fullUrl, type: 'string' }],
        ['method', { value: httpMethod, type: 'string' }],
        ['responseVariable', { value: responseVariable, type: 'string' }]
      ])

      onFieldsChange(fields)
    }
  }

  const handleRequestBodyChange = (value: string) => {
    setRequestBody(value)

    // Validate JSON
    try {
      JSON.parse(value)
      // Valid JSON - update fields
      const fields = new Map([
        ['url', { value: `${services?.find(s => s.name === selectedService)?.baseUrl}${selectedEndpoint}`, type: 'string' }],
        ['method', { value: endpoints?.find(e => e.path === selectedEndpoint)?.method || 'POST', type: 'string' }],
        ['body', { value: `#{${value}}`, type: 'expression' }],
        ['responseVariable', { value: responseVariable, type: 'string' }]
      ])
      onFieldsChange(fields)
    } catch (error) {
      // Invalid JSON - don't update
      console.warn('Invalid JSON:', error)
    }
  }

  const selectedServiceData = services?.find(s => s.name === selectedService)
  const selectedEndpointData = endpoints?.find(e => e.path === selectedEndpoint)

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">REST Service Configuration</CardTitle>
          <CardDescription>
            Configure HTTP call to another service
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {/* Service Selector */}
          <div>
            <Label className="text-xs">Target Service</Label>
            <Select value={selectedService} onValueChange={handleServiceChange}>
              <SelectTrigger className="h-8 text-xs">
                <SelectValue placeholder="Select service" />
              </SelectTrigger>
              <SelectContent>
                {services?.map((service) => (
                  <SelectItem key={service.id} value={service.name}>
                    {service.displayName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Endpoint Selector */}
          {selectedService && endpoints && endpoints.length > 0 && (
            <div>
              <Label className="text-xs">Endpoint</Label>
              <Select value={selectedEndpoint} onValueChange={handleEndpointChange}>
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue placeholder="Select endpoint" />
                </SelectTrigger>
                <SelectContent>
                  {endpoints.map((endpoint, index) => (
                    <SelectItem key={index} value={endpoint.path}>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          {endpoint.method}
                        </Badge>
                        <span className="text-xs">{endpoint.path}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {selectedEndpointData && (
                <p className="text-xs text-muted-foreground mt-1">
                  {selectedEndpointData.description}
                </p>
              )}
            </div>
          )}

          {/* Request Body (for POST/PUT) */}
          {selectedEndpointData && ['POST', 'PUT', 'PATCH'].includes(selectedEndpointData.method) && (
            <div>
              <Label className="text-xs">Request Body (JSON)</Label>
              <textarea
                value={requestBody}
                onChange={(e) => handleRequestBodyChange(e.target.value)}
                className="w-full h-32 p-2 text-xs font-mono border rounded"
                placeholder='{"key": "${processVariable}"}'
              />
              <p className="text-xs text-muted-foreground mt-1">
                Use ${'{variableName}'} for process variables
              </p>
            </div>
          )}

          {/* Response Variable */}
          <div>
            <Label className="text-xs">Response Variable Name</Label>
            <Input
              value={responseVariable}
              onChange={(e) => setResponseVariable(e.target.value)}
              className="h-8 text-xs"
              placeholder="serviceResponse"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Store API response in this variable
            </p>
          </div>

          {/* Preview */}
          {selectedServiceData && selectedEndpointData && (
            <Card className="bg-muted">
              <CardHeader className="p-3">
                <CardTitle className="text-xs">Preview</CardTitle>
              </CardHeader>
              <CardContent className="p-3">
                <div className="space-y-1 text-xs font-mono">
                  <div>
                    <span className="text-muted-foreground">URL:</span>{' '}
                    <span>{selectedServiceData.baseUrl}{selectedEndpointData.path}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Method:</span>{' '}
                    <span>{selectedEndpointData.method}</span>
                  </div>
                  {requestBody && (
                    <div>
                      <span className="text-muted-foreground">Body:</span>{' '}
                      <pre className="mt-1 p-2 bg-background rounded">
                        {requestBody}
                      </pre>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
```

**Day 5: Integration**

Update `/components/bpmn/ServiceTaskPropertiesPanel.tsx`:

```typescript
'use client'

import { useEffect, useState } from 'react'
import { detectDelegatePattern, type PatternInfo } from '@/lib/bpmn/pattern-detector'
import RestServicePatternUI from './patterns/RestServicePatternUI'
import ExtensionElementsEditor from './ExtensionElementsEditor'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Label } from '@/components/ui/label'

interface ServiceTaskPropertiesPanelProps {
  element: any
  modeler: any
}

export default function ServiceTaskPropertiesPanel({
  element,
  modeler
}: ServiceTaskPropertiesPanelProps) {
  const [patternInfo, setPatternInfo] = useState<PatternInfo | null>(null)
  const [selectedPattern, setSelectedPattern] = useState<string>('')

  useEffect(() => {
    if (element && modeler) {
      const info = detectDelegatePattern(element)
      setPatternInfo(info)
      setSelectedPattern(info.type)
    }
  }, [element, modeler])

  const handlePatternChange = (pattern: string) => {
    setSelectedPattern(pattern)

    const modeling = modeler.get('modeling')
    let delegateExpr = ''

    switch (pattern) {
      case 'REST_SERVICE':
        delegateExpr = '${restServiceDelegate}'
        break
      case 'NOTIFICATION':
        delegateExpr = '${notificationDelegate}'
        break
      case 'LOCAL_SERVICE':
        delegateExpr = '' // User will input custom bean name
        break
    }

    modeling.updateProperties(element, {
      'flowable:delegateExpression': delegateExpr || undefined,
      delegateExpression: delegateExpr || undefined
    })
  }

  const handleFieldsChange = (fields: Map<string, any>) => {
    // Convert Map to array for ExtensionElementsEditor
    const fieldArray = Array.from(fields.entries()).map(([name, { value, type }]) => ({
      name,
      value,
      type
    }))

    // Trigger extension elements update via ExtensionElementsEditor
    // (The editor component will handle the XML update)
  }

  return (
    <div className="space-y-4 p-4">
      {/* Pattern Selector */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Delegate Type</CardTitle>
          <CardDescription>
            Choose the type of service task delegate
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Label className="text-xs">Pattern</Label>
          <Select value={selectedPattern} onValueChange={handlePatternChange}>
            <SelectTrigger className="h-8 text-xs">
              <SelectValue placeholder="Select pattern" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="REST_SERVICE">REST Service Call</SelectItem>
              <SelectItem value="LOCAL_SERVICE">Local Service Bean</SelectItem>
              <SelectItem value="NOTIFICATION">Notification</SelectItem>
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {/* Pattern-Specific UI */}
      {selectedPattern === 'REST_SERVICE' && (
        <RestServicePatternUI
          element={element}
          modeler={modeler}
          onFieldsChange={handleFieldsChange}
        />
      )}

      {/* Extension Elements Editor (always show for manual editing) */}
      <ExtensionElementsEditor
        element={element}
        modeler={modeler}
      />
    </div>
  )
}
```

#### Week 2: Testing & Polish

**Day 6-7: Unit Tests**

```bash
npm run test -- pattern-detector.test.ts
npm run test -- RestServicePatternUI.test.tsx
```

**Day 8-9: Integration Testing**

Manual testing in browser:
1. Create new process
2. Add service task
3. Select REST pattern
4. Configure finance service
5. Deploy and verify XML

**Day 10: Documentation & Demo**

- Create user guide
- Record demo video
- Update README

---

### Phase 2: Complete Patterns (1 week)

#### Day 11-12: Local Service Pattern

Create `/components/bpmn/patterns/LocalServicePatternUI.tsx`:

```typescript
'use client'

import { useState } from 'react'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface LocalServicePatternUIProps {
  element: any
  modeler: any
}

// Common delegate beans in the system
const COMMON_DELEGATES = [
  { value: 'vendorValidationDelegate', label: 'Vendor Validation' },
  { value: 'quoteValidationDelegate', label: 'Quote Validation' },
  { value: 'budgetCheckDelegate', label: 'Budget Check' },
  { value: 'poCreationDelegate', label: 'Purchase Order Creation' },
  { value: 'inventoryUpdateDelegate', label: 'Inventory Update' },
]

export default function LocalServicePatternUI({
  element,
  modeler
}: LocalServicePatternUIProps) {
  const [beanName, setBeanName] = useState('')
  const [isCustomBean, setIsCustomBean] = useState(false)

  const handleBeanChange = (value: string) => {
    if (value === 'custom') {
      setIsCustomBean(true)
      setBeanName('')
    } else {
      setIsCustomBean(false)
      setBeanName(value)
      updateDelegate(value)
    }
  }

  const handleCustomBeanChange = (value: string) => {
    setBeanName(value)
    if (value) {
      updateDelegate(value)
    }
  }

  const updateDelegate = (bean: string) => {
    const modeling = modeler.get('modeling')
    modeling.updateProperties(element, {
      'flowable:delegateExpression': `\${${bean}}`,
      delegateExpression: `\${${bean}}`
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Local Service Bean</CardTitle>
        <CardDescription>
          Execute a Spring bean delegate
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div>
          <Label className="text-xs">Service Bean</Label>
          <Select value={isCustomBean ? 'custom' : beanName} onValueChange={handleBeanChange}>
            <SelectTrigger className="h-8 text-xs">
              <SelectValue placeholder="Select delegate bean" />
            </SelectTrigger>
            <SelectContent>
              {COMMON_DELEGATES.map((delegate) => (
                <SelectItem key={delegate.value} value={delegate.value}>
                  {delegate.label}
                </SelectItem>
              ))}
              <SelectItem value="custom">Custom Bean...</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {isCustomBean && (
          <div>
            <Label className="text-xs">Custom Bean Name</Label>
            <Input
              value={beanName}
              onChange={(e) => handleCustomBeanChange(e.target.value)}
              className="h-8 text-xs"
              placeholder="myCustomDelegate"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Must match a Spring bean name in the backend
            </p>
          </div>
        )}

        {beanName && (
          <Card className="bg-muted">
            <CardContent className="p-3">
              <div className="text-xs font-mono">
                <span className="text-muted-foreground">Delegate Expression:</span>
                <br />
                <code>${'{' + beanName + '}'}</code>
              </div>
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  )
}
```

#### Day 13-14: Notification Pattern

Create `/components/bpmn/patterns/NotificationPatternUI.tsx`:

```typescript
'use client'

import { useState } from 'react'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface NotificationPatternUIProps {
  element: any
  modeler: any
  onFieldsChange: (fields: Map<string, any>) => void
}

// Notification types from backend
const NOTIFICATION_TYPES = [
  { value: 'PR_BUDGET_SHORTFALL', label: 'PR Budget Shortfall' },
  { value: 'PR_MANAGER_REJECTED', label: 'PR Manager Rejected' },
  { value: 'PO_CREATED', label: 'Purchase Order Created' },
  { value: 'LEAVE_APPROVED', label: 'Leave Approved' },
  { value: 'LEAVE_REJECTED', label: 'Leave Rejected' },
]

export default function NotificationPatternUI({
  element,
  modeler,
  onFieldsChange
}: NotificationPatternUIProps) {
  const [notificationType, setNotificationType] = useState('')

  const handleTypeChange = (value: string) => {
    setNotificationType(value)

    // Update extension fields
    const fields = new Map([
      ['notificationType', { value, type: 'string' }]
    ])

    onFieldsChange(fields)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Notification Configuration</CardTitle>
        <CardDescription>
          Send email, SMS, or push notification
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div>
          <Label className="text-xs">Notification Type</Label>
          <Select value={notificationType} onValueChange={handleTypeChange}>
            <SelectTrigger className="h-8 text-xs">
              <SelectValue placeholder="Select notification type" />
            </SelectTrigger>
            <SelectContent>
              {NOTIFICATION_TYPES.map((type) => (
                <SelectItem key={type.value} value={type.value}>
                  {type.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {notificationType && (
          <Card className="bg-muted">
            <CardContent className="p-3">
              <div className="text-xs">
                <p className="text-muted-foreground mb-2">Template Preview:</p>
                <p className="font-medium">
                  {NOTIFICATION_TYPES.find(t => t.value === notificationType)?.label}
                </p>
                <p className="text-muted-foreground mt-2">
                  Notification will be sent using predefined template
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  )
}
```

#### Day 15: Integration & Testing

Update `ServiceTaskPropertiesPanel.tsx` to include all three patterns.

---

### Phase 3: Polish & Production (1 week)

#### Day 16-17: Advanced Features

- JSON syntax highlighting
- Process variable autocomplete
- Template library

#### Day 18-19: E2E Tests

```typescript
// e2e/bpmn-delegate-config.spec.ts
import { test, expect } from '@playwright/test'

test('configure REST service delegate end-to-end', async ({ page }) => {
  await page.goto('/studio/processes/new')

  // Wait for BPMN designer to load
  await page.waitForSelector('.djs-container')

  // Add service task to canvas
  await page.getByRole('button', { name: 'Service Task' }).click()
  await page.getByTestId('bpmn-canvas').click({ position: { x: 200, y: 200 } })

  // Configure delegate
  await page.getByLabel('Pattern').selectOption('REST_SERVICE')
  await page.getByLabel('Target Service').selectOption('finance')
  await page.getByLabel('Endpoint').selectOption('/budget/check')

  // Verify extension fields created
  const xmlButton = page.getByRole('button', { name: 'View XML' })
  await xmlButton.click()

  const xml = await page.getByTestId('xml-preview').textContent()
  expect(xml).toContain('restServiceDelegate')
  expect(xml).toContain('flowable:field')
  expect(xml).toContain('finance-service')
})
```

#### Day 20: Documentation & Launch

- Final testing
- User documentation
- Deploy to production

---

## Key Integration Points

### Wiring into Properties Provider

Update `/lib/bpmn/flowable-properties-provider.ts`:

```typescript
// Around line 103-137
if (is(element, 'bpmn:ServiceTask')) {
  groups.splice(generalIdx + 1, 0, {
    id: 'flowable-service-task',
    label: 'Service Configuration',
    entries: [
      {
        id: 'serviceTaskPropertiesPanel',
        element,
        component: ServiceTaskPropertiesPanelEntry,
        isEdited: () => false
      }
    ]
  })
}

// Add entry component
function ServiceTaskPropertiesPanelEntry(props: any) {
  const { element, id } = props

  return {
    id,
    element,
    label: '',
    html: `<div id="service-task-properties-${element.id}"></div>`,
    get: () => ({}),
    set: () => {}
  }
}

// Then render React component into that div using createRoot
```

**Alternative Approach:** Use existing pattern from `ServiceTaskPropertiesPanel.tsx` which already works.

---

## Testing Checklist

### Manual Testing

- [ ] Create new service task
- [ ] Select REST pattern
- [ ] Configure service (finance)
- [ ] Configure endpoint (/budget/check)
- [ ] Add request body JSON
- [ ] Save and download BPMN
- [ ] Re-import BPMN and verify properties
- [ ] Deploy to Flowable
- [ ] Verify process executes correctly

### Unit Tests

- [ ] Pattern detection for all 3 patterns
- [ ] Extension field extraction
- [ ] Extension field creation
- [ ] JSON validation
- [ ] URL validation

### Integration Tests

- [ ] BPMN import with delegates
- [ ] BPMN export with delegates
- [ ] Pattern switching (REST -> Local -> Notification)
- [ ] Undo/redo operations

### E2E Tests

- [ ] Full workflow creation with service tasks
- [ ] Deployment to Flowable
- [ ] Process execution

---

## Troubleshooting

### Common Issues

**Issue:** Properties panel not showing custom UI

**Solution:** Check that `FlowablePropertiesProvider` is registered:

```typescript
// In BpmnDesigner.tsx
additionalModules: [
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  FlowablePropertiesProvider  // Add this
]
```

**Issue:** Extension elements not saving

**Solution:** Verify moddle service is available:

```typescript
const moddle = modeler.get('moddle')
if (!moddle) {
  console.error('Moddle service not available')
}
```

**Issue:** Service Registry returns empty list

**Solution:** Check mock data fallback is working:

```typescript
// In services.ts
export async function getServices() {
  try {
    const response = await apiClient.get('/services')
    return response.data
  } catch (error) {
    console.warn('Using mock data:', error)
    return getMockServices()  // Should always work
  }
}
```

---

## Performance Tips

1. **Memoize pattern detection:**
```typescript
const patternInfo = useMemo(
  () => detectDelegatePattern(element),
  [element]
)
```

2. **Debounce JSON validation:**
```typescript
const debouncedValidation = useDebouncedCallback(
  (value: string) => {
    try {
      JSON.parse(value)
      setIsValid(true)
    } catch {
      setIsValid(false)
    }
  },
  300
)
```

3. **Lazy load pattern UIs:**
```typescript
const RestServicePatternUI = lazy(() =>
  import('./patterns/RestServicePatternUI')
)
```

---

## Next Steps After MVP

1. **Variable Autocomplete:** Show available process variables in request body editor
2. **JSON Schema Validation:** Validate against endpoint schema
3. **Request Testing:** Test API call directly from UI
4. **Template Library:** Save and reuse common configurations
5. **Batch Configuration:** Configure multiple service tasks at once

---

**Document Status:** Implementation Ready
**Last Updated:** 2025-11-23
**Owner:** Frontend Team
