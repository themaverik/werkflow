# Form Designer Migration - Code Changes

## File Changes Overview

### Files Modified
1. `/frontends/admin-portal/app/(studio)/forms/new/page.tsx`
2. `/frontends/admin-portal/app/(studio)/forms/edit/[key]/page.tsx`

### Files Created
1. `/frontends/admin-portal/components/forms/FormJsBuilder.tsx`
2. `/docs/Form-Designer-Migration-Summary.md`
3. `/test-form-migration.sh`

## Detailed Changes

### 1. New Form Page (`/app/(studio)/forms/new/page.tsx`)

#### Before (Form.io)
```typescript
import DynamicFormBuilder from '@/components/forms/FormBuilder'

export default function NewFormPage() {
  return <DynamicFormBuilder />
}
```

#### After (form-js)
```typescript
import FormJsBuilder from '@/components/forms/FormJsBuilder'

export default function NewFormPage() {
  return <FormJsBuilder />
}
```

**Changes**:
- Replaced import from `FormBuilder` to `FormJsBuilder`
- Component usage remains the same (no prop changes needed)

---

### 2. Edit Form Page (`/app/(studio)/forms/edit/[key]/page.tsx`)

#### Before (Form.io)
```typescript
'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getFormDefinition } from '@/lib/api/flowable'
import DynamicFormBuilder from '@/components/forms/FormBuilder'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function EditFormPage() {
  // ... loading and error states ...

  return (
    <div className="h-screen flex flex-col">
      <DynamicFormBuilder
        initialForm={formDef?.formJson}
        formKey={formKey}
      />
    </div>
  )
}
```

#### After (form-js)
```typescript
'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getFormDefinition } from '@/lib/api/flowable'
import FormJsBuilder from '@/components/forms/FormJsBuilder'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function EditFormPage() {
  // ... loading and error states ...

  return (
    <div className="h-screen flex flex-col">
      <FormJsBuilder
        initialForm={formDef?.formJson}
        formKey={formKey}
      />
    </div>
  )
}
```

**Changes**:
- Replaced import from `FormBuilder` to `FormJsBuilder`
- Updated component usage from `DynamicFormBuilder` to `FormJsBuilder`
- Props interface remains compatible

---

### 3. New FormJsBuilder Component

#### Component Structure

```typescript
interface FormJsBuilderProps {
  initialForm?: string      // JSON string of form schema
  formKey?: string         // Form identifier
  onSave?: (formJson: string) => void  // Optional save callback
}
```

#### Key Features

**State Management**:
```typescript
const [formSchema, setFormSchema] = useState<any>({
  type: 'default',
  components: [],
  schemaVersion: 9
})
const [formKey, setFormKey] = useState(initialFormKey || '')
const [formName, setFormName] = useState('')
const [hasChanges, setHasChanges] = useState(false)
```

**Form Deployment**:
```typescript
const deployMutation = useMutation({
  mutationFn: async () => {
    const schemaToSave = {
      ...formSchema,
      id: formKey,
      title: formName || formKey
    }

    return deployForm({
      key: formKey,
      name: formName || formKey,
      formJson: JSON.stringify(schemaToSave, null, 2)
    })
  },
  onSuccess: () => {
    setHasChanges(false)
    queryClient.invalidateQueries({ queryKey: ['formDefinitions'] })
    alert('Form deployed successfully!')
    router.push('/studio/forms')
  }
})
```

**Toolbar**:
```typescript
<Card className="border-b rounded-none">
  <div className="flex items-center justify-between p-4">
    <div className="flex items-center gap-4 flex-1">
      <input
        type="text"
        value={formKey}
        placeholder="Form key (e.g., leave-request-form)"
        className="border rounded px-3 py-2 w-64 text-sm"
      />
      <input
        type="text"
        value={formName}
        placeholder="Form name"
        className="border rounded px-3 py-2 w-64 text-sm"
      />
      {hasChanges && (
        <span className="text-sm text-amber-600">Unsaved changes</span>
      )}
    </div>

    <div className="flex items-center gap-2">
      <Button variant="outline" size="sm" onClick={handleUpload}>
        <Upload className="h-4 w-4 mr-2" />
        Load
      </Button>
      <Button variant="outline" size="sm" onClick={handleDownload}>
        <Download className="h-4 w-4 mr-2" />
        Download
      </Button>
      <Button size="sm" onClick={() => deployMutation.mutate()}>
        Deploy Form
      </Button>
    </div>
  </div>
</Card>
```

