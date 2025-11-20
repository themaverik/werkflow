# BPMN Process Viewer UI - Viewing Existing XML Designs

## Quick Answer

**The UI that shows your existing BPMN process designs is the Processes Page**:
- **URL**: `http://localhost:4000/studio/processes`
- **Component**: `frontends/admin-portal/app/(studio)/processes/page.tsx`
- **Access**: Studio → Processes tab (after auth with HR_ADMIN role)
- **Purpose**: Display deployed BPMN processes and allow viewing/editing designs

---

## The Processes Page Interface

### Main Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Werkflow Studio     Processes  Forms  Services        👤 User  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Process Designer                     [+ Create New Process]   │
│  Visual BPMN workflow designer with bpmn-js                   │
│                                                                  │
│  DEPLOYED PROCESSES:                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐  │
│  │ 📄               │  │ 📄               │  │ 📄         │  │
│  │ Process Name 1   │  │ Process Name 2   │  │ Process 3  │  │
│  │                  │  │                  │  │            │  │
│  │ Version 3        │  │ Version 1        │  │ Version 2  │  │
│  │ 1 version        │  │ 3 versions       │  │ 2 versions │  │
│  │ deployed         │  │ deployed         │  │ deployed   │  │
│  │                  │  │                  │  │            │  │
│  │ [View] [↓]       │  │ [View] [↓]       │  │ [View] [↓] │  │
│  │ [Delete]         │  │ [Delete]         │  │ [Delete]   │  │
│  └──────────────────┘  └──────────────────┘  └────────────┘  │
│                                                                  │
│  QUICK START:                                                  │
│  ┌──────────────────────┐                                      │
│  │ + Blank Process      │                                      │
│  │ Start with empty     │  [Create]                           │
│  │ BPMN diagram         │                                      │
│  └──────────────────────┘                                      │
│                                                                  │
│  Features & Status:                                            │
│  ✅ Phase 2 Week 4: Process Management & Properties Panel     │
│  ✅ Visual BPMN editor with bpmn-js                           │
│  ✅ Load and edit existing processes from backend             │
│  ... (feature list continues)                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Process Cards - Understanding the Display

### What Each Card Shows

```
┌─────────────────────────────────┐
│ 📄 Process Name                 │  ← Icon + Name of process
│                                 │
│ Version 3 • 1 version deployed  │  ← Latest version + total versions
│                                 │
│ [View] [↓ Download]             │  ← Action buttons
│                                 │
│ [Show all versions]             │  ← Expandable if multiple versions
│                                 │
│ [Delete Process]                │  ← Remove entire deployment
└─────────────────────────────────┘
```

### Card Elements Explained

#### 1. Process Name & Icon
```
📄 My Approval Workflow
```
- **Icon**: FileText icon (indicates BPMN process)
- **Name**: Process display name
- **From**: Backend process definition name

#### 2. Version Information
```
Version 3 • 2 versions deployed
```
- **Version**: Currently deployed version number
- **Deployment Count**: How many versions exist in backend
- **Auto-calculated**: Shows latest version info

#### 3. Primary Actions

**[View] Button**
- Click to open process in visual editor
- Loads BPMN XML from backend
- Shows in bpmn-js canvas
- Allows editing element properties
- Can deploy changes

**[↓ Download] Button**
- Downloads process as XML file
- Format: `{ProcessName}.bpmn20.xml`
- Uses for backup or sharing with others
- File can be imported back later

#### 4. Version History (if multiple versions)

```
[Show all versions]  ← Click to expand
  ├─ v3  [Edit] [↓]   ← Latest version
  ├─ v2  [Edit] [↓]   ← Previous version
  └─ v1  [Edit] [↓]   ← First version
```

- Shows all historical versions
- Can view/edit each version separately
- Can download individual versions
- Newest version listed first

#### 5. Delete Button
```
[Delete Process]
```
- Removes entire deployment (all versions)
- Confirmation dialog appears
- Irreversible action
- Disables while deletion in progress

---

## Data Flow: XML to Display

### How Existing BPMN XML Gets Displayed

```
┌─────────────────────────────────────────────────────────────┐
│  Backend: Flowable Engine (Spring Boot)                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  PostgreSQL Database                                 │   │
│  │  ┌─────────────────────────────────────────────────┐ │   │
│  │  │  BPMN_PROCESS_DEFINITIONS Table                  │ │   │
│  │  │  ┌──────────────────────────────────────────┐   │ │   │
│  │  │  │ id: "proc123"                             │   │ │   │
│  │  │  │ key: "approval_workflow"                 │   │ │   │
│  │  │  │ name: "My Approval Workflow"             │   │ │   │
│  │  │  │ version: 3                               │   │ │   │
│  │  │  │ deploymentId: "deploy456"                │   │ │   │
│  │  │  │ resourceName: "approval.bpmn20.xml"     │   │ │   │
│  │  │  └──────────────────────────────────────────┘   │ │   │
│  │  └─────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ↓ GET /api/flowable/process-definitions
                 │
┌─────────────────────────────────────────────────────────────┐
│  Frontend: Admin Portal (Next.js)                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Processes Page (React Component)                   │   │
│  │  ┌─────────────────────────────────────────────────┐ │   │
│  │  │  1. useQuery fetches process definitions        │ │   │
│  │  │  2. Grouping by process key                     │ │   │
│  │  │  3. Sort versions (newest first)                │ │   │
│  │  │  4. Render Process Cards                        │ │   │
│  │  │     - Name from: process.name                   │ │   │
│  │  │     - Version from: process.version             │ │   │
│  │  │     - Count from: versions array length         │ │   │
│  │  └─────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│         ↓                                                    │
│    Browser Display (Card Grid)                              │
└─────────────────────────────────────────────────────────────┘
```

