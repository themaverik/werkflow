# Admin Portal Navigation Guide

## Overview

The **Werkflow Admin Portal** (`http://localhost:4000`) is your central hub for designing and managing BPMN processes and forms. It features a clean, intuitive interface with three main sections.

---

## Landing Page - Clean & Simple Design

**URL**: `http://localhost:4000`

The landing page provides a welcoming overview with three main action buttons:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Welcome to Werkflow                          │
│        Visual BPMN Workflow Designer & HR Management            │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ Process Studio   │  │  Form Builder    │  │  My Tasks    │  │
│  │  Design BPMN     │  │  Create Forms    │  │ View Tasks   │  │
│  │  workflows       │  │  & Link to Tasks │  │ & Complete   │  │
│  │      →           │  │       →          │  │      →       │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│                                                                  │
│  Built with: Next.js 14, React 18, Tailwind CSS, bpmn-js      │
│  Backend: Spring Boot 3 + Flowable BPM + Keycloak             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Studio Layout - Access Requirements

The **Studio** section (`/studio/*`) requires authentication with the **HR_ADMIN** role.

### Authentication Check

```
Landing Page → Click Studio Link → Auth Check
                                    ├─ Has HR_ADMIN role → Access granted
                                    └─ No HR_ADMIN role → Access denied
```

**Error Page** (if no HR_ADMIN role):
- Clean message: "Access Denied"
- Shows your current roles
- Requests administrator contact to gain access

---

## Processes Page - Viewing Existing BPMN Designs

### URL & Navigation

**Direct URL**: `http://localhost:4000/studio/processes`

