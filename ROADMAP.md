# Frontend Development Roadmap
# BPMN Designer & Form Builder Integration with Next.js

## 🎯 Project Overview

Build a production-ready React/Next.js frontend with visual BPMN workflow designer and dynamic form builder that integrates seamlessly with the existing Spring Boot + Flowable backend.

**Timeline**: 8-10 weeks (2 developers) or 12-16 weeks (1 developer)
**Status**: Phase 1 in progress

---

## 🛠️ Technology Stack (All Compatible & Production-Ready)

### Core Framework
```json
{
  "next": "14.2.x",           // Latest stable with App Router
  "react": "18.3.x",          // Required by all libraries
  "react-dom": "18.3.x",
  "typescript": "5.4.x"       // Type safety
}
```

### BPMN Designer
```json
{
  "bpmn-js": "17.x",                    // Core BPMN renderer/editor (Camunda)
  "bpmn-js-properties-panel": "5.x",   // Properties panel
  "@bpmn-io/properties-panel": "3.x"   // Modern properties panel
}
```
✅ **Production-ready**: Used by Camunda, millions of downloads, active maintenance

### Form Builder & Renderer
```json
{
  "@formio/react": "5.x",              // Form.io builder + renderer
  "formiojs": "4.19.x"                 // Form.io core
}
```
✅ **Recommended**: Comprehensive form builder with drag-drop UI

### UI Components
```json
{
  "@radix-ui/react-*": "1.x",         // Headless UI primitives
  "tailwindcss": "3.4.x",             // Styling framework
  "shadcn/ui": "latest",              // Pre-built components
  "lucide-react": "0.x"               // Icons
}
```
✅ **Modern, composable**: Copy-paste components, fully customizable

### Data Fetching & State
```json
{
  "@tanstack/react-query": "5.x",     // Server state management
  "axios": "1.6.x",                   // HTTP client
  "zustand": "4.5.x"                  // Client state (optional)
}
```

### Authentication
```json
{
  "next-auth": "5.x",                 // NextAuth v5 with Keycloak
  "@auth/core": "0.x"                 // Auth.js core
}
```

### Development & Testing
```json
{
  "vitest": "1.x",                    // Unit testing
  "@testing-library/react": "14.x",   // Component testing
  "playwright": "1.x",                // E2E testing
  "eslint": "8.x",                    // Linting
  "prettier": "3.x"                   // Code formatting
}
```

---

## 📋 Implementation Phases

### **Phase 1: Foundation & Setup** (Week 1-2) ⏳ IN PROGRESS

#### Week 1: Project Initialization
**Goal**: Create Next.js project with TypeScript and modern tooling

**Tasks:**
- [x] ~~Create monorepo structure~~ ✅ Complete
- [ ] Initialize Next.js 14 project with TypeScript
- [ ] Configure project structure (App Router layout)
- [ ] Set up Tailwind CSS configuration
- [ ] Install and configure shadcn/ui
- [ ] Set up ESLint + Prettier
- [ ] Configure environment variables (.env.local)
- [ ] Set up Axios instance with base URL
- [ ] Install React Query for data fetching
- [ ] Create basic layout components

**Deliverables:**
```
frontend/
├── app/
│   ├── layout.tsx              # Root layout
│   ├── page.tsx                # Landing page
│   ├── globals.css             # Global styles
│   └── providers.tsx           # React Query provider
├── components/
│   └── ui/                     # shadcn components
├── lib/
│   ├── api/                    # API client functions
│   │   └── client.ts           # Axios instance
│   ├── hooks/                  # Custom React hooks
│   └── utils.ts                # Utilities
├── public/                     # Static assets
├── .env.local                  # Environment variables
├── tailwind.config.ts
├── tsconfig.json
├── next.config.mjs
└── package.json
```

**Commands to Run:**
```bash
cd frontend

# Initialize Next.js project
npx create-next-app@14 . --typescript --tailwind --app --no-src-dir

# Install UI components
npx shadcn-ui@latest init

# Install dependencies
npm install @tanstack/react-query axios zustand
npm install -D @types/node

# Add shadcn components
npx shadcn-ui@latest add button
npx shadcn-ui@latest add card
npx shadcn-ui@latest add dialog
npx shadcn-ui@latest add dropdown-menu
npx shadcn-ui@latest add tabs
npx shadcn-ui@latest add toast

# Run development server
npm run dev
```

