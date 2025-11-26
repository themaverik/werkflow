# Form.io to Form-JS Replacement Feasibility Analysis

**Project**: Werkflow Platform
**Date**: 2025-11-24
**Status**: Analysis Complete
**Priority**: Medium (P2)
**Target**: Phase 4-6 Parallel Track (Optional Enhancement)

---

## Executive Summary

This document analyzes the feasibility of replacing Form.io with form-js (by bpmn-io team) in the Werkflow platform. After comprehensive analysis, the recommendation is **DEFER** - do not pursue this replacement during Phase 4-6 critical path work, revisit as Phase 7 enhancement.

### Key Findings

| Category | Current (Form.io) | Target (form-js) | Assessment |
|----------|-------------------|-------------------|------------|
| Feature Completeness | 95% | 60% | RISK: Major gaps |
| Visual Form Builder | Commercial/Open | None (manual JSON) | BLOCKER: No official builder |
| JSON Schema Format | Form.io v4 | form-js custom | EFFORT: Complete transformation needed |
| Field Types | 30+ types | 12 basic types | RISK: Missing HR-specific fields |
| Conditional Logic | Advanced (complex rules) | Basic (simple conditions) | RISK: Workflow logic gaps |
| File Upload | Full support | Limited | RISK: Document attachments critical |
| Validation | Comprehensive | Basic | RISK: Form validation incomplete |
| Community Support | Large, mature | Small, BPMN-focused | RISK: Limited help resources |
| License | MIT (Open) + Commercial | MIT (Open) | NEUTRAL: Both open source |

### Recommendation: **DEFER (NO-GO for Phase 4-6)**

**Reasoning**:
1. Phase 4-6 critical path already at capacity (Service Registry, RBAC, Task UI)
2. Form.io currently working well - no blocking issues
3. form-js lacks visual form builder (critical for POC self-service)
4. Migration effort estimated at 4-6 weeks - unacceptable for non-critical work
5. High risk of disrupting existing form workflows
6. Limited ROI - form-js doesn't provide significant advantages over Form.io

**Alternative Path**: Keep Form.io for Phase 4-6, revisit form-js in Phase 7 if:
- bpmn-io team releases visual form builder
- Form.io licensing costs become prohibitive
- Business requirements change to favor lightweight forms

---

## 1. Feature Comparison Analysis

### 1.1 Field Types Comparison

#### Form.io Supported Field Types (Current)

```typescript
// Current Form.io Fields Used in Werkflow
const formioFieldTypes = [
  // Basic Input
  'textfield',      // Single-line text
  'textarea',       // Multi-line text
  'number',         // Numeric input
  'email',          // Email validation
  'phoneNumber',    // Phone formatting
  'url',            // URL validation

  // Date/Time
  'datetime',       // Date + time picker
  'day',            // Day selector
  'time',           // Time picker

  // Selection
  'select',         // Dropdown
  'selectboxes',    // Multi-select checkboxes
  'radio',          // Radio buttons
  'checkbox',       // Single checkbox

  // File Handling
  'file',           // File upload with preview

  // Advanced
  'signature',      // Digital signature pad
  'currency',       // Currency formatting
  'button',         // Action buttons
  'htmlelement',    // Custom HTML
  'content',        // Rich content display

  // Layout
  'panel',          // Grouping container
  'columns',        // Multi-column layout
  'fieldset',       // Fieldset grouping
  'well',           // Styled container
  'tabs',           // Tabbed interface

  // Data
  'datagrid',       // Repeating data table
  'editgrid',       // Editable data grid
  'table',          // Layout table
];
```

#### form-js Supported Field Types

```typescript
// form-js Supported Fields (Limited)
const formJsFieldTypes = [
  // Basic Input
  'textfield',      // Single-line text
  'textarea',       // Multi-line text (basic)
  'number',         // Numeric input (basic)

  // Selection
  'select',         // Dropdown (limited options)
  'radio',          // Radio buttons (basic)
  'checkbox',       // Single checkbox
  'checklist',      // Multi-select (basic)

  // Date/Time
  'datetime',       // Date picker (basic)

  // Structural
  'group',          // Field grouping
  'spacer',         // Layout spacing
  'text',           // Static text display
  'button',         // Action button (limited)
];

// MISSING in form-js (Critical for Werkflow):
const missingInFormJs = [
  'email',          // Email validation built-in
  'phoneNumber',    // Phone formatting
  'url',            // URL validation
  'signature',      // Digital signature (required for approvals)
  'currency',       // Currency formatting (CapEx workflows)
  'file',           // File upload (document attachments)
  'datagrid',       // Repeating data (expense items, invoice lines)
  'editgrid',       // Editable grids
  'tabs',           // Multi-step forms
  'columns',        // Multi-column layouts
  'htmlelement',    // Custom HTML content
];
```

**Assessment**: **HIGH RISK** - Missing 11 critical field types used in HR and Finance workflows.

### 1.2 Validation Capabilities

#### Form.io Validation (Current)

```javascript
// Form.io Validation Rules
{
  "validate": {
    "required": true,
    "minLength": 5,
    "maxLength": 100,
    "pattern": "^[A-Za-z0-9]+$",
    "custom": "valid = (input > 0);",  // JavaScript expression
    "customMessage": "Amount must be greater than zero",
    "json": {
      "if": { "var": "leaveType" },
      "==": "sick",
      "then": { "required": true }
    },
    "minDate": "2025-01-01",
    "maxDate": "2025-12-31",
    "email": true,
    "url": true,
    "min": 0,
    "max": 1000000,
    "step": 0.01
  }
}
```

#### form-js Validation

```javascript
// form-js Validation (Limited)
{
  "validate": {
    "required": true,
    "minLength": 5,
    "maxLength": 100,
    "pattern": "^[A-Za-z0-9]+$"
  }
  // No custom JavaScript validation
  // No conditional validation
  // No complex business rules
  // No cross-field validation
}
```

**Assessment**: **MEDIUM RISK** - Missing custom validation critical for business rules (e.g., DOA levels, budget checks).

### 1.3 Conditional Logic

#### Form.io Conditional Logic (Current)

```javascript
// Example: Show justification field if amount > $10K
{
  "key": "justification",
  "type": "textarea",
  "label": "Justification",
  "conditional": {
    "show": true,
    "when": "amount",
    "eq": "",
    "json": {
      "if": [
        { ">": [{ "var": "amount" }, 10000] },
        true,
        false
      ]
    }
  }
}

// Example: Multi-condition logic
{
  "conditional": {
    "json": {
      "and": [
        { "==": [{ "var": "leaveType" }, "sick"] },
        { ">": [{ "var": "days" }, 3] }
      ]
    }
  }
}
```

#### form-js Conditional Logic