---

## User Journey: Viewing an Existing Process Design

### Step 1: Navigate to Processes Page

```
Landing Page (http://localhost:4000)
         ↓
    Click "Process Studio"
         ↓
    Auth Check (Keycloak)
         ↓
    Studio Layout Loads
         ↓
    You see header with tabs:
    Processes | Forms | Services
         ↓
    Processes page already showing
    (or click "Processes" tab)
```

### Step 2: See Existing Processes

```
Page loads and displays:
    ↓
useQuery gets process definitions
    ↓
groupedProcesses calculates:
  {
    "approval_workflow": [
      { id: "v3", version: 3, name: "My Approval..." },
      { id: "v2", version: 2, name: "My Approval..." },
      { id: "v1", version: 1, name: "My Approval..." }
    ],
    "hr_request_process": [
      { id: "v1", version: 1, name: "HR Request..." }
    ]
  }
    ↓
Render process cards
```

### Step 3: Click "View" to Edit

```
Click [View] button on process card
         ↓
    Navigate to: /studio/processes/edit/{processId}
         ↓
    Edit page loads (different component)
    (/studio/processes/edit/[id]/page.tsx)
         ↓
    1. getProcessDefinitionXml(processId) called
    2. XML fetched from backend
    3. bpmn-js Modeler created
    4. XML loaded into canvas
         ↓
    You see BPMN diagram visualized
    - All shapes rendered
    - All connections shown
    - Properties panel on right
         ↓
    You can:
    - Click elements to select
    - View properties
    - Edit properties
    - Add new elements
    - Save/deploy changes
```

---

## Key Component: Process Card Rendering

### Source Code Location
`frontends/admin-portal/app/(studio)/processes/page.tsx` (lines 82-177)

### How It Works

```typescript
// Grouped processes structure:
const groupedProcesses = {
  "approval_workflow": [
    { id: "abc123", name: "My Approval", version: 3, ... },
    { id: "abc122", name: "My Approval", version: 2, ... },
    { id: "abc121", name: "My Approval", version: 1, ... }
  ],
  "expense_approval": [
    { id: "xyz789", name: "Expense", version: 1, ... }
  ]
}

// For each process key:
Object.entries(groupedProcesses).map(([key, versions]) => {
  // Sort by version descending (newest first)
  const latestVersion = versions.sort((a,b) => b.version - a.version)[0]

  // Render card showing:
  // - Latest version info (name, version, count)
  // - View button → /studio/processes/edit/{latestVersion.id}
  // - Download button → getProcessDefinitionXml()
  // - Delete button → deleteDeployment()
  // - Version details (if multiple versions)
})
```

---

## Special Cases

### Empty State (No Processes)

If no processes deployed yet:

```
┌─────────────────────────────────────────────┐
│         No processes deployed yet           │
│                                             │
│      📄 (large icon)                        │
│                                             │
│   Create your first BPMN process            │
│   to get started                            │
│                                             │
│   [+ Create New Process]                    │
└─────────────────────────────────────────────┘
```

### Loading State

While fetching processes:

```
┌─────────────────────────────────────────────┐
│                                             │
│   Loading process definitions...            │
│                                             │
└─────────────────────────────────────────────┘
```

### Version History Expansion

When process has 3+ versions:

```
BEFORE:
┌──────────────────────┐
│ Process Name         │
│ Version 3            │
│ 3 versions deployed  │
│                      │
│ [View] [↓]           │
│                      │
│ [Show all versions]  │ ← Click to expand
│ [Delete]             │
└──────────────────────┘

AFTER (expanded):
┌──────────────────────────────────┐
│ Process Name                     │
│ Version 3                        │
│ 3 versions deployed              │
│                                  │
│ [View] [↓]                       │
│                                  │
│ ▼ Hide all versions              │
│   ┌────────────────────────────┐ │
│   │ v3 [Edit] [↓]              │ │ ← Latest
│   │ v2 [Edit] [↓]              │ │
│   │ v1 [Edit] [↓]              │ │
│   └────────────────────────────┘ │
│ [Delete]                         │
└──────────────────────────────────┘
```

---

## File & Edit Page vs Processes Page