**Success Criteria:**
- ✅ Next.js dev server runs on http://localhost:3000
- ✅ TypeScript compilation works without errors
- ✅ Tailwind CSS styles applied correctly
- ✅ shadcn/ui components render properly
- ✅ React Query provider configured

---

#### Week 2: Authentication Integration
**Goal**: Implement Keycloak authentication with NextAuth v5

**Tasks:**
- [ ] Install NextAuth v5 packages
- [ ] Configure Keycloak provider
- [ ] Create authentication middleware
- [ ] Build login page UI
- [ ] Implement session management
- [ ] Add role-based route protection
- [ ] Create user profile component
- [ ] Test authentication flow

**Key Files to Create:**
```typescript
// lib/auth.ts - NextAuth configuration
import NextAuth from "next-auth"
import Keycloak from "next-auth/providers/keycloak"

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER,
    })
  ],
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token
        token.roles = account.realm_access?.roles || []
      }
      return token
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken
      session.user.roles = token.roles
      return session
    }
  }
})

// middleware.ts - Route protection
export { auth as middleware } from "@/lib/auth"

export const config = {
  matcher: ["/studio/:path*", "/portal/:path*"]
}

// app/(auth)/login/page.tsx - Login page
export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <Card className="w-[400px]">
        <CardHeader>
          <CardTitle>Login to Werkflow</CardTitle>
        </CardHeader>
        <CardContent>
          <form action={signIn}>
            <Button type="submit" className="w-full">
              Sign in with Keycloak
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
```

**Environment Variables (.env.local):**
```env
# Backend API
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# NextAuth
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-here-generate-with-openssl-rand-base64-32

# Keycloak
KEYCLOAK_CLIENT_ID=werkflow-frontend
KEYCLOAK_CLIENT_SECRET=your-client-secret
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
```

**Success Criteria:**
- ✅ Login page accessible
- ✅ Keycloak authentication works
- ✅ Session persists across page refreshes
- ✅ Protected routes redirect to login
- ✅ User roles extracted from JWT
- ✅ Logout functionality works

---

### **Phase 2: BPMN Designer Integration** (Week 3-4) 📝 PENDING

#### Week 3: Basic BPMN Editor
**Goal**: Integrate bpmn-js and create visual process designer

**Tasks:**
- [ ] Install bpmn-js packages
- [ ] Create BpmnDesigner component
- [ ] Implement load/save BPMN XML functionality
- [ ] Add editor toolbar (save, load, deploy, download)
- [ ] Create blank BPMN template generator
- [ ] Handle BPMN rendering and interactions
- [ ] Add zoom and pan controls
- [ ] Test BPMN import/export

**Key Component:**
```typescript
// components/bpmn/BpmnDesigner.tsx
'use client'

import { useEffect, useRef, useState } from 'react'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { Button } from '@/components/ui/button'
import { useMutation } from '@tanstack/react-query'
import { deployBpmn } from '@/lib/api/flowable'

export default function BpmnDesigner() {
  const containerRef = useRef<HTMLDivElement>(null)
  const [modeler, setModeler] = useState<BpmnModeler | null>(null)
  const [processName, setProcessName] = useState('')

  useEffect(() => {
    if (!containerRef.current) return

    const bpmnModeler = new BpmnModeler({
      container: containerRef.current,
      keyboard: { bindTo: document }
    })

    // Load empty diagram
    const emptyBpmn = `<?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   xmlns:flowable="http://flowable.org/bpmn">
        <process id="process" isExecutable="true" />
      </definitions>`

    bpmnModeler.importXML(emptyBpmn)
    setModeler(bpmnModeler)

    return () => bpmnModeler.destroy()
  }, [])

  const deployMutation = useMutation({
    mutationFn: async () => {
      if (!modeler) return
      const { xml } = await modeler.saveXML({ format: true })
      return deployBpmn({
        name: processName,
        resourceName: `${processName}.bpmn20.xml`,
        bpmnXml: xml
      })
    }
  })

  return (
    <div className="flex flex-col h-screen">
      <div className="border-b p-4 flex gap-2 items-center">
        <input
          value={processName}
          onChange={(e) => setProcessName(e.target.value)}
          placeholder="Process name"
          className="border px-3 py-2 rounded flex-1"
        />
        <Button
          onClick={() => deployMutation.mutate()}
          disabled={!processName || deployMutation.isPending}
        >
          {deployMutation.isPending ? 'Deploying...' : 'Deploy Process'}
        </Button>
      </div>
      <div ref={containerRef} className="flex-1 bg-gray-50" />
    </div>
  )
}
```

