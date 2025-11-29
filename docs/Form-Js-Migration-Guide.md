# Form.js Migration Guide

## Overview

This document describes the migration from Flowable form definitions to bpmn-io form-js library for the Werkflow HR platform.

## Why Form.js?

### Benefits

1. **Modern JSON-based Schema**: Forms are defined as JSON, making them easy to version control, diff, and migrate
2. **Visual Editor**: Built-in drag-and-drop editor for creating forms without coding
3. **Type Safety**: Strong TypeScript support with proper type definitions
4. **Validation**: Built-in validation rules with custom validation support
5. **Conditional Logic**: Show/hide fields based on form data
6. **Framework Agnostic**: Can be used with React, Angular, Vue, or vanilla JavaScript
7. **Open Source**: Actively maintained by bpmn-io team

### Comparison with Flowable Forms

| Feature | Flowable Forms | Form.js |
|---------|---------------|---------|
| Schema Format | XML | JSON |
| Visual Editor | Limited | Full-featured |
| TypeScript Support | Basic | Excellent |
| Custom Components | Complex | Easy to extend |
| Validation | Basic | Advanced |
| Conditional Logic | Limited | Powerful |
| Documentation | Moderate | Comprehensive |

## Installation

### NPM Packages

```bash
npm install @bpmn-io/form-js @bpmn-io/form-js-editor @bpmn-io/form-js-viewer
```

### Dependencies Installed

- `@bpmn-io/form-js`: Core form library
- `@bpmn-io/form-js-editor`: Visual form editor
- `@bpmn-io/form-js-viewer`: Form viewer component

## Form Schema Structure

### Basic Schema

```json
{
  "type": "default",
  "id": "form-id",
  "schemaVersion": 9,
  "exporter": {
    "name": "werkflow form-js converter",
    "version": "1.0.0"
  },
  "components": [
    {
      "type": "textfield",
      "id": "Field_username",
      "key": "username",
      "label": "Username",
      "validate": {
        "required": true,
        "minLength": 3,
        "maxLength": 50
      }
    }
  ]
}
```

### Component Types

#### Input Components

- `textfield`: Single-line text input
- `textarea`: Multi-line text input
- `number`: Numeric input with decimal support
- `datetime`: Date and time picker
- `checkbox`: Boolean checkbox
- `radio`: Radio button group
- `select`: Dropdown selection
- `checklist`: Multiple selection checkboxes
- `taglist`: Tag input

#### Display Components

- `text`: Static text or markdown
- `image`: Display images

#### Action Components

- `button`: Submit or custom action buttons

### Validation Rules

```json
{
  "validate": {
    "required": true,
    "minLength": 10,
    "maxLength": 500,
    "min": 0,
    "max": 1000,
    "pattern": "^[A-Za-z0-9]+$",
    "validationType": "email"
  }
}
```

### Conditional Logic

```json
{
  "conditional": {
    "hide": "=approved = false"
  }
}
```

## Migrated Forms

### Finance Service

#### 1. CapEx Request Form

**Location**: `services/finance/src/main/resources/forms/formjs/capex-request-form.json`

**Features**:
- Multi-section layout (Request Info, Asset Details, Justification, Vendor Info)
- Currency input with 2 decimal places
- Date validation (future dates only)
- Dynamic cost center lookup
- File upload support
- ROI and payback period calculations

**BPMN Integration**:
```xml
<userTask id="createCapExRequest" name="Create CapEx Request"
         flowable:formKey="formjs:capex-request-form">
```

#### 2. CapEx Approval Form

**Location**: `services/finance/src/main/resources/forms/formjs/capex-approval-form.json`

**Features**:
- Read-only display of request details
- Approval/rejection radio buttons
- Conditional rejection reason field
- Approved amount override
- Approval conditions/notes

**Usage**: Displayed to managers/executives for approval decisions

### HR Service

#### 3. Leave Request Form

**Location**: `services/hr/src/main/resources/forms/formjs/leave-request-form.json`

**Features**:
- Leave type selection (Annual, Sick, Maternity, etc.)
- Date range picker with validation
- Half-day leave support
- Emergency contact information
- Work coverage arrangement
- Medical certificate requirement (conditional on sick leave > 3 days)

**Validation**:
- End date must be after start date
- Total days must be positive
- Coverage arrangement required for leave > 5 days

#### 4. Leave Approval Form

**Location**: `services/hr/src/main/resources/forms/formjs/leave-approval-form.json`

**Features**:
- Display employee leave balance
- Calculate balance after request
- Approve/reject with comments
- Approved days override
- Approval conditions

### Procurement Service

#### 5. Purchase Requisition Form

**Location**: `services/procurement/src/main/resources/forms/formjs/purchase-requisition-form.json`

**Features**:
- PR type selection (Goods, Services, Software, Equipment)
- Quantity and unit of measure
- Estimated unit price and total calculation
- Vendor information (primary and alternates)
- Delivery requirements
- Budget confirmation checkbox
- GL account code validation

**Validation**:
- Cost center format: `CC-XXXX`
- GL account format: `GL-XXXX-XXXX`
- Required delivery date (future)
- Budget confirmation required

## React Integration

### FormJsViewer Component

```typescript
import FormJsViewer from '@/components/forms/FormJsViewer';
import formSchema from './schema.json';

function MyForm() {
  const handleSubmit = (data) => {
    console.log('Submitted:', data);
  };

  return (
    <FormJsViewer
      schema={formSchema}
      data={{ userId: 'john.doe' }}
      onSubmit={handleSubmit}
      onChange={(data) => console.log('Changed:', data)}
    />
  );
}
```