```javascript
// form-js Conditional (Basic)
{
  "key": "justification",
  "type": "textarea",
  "label": "Justification",
  "conditional": {
    "hide": "=amount <= 10000"  // Simple expression only
  }
}

// No support for:
// - Complex AND/OR conditions
// - Cross-field dependencies
// - Dynamic dropdown options
// - Calculated fields
```

**Assessment**: **HIGH RISK** - Conditional logic powers dynamic approval flows in CapEx, leave, expense workflows.

### 1.4 File Upload Support

#### Form.io File Upload (Current)

```javascript
{
  "type": "file",
  "label": "Attachments",
  "storage": "base64",  // or 'url', 's3', 'azure'
  "url": "/api/files/upload",
  "options": {
    "withCredentials": true
  },
  "filePattern": ".pdf,.doc,.docx,.xls,.xlsx,.jpg,.png",
  "fileMinSize": "1KB",
  "fileMaxSize": "10MB",
  "multiple": true,
  "image": true,  // Image preview
  "webcam": false
}
```

#### form-js File Upload

```text
NOT SUPPORTED OUT-OF-THE-BOX

Workarounds:
1. Custom component extension (requires JavaScript coding)
2. External file upload service
3. Base64 encoding (not scalable for large files)
```

**Assessment**: **BLOCKER** - File uploads critical for:
- CapEx: Supporting documents (vendor quotes, specifications)
- HR: Medical certificates, training certificates, resumes
- Procurement: RFQ documents, contracts
- Legal: Contract templates, signed agreements

---

## 2. JSON Schema Compatibility

### 2.1 Form.io Schema Format (Current)

```json
{
  "display": "form",
  "components": [
    {
      "type": "textfield",
      "key": "employeeName",
      "label": "Employee Name",
      "placeholder": "Enter full name",
      "tooltip": "As per official records",
      "description": "Official name for payroll",
      "validate": {
        "required": true,
        "minLength": 3,
        "maxLength": 100
      },
      "defaultValue": "${currentUser.name}",
      "disabled": false,
      "hidden": false,
      "clearOnHide": true,
      "customClass": "form-control-lg",
      "tabindex": "1",
      "autofocus": true,
      "spellcheck": true,
      "prefix": "",
      "suffix": "",
      "inputMask": ""
    },
    {
      "type": "number",
      "key": "amount",
      "label": "Amount",
      "validate": {
        "required": true,
        "min": 0,
        "max": 1000000,
        "step": 0.01
      },
      "currency": "USD",
      "delimiter": true,
      "requireDecimal": true,
      "decimalLimit": 2
    },
    {
      "type": "datetime",
      "key": "startDate",
      "label": "Start Date",
      "format": "yyyy-MM-dd",
      "enableTime": false,
      "enableDate": true,
      "datePicker": {
        "minDate": "2025-01-01",
        "maxDate": "2025-12-31",
        "disableWeekends": false,
        "disableWeekdays": [],
        "disabledDates": []
      }
    }
  ],
  "settings": {
    "pdf": {
      "id": "form-123",
      "src": "https://..."
    }
  }
}
```

### 2.2 form-js Schema Format

```json
{
  "type": "default",
  "id": "Form_1",
  "components": [
    {
      "type": "textfield",
      "id": "Field_1",
      "key": "employeeName",
      "label": "Employee Name",
      "description": "Official name",
      "validate": {
        "required": true,
        "minLength": 3,
        "maxLength": 100
      }
    },
    {
      "type": "number",
      "id": "Field_2",
      "key": "amount",
      "label": "Amount",
      "validate": {
        "required": true,
        "min": 0,
        "max": 1000000
      }
      // No currency formatting
      // No delimiter
      // No decimal control
    },
    {
      "type": "datetime",
      "id": "Field_3",
      "key": "startDate",
      "label": "Start Date",
      "dateLabel": "Date",
      "timeLabel": "Time",
      "timeSerializingFormat": "utc_offset",
      "timeInterval": 15
      // No date range constraints
      // No weekend/weekday disabling
      // No date formatting options
    }
  ],
  "schemaVersion": 4
}
```

### 2.3 Schema Transformation Complexity

#### Migration Requirements

```typescript
// Schema Transformation Service
class FormioToFormJsTransformer {

  /**
   * Transform Form.io schema to form-js schema
   *
   * Complexity: HIGH
   * Effort: 5-7 days of development + testing
   */
  transform(formioSchema: FormioSchema): FormJsSchema {
    // 1. Map field types (with fallbacks for missing types)
    const components = formioSchema.components.map(comp => {
      return this.mapFieldType(comp);
    });

    // 2. Transform validation rules
    // - Remove unsupported validators (custom, json logic)
    // - Convert patterns to form-js format
    // - Handle validation message customization

    // 3. Handle conditional logic
    // - Convert json-logic to simple expressions
    // - Warn on unsupported complex conditions
    // - Provide fallback to "always show"

    // 4. Remove unsupported features
    // - File uploads -> custom component
    // - Currency formatting -> plain number
    // - Date constraints -> basic date picker
    // - Signature -> custom component
    // - Datagrid -> manual table entry

    // 5. Layout transformation
    // - Columns -> group with custom CSS
    // - Tabs -> sequential fields
    // - Panels -> groups

    return {
      type: 'default',
      id: generateId(),
      components: components,
      schemaVersion: 4
    };
  }

  /**
   * Identify non-transformable features
   */
  analyzeIncompatibilities(formioSchema: FormioSchema): string[] {
    const issues: string[] = [];

    // Check for file uploads
    if (hasFileFields(formioSchema)) {
      issues.push('BLOCKER: File upload fields require custom components');
    }

    // Check for complex conditional logic
    if (hasComplexConditionals(formioSchema)) {
      issues.push('HIGH: Complex conditional logic will be simplified');
    }

    // Check for custom validation
    if (hasCustomValidation(formioSchema)) {
      issues.push('HIGH: Custom JavaScript validation will be lost');
    }

    // Check for currency fields
    if (hasCurrencyFields(formioSchema)) {
      issues.push('MEDIUM: Currency formatting will be plain numbers');
    }

    // Check for signature fields
    if (hasSignatureFields(formioSchema)) {
      issues.push('HIGH: Digital signatures require custom component');
    }

    // Check for data grids
    if (hasDataGrids(formioSchema)) {
      issues.push('HIGH: Data grids not supported, need manual table');
    }

    return issues;
  }
}
```

#### Migration Path for Existing Forms