**API Client:**
```typescript
// lib/api/flowable.ts
import { apiClient } from './client'

export interface BpmnDeploymentRequest {
  name: string
  resourceName: string
  bpmnXml: string
}

export async function deployBpmn(data: BpmnDeploymentRequest) {
  const response = await apiClient.post('/flowable/deployments', data)
  return response.data
}

export async function getProcessDefinitions() {
  const response = await apiClient.get('/flowable/process-definitions')
  return response.data
}
```

**Success Criteria:**
- ✅ BPMN modeler renders correctly
- ✅ Can create start/end events, tasks, gateways
- ✅ Can connect elements with sequence flows
- ✅ Can save BPMN XML
- ✅ Can deploy to backend successfully
- ✅ Zoom and pan work smoothly

---

#### Week 4: Properties Panel & Process Management
**Goal**: Add properties editing and process list management

**Tasks:**
- [ ] Install bpmn-js-properties-panel
- [ ] Integrate properties panel with modeler
- [ ] Add Flowable-specific properties (assignee, groups, form keys)
- [ ] Create process definition list page
- [ ] Implement process versioning display
- [ ] Add load existing process functionality
- [ ] Create process deletion functionality
- [ ] Add validation before deployment

**Pages:**
```typescript
// app/(studio)/processes/page.tsx - Process list
import ProcessList from '@/components/processes/ProcessList'

export default function ProcessesPage() {
  return (
    <div className="container py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Process Definitions</h1>
        <Link href="/studio/processes/new">
          <Button>Create New Process</Button>
        </Link>
      </div>
      <ProcessList />
    </div>
  )
}

// app/(studio)/processes/new/page.tsx - BPMN Designer
import BpmnDesigner from '@/components/bpmn/BpmnDesigner'

export default function NewProcessPage() {
  return <BpmnDesigner />
}

// app/(studio)/processes/[id]/page.tsx - Edit existing process
import BpmnDesigner from '@/components/bpmn/BpmnDesigner'

export default function EditProcessPage({ params }: { params: { id: string } }) {
  return <BpmnDesigner processId={params.id} />
}
```

**Success Criteria:**
- ✅ Properties panel displays element properties
- ✅ Can edit task assignee and groups
- ✅ Can link forms to tasks via form key
- ✅ Process list displays all definitions
- ✅ Can load and edit existing processes
- ✅ Validation prevents invalid deployments

---

### **Phase 3: Form Builder Integration** (Week 5-6) 📝 PENDING

#### Week 5: Form.io Builder
**Goal**: Integrate Form.io for visual form creation

**Tasks:**
- [ ] Install @formio/react packages
- [ ] Create FormBuilder component with Form.io
- [ ] Implement form save functionality
- [ ] Add form validation rules
- [ ] Create form preview functionality
- [ ] Link forms to BPMN tasks (form key)
- [ ] Create form list page
- [ ] Add form versioning