**Via Navigation**:
1. Go to landing page: `http://localhost:4000`
2. Click "Process Studio" button
3. Wait for auth check (needs HR_ADMIN role)
4. You'll see the Studio header with navigation tabs
5. Click "Processes" tab (or you're already there)

### Studio Header Navigation
```
┌────────────────────────────────────────────────────────────┐
│  Werkflow Studio    Processes | Forms | Services    User   │
└────────────────────────────────────────────────────────────┘
```

Three main sections accessible from Studio:
1. **Processes** - BPMN workflow designer
2. **Forms** - Form builder (Form.io)
3. **Services** - Service registry for microservices

---

## Processes Page - Features & Interface

### Layout & Content

```
┌─────────────────────────────────────────────────────────────┐
│  Process Designer                 [+ Create New Process]   │
│  Visual BPMN workflow designer with bpmn-js               │
│                                                             │
│  DEPLOYED PROCESSES:                                       │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ Process Name 1   │  │ Process Name 2   │  ...          │
│  │ Version 3        │  │ Version 1        │               │
│  │ 1 version        │  │ 3 versions       │               │
│  │                  │  │                  │               │
│  │ [View] [↓]       │  │ [View] [↓]       │               │
│  │ [Delete]         │  │ [Delete]         │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                             │
│  QUICK ACTIONS:                                            │
│  ┌──────────────────────┐                                  │
│  │ + Blank Process      │                                  │
│  │ Start with empty     │  [Create]                        │
│  │ BPMN diagram         │                                  │
│  └──────────────────────┘                                  │
└─────────────────────────────────────────────────────────────┘
```

### Existing Process Cards

Each deployed process displays:

**Card Information**:
- **Process Name** with FileText icon
- **Version Info**: "Version X • Y version(s) deployed"
- **Latest Version**: Always shows the most recent version

**Card Actions**:

1. **[View]** Button - Opens process editor
   - Load existing BPMN XML from backend
   - View current design in visual editor
   - Edit properties and elements
   - See element configurations

2. **[↓] Download** Button - Download as XML
   - Exports BPMN20 XML file
   - Format: `{ProcessName}.bpmn20.xml`
   - Use for backup or sharing

3. **Show all versions** (if multiple versions)
   - Expandable details section
   - Lists all historical versions
   - Can view/download each version separately
   - Version numbers shown (v1, v2, etc.)

4. **[Delete]** Button - Remove all versions
   - Removes entire deployment
   - Confirmation required
   - Irreversible action

---

## Key Features & Capabilities

### 1. Visual BPMN Designer
- **drag-and-drop** interface for workflow elements
- Add tasks, gateways, events, connections
- Real-time visual feedback
- Zoom controls (in, out, fit to viewport)

### 2. Load Existing Processes
- Click **[View]** on any deployed process
- Visual editor loads the XML from backend
- Full edit capabilities for existing designs
- Save changes back to backend

### 3. Properties Panel
- Configure element properties:
  - Task assignees and groups
  - Form keys for task forms
  - Condition expressions
  - Timers and deadlines
  - Flowable-specific attributes

### 4. Process Management
- **Create** new processes from blank template
- **View** existing designs visually
- **Edit** process definitions
- **Download** XML for backup/sharing
- **Delete** outdated processes
- **Version history** tracking

### 5. One-Click Deployment
- Deploy updated processes to Flowable backend
- Automatic versioning
- No manual backend configuration needed

---

## Data Flow: XML to Visual Display

```
Backend (Flowable)                    Frontend (Admin Portal)
    ↓                                        ↓
 BPMN XML                            Display in Page
 (stored in DB)      ←─────────────→   JSON Response
    ↓                                        ↓
Process Definition List              Process Cards
 - Version tracking                   - Name, Version
 - Deployment metadata                - Actions (View/Edit/Download/Delete)
    ↓                                        ↓
On Click "View"                      Editor Loads XML
 - Fetch XML content                  ↓
 - Send to frontend                   BPMN.js renders
    ↓                                   visual diagram
Diagram XML                           Users can edit
(bpmn-js renders)                     & save changes
```

---

## Troubleshooting

### Issue: 404 Error on `/studio/processes`

**Cause 1: Not Authenticated**
- Solution: Log in first via login page
- Browser should redirect automatically

**Cause 2: Missing HR_ADMIN Role**
- Solution: Contact administrator
- Need HR_ADMIN role assigned in Keycloak
- Current roles shown on access denied page

**Cause 3: Services Not Running**
- Solution: Verify Docker services running
  ```bash
  docker-compose ps
  # Should see: engine-service, admin-service, etc. as "Up"
  ```

### Issue: Processes Not Loading

**Check**:
1. Backend connectivity: Can reach `http://localhost:8081/api/flowable/process-definitions`
2. No deployments yet: Show empty state "No processes deployed yet"
3. Server error: Check browser console for detailed error

**Solutions**:
```bash
# Check engine service logs
docker logs werkflow-engine

# Verify API endpoint
curl http://localhost:8081/api/flowable/process-definitions

# Create first process to populate list
# Via UI: Click "+ Create New Process"
```

### Issue: Can't Download Process XML

**Possible Causes**:
- Network issue
- Backend server down
- Invalid process ID

**Check**:
```bash
# Test backend is responding
curl http://localhost:8081/api/flowable/process-definitions/{processId}/xml
```

---

## API Endpoints Reference

The Process page calls these backend endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/flowable/process-definitions` | GET | List all processes |
| `/flowable/process-definitions/{id}/xml` | GET | Fetch BPMN XML |
| `/flowable/deployments/{id}` | DELETE | Delete deployment |
| `/flowable/deployments` | POST | Deploy new process |

All requests go through the **Engine Service** (`http://localhost:8081/api`)

---

## Design Philosophy

The Admin Portal's design is intentionally **clean and simple**:

### Landing Page
- Single, focused value proposition
- Three clear action buttons
- Minimal navigation
- Professional appearance

### Studio Layout
- Clean header with minimal branding
- Simple tab navigation (Processes, Forms, Services)
- User menu in top right
- Plenty of whitespace

### Processes Page
- Card-based layout for visual grouping
- Consistent button styling
- Clear action hierarchy
  - Primary: View (see design)
  - Secondary: Download (backup)
  - Tertiary: Delete (cleanup)
- Empty state guidance

### UI Components Used
- shadcn/ui components (based on Radix UI)
- Tailwind CSS for styling
- Lucide icons for visual clarity
- Responsive grid layout

---

## Next Steps

1. **Ensure you have HR_ADMIN role**
   - Contact your administrator
   - Or add to Keycloak (if you're admin)

2. **Access the Process Studio**
   - Go to: `http://localhost:4000`
   - Click: "Process Studio"
   - Wait for auth redirect

3. **View Existing Processes**
   - Click "Processes" in studio header
   - See deployed process definitions
   - Click [View] to edit any process

4. **Create New Processes**
   - Click "+ Create New Process"
   - Start from blank template
   - Use drag-and-drop designer
   - Configure element properties
   - Deploy when ready

5. **View Other Features**
   - **Forms**: Create dynamic forms with Form.io
   - **Services**: Register microservice endpoints
   - **My Tasks**: Go to HR Portal to view assigned tasks

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Admin Portal (Next.js)                  │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Landing Page (/)          Clean & Simple Design      │ │
│  │  Studio Layout (/studio/)  Header + Navigation        │ │
│  │  Processes (/studio/processes) Card Layout            │ │
│  │  Forms (/studio/forms)     Form Builder               │ │
│  │  Services (/studio/services) Service Registry         │ │
│  └───────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓ HTTP API Calls
                     │
┌────────────────────────────────────────────────────────────┐
│         Backend Services (Spring Boot)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Engine       │  │ Admin        │  │ HR Service   │    │
│  │ Service      │  │ Service      │  │              │    │
│  │ (Flowable)   │  │              │  │              │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│         ↓
│    Flowable BPM Engine
│         ↓
│    PostgreSQL Database
└────────────────────────────────────────────────────────────┘
```

---

## Summary

The **Admin Portal** provides:
- ✅ Clean, simple landing page
- ✅ Intuitive studio layout
- ✅ Visual BPMN process designer (processes page)
- ✅ Form builder integration
- ✅ Service registry management
- ✅ Full process lifecycle management
- ✅ XML export/import capability
- ✅ Version tracking
- ✅ Role-based access control

**Current URL**: `http://localhost:4000`
**Processes Page**: `http://localhost:4000/studio/processes`
**Direct Access**: Studio header → Processes tab (after authentication)