```typescript
// Example: 8 HR Form Templates Migration

const formTemplates = [
  'leave-request-form',           // COMPLEXITY: Medium (date validation, file upload)
  'expense-reimbursement-form',   // COMPLEXITY: High (currency, file upload, datagrid)
  'training-request-form',        // COMPLEXITY: Low (basic fields)
  'asset-request-form',           // COMPLEXITY: Medium (conditional logic)
  'capex-approval-form',          // COMPLEXITY: Very High (currency, file upload, complex validation)
  'timesheet-form',               // COMPLEXITY: High (datagrid, calculations)
  'employee-onboarding-form',     // COMPLEXITY: Very High (multi-step, file uploads, signature)
  'exit-clearance-form'           // COMPLEXITY: High (multi-department, signatures)
];

// Migration Effort Estimation:
const migrationEffort = {
  'leave-request-form': {
    analysis: '0.5 days',
    transformation: '1 day',
    customComponents: '1 day (file upload)',
    testing: '1 day',
    total: '3.5 days'
  },
  'expense-reimbursement-form': {
    analysis: '0.5 days',
    transformation: '1.5 days',
    customComponents: '3 days (currency, file upload, datagrid)',
    testing: '1.5 days',
    total: '6.5 days'
  },
  'capex-approval-form': {
    analysis: '1 day',
    transformation: '2 days',
    customComponents: '4 days (currency, file upload, complex validation)',
    testing: '2 days',
    total: '9 days'
  }
  // ... other forms
};

// TOTAL MIGRATION EFFORT: 35-40 days (7-8 weeks)
```

**Assessment**: **VERY HIGH EFFORT** - Schema transformation is complex and error-prone. Each form requires custom development for missing features.

---

## 3. Visual Form Builder Analysis

### 3.1 Current: Form.io Form Builder

#### Features

```typescript
// Form.io FormBuilder Component
import { FormBuilder } from '@formio/react';

<FormBuilder
  form={formSchema}
  onChange={handleFormChange}
  options={{
    builder: {
      basic: true,        // Basic fields
      advanced: true,     // Advanced fields
      data: true,         // Data components
      layout: true,       // Layout components
      premium: false      // Premium features (commercial)
    },
    editForm: {
      textfield: [
        { key: 'display', components: [...] },
        { key: 'data', components: [...] },
        { key: 'validation', components: [...] },
        { key: 'api', components: [...] },
        { key: 'conditional', components: [...] },
        { key: 'logic', components: [...] }
      ]
    },
    language: 'en',
    i18n: {}
  }}
/>
```

#### Capabilities

- Drag-and-drop field placement
- Visual property editor (no JSON knowledge required)
- Field preview in real-time
- Validation rule builder
- Conditional logic visual editor
- Layout grid system
- Form templates library
- Export/import JSON
- Undo/redo functionality
- Field search and filtering

**User Experience**: **EXCELLENT** - Department POCs can create forms without coding.

### 3.2 Target: form-js Form Builder

#### Official Support

```text
STATUS: NO OFFICIAL VISUAL FORM BUILDER

The form-js library provides:
- Form renderer (display forms)
- Form player (fill out forms)
- Programmatic form creation (JavaScript API)

What's MISSING:
- Drag-and-drop form designer
- Visual property editor
- Field palette
- Layout tools
- No-code form creation
```

#### Available Options

**Option A: Build Custom Visual Builder (Full-Featured)**

```typescript
// Estimated Effort: 6-8 weeks (240-320 hours)

class CustomFormBuilder extends React.Component {

  // Features to Build:
  features = [
    'Drag-and-drop canvas',                // 2 weeks
    'Field palette (left sidebar)',        // 1 week
    'Property editor (right sidebar)',     // 2 weeks
    'Form preview mode',                   // 1 week
    'JSON import/export',                  // 0.5 weeks
    'Validation rule builder',             // 1.5 weeks
    'Conditional logic editor',            // 2 weeks
    'Layout grid system',                  // 1 week
    'Undo/redo functionality',             // 1 week
    'Field templates library',             // 1 week
    'Form versioning',                     // 1 week
    'Responsive design',                   // 1 week
    'Accessibility compliance',            // 1 week
    'Unit and integration tests'           // 2 weeks
  ];

  // Technology Stack:
  technologies = [
    'React DnD or react-beautiful-dnd',    // Drag-and-drop
    'Monaco Editor or CodeMirror',         // JSON editor
    'Zod or Yup',                          // Validation schema
    'Zustand or Redux',                    // State management
    'Tailwind CSS + shadcn/ui',            // UI components
    'form-js',                             // Form rendering
    'Immer',                               // Immutable state
    'React Testing Library'                // Testing
  ];

  render() {
    return (
      <div className="form-builder">
        <FieldPalette onDragStart={this.handleDragStart} />
        <Canvas fields={this.state.fields} onDrop={this.handleDrop} />
        <PropertyEditor selectedField={this.state.selectedField} />
      </div>
    );
  }
}
```

**Effort**: 6-8 weeks (1.5-2 months)
**Risk**: High - Complex UI interactions, testing, accessibility
**Maintenance**: Ongoing - Bug fixes, feature requests, updates

**Option B: JSON Editor (Monaco Editor)**

```typescript
// Estimated Effort: 1-2 weeks (40-80 hours)

import MonacoEditor from '@monaco-editor/react';

function FormJsonEditor() {
  const [formJson, setFormJson] = useState('{}');
  const [errors, setErrors] = useState([]);

  const validateJson = (json: string) => {
    try {
      const parsed = JSON.parse(json);
      // Validate against form-js schema
      const validationErrors = validateFormJsSchema(parsed);
      setErrors(validationErrors);
    } catch (err) {
      setErrors([{ message: 'Invalid JSON syntax' }]);
    }
  };

  return (
    <div className="json-editor">
      <MonacoEditor
        height="80vh"
        language="json"
        value={formJson}
        onChange={(value) => {
          setFormJson(value);
          validateJson(value);
        }}
        options={{
          minimap: { enabled: true },
          formatOnPaste: true,
          formatOnType: true,
          autoIndent: 'full',
          quickSuggestions: true,
          suggest: {
            showFields: true,
            showConstants: true
          }
        }}
      />
      <ErrorPanel errors={errors} />
      <FormPreview formJson={formJson} />
    </div>
  );
}
```

**Effort**: 1-2 weeks
**Risk**: Low - Proven editor component
**Limitation**: Requires JSON/technical knowledge - **NOT suitable for POCs**

**Option C: Use form-js Playground (External Tool)**

```text
Option: Use bpmn-io's form-js playground as external tool

Workflow:
1. POC creates form in form-js playground (https://demo.bpmn.io/form)
2. Export JSON schema
3. Upload to Werkflow
4. Deploy to Flowable

Drawbacks:
- Not integrated into Werkflow
- No Werkflow-specific field types
- No process variable integration
- No form versioning
- No workflow context
- Extra step for POCs (bad UX)
```

**Effort**: 0 weeks (use external tool)
**Risk**: Low - Tool already exists
**Limitation**: Poor UX, breaks self-service vision