**Key Component:**
```typescript
// components/forms/FormBuilder.tsx
'use client'

import { FormBuilder } from '@formio/react'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { useMutation } from '@tanstack/react-query'
import { deployForm } from '@/lib/api/flowable'

export default function DynamicFormBuilder() {
  const [formSchema, setFormSchema] = useState({
    display: 'form',
    components: []
  })
  const [formKey, setFormKey] = useState('')
  const [formName, setFormName] = useState('')

  const deployMutation = useMutation({
    mutationFn: () => deployForm({
      key: formKey,
      name: formName || formKey,
      formJson: JSON.stringify(formSchema)
    })
  })

  return (
    <div className="p-6">
      <div className="mb-4 flex gap-2">
        <input
          value={formKey}
          onChange={(e) => setFormKey(e.target.value)}
          placeholder="Form key (e.g., jobApplicationForm)"
          className="border px-3 py-2 rounded flex-1"
        />
        <input
          value={formName}
          onChange={(e) => setFormName(e.target.value)}
          placeholder="Form name"
          className="border px-3 py-2 rounded flex-1"
        />
        <Button
          onClick={() => deployMutation.mutate()}
          disabled={!formKey || deployMutation.isPending}
        >
          {deployMutation.isPending ? 'Saving...' : 'Save Form'}
        </Button>
      </div>

      <FormBuilder
        form={formSchema}
        onChange={(schema) => setFormSchema(schema)}
      />
    </div>
  )
}
```

**Success Criteria:**
- ✅ Form builder renders with drag-drop interface
- ✅ Can add text, email, dropdown, date fields
- ✅ Can configure field validation
- ✅ Form saves to backend successfully
- ✅ Form preview works correctly

---

#### Week 6: Dynamic Form Renderer
**Goal**: Create form renderer for task completion

**Tasks:**
- [ ] Create FormRenderer component
- [ ] Fetch form definitions from backend
- [ ] Implement form submission
- [ ] Add client-side validation
- [ ] Handle file uploads
- [ ] Create reusable form wrapper
- [ ] Add form error handling
- [ ] Test with various field types

**Key Component:**
```typescript
// components/forms/FormRenderer.tsx
'use client'

import { Form } from '@formio/react'
import { useState, useEffect } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getFormDefinition } from '@/lib/api/flowable'
import { completeTask } from '@/lib/api/workflows'

interface FormRendererProps {
  formKey: string
  taskId: string
  onComplete?: () => void
}

export default function FormRenderer({ formKey, taskId, onComplete }: FormRendererProps) {
  const { data: formDef, isLoading } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => getFormDefinition(formKey)
  })

  const completeMutation = useMutation({
    mutationFn: (formData: any) => completeTask({
      taskId,
      variables: formData,
      comment: ''
    }),
    onSuccess: () => onComplete?.()
  })

  if (isLoading) return <div>Loading form...</div>

  const formSchema = formDef ? JSON.parse(formDef.formJson) : null
  if (!formSchema) return <div>Form not found</div>

  return (
    <div className="p-6">
      <Form
        form={formSchema}
        onSubmit={(submission: any) => completeMutation.mutate(submission.data)}
      />
    </div>
  )
}
```

**Success Criteria:**
- ✅ Forms render from backend definitions
- ✅ Form validation works client-side
- ✅ Form submission completes tasks
- ✅ Error messages display properly
- ✅ File uploads work correctly

---

### **Phase 4: Runtime Portal (Task Management)** (Week 7-8) 📝 PENDING

#### Week 7: Task List & Process Start
**Goal**: Build user-friendly task management interface

**Tasks:**
- [ ] Create task list page with filters
- [ ] Implement task claiming functionality
- [ ] Create process start page
- [ ] Add process instance list
- [ ] Implement search and pagination
- [ ] Add real-time updates (polling)
- [ ] Create task detail view
- [ ] Add task comments