**Editor Integration**:
```typescript
<div className="flex-1 overflow-hidden">
  <FormJsEditor
    schema={formSchema}
    onSchemaChange={handleSchemaChange}
    onSave={handleEditorSave}
    className="h-full"
  />
</div>
```

---

## Schema Format Comparison

### Form.io Schema (Old)
```json
{
  "display": "form",
  "title": "My Form",
  "components": [
    {
      "type": "textfield",
      "key": "firstName",
      "label": "First Name",
      "input": true
    }
  ]
}
```

### form-js Schema (New)
```json
{
  "type": "default",
  "id": "my-form",
  "title": "My Form",
  "schemaVersion": 9,
  "components": [
    {
      "type": "textfield",
      "id": "firstName",
      "label": "First Name"
    }
  ]
}
```

**Key Differences**:
- `display` → `type` (values: "default")
- Added `schemaVersion` field
- Component `key` → `id`
- Removed `input` property
- Simpler component structure

---

## Component Hierarchy

### Before (Form.io)
```
FormBuilder (Form.io)
  └── @formio/react FormBuilder
      └── formio.js library
```

### After (form-js)
```
FormJsBuilder (wrapper)
  ├── Toolbar (metadata + actions)
  ├── State Management
  ├── API Integration
  └── FormJsEditor
      └── @bpmn-io/form-js-editor
          └── form-js library
```

---

## Import Changes

### Before
```typescript
import 'formiojs/dist/formio.full.min.css'
import { FormBuilder } from '@formio/react'
```

### After
```typescript
import '@bpmn-io/form-js/dist/assets/form-js.css'
import '@bpmn-io/form-js/dist/assets/form-js-editor.css'
import '@bpmn-io/form-js-editor/dist/assets/form-js-editor.css'
import { FormEditor } from '@bpmn-io/form-js-editor'
```

---

## API Integration (Unchanged)

The API integration remains the same, using the existing Flowable API:

```typescript
// From /lib/api/flowable.ts
export async function deployForm(data: FormDeploymentRequest): Promise<DeploymentResponse> {
  const response = await apiClient.post('/forms', data)
  return response.data
}

export async function getFormDefinition(formKey: string): Promise<FormDefinitionResponse> {
  const response = await apiClient.get(`/forms/${formKey}`)
  return response.data
}
```

**Request Format**:
```typescript
interface FormDeploymentRequest {
  key: string        // Form identifier
  name: string       // Human-readable name
  formJson: string   // JSON string of form schema
}
```

---

## User Workflow

### Before (Form.io)
1. Navigate to Forms page
2. Click "New Form"
3. Use Form.io builder to design form
4. Enter form key and name
5. Click "Deploy Form"

### After (form-js)
1. Navigate to Forms page
2. Click "New Form"
3. Use form-js editor to design form
4. Enter form key and name
5. Click "Deploy Form"

**No change in user workflow** - only the underlying editor is different.

---

## Testing Verification

Run the test script:
```bash
./test-form-migration.sh
```

Expected output:
```
✓ Admin Portal is running
✓ FormJsBuilder.tsx exists
✓ FormJsEditor.tsx exists
✓ New form page uses FormJsBuilder
✓ Edit form page uses FormJsBuilder
```

---

## Migration Benefits

1. **Smaller Bundle Size**: form-js is lighter than Form.io
2. **Better BPMN Integration**: Native support for Flowable/Camunda
3. **Modern Architecture**: Built on latest web standards
4. **Active Development**: Regular updates from bpmn-io team
5. **Cleaner Schema**: Simpler, more maintainable format
6. **Better Performance**: Optimized rendering engine

---

## Backward Compatibility Note

Existing Form.io forms can be loaded but will show a warning in the console:
```
"Form.io schema detected, converting to form-js format"
```

For full compatibility, recreate forms using the new form-js editor.