### 3.3 Form Builder Assessment

| Option | Effort | Risk | POC Usability | Integration | Maintenance | Recommendation |
|--------|--------|------|---------------|-------------|-------------|----------------|
| Form.io Builder (Current) | 0 weeks | None | Excellent | Perfect | Vendor-supported | **KEEP** |
| Custom Builder (Option A) | 6-8 weeks | High | Good (after dev) | Perfect | High (ongoing) | **NOT RECOMMENDED** |
| JSON Editor (Option B) | 1-2 weeks | Low | Poor (technical) | Good | Low | **NOT SUITABLE** |
| External Playground (Option C) | 0 weeks | Low | Fair | Poor | None | **NOT RECOMMENDED** |

**Assessment**: **BLOCKER** - No viable visual form builder alternative exists. Building custom form builder requires 6-8 weeks of dedicated work, introducing high risk and ongoing maintenance burden.

---

## 4. Renderer Integration

### 4.1 Current: FormRenderer (Form.io)

```typescript
// Current FormRenderer Component
import { Form } from '@formio/react';
import 'formiojs/dist/formio.full.min.css';

export default function FormRenderer({
  formKey,
  taskId,
  onComplete,
  initialData = {},
  readOnly = false
}: FormRendererProps) {

  // Fetch form definition from backend
  const { data: formDef } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => getFormDefinition(formKey)
  });

  const formSchema = JSON.parse(formDef.formJson);

  // Handle form submission
  const handleSubmit = (submission: any) => {
    completeMutation.mutate(submission.data);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{formSchema.title}</CardTitle>
      </CardHeader>
      <CardContent>
        <Form
          form={formSchema}
          submission={{ data: initialData }}
          onSubmit={handleSubmit}
          options={{
            readOnly,
            noAlerts: true,
            i18n: { en: { ... } }
          }}
        />
      </CardContent>
    </Card>
  );
}
```

**Features**:
- Automatic form rendering from JSON
- Built-in validation display
- Read-only mode support
- i18n support
- Custom CSS integration
- Event handling (onChange, onSubmit, onError)
- File upload handling
- Signature pad integration
- Date picker localization
- Currency formatting
- Auto-save (draft) support

### 4.2 Target: form-js Renderer

```typescript
// form-js Renderer Integration
import { Form } from '@bpmn-io/form-js';
import '@bpmn-io/form-js/dist/assets/form-js.css';

export default function FormJsRenderer({
  formKey,
  taskId,
  onComplete,
  initialData = {},
  readOnly = false
}: FormRendererProps) {

  const formRef = useRef(null);

  useEffect(() => {
    if (formRef.current) {
      const form = new Form({
        container: formRef.current,
        schema: formSchema,
        data: initialData,
        readOnly: readOnly
      });

      // Handle form submission
      form.on('submit', (event) => {
        const { data, errors } = event;
        if (errors) {
          console.error('Validation errors:', errors);
          return;
        }
        handleSubmit(data);
      });

      // Manual validation handling (not automatic)
      form.on('changed', (event) => {
        const { data, errors } = form.validate();
        // Display errors manually (no built-in UI)
      });

      return () => form.destroy();
    }
  }, [formSchema, initialData, readOnly]);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{formSchema.title || formKey}</CardTitle>
      </CardHeader>
      <CardContent>
        <div ref={formRef} className="form-js-container" />
        {/* Manual error display */}
        {errors && <ErrorDisplay errors={errors} />}
      </CardContent>
    </Card>
  );
}
```

**Features**:
- Basic form rendering
- Simple validation
- Programmatic API
- Lightweight (smaller bundle size)

**Missing Features**:
- No built-in validation error display (manual implementation required)
- No file upload handling (custom component needed)
- No signature pad (custom component needed)
- No currency formatting (plain number input)
- No auto-save/draft support
- Limited i18n support
- No built-in date localization

### 4.3 Integration Effort

```typescript
// Additional Components Required for form-js

// 1. Custom Error Display Component
function ValidationErrorDisplay({ errors }: { errors: ValidationError[] }) {
  return (
    <div className="validation-errors">
      {errors.map((error, idx) => (
        <Alert key={idx} variant="destructive">
          <AlertTitle>{error.field}</AlertTitle>
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      ))}
    </div>
  );
}
// Effort: 1-2 days

// 2. Custom File Upload Component
function FileUploadField({ field, value, onChange }: FieldProps) {
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    // Upload to backend
    const urls = await uploadFiles(files);
    onChange(urls);
  };

  return (
    <div className="file-upload-field">
      <Input type="file" multiple onChange={handleFileChange} />
      <FileList files={value} onRemove={handleRemove} />
    </div>
  );
}
// Effort: 2-3 days

// 3. Custom Signature Component
function SignatureField({ field, value, onChange }: FieldProps) {
  // Use react-signature-canvas or similar
  return <SignatureCanvas onEnd={handleSignature} />;
}
// Effort: 1-2 days

// 4. Custom Currency Component
function CurrencyField({ field, value, onChange }: FieldProps) {
  // Use react-currency-input-field or similar
  return (
    <CurrencyInput
      value={value}
      onValueChange={(value) => onChange(value)}
      prefix="$"
      decimalsLimit={2}
      decimalSeparator="."
      groupSeparator=","
    />
  );
}
// Effort: 1-2 days

// 5. Custom DataGrid Component
function DataGridField({ field, value, onChange }: FieldProps) {
  // Build editable table
  return <EditableTable data={value} onChange={onChange} />;
}
// Effort: 3-5 days

// 6. Custom Conditional Logic Handler
function ConditionalFieldRenderer({ field, formData }: Props) {
  const shouldShow = evaluateCondition(field.conditional, formData);
  if (!shouldShow) return null;
  return <FieldRenderer field={field} />;
}
// Effort: 2-3 days

// TOTAL CUSTOM COMPONENT EFFORT: 10-17 days (2-3.5 weeks)
```

**Assessment**: **HIGH EFFORT** - Renderer integration requires building 6+ custom components to match current Form.io functionality. Estimated 2-3.5 weeks of development.

---

## 5. Timeline & Effort Analysis

### 5.1 Migration Effort Breakdown