**Key Pages:**
```typescript
// app/(portal)/tasks/page.tsx
import TaskList from '@/components/tasks/TaskList'

export default function TasksPage() {
  return (
    <div className="container py-6">
      <h1 className="text-3xl font-bold mb-6">My Tasks</h1>
      <TaskList />
    </div>
  )
}

// components/tasks/TaskList.tsx
'use client'

import { useQuery } from '@tanstack/react-query'
import { getTasks } from '@/lib/api/workflows'
import TaskCard from './TaskCard'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

export default function TaskList() {
  const { data: myTasks } = useQuery({
    queryKey: ['tasks', 'assigned'],
    queryFn: () => getTasks({ assignee: 'me' }),
    refetchInterval: 30000 // Poll every 30 seconds
  })

  const { data: groupTasks } = useQuery({
    queryKey: ['tasks', 'group'],
    queryFn: () => getTasks({ candidateGroup: 'MANAGER' }),
    refetchInterval: 30000
  })

  return (
    <Tabs defaultValue="mine">
      <TabsList>
        <TabsTrigger value="mine">My Tasks ({myTasks?.length || 0})</TabsTrigger>
        <TabsTrigger value="group">Group Tasks ({groupTasks?.length || 0})</TabsTrigger>
      </TabsList>

      <TabsContent value="mine" className="space-y-4">
        {myTasks?.map(task => (
          <TaskCard key={task.taskId} task={task} />
        ))}
      </TabsContent>

      <TabsContent value="group" className="space-y-4">
        {groupTasks?.map(task => (
          <TaskCard key={task.taskId} task={task} />
        ))}
      </TabsContent>
    </Tabs>
  )
}
```

**Success Criteria:**
- ✅ Task list displays all user tasks
- ✅ Can filter by assigned vs group tasks
- ✅ Task claiming works correctly
- ✅ Real-time updates via polling
- ✅ Search and pagination work

---

#### Week 8: Task Completion & Process Timeline
**Goal**: Complete task workflow and add process tracking

**Tasks:**
- [ ] Create task detail page
- [ ] Integrate FormRenderer for task forms
- [ ] Implement task completion flow
- [ ] Add comment/attachment support
- [ ] Create process timeline component
- [ ] Add process diagram visualization
- [ ] Implement task reassignment
- [ ] Add task history view

**Success Criteria:**
- ✅ Tasks complete successfully with forms
- ✅ Process timeline shows progress
- ✅ Can view process history
- ✅ Task reassignment works
- ✅ Comments saved correctly

---

### **Phase 5: Backend API Extensions** (Week 9) 📝 PENDING

**Goal**: Add Flowable deployment endpoints to Spring Boot

**New Controller:**
```java
// FlowableDeploymentController.java
@RestController
@RequestMapping("/flowable")
@RequiredArgsConstructor
public class FlowableDeploymentController {

    private final RepositoryService repositoryService;
    private final FormRepositoryService formRepositoryService;

    @PostMapping("/deployments")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<DeploymentResponse> deployBpmn(
            @RequestBody BpmnDeploymentRequest request) {

        Deployment deployment = repositoryService.createDeployment()
            .name(request.getName())
            .addString(request.getResourceName(), request.getBpmnXml())
            .deploy();

        return ResponseEntity.ok(new DeploymentResponse(
            deployment.getId(),
            deployment.getName()
        ));
    }

    @GetMapping("/process-definitions")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<ProcessDefinitionResponse>> getProcessDefinitions() {
        List<ProcessDefinition> definitions = repositoryService
            .createProcessDefinitionQuery()
            .latestVersion()
            .list();

        return ResponseEntity.ok(definitions.stream()
            .map(this::toResponse)
            .collect(Collectors.toList()));
    }

    @PostMapping("/forms")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<FormDeploymentResponse> deployForm(
            @RequestBody FormDeploymentRequest request) {

        FormDeployment deployment = formRepositoryService.createDeployment()
            .addFormDefinition(request.getKey(), request.getFormJson())
            .name(request.getName())
            .deploy();

        return ResponseEntity.ok(new FormDeploymentResponse(deployment.getId()));
    }

    @GetMapping("/forms/{formKey}")
    public ResponseEntity<FormDefinitionResponse> getFormDefinition(
            @PathVariable String formKey) {

        FormDefinition formDef = formRepositoryService
            .createFormDefinitionQuery()
            .formDefinitionKey(formKey)
            .latestVersion()
            .singleResult();

        if (formDef == null) {
            return ResponseEntity.notFound().build();
        }

        String formJson = formRepositoryService.getFormModelById(formDef.getId());

        return ResponseEntity.ok(new FormDefinitionResponse(
            formDef.getKey(),
            formDef.getName(),
            formJson
        ));
    }
}
```