### FormJsEditor Component

```typescript
import FormJsEditor from '@/components/forms/FormJsEditor';

function FormDesigner() {
  const handleSave = async (schema) => {
    await saveToBackend(schema);
  };

  return (
    <FormJsEditor
      schema={initialSchema}
      onSchemaChange={(schema) => console.log('Schema updated')}
      onSave={handleSave}
    />
  );
}
```

## Backend Integration

### Flowable BPMN Integration

#### Old Way (Flowable Forms)

```xml
<userTask id="task1" name="User Task"
         flowable:formKey="capex-request-v1">
```

#### New Way (Form.js)

```xml
<userTask id="task1" name="User Task"
         flowable:formKey="formjs:capex-request-form">
```

### Form Service API

Create a form service to load form schemas:

```java
@Service
public class FormService {

    @Value("classpath:forms/formjs/*.json")
    private Resource[] formResources;

    public String loadFormSchema(String formId) throws IOException {
        Resource resource = findFormResource(formId);
        return new String(resource.getInputStream().readAllBytes());
    }

    private Resource findFormResource(String formId) {
        for (Resource resource : formResources) {
            if (resource.getFilename().equals(formId + ".json")) {
                return resource;
            }
        }
        throw new FormNotFoundException("Form not found: " + formId);
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/forms")
public class FormController {

    @Autowired
    private FormService formService;

    @GetMapping("/{formId}")
    public ResponseEntity<String> getFormSchema(@PathVariable String formId) {
        try {
            String schema = formService.loadFormSchema(formId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(schema);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

## Migration Checklist

### Phase 1: Setup
- [x] Install form-js packages
- [x] Create form schema directories
- [x] Set up React components (FormJsViewer, FormJsEditor)

### Phase 2: Form Migration
- [x] Migrate CapEx request form
- [x] Migrate CapEx approval form
- [x] Migrate Leave request form
- [x] Migrate Leave approval form
- [x] Migrate Purchase Requisition form

### Phase 3: Integration
- [ ] Update BPMN process definitions
- [ ] Create Form Service API
- [ ] Update frontend task forms
- [ ] Test form submissions
- [ ] Validate data binding

### Phase 4: Testing
- [ ] Unit tests for form validation
- [ ] Integration tests for form submissions
- [ ] E2E tests for complete workflows
- [ ] User acceptance testing

### Phase 5: Deployment
- [ ] Deploy form schemas to production
- [ ] Update BPMN processes
- [ ] Monitor form submissions
- [ ] Collect user feedback

## Best Practices

### 1. Schema Versioning

Always include version information in form schemas:

```json
{
  "id": "capex-request-form",
  "schemaVersion": 9,
  "exporter": {
    "name": "werkflow",
    "version": "1.0.0"
  }
}
```

### 2. Field Naming

Use camelCase for field keys:

```json
{
  "key": "requesterId",  // Good
  "key": "requester_id"  // Avoid
}
```

### 3. Validation Messages

Provide clear validation messages:

```json
{
  "validate": {
    "required": true,
    "minLength": 10,
    "pattern": "^CC-[0-9]{4}$"
  },
  "description": "Cost center format: CC-XXXX"
}
```

### 4. Default Values

Use default values for common fields:

```json
{
  "key": "budgetYear",
  "type": "number",
  "defaultValue": "2025"
}
```

### 5. Conditional Fields

Use conditional logic to reduce form complexity:

```json
{
  "key": "rejectionReason",
  "type": "textarea",
  "conditional": {
    "hide": "=approved = true"
  }
}
```

## Troubleshooting

### Issue: Form Not Rendering

**Cause**: Invalid schema or missing required fields

**Solution**: Validate schema against form-js documentation

### Issue: Validation Not Working

**Cause**: Incorrect validation syntax

**Solution**: Check validation rules match form-js spec:

```json
{
  "validate": {
    "required": true,
    "min": 0,
    "max": 100
  }
}
```

### Issue: Conditional Logic Not Working

**Cause**: Expression syntax error

**Solution**: Use proper expression syntax:

```json
{
  "conditional": {
    "hide": "=fieldName = 'value'"
  }
}
```

## Resources

### Documentation

- [form-js GitHub Repository](https://github.com/bpmn-io/form-js)
- [form-js Examples](https://github.com/bpmn-io/form-js-examples)
- [Form Schema Documentation](https://github.com/bpmn-io/form-js/blob/develop/docs/FORM_SCHEMA.md)
- [Flowable Migration Guide](https://documentation.flowable.com/latest/develop/migration/new-design-migration-guide)

### NPM Packages

- [@bpmn-io/form-js](https://www.npmjs.com/package/@bpmn-io/form-js)
- [@bpmn-io/form-js-editor](https://www.npmjs.com/package/@bpmn-io/form-js-editor)
- [@bpmn-io/form-js-viewer](https://www.npmjs.com/package/@bpmn-io/form-js-viewer)

## Next Steps

1. **Complete Backend Integration**: Implement Form Service API
2. **Update BPMN Processes**: Change formKey references
3. **Testing**: Comprehensive testing of all forms
4. **Training**: Train users on new form editor
5. **Monitoring**: Monitor form submission errors
6. **Optimization**: Performance tuning and caching

## Conclusion

The migration to form-js provides a modern, maintainable, and user-friendly approach to form management in the Werkflow HR platform. The JSON-based schema, visual editor, and powerful validation capabilities make it a significant improvement over traditional Flowable forms.

For questions or issues, refer to the form-js documentation or contact the development team.