```typescript
interface MigrationPhase {
  phase: string;
  tasks: string[];
  effort: string;  // in days
  risk: 'Low' | 'Medium' | 'High';
  dependencies: string[];
}

const migrationPhases: MigrationPhase[] = [
  {
    phase: 'Phase 1: Analysis & Planning',
    tasks: [
      'Analyze all 8 existing form templates',
      'Identify incompatibilities',
      'Design schema transformation approach',
      'Plan custom component architecture',
      'Create migration project plan'
    ],
    effort: '3-5 days',
    risk: 'Low',
    dependencies: []
  },
  {
    phase: 'Phase 2: Schema Transformation Layer',
    tasks: [
      'Build FormioToFormJsTransformer service',
      'Implement field type mappings',
      'Handle validation transformation',
      'Convert conditional logic',
      'Build incompatibility detection',
      'Unit tests for transformer'
    ],
    effort: '5-7 days',
    risk: 'Medium',
    dependencies: ['Phase 1']
  },
  {
    phase: 'Phase 3: Custom Component Development',
    tasks: [
      'Build FileUploadField component',
      'Build SignatureField component',
      'Build CurrencyField component',
      'Build DataGridField component',
      'Build ConditionalFieldRenderer',
      'Build ValidationErrorDisplay',
      'Integration tests for components'
    ],
    effort: '10-17 days',
    risk: 'High',
    dependencies: ['Phase 1']
  },
  {
    phase: 'Phase 4: Form Builder Development',
    tasks: [
      'Build drag-and-drop canvas',
      'Build field palette',
      'Build property editor',
      'Build preview mode',
      'Build validation rule builder',
      'Build conditional logic editor',
      'Build form templates library',
      'Integration testing'
    ],
    effort: '30-40 days',
    risk: 'Very High',
    dependencies: ['Phase 3']
  },
  {
    phase: 'Phase 5: Renderer Integration',
    tasks: [
      'Replace FormRenderer component',
      'Integrate custom components',
      'Handle validation display',
      'Implement error handling',
      'Add loading states',
      'Integration tests'
    ],
    effort: '3-5 days',
    risk: 'Medium',
    dependencies: ['Phase 3']
  },
  {
    phase: 'Phase 6: Form Migration',
    tasks: [
      'Transform 8 existing form templates',
      'Test each migrated form',
      'Fix transformation issues',
      'Update form documentation',
      'User acceptance testing'
    ],
    effort: '5-8 days',
    risk: 'High',
    dependencies: ['Phase 2', 'Phase 5']
  },
  {
    phase: 'Phase 7: Testing & Validation',
    tasks: [
      'End-to-end workflow testing',
      'Form submission testing',
      'Validation testing',
      'Conditional logic testing',
      'File upload testing',
      'Signature testing',
      'Regression testing',
      'Performance testing',
      'Accessibility testing'
    ],
    effort: '5-7 days',
    risk: 'Medium',
    dependencies: ['Phase 6']
  },
  {
    phase: 'Phase 8: Documentation & Training',
    tasks: [
      'Update form builder documentation',
      'Create POC training materials',
      'Update API documentation',
      'Create troubleshooting guide',
      'Record video tutorials'
    ],
    effort: '3-5 days',
    risk: 'Low',
    dependencies: ['Phase 7']
  }
];

// TOTAL EFFORT CALCULATION
const totalEffort = {
  minimum: 64 days,   // ~12.8 weeks (3.2 months)
  maximum: 94 days,   // ~18.8 weeks (4.7 months)
  realistic: 79 days  // ~15.8 weeks (4 months)
};
```

### 5.2 Phase 4-6 Timeline Fit Analysis

```typescript
// Current Phase 4-6 Schedule
const phase4to6Schedule = {
  duration: '5 weeks',
  criticalPath: [
    'Service Registry Backend (Week 1-2)',
    'Keycloak RBAC Integration (Week 2-3)',
    'CapEx Workflow Migration (Week 3-4)',
    'Task UI Development (Week 4-5)',
    'Integration Testing (Week 5)'
  ],
  teamCapacity: {
    backend: '1 developer (full-time)',
    frontend: '1 developer (full-time)',
    qa: '0.5 developer',
    techLead: '0.25 developer'
  },
  buffer: 'None - Already at 100% capacity'
};

// Form.io to form-js Migration Requirements
const formMigrationRequirements = {
  duration: '15-19 weeks (4-4.7 months)',
  teamRequired: {
    backend: '0.5 developer (schema transformation, custom components)',
    frontend: '1.5 developers (form builder, renderer, components)',
    qa: '0.5 developer (testing)',
    techLead: '0.25 developer (architecture, reviews)'
  },
  conflicts: {
    backend: 'Service Registry, RBAC, CapEx Migration already scheduled',
    frontend: 'Service Registry UI, Task UI, RBAC UI already scheduled',
    qa: 'Integration testing for Phase 4-6 already scheduled',
    techLead: 'Phase 4-6 coordination already committed'
  }
};

// Conflict Analysis
const conflicts = [
  {
    conflict: 'Frontend Developer Capacity',
    current: 'Service Registry UI, Keycloak Login, Task UI (100% allocated)',
    additional: 'Form Builder + Custom Components (150% required)',
    result: 'BLOCKER: Cannot add 50% more work to same developer'
  },
  {
    conflict: 'Backend Developer Capacity',
    current: 'Service Registry, RBAC, CapEx Migration (100% allocated)',
    additional: 'Schema Transformer, Custom Component APIs (50% required)',
    result: 'BLOCKER: Cannot add 50% more work to same developer'
  },
  {
    conflict: 'Timeline',
    current: '5 weeks for Phase 4-6',
    additional: '16-19 weeks for form-js migration',
    result: 'BLOCKER: Migration takes 3-4x longer than Phase 4-6'
  },
  {
    conflict: 'Risk',
    current: 'Phase 4-6 already has medium-high risk',
    additional: 'form-js migration adds very high risk',
    result: 'RISK: Compounding risks unacceptable'
  }
];

// Recommendation
const recommendation = {
  decision: 'DEFER',
  reasoning: [
    'Phase 4-6 team at 100% capacity - no room for additional work',
    'Form.io replacement requires 3-4x the time of Phase 4-6',
    'Form.io currently working - no blocking issues',
    'form-js migration adds very high risk to critical path',
    'ROI unclear - Form.io provides better features'
  ],
  alternativePath: 'Keep Form.io for Phase 4-6, revisit in Phase 7'
};
```

**Assessment**: **CANNOT FIT IN PHASE 4-6** - Team already at capacity, form-js migration would derail critical path work.

### 5.3 Parallel Track Feasibility

```typescript
// Can form-js migration run in parallel with Phase 4-6?

const parallelTrackAnalysis = {
  question: 'Can we add a dedicated team for form-js migration?',

  additionalTeamRequired: {
    frontend: '1.5 developers (form builder, custom components)',
    backend: '0.5 developer (schema transformer)',
    qa: '0.5 developer',
    techLead: '0.25 developer',
    total: '2.75 developers (effectively 3 people)'
  },

  coordination: {
    dependencies: [
      'Must coordinate with main team on schema changes',
      'Must not break existing Form.io workflows',
      'Must align on UI/UX patterns with admin portal',
      'Must integrate with Flowable process variables',
      'Must sync on form deployment approach'
    ],
    coordinationOverhead: '20-30% of tech lead time',
    riskOfConflicts: 'High'
  },

  businessCase: {
    cost: '3 additional developers x 4 months = 12 person-months',
    benefit: 'Replace working Form.io with less-featured form-js',
    roi: 'Negative - no clear business value',
    justification: 'None - Form.io works well and is more capable'
  },

  recommendation: {
    decision: 'NOT RECOMMENDED',
    reasons: [
      'High cost (12 person-months) for questionable benefit',
      'Coordination overhead with main team',
      'Risk of introducing bugs in working forms',
      'No business driver for replacement',
      'Better to invest in Phase 7 features instead'
    ]
  }
};
```