**DTOs to Create:**
- `BpmnDeploymentRequest.java`
- `DeploymentResponse.java`
- `FormDeploymentRequest.java`
- `FormDeploymentResponse.java`
- `FormDefinitionResponse.java`
- `ProcessDefinitionResponse.java`

**Success Criteria:**
- ✅ All endpoints implemented
- ✅ DTOs created with validation
- ✅ Role-based security applied
- ✅ Swagger documentation updated
- ✅ Integration tests pass

---

### **Phase 6: Testing & Polish** (Week 10) 📝 PENDING

#### Testing
- [ ] Write unit tests for API functions
- [ ] Component tests with React Testing Library
- [ ] E2E tests with Playwright
- [ ] Integration tests for BPMN deployment
- [ ] Form validation testing
- [ ] Authentication flow testing

#### Polish
- [ ] Error handling and user feedback
- [ ] Loading states and skeleton screens
- [ ] Responsive design for tablets/mobile
- [ ] Accessibility (ARIA labels, keyboard navigation)
- [ ] Dark mode support (optional)
- [ ] Performance optimization
- [ ] SEO optimization

#### Documentation
- [ ] User guide for process designers
- [ ] API documentation
- [ ] Deployment guide
- [ ] Video tutorials (optional)
- [ ] Troubleshooting guide

**Success Criteria:**
- ✅ 80%+ test coverage
- ✅ All E2E tests pass
- ✅ Accessibility score 90+
- ✅ Lighthouse score 90+
- ✅ Documentation complete

---

## 🚀 Production Deployment

### Frontend (Next.js)

**Build:**
```bash
cd frontend
npm run build
npm start
```