### Processes Page (`/studio/processes`)
- **Purpose**: List all deployed processes
- **Shows**: Process names, versions, deployment metadata
- **Actions**: View (go to edit), Download XML, Delete
- **No Editing**: Just display and navigation
- **API Called**: `GET /flowable/process-definitions`

### Edit Page (`/studio/processes/edit/[id]`)
- **Purpose**: Visual editor for single process
- **Shows**: BPMN diagram rendered by bpmn-js
- **Actions**: Add/edit elements, edit properties, save, deploy
- **Full Editing**: Complete BPMN designer
- **API Called**: `GET /flowable/process-definitions/{id}/xml`
- **Component**: Different file (not shown here)

---

## File Information

### Key Files

| File | Purpose |
|------|---------|
| `app/(studio)/processes/page.tsx` | Processes list page (what you see) |
| `lib/api/flowable.ts` | API client for Flowable backend |
| `components/ui/card.tsx` | Card component (from shadcn) |
| `components/ui/button.tsx` | Button component (from shadcn) |

### API Endpoints Used

```typescript
// From lib/api/flowable.ts

// Get all processes
GET /flowable/process-definitions
Response: ProcessDefinitionResponse[]

// Get process XML
GET /flowable/process-definitions/{processId}/xml
Response: string (XML)

// Delete process
DELETE /flowable/deployments/{deploymentId}
Response: void

// Deploy new process
POST /flowable/deployments
Body: { name, resourceName, bpmnXml }
Response: DeploymentResponse
```

---

## Styling & UX Details

### Design System
- **UI Framework**: shadcn/ui (Radix UI + Tailwind CSS)
- **Layout**: Responsive grid (md: 2 columns, lg: 3 columns)
- **Spacing**: py-6 (padding vertical)
- **Colors**: Primary for buttons, muted-foreground for text

### Button Hierarchy
1. **Primary**: [View] - Main action (blue)
2. **Secondary**: [↓ Download] - Alternative action (outline)
3. **Tertiary**: [Delete] - Destructive action (red)
4. **Info**: [Show all versions] - Additional info (text/details)

### Icons Used
- **FileText** (📄) - Process icon
- **Plus** (+) - Create new action
- **Trash2** (🗑) - Delete action
- **Download** (↓) - Download action
- **Eye** (👁) - View action

---

## Summary Table

| Aspect | Details |
|--------|---------|
| **URL** | `http://localhost:4000/studio/processes` |
| **Component** | `processes/page.tsx` |
| **Route** | `(studio)/processes/page.tsx` |
| **Purpose** | Display deployed BPMN processes |
| **Requires** | HR_ADMIN role + authenticated session |
| **Data Source** | Backend Flowable process definitions |
| **Actions** | View, Download, Delete, Create new |
| **Edit Via** | Click [View] → goes to `/edit/[id]` |
| **Download Format** | BPMN 2.0 XML (`.bpmn20.xml`) |
| **Responsive** | Yes (1, 2, or 3 columns based on screen) |

---

## How to Use It

### To View Existing BPMN Processes:

1. **Go to landing page**: `http://localhost:4000`
2. **Click "Process Studio"** button
3. **Wait for auth** (may redirect to Keycloak login)
4. **You'll be on /studio/processes automatically**
5. **See process cards** - each represents a deployed BPMN
6. **Click [View]** on any process to see the visual design

### To Edit a Process:

1. On processes page, click **[View]** button
2. BPMN diagram loads in visual editor
3. Edit elements, properties, connections
4. Deploy when ready

### To Download BPMN XML:

1. On processes page, click **[↓]** button
2. XML file downloads to Downloads folder
3. Can open in any BPMN tool or import back

### To Delete Process:

1. Click **[Delete]** button
2. Confirm deletion (irreversible)
3. All versions removed from backend

---

## Architecture Summary

```
┌──────────────────────────────────────────────────────┐
│  Admin Portal Landing Page (/)                       │
│  Clean, simple design with 3 main action cards       │
└────────────────┬─────────────────────────────────────┘
                 │
                 └─→ Click "Process Studio"
                         ↓
    ┌───────────────────────────────────────────────┐
    │  Studio Layout (/studio/*)                    │
    │  Header with Processes | Forms | Services     │
    └────────────────────────────┬──────────────────┘
                                 │
                                 ├─→ /studio/processes ← You are here
                                 │   Process list & cards
                                 │   ↓ Click View
                                 │   /studio/processes/edit/[id]
                                 │   BPMN visual editor
                                 │
                                 ├─→ /studio/forms
                                 │   Form builder
                                 │
                                 └─→ /studio/services
                                     Service registry
```

---

## Conclusion

The **Processes Page** (`/studio/processes`) is your main UI for:
- ✅ Viewing all deployed BPMN processes
- ✅ Seeing process versions and deployment info
- ✅ Accessing the visual editor
- ✅ Downloading BPMN XML files
- ✅ Managing process deployments

It displays **existing BPMN XML designs** that are stored in the backend database as deployed process definitions, making them easily accessible and manageable through an intuitive card-based interface.