**Assessment**: **NOT VIABLE** - Even with dedicated team, parallel track doesn't make business sense. High cost, high risk, low benefit.

---

## 6. Risk Assessment

### 6.1 Technical Risks

| Risk ID | Risk | Probability | Impact | Severity | Mitigation |
|---------|------|-------------|--------|----------|------------|
| **R1** | form-js missing critical field types (file, signature, currency, datagrid) | High | High | **CRITICAL** | Build 6+ custom components (2-3.5 weeks effort) |
| **R2** | form-js validation insufficient for business rules | High | High | **CRITICAL** | Build custom validation engine or fallback to backend validation only |
| **R3** | form-js conditional logic too simple for complex workflows | High | Medium | **HIGH** | Simplify workflow logic OR build custom conditional engine |
| **R4** | No visual form builder - POCs cannot self-serve | High | High | **CRITICAL** | Build custom form builder (6-8 weeks) OR use JSON editor (breaks self-service vision) |
| **R5** | Schema transformation introduces bugs in migrated forms | High | High | **CRITICAL** | Extensive testing (5-7 days), parallel run with Form.io, rollback plan |
| **R6** | Custom components have bugs/poor UX | Medium | Medium | **MEDIUM** | Thorough testing, user feedback, iterations |
| **R7** | form-js library has breaking changes in future updates | Low | Medium | **LOW** | Pin version, monitor changelog, plan upgrade testing |
| **R8** | Maintenance burden for custom components | Medium | Medium | **MEDIUM** | Good documentation, unit tests, code reviews |
| **R9** | Users resist change from familiar Form.io UI | Medium | Low | **LOW** | Change management, training, gradual rollout |
| **R10** | Integration with Flowable process variables breaks | Low | High | **MEDIUM** | Integration tests, validate all form-to-process bindings |

### 6.2 Business Risks

| Risk ID | Risk | Impact | Likelihood | Mitigation |
|---------|------|--------|------------|------------|
| **B1** | Project timeline extends beyond Phase 4-6 | High | Very High | Defer to Phase 7 or cancel |
| **B2** | Team bandwidth diverted from critical Phase 4-6 work | High | Very High | Do not start migration during Phase 4-6 |
| **B3** | Existing form workflows break during migration | High | Medium | Phased migration, parallel run, rollback plan |
| **B4** | POCs cannot create forms (no visual builder) | High | High | Must build custom builder (6-8 weeks) |
| **B5** | User satisfaction drops due to missing features | Medium | High | Communicate changes, gather feedback early |
| **B6** | Cost overruns (team, timeline, budget) | High | High | Strict scope control, phased approach, kill switch |
| **B7** | No ROI - replacement doesn't provide value | High | Very High | **DO NOT PROCEED** - No business justification |
| **B8** | Support burden increases due to form issues | Medium | Medium | Good testing, documentation, training |

### 6.3 Risk Summary

```typescript
const riskSummary = {
  criticalRisks: 5,   // R1, R2, R4, R5 (Technical), B2 (Business)
  highRisks: 3,       // R3 (Technical), B1, B3, B4, B5, B6, B7 (Business)
  mediumRisks: 4,     // R6, R8, R10 (Technical), B8 (Business)
  lowRisks: 2,        // R7, R9

  overallRiskLevel: 'VERY HIGH',

  recommendation: {
    decision: 'DO NOT PROCEED',
    primaryReasons: [
      '5 CRITICAL risks that are difficult to mitigate',
      'No clear business value (Risk B7)',
      'Would derail Phase 4-6 critical path (Risk B2)',
      'Timeline not feasible within Phase 4-6 (Risk B1)',
      'Missing visual form builder is a blocker (Risk R4)'
    ]
  }
};
```

**Assessment**: **VERY HIGH RISK** - Multiple critical risks with difficult mitigation. Risk profile unacceptable for a non-critical enhancement.

---

## 7. Implementation Strategy (IF Approved)

### 7.1 Phased Approach

```typescript
// IF management decides to proceed (NOT RECOMMENDED)

const implementationStrategy = {

  // Phase 0: Pre-Flight Checks (1 week)
  phase0: {
    tasks: [
      'Get stakeholder approval for 4-5 month timeline',
      'Allocate dedicated team (3 developers)',
      'Get budget approval',
      'Communicate to POCs that form builder will be temporarily unavailable',
      'Set up project tracking and governance'
    ],
    goNoGo: 'If cannot secure resources and timeline, CANCEL project'
  },

  // Phase 1: MVP Custom Form Builder (6-8 weeks)
  phase1: {
    approach: 'Build minimal visual form builder',
    scope: [
      'Basic drag-and-drop (no complex layouts)',
      'Essential field types only (text, number, date, select)',
      'Simple validation (required, min, max)',
      'JSON export/import',
      'Form preview'
    ],
    deferredFeatures: [
      'Complex layouts (columns, tabs)',
      'Advanced field types (file, signature, currency)',
      'Conditional logic',
      'Data grids',
      'Custom validation rules'
    ],
    milestones: [
      'Week 2: Drag-and-drop working',
      'Week 4: Property editor functional',
      'Week 6: Form preview working',
      'Week 8: MVP ready for testing'
    ],
    goNoGo: 'If MVP not ready by Week 8, CANCEL or delay'
  },

  // Phase 2: Custom Components (2-3 weeks)
  phase2: {
    parallel: 'Run in parallel with Phase 1',
    components: [
      'FileUploadField (priority: critical)',
      'CurrencyField (priority: critical)',
      'SignatureField (priority: medium)',
      'ValidationErrorDisplay (priority: critical)',
      'ConditionalFieldRenderer (priority: low - defer)'
    ],
    milestones: [
      'Week 2: FileUpload and Currency ready',
      'Week 3: Signature and ErrorDisplay ready'
    ],
    goNoGo: 'If critical components not ready by Week 3, DELAY migration'
  },

  // Phase 3: Pilot Migration (2 weeks)
  phase3: {
    approach: 'Migrate 2 simple forms as pilot',
    pilotForms: [
      'training-request-form (simple, low risk)',
      'asset-request-form (medium complexity)'
    ],
    parallelRun: 'Keep Form.io versions active as fallback',
    userGroup: '10-20 pilot users',
    successCriteria: [
      'Forms render correctly',
      'Validation works',
      'File uploads work',
      'No critical bugs',
      'User satisfaction > 3.5/5'
    ],
    goNoGo: 'If pilot fails, ROLLBACK and reassess'
  },

  // Phase 4: Gradual Migration (3-4 weeks)
  phase4: {
    approach: 'Migrate remaining 6 forms incrementally',
    schedule: [
      'Week 1: leave-request-form, timesheet-form',
      'Week 2: expense-reimbursement-form (complex)',
      'Week 3: capex-approval-form (very complex)',
      'Week 4: employee-onboarding-form, exit-clearance-form'
    ],
    strategy: 'One form at a time, validate before next',
    rollback: 'Keep Form.io version for 1 month after migration',
    goNoGo: 'If any form fails, PAUSE and fix before continuing'
  },

  // Phase 5: Stabilization & Decommission (2 weeks)
  phase5: {
    tasks: [
      'Monitor all migrated forms for 2 weeks',
      'Fix any bugs discovered',
      'Gather user feedback',
      'Update documentation',
      'Train POCs on new form builder',
      'Decommission Form.io (if all stable)',
      'Archive Form.io schemas as backup'
    ],
    goNoGo: 'If critical bugs found, REVERT to Form.io'
  },

  totalDuration: '15-19 weeks (4-5 months)',
  totalEffort: '12-15 person-months',

  killSwitch: {
    condition: 'If timeline exceeds 6 months OR critical bugs found',
    action: 'Abandon migration, keep Form.io permanently'
  }
};
```