**Docker:**
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/package*.json ./
RUN npm ci --only=production
EXPOSE 3000
CMD ["npm", "start"]
```

**Vercel (Easiest):**
```bash
npm install -g vercel
vercel deploy --prod
```

### Backend (Spring Boot)
Already production-ready in `backend/` folder.

### Environment Variables

**Production (.env.production):**
```env
NEXT_PUBLIC_API_URL=https://api.werkflow.com
NEXTAUTH_URL=https://werkflow.com
NEXTAUTH_SECRET=your-production-secret
KEYCLOAK_CLIENT_ID=werkflow-frontend
KEYCLOAK_CLIENT_SECRET=your-production-secret
KEYCLOAK_ISSUER=https://keycloak.werkflow.com/realms/werkflow
```

---

## 📊 Progress Tracking

### Backend Services Progress

#### Phase 3: CapEx, Procurement, and Inventory Services ✅ COMPLETED
**Completion Date**: 2025-11-17
**Status**: Completed

**Services Implemented:**
- [x] Finance Service (Port 8084) - CapEx domain ✅
  - Complete entity models (CapExRequest, CapExApproval, Budget)
  - REST API controllers for CapEx management
  - Flyway database migrations
  - Integration with Engine Service

- [x] Procurement Service (Port 8085) - Procurement domain ✅
  - Complete entity models (PurchaseRequest, Vendor, PurchaseOrder)
  - REST API controllers for procurement and vendor management
  - Flyway database migrations
  - Integration with Engine Service

- [x] Inventory Service (Port 8086) - Asset management domain ✅
  - Complete entity models (AssetCategory, AssetDefinition, AssetInstance, CustodyRecord, TransferRequest, MaintenanceRecord)
  - REST API controllers for asset and custody management
  - Flyway database migrations
  - Inter-department custody tracking
  - Integration with Engine Service

**BPMN Workflows Created:**
- [x] CapEx Approval Process (capex-approval-process.bpmn20.xml) ✅
  - Multi-level approval workflow based on amount thresholds
  - Budget verification and reservation
  - Manager, VP, and CFO approval gates

- [x] Procurement Approval Process (procurement-approval-process.bpmn20.xml) ✅
  - Vendor selection and quotation workflow
  - Multi-level approval based on purchase amount
  - Purchase order generation

- [x] Asset Transfer Approval Process (asset-transfer-approval-process.bpmn20.xml) ✅
  - Custody tracking and transfer workflow
  - Current custodian release approval
  - Manager approval for high-value assets
  - New custodian acceptance

**Form.io Templates Created:**
- [x] capex-request - Capital expenditure request form with ROI and payback period ✅
- [x] procurement-request - Purchase request form with vendor and quantities ✅
- [x] asset-transfer-request - Asset transfer form with transfer types and custody ✅

**Infrastructure Updates:**
- [x] Docker Compose configuration updated with all three services ✅
- [x] Database initialization with finance_service, procurement_service, inventory_service schemas ✅
- [x] Engine Service configuration updated with service URLs ✅

**Architecture Corrections:**
- Corrected misunderstanding that CapEx, Procurement, and Inventory are WORKFLOWS, not DEPARTMENTS
- CapEx workflows orchestrated by Engine Service, data managed by Finance Service
- Procurement workflows orchestrated by Engine Service, data managed by Procurement Service
- Asset transfer workflows orchestrated by Engine Service, data managed by Inventory Service

---

### Frontend Development Progress

#### Overall Progress
- [x] Phase 1: Foundation (0% complete)
- [ ] Phase 2: BPMN Designer (0% complete)
- [ ] Phase 3: Form Builder (0% complete)
- [ ] Phase 4: Runtime Portal (0% complete)
- [ ] Phase 5: Backend API (0% complete)
- [ ] Phase 6: Testing & Polish (0% complete)

### Current Sprint: Phase 1 Week 1
**Status**: In Progress
**Start Date**: 2024-11-15
**Target Completion**: 2024-11-22

**Tasks This Week:**
- [x] ~~Monorepo migration~~ ✅ Complete
- [ ] Initialize Next.js project
- [ ] Configure Tailwind and shadcn/ui
- [ ] Set up React Query
- [ ] Create basic layout

---

## 🎯 Key Features Delivered (End State)

✅ **Visual BPMN Designer** - No XML knowledge required
✅ **Dynamic Form Builder** - Drag-drop form creation
✅ **Form-Task Linking** - Forms automatically render in tasks
✅ **Process Deployment** - One-click deployment from UI
✅ **Version Management** - Track process versions
✅ **Task Portal** - Manage and complete tasks
✅ **Role-Based Access** - HR_ADMIN can design, others use
✅ **Production-Ready** - Tested, documented, deployable

---

## 🔧 Development Guidelines

### Code Style
- Use TypeScript for all components
- Follow Next.js App Router conventions
- Use server components by default
- Client components only when needed (use 'use client')
- Prefer composition over inheritance

### Component Organization
```
components/
├── ui/              # shadcn components (auto-generated)
├── bpmn/            # BPMN designer components
├── forms/           # Form builder/renderer
├── tasks/           # Task management
├── layout/          # Layout components
└── common/          # Shared components
```

### API Client Pattern
```typescript
// Always use React Query for data fetching
const { data, isLoading, error } = useQuery({
  queryKey: ['resource', id],
  queryFn: () => fetchResource(id)
})

// Mutations for write operations
const mutation = useMutation({
  mutationFn: createResource,
  onSuccess: () => queryClient.invalidateQueries(['resources'])
})
```

### Error Handling
- Use try/catch in API calls
- Display user-friendly error messages
- Log errors to console in development
- Use toast notifications for feedback

---

## 📚 Resources & References

### Documentation
- [Next.js 14 Docs](https://nextjs.org/docs)
- [bpmn-js Documentation](https://bpmn.io/toolkit/bpmn-js/)
- [Form.io Documentation](https://help.form.io/)
- [shadcn/ui Components](https://ui.shadcn.com/)
- [NextAuth v5 Guide](https://authjs.dev/)

### Code Examples
- [bpmn-js Examples](https://github.com/bpmn-io/bpmn-js-examples)
- [Next.js Examples](https://github.com/vercel/next.js/tree/canary/examples)
- [shadcn/ui Examples](https://ui.shadcn.com/examples)

---

## 🤝 Contributing

This roadmap is a living document. Update it as the project progresses:
- Mark tasks complete with ✅
- Update progress percentages
- Add new tasks as discovered
- Document learnings and gotchas

---

**Last Updated**: 2024-11-15
**Next Review**: 2024-11-22 (End of Phase 1 Week 1)