### 7.2 Rollback Strategy

```typescript
const rollbackStrategy = {

  // Scenario 1: Phase 1 (Form Builder) fails
  scenario1: {
    trigger: 'Form builder MVP not usable by Week 8',
    action: [
      'Stop all development',
      'Abandon form-js migration',
      'Keep Form.io as permanent solution',
      'Write post-mortem document'
    ],
    impact: 'Sunk cost: 8-10 weeks of development effort'
  },

  // Scenario 2: Pilot Migration fails
  scenario2: {
    trigger: 'Pilot forms have critical bugs or poor UX',
    action: [
      'Rollback pilot forms to Form.io versions',
      'Investigate root cause',
      'Decision point: Fix and retry OR abandon'
    ],
    impact: 'Sunk cost: 10-13 weeks of effort'
  },

  // Scenario 3: Gradual Migration fails
  scenario3: {
    trigger: 'Complex forms (expense, CapEx) cannot be migrated',
    action: [
      'Keep migrated simple forms on form-js',
      'Keep complex forms on Form.io',
      'Run hybrid approach (both libraries)',
      'Re-evaluate in 6 months'
    ],
    impact: 'Dual maintenance burden, technical debt'
  },

  // Scenario 4: Post-Migration Issues
  scenario4: {
    trigger: 'After decommission, critical bugs found',
    action: [
      'Restore Form.io from backup',
      'Migrate forms back to Form.io',
      'Keep form-js as alternative renderer only'
    ],
    impact: 'User disruption, loss of confidence, wasted effort'
  },

  // Rollback Readiness
  rollbackReadiness: [
    'Keep Form.io library in dependencies (do not uninstall)',
    'Keep Form.io schemas in database (do not delete)',
    'Keep FormBuilder and FormRenderer components (do not remove)',
    'Document rollback procedure',
    'Test rollback procedure before each phase',
    'Set rollback deadline (e.g., "if not stable by Week 20, rollback")'
  ]
};
```

**Assessment**: Even with phased approach and rollback strategy, risk remains very high. Not recommended.

---

## 8. Recommendation Matrix

### 8.1 Decision Framework

| Criteria | Form.io (Current) | form-js (Target) | Winner |
|----------|-------------------|-------------------|--------|
| **Features** |
| Field Types | 30+ types | 12 types | Form.io |
| Validation | Comprehensive | Basic | Form.io |
| Conditional Logic | Advanced | Simple | Form.io |
| File Upload | Built-in | Not supported | Form.io |
| Signature | Built-in | Not supported | Form.io |
| Currency | Built-in | Not supported | Form.io |
| Data Grids | Built-in | Not supported | Form.io |
| Visual Form Builder | Excellent | None (must build) | Form.io |
| **Technical** |
| Bundle Size | Large (~500KB) | Small (~150KB) | form-js |
| Performance | Good | Good | Tie |
| Documentation | Excellent | Basic | Form.io |
| Community | Large | Small | Form.io |
| Maintenance | Vendor-supported | Open-source only | Form.io |
| **Integration** |
| Flowable Integration | Proven (working) | Unknown | Form.io |
| Schema Format | Form.io v4 | Custom | Form.io |
| Migration Path | N/A | Complex | Form.io |
| **Business** |
| Current Status | Working | Not implemented | Form.io |
| POC Usability | Excellent | Poor (no builder) | Form.io |
| Learning Curve | Low (visual) | High (JSON) | Form.io |
| Support Burden | Low | High (custom code) | Form.io |
| License Cost | Free (MIT) | Free (MIT) | Tie |
| **Risk & Effort** |
| Implementation Effort | 0 weeks | 15-19 weeks | Form.io |
| Risk Level | None | Very High | Form.io |
| Team Capacity | Available | Not available | Form.io |
| Timeline Fit | N/A | Does not fit | Form.io |

**Score**: Form.io wins 22 out of 25 criteria

### 8.2 Final Recommendation

```typescript
const finalRecommendation = {
  decision: 'DEFER - DO NOT REPLACE Form.io with form-js',

  recommendation: 'KEEP Form.io',

  primaryReasons: [
    {
      reason: 'Form.io provides significantly more features',
      impact: 'Critical',
      details: 'form-js missing file upload, signature, currency, data grids, conditional logic'
    },
    {
      reason: 'No visual form builder for form-js',
      impact: 'Blocker',
      details: 'Building custom form builder requires 6-8 weeks, breaks POC self-service vision'
    },
    {
      reason: 'Timeline incompatible with Phase 4-6',
      impact: 'Critical',
      details: 'Migration takes 15-19 weeks vs. Phase 4-6 duration of 5 weeks. Cannot fit.'
    },
    {
      reason: 'Team at 100% capacity',
      impact: 'Blocker',
      details: 'No bandwidth for additional 12-15 person-months of work'
    },
    {
      reason: 'Very high risk, unclear ROI',
      impact: 'Critical',
      details: '5 critical risks, no clear business benefit'
    },
    {
      reason: 'Form.io currently working well',
      impact: 'Important',
      details: 'No blocking issues, users satisfied, stable integration'
    }
  ],

  alternativePath: {
    phase: 'Phase 7 (Post-Phase 6)',
    condition: 'Revisit IF:',
    conditions: [
      'bpmn-io team releases official visual form builder',
      'Form.io licensing becomes problematic (commercial costs)',
      'Business requirements change to favor lightweight forms',
      'Team has 3-5 months of available capacity',
      'Strong business case emerges (e.g., integration with bpmn-js benefits)'
    ],
    action: 'Re-evaluate feasibility with updated form-js capabilities'
  },

  immediateActions: [
    'Document decision in ADR (Architecture Decision Record)',
    'Communicate to stakeholders that Form.io will remain',
    'Close form-js exploration as "Not Feasible at this time"',
    'Focus team on Phase 4-6 critical path work',
    'Revisit in Phase 7 planning'
  ],

  architectureDecision: {
    title: 'ADR-007: Keep Form.io for Form Management',
    status: 'ACCEPTED',
    date: '2025-11-24',
    context: 'Evaluated replacing Form.io with form-js',
    decision: 'Continue using Form.io for all form workflows',
    consequences: {
      positive: [
        'No disruption to working forms',
        'Team can focus on Phase 4-6 critical work',
        'POCs continue to have visual form builder',
        'All features remain available',
        'Zero migration risk'
      ],
      negative: [
        'Larger bundle size (~500KB vs. ~150KB)',
        'Potential future licensing concerns (unlikely for MIT license)',
        'Not from bpmn-io team (would be nice for consistency)'
      ]
    },
    reviewDate: 'Phase 7 planning (after Phase 6 completion)'
  }
};
```

---

## 9. Conclusion

### Summary

After comprehensive analysis across 7 dimensions (features, schema, builder, renderer, timeline, risk, strategy), the recommendation is clear: **DO NOT REPLACE Form.io with form-js during Phase 4-6**.

### Key Findings

1. **Feature Gap**: form-js missing 11 critical field types (file, signature, currency, data grids, etc.)
2. **No Visual Builder**: form-js has no official visual form builder - would require 6-8 weeks to build custom
3. **Timeline Mismatch**: Migration requires 15-19 weeks vs. Phase 4-6 duration of 5 weeks
4. **Team Capacity**: Team at 100% capacity, no bandwidth for 12-15 person-months of additional work
5. **High Risk**: 5 critical risks with difficult mitigation
6. **No ROI**: Form.io works well, unclear business benefit from replacement
7. **Form.io Wins**: Form.io superior in 22 out of 25 evaluation criteria

### Decision

**DEFER** - Keep Form.io for Phase 4-6 (and likely beyond)

### Action Items

- [ ] Document decision in Architecture Decision Record (ADR-007)
- [ ] Communicate to stakeholders
- [ ] Close form-js exploration
- [ ] Focus on Phase 4-6 critical path
- [ ] Revisit in Phase 7 IF conditions change

### Future Re-Evaluation Triggers

Revisit form-js replacement in Phase 7 IF:
- bpmn-io releases official visual form builder
- Form.io licensing becomes problematic
- Business requirements favor lightweight forms
- Team has 3-5 months available capacity
- Strong business case emerges

---

**Document Version**: 1.0
**Date**: 2025-11-24
**Status**: Analysis Complete
**Recommendation**: DEFER - Keep Form.io
**Review Date**: Phase 7 Planning

**Prepared By**: Architecture Team
**Approved By**: Tech Lead (pending)
**Distribution**: Development Team, Product Owner, Stakeholders

---

## Appendix A: Forms Inventory

Current Form Templates in Werkflow:

| Form Name | Complexity | Field Count | File Upload | Signature | Currency | Data Grid | Conditional Logic |
|-----------|------------|-------------|-------------|-----------|----------|-----------|-------------------|
| Leave Request | Medium | 8 | Yes | No | No | No | Yes (leave type) |
| Expense Reimbursement | High | 12 | Yes | No | Yes | Yes (items) | Yes (expense type) |
| Training Request | Low | 6 | No | No | No | No | No |
| Asset Request | Medium | 9 | Yes | No | No | No | Yes (asset type) |
| CapEx Approval | Very High | 15 | Yes | Yes | Yes | No | Yes (DOA level) |
| Timesheet | High | 10 | No | No | No | Yes (hours) | Yes (project) |
| Employee Onboarding | Very High | 25 | Yes | Yes | No | No | Yes (multi-step) |
| Exit Clearance | High | 18 | No | Yes | No | No | Yes (department) |

**Total Forms**: 8
**Forms with File Upload**: 5 (62.5%)
**Forms with Signature**: 3 (37.5%)
**Forms with Currency**: 2 (25%)
**Forms with Data Grids**: 2 (25%)
**Forms with Conditional Logic**: 6 (75%)

**Conclusion**: Majority of forms use features NOT supported by form-js.

---

## Appendix B: Effort Estimation Detail

| Phase | Task | Min (days) | Max (days) | Realistic (days) |
|-------|------|------------|------------|------------------|
| Analysis | Form analysis | 2 | 3 | 2.5 |
| Analysis | Incompatibility detection | 1 | 2 | 1.5 |
| Schema | Build transformer | 3 | 5 | 4 |
| Schema | Field mapping | 2 | 2 | 2 |
| Components | File upload | 2 | 3 | 2.5 |
| Components | Signature | 1 | 2 | 1.5 |
| Components | Currency | 1 | 2 | 1.5 |
| Components | Data grid | 3 | 5 | 4 |
| Components | Conditional renderer | 2 | 3 | 2.5 |
| Components | Error display | 1 | 2 | 1.5 |
| Form Builder | Drag-drop canvas | 10 | 14 | 12 |
| Form Builder | Field palette | 3 | 5 | 4 |
| Form Builder | Property editor | 8 | 12 | 10 |
| Form Builder | Preview mode | 3 | 5 | 4 |
| Form Builder | Validation builder | 4 | 6 | 5 |
| Form Builder | Conditional editor | 5 | 8 | 6.5 |
| Form Builder | Templates library | 2 | 3 | 2.5 |
| Form Builder | Testing | 5 | 7 | 6 |
| Integration | Renderer replacement | 2 | 3 | 2.5 |
| Integration | Custom components | 1 | 2 | 1.5 |
| Migration | Transform 8 forms | 5 | 8 | 6.5 |
| Testing | End-to-end tests | 3 | 5 | 4 |
| Testing | Regression tests | 2 | 2 | 2 |
| Documentation | User docs | 2 | 3 | 2.5 |
| Documentation | Training materials | 1 | 2 | 1.5 |
| **TOTAL** | | **64** | **94** | **79** |

**Total**: 64-94 days (12.8-18.8 weeks) **Realistic**: 79 days (15.8 weeks ~ 4 months)

---

**END OF DOCUMENT**
