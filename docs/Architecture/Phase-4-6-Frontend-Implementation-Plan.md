# Phase 4 and Phase 6 Frontend Implementation Plan

**Date**: 2025-11-24
**Project**: Werkflow HR Platform
**Scope**: BPMN Editor Enhancement and Task Portal UI
**Related Documents**:
- BPMN-Visual-Delegate-Editor-Feasibility-Analysis.md
- BPMN-Visual-Delegate-Editor-Implementation-Guide.md
- Keycloak-RBAC-Role-Matrix-Design.md

---

## Executive Summary

This document provides a detailed architectural and task breakdown for:
- **Phase 4**: BPMN Editor Enhancement for visual delegate configuration
- **Phase 6**: Task Portal and Approval UI with DOA-based visibility

Both phases focus on frontend component architecture, user experience patterns, and integration strategies with backend services.

---

## PHASE 4: BPMN EDITOR ENHANCEMENT

### Overview

Enable visual configuration of three delegate patterns without manual XML editing:
1. REST Service Delegate - Inter-service HTTP calls
2. Local Service Delegate - Spring bean execution
3. Notification Delegate - Email/SMS/Push notifications

### Timeline: 4 Weeks

**Week 1-2**: REST Service Pattern UI (MVP)
**Week 3**: Local Service and Notification Pattern UI
**Week 4**: Testing, Polish, and Integration

---

### 4.1 REST Service Pattern UI Implementation

#### 4.1.1 Component Architecture

**Primary Components**:

1. **ServiceTaskPropertiesPanel** (Orchestrator)
   - Responsibility: Pattern detection, pattern routing, state coordination
   - State Management: Track current pattern, delegate expression, selected service
   - Integration Point: BPMN-js properties panel entry point
   - Dependencies: Pattern detector utility, BPMN modeler instance

2. **RestServicePatternUI** (Specialized Form)
   - Responsibility: Visual configuration for HTTP service calls
   - Form Fields: Service selector, endpoint picker, HTTP method, request body, response variable
   - Validation: JSON syntax, URL format, variable name patterns
   - Auto-fill Logic: Construct full URL from service base URL and endpoint path

3. **ExtensionElementsEditor** (XML Synchronizer)
   - Responsibility: Bidirectional sync between UI form and BPMN XML
   - XML Operations: Create flowable:field elements, handle string vs expression types
   - Status: Already 100% functional, reuse as-is

**Component Hierarchy**:
```
BpmnDesigner (Root)
└── Properties Panel (BPMN-js)
    └── FlowablePropertiesProvider
        └── ServiceTaskPropertiesPanel
            ├── Pattern Selector (Dropdown: REST/Local/Notification)
            ├── RestServicePatternUI (Conditional Render)
            │   ├── Service Selector (useServices hook)
            │   ├── Endpoint Selector (useServiceEndpoints hook)
            │   ├── HTTP Method Selector
            │   ├── Request Body Editor (Textarea with JSON validation)
            │   └── Response Variable Input
            ├── LocalServicePatternUI (Conditional Render)
            └── NotificationPatternUI (Conditional Render)
```

#### 4.1.2 Step-by-Step UI Tasks

**Task 1: Pattern Detection System** (2 days)
- Create pattern-detector utility module
- Implement detection algorithm: Analyze delegateExpression attribute and extension fields
- Define TypeScript interfaces: DelegatePattern enum, PatternInfo structure
- Handle edge cases: Missing delegate expression, unknown patterns, legacy formats
- Unit test coverage: All three patterns plus edge cases

**Task 2: Service Registry Integration** (1 day)
- Verify useServices React hook functionality
- Implement service filtering by environment
- Add loading states and error handling
- Mock data validation: Ensure UI works without backend
- Cache strategy: React Query with 60-second stale time

**Task 3: Endpoint Path Builder** (2 days)
- Cascading dropdown logic: Service selection triggers endpoint load
- Display format: HTTP method badge with visual distinction (GET=blue, POST=green, etc.)
- URL construction: Combine service baseUrl with endpoint path
- Endpoint metadata display: Show description, parameters, expected response
- Validation: Ensure selected endpoint exists and service is healthy

**Task 4: Request Body Editor** (3 days)
- JSON textarea with monospace font
- Real-time validation: Parse JSON on blur, show error indicators
- Process variable hints: Display available variables with autocomplete suggestion
- Expression builder helper: Inline variable insertion UI
- Template snippets: Common patterns like pagination, authentication
- Preview mode: Show final resolved JSON with variable substitution

**Task 5: HTTP Configuration Fields** (1 day)
- Method selector: Dropdown with GET, POST, PUT, PATCH, DELETE
- Response variable input: Text field with identifier validation
- Header configuration: Key-value pairs with add/remove capability
- Timeout settings: Optional advanced configuration
- Retry policy: Optional exponential backoff configuration

**Task 6: Extension Field Auto-Population** (2 days)
- Map UI form values to flowable:field elements
- Determine field type: string vs expression based on content
- Handle dynamic fields: Add fields only when user provides values
- Update strategy: Debounced updates to avoid excessive XML regeneration
- Conflict resolution: Handle manual XML edits vs UI changes

**Task 7: Visual Preview and Validation** (2 days)
- Preview card: Show complete HTTP call configuration
- Visual indicators: Success/warning/error states
- Connectivity test: Optional ping to verify service availability
- XML preview: Show generated BPMN XML in collapsible section
- Validation summary: List all configuration issues before save

#### 4.1.3 Component Dependencies and Data Flow

**Data Flow: User Configuration to BPMN XML**

1. User selects service from dropdown
   - Trigger: onChange event
   - Action: useServiceEndpoints hook fetches endpoints
   - State Update: selectedService, availableEndpoints

2. User selects endpoint
   - Trigger: onChange event
   - Action: Extract method, path, parameters from endpoint object
   - State Update: selectedEndpoint, httpMethod, urlPath
   - Side Effect: Auto-populate URL field in extension elements

3. User enters request body JSON
   - Trigger: onBlur event (debounced)
   - Validation: JSON.parse() check
   - Success Path: Convert to expression format, update extension fields
   - Error Path: Show validation error, prevent save

4. Extension fields update
   - Trigger: Any configuration change
   - Action: Call ExtensionElementsEditor.updateExtensionElements()
   - BPMN Update: moddle.create() for each field, modeling.updateProperties()
   - XML Result: Flowable-compliant serviceTask with extensionElements

**Dependency Map**:
- ServiceTaskPropertiesPanel depends on: BPMN modeler, pattern detector
- RestServicePatternUI depends on: Service Registry API, form validation utilities
- ExtensionElementsEditor depends on: BPMN moddle service, modeling service
- Service Registry hooks depend on: React Query, API client, mock data fallback

#### 4.1.4 Testing Strategy for REST Pattern UI

**Unit Tests** (3 days)
- Pattern detection accuracy: Test all delegate expression formats
- Service selector filtering: Environment-based filtering
- Endpoint parsing: Extract method, path, parameters correctly
- JSON validation: Valid/invalid JSON strings, expression syntax
- Extension field mapping: UI values to BPMN field elements
- URL construction: Base URL concatenation logic

**Integration Tests** (2 days)
- Full workflow: Select service, configure endpoint, save BPMN
- BPMN import: Load existing BPMN with REST delegate, verify UI population
- Pattern switching: Change from REST to Local, verify cleanup
- Service Registry mock: UI works without backend connection
- Error scenarios: Service unavailable, invalid JSON, missing fields

**Manual QA Checklist**:
- Create new service task, verify blank slate
- Select finance service, verify endpoints load
- Configure budget check endpoint, verify URL auto-fill
- Enter request body with variables, verify expression format
- Save and download BPMN, verify XML structure
- Re-import BPMN, verify properties populate correctly
- Test undo/redo operations
- Test on Safari, Chrome, Firefox, Edge

#### 4.1.5 Integration Points with Backend APIs

**Service Registry API**:
- Endpoint: GET /api/v1/registry/services?environment=dev
- Response Format: Array of Service objects with id, name, baseUrl, endpoints
- Error Handling: Fallback to mock data, display warning banner
- Caching: React Query 60-second cache, manual refresh option

**BPMN Deployment API**:
- Endpoint: POST /api/process-definitions/deploy
- Payload: multipart/form-data with BPMN XML file
- Validation: Server-side XML schema validation
- Response: Deployment ID, version, validation errors

**Service Connectivity Test**:
- Endpoint: POST /api/v1/registry/test-connectivity
- Payload: service URL
- Response: HTTP status, response time, reachability
- UI Display: Green/yellow/red indicator with latency

#### 4.1.6 User Experience Considerations

**Progressive Disclosure**:
- Show pattern selector first, hide complex fields
- Reveal endpoint configuration only after service selection
- Display advanced options in collapsible sections
- Provide contextual help tooltips

**Visual Feedback**:
- Loading spinners during service/endpoint fetch
- Success animations on successful configuration
- Error messages with actionable remediation steps
- Preview updates in real-time as user types

**Keyboard Navigation**:
- Tab order follows logical flow
- Escape key closes dialogs
- Enter key submits forms
- Arrow keys navigate dropdowns

**Accessibility**:
- ARIA labels for all form fields
- Screen reader announcements for dynamic updates
- Focus management when switching patterns
- Color contrast ratio compliance WCAG 2.1 AA

**Error Prevention**:
- Disable save button until valid configuration
- Warn before pattern switch if unsaved changes
- Confirmation dialog for destructive actions
- Auto-save draft to browser localStorage

---

### 4.2 Local Service Pattern UI Implementation

#### 4.2.1 Component Architecture

**LocalServicePatternUI** (Simplified Form)
- Responsibility: Configure Spring bean delegate execution
- Form Fields: Bean name selector with autocomplete
- Bean Registry: Hardcoded list of common delegates, custom bean option
- Validation: Valid Java identifier pattern

#### 4.2.2 Step-by-Step UI Tasks

**Task 1: Bean Selector Component** (1 day)
- Combobox with common delegates: vendorValidationDelegate, quoteValidationDelegate, etc.
- Custom bean input: Free-text field for user-defined beans
- Bean documentation: Inline description for each common delegate
- Validation: Java identifier format, no reserved keywords

**Task 2: Delegate Expression Update** (0.5 days)
- Format: Wrap bean name in Spring expression syntax ${beanName}
- BPMN Update: modeling.updateProperties() with delegateExpression
- No extension elements needed for local delegates
- Preview: Show final delegate expression

**Task 3: Testing and Integration** (0.5 days)
- Unit tests: Bean name validation, expression formatting
- Integration: Switch from REST to Local pattern
- Manual QA: Create local service task, verify XML

---

### 4.3 Notification Pattern UI Implementation

#### 4.3.1 Component Architecture

**NotificationPatternUI** (Template-Based Form)
- Responsibility: Configure notification delegate with templates
- Form Fields: Notification type selector, template picker, recipient variables
- Template Registry: Hardcoded notification types from backend enum

#### 4.3.2 Step-by-Step UI Tasks

**Task 1: Notification Type Selector** (0.5 days)
- Dropdown: PR_BUDGET_SHORTFALL, PR_MANAGER_REJECTED, PO_CREATED, etc.
- Category grouping: Procurement, HR, Finance notifications
- Preview: Show template description and sample content

**Task 2: Template Configuration** (1 day)
- Template metadata display: Subject, body preview, required variables
- Variable mapping: Map process variables to template placeholders
- Recipient selector: Autocomplete for user emails, role-based distribution
- Preview mode: Render template with sample data

**Task 3: Extension Field Generation** (0.5 days)
- Single field: notificationType with string value
- Optional fields: recipientOverride, templateOverride for custom scenarios
- BPMN Update: Create flowable:field for notification type

**Task 4: Testing** (0.5 days)
- Unit tests: Notification type validation, field generation
- Integration: Switch patterns, verify cleanup
- Manual QA: Configure notification, verify XML

---

### 4.4 Pattern Switching Logic

#### 4.4.1 Architecture

**Pattern Router Responsibility**:
- Detect current pattern from BPMN element
- Unmount current pattern UI component
- Clear irrelevant extension fields
- Mount new pattern UI component
- Preserve common fields if applicable

#### 4.4.2 Implementation Tasks

**Task 1: Pattern Change Handler** (1 day)
- State management: Track previous and current pattern
- Cleanup logic: Remove extension fields specific to old pattern
- Confirmation dialog: Warn user of data loss if configuration exists
- Animation: Smooth transition between pattern UIs

**Task 2: State Preservation** (0.5 days)
- Draft state: Save incomplete configuration to localStorage
- Restore logic: Repopulate fields if user switches back
- Expiration: Clear draft after 24 hours or explicit discard

**Task 3: Edge Case Handling** (0.5 days)
- Unknown pattern: Default to manual XML editor
- Corrupted XML: Parse error handling with fallback
- Legacy BPMN: Support old delegate formats with migration prompt

---

### 4.5 Service Registry Dropdown Integration

#### 4.5.1 Architecture

**Service Registry Client**:
- Location: /lib/api/services.ts (already implemented)
- Mock Fallback: 4 services with full endpoint definitions
- React Query Integration: useServices, useServiceEndpoints hooks

#### 4.5.2 Implementation Tasks

**Task 1: Verify Mock Data** (0.5 days)
- Validate mock service structure matches API contract
- Test all endpoints return valid data
- Ensure health check simulation works

**Task 2: Dropdown Population** (Already Complete)
- Service selector uses useServices hook
- Endpoint selector uses useServiceEndpoints hook
- Loading states handled by React Query
- Error states show user-friendly messages

**Task 3: Backend Connection Readiness** (0.5 days)
- API client configured with base URL
- Authentication headers prepared
- Error handling for network failures
- Fallback to mock on API unavailable

---

### 4.6 Testing Strategy Summary

#### 4.6.1 Unit Testing Approach

**Tools**: Vitest, React Testing Library
**Coverage Target**: 80% code coverage

**Test Categories**:
1. Pattern detection logic
2. Form validation functions
3. Extension field mapping
4. URL construction utilities
5. JSON parsing and validation

**Mock Strategy**:
- Mock BPMN modeler instance
- Mock modeling and moddle services
- Mock Service Registry API responses
- Mock React Query hooks

#### 4.6.2 Integration Testing Approach

**Tools**: Vitest with jsdom, Testing Library

**Test Scenarios**:
1. Create service task, configure REST delegate, verify XML
2. Import BPMN with delegate, verify UI population
3. Switch patterns, verify state cleanup
4. Service Registry unavailable, verify mock fallback
5. Undo/redo operations maintain state consistency

#### 4.6.3 End-to-End Testing Approach

**Tools**: Playwright

**Critical User Journeys**:
1. New workflow creation with REST service task
2. Configure finance service budget check
3. Deploy to Flowable
4. Start process instance, verify REST call executes
5. Edit existing workflow, modify delegate configuration

#### 4.6.4 Accessibility Testing

**Tools**: axe-core, NVDA/JAWS screen readers

**Checklist**:
- All form fields have labels
- Error messages announced to screen readers
- Focus visible on all interactive elements
- Color contrast meets WCAG AA
- Keyboard navigation works without mouse

---

### 4.7 Performance Considerations

#### 4.7.1 Optimization Strategies

**Component Memoization**:
- useMemo for pattern detection result
- useMemo for filtered endpoint list
- useCallback for event handlers

**Lazy Loading**:
- Code split pattern UI components
- Load only active pattern component
- Suspense boundaries with loading fallback

**Service Registry Caching**:
- React Query 60-second stale time
- Background refetch on window focus
- Persistent cache across page navigations

#### 4.7.2 Performance Budgets

**Target Metrics**:
- Properties panel render: Less than 100ms
- Pattern switch: Less than 200ms
- Service dropdown load: Less than 500ms
- BPMN save operation: Less than 1 second

**Monitoring**:
- React DevTools Profiler
- Chrome Lighthouse performance audit
- Real User Monitoring in production

---

### 4.8 Implementation Checklist (4-Week MVP)

#### Week 1: Foundation and REST Pattern MVP
- [ ] Create pattern-detector.ts utility module
- [ ] Define TypeScript interfaces for all patterns
- [ ] Implement REST pattern detection logic
- [ ] Create RestServicePatternUI component skeleton
- [ ] Integrate Service Registry dropdown
- [ ] Implement endpoint selector with cascading logic
- [ ] Add HTTP method selector
- [ ] Unit tests for pattern detection

#### Week 2: REST Pattern Completion
- [ ] Build request body editor with JSON validation
- [ ] Add response variable input field
- [ ] Implement extension field auto-population
- [ ] Create preview card component
- [ ] Add connectivity test integration
- [ ] Complete unit tests for REST pattern
- [ ] Integration testing for REST workflow
- [ ] Bug fixes and edge case handling

#### Week 3: Additional Patterns
- [ ] Create LocalServicePatternUI component
- [ ] Implement bean selector with autocomplete
- [ ] Create NotificationPatternUI component
- [ ] Implement notification type selector
- [ ] Add template preview functionality
- [ ] Implement pattern switching logic
- [ ] Add confirmation dialogs for pattern changes
- [ ] Integration tests for all three patterns

#### Week 4: Polish and Production Readiness
- [ ] E2E tests with Playwright
- [ ] Accessibility audit and fixes
- [ ] Performance optimization
- [ ] Documentation: User guide and developer docs
- [ ] Error handling refinement
- [ ] Loading states and animations
- [ ] Final QA across browsers
- [ ] Deploy to staging environment

---

## PHASE 6: TASK PORTAL AND APPROVAL UI

### Overview

Build user-facing task management interface with:
- Role-based task visibility using Keycloak JWT claims
- DOA-based approval workflows
- Manager delegation capabilities
- Touchpoint notification system

### Timeline: 3 Weeks

**Week 1**: Task list and claiming interface
**Week 2**: Approval workflow UI with DOA logic
**Week 3**: Delegation UI and notification integration

---

### 6.1 Task Management Interface Architecture

#### 6.1.1 Component Architecture

**Primary Components**:

1. **TaskPortal** (Container)
   - Responsibility: Layout, navigation, role-based routing
   - State Management: Global filter state, selected task, notification count
   - Route Structure: /tasks (list), /tasks/:id (detail), /tasks/delegated (delegation view)

2. **TaskList** (Data Grid)
   - Responsibility: Display tasks with filtering, sorting, pagination
   - Columns: Task name, process name, assignee, due date, priority, status
   - Filters: Department, status, date range, assigned to me/my team
   - Actions: Claim, complete, delegate, view details

3. **TaskDetail** (Form Container)
   - Responsibility: Show task form, process context, history
   - Sections: Task info, form data, process variables, audit trail
   - Actions: Complete, reject, delegate, request information

4. **TaskClaimButton** (Action Component)
   - Responsibility: Claim unassigned tasks, verify user eligibility
   - Authorization Check: Role-based, group membership, DOA level
   - Optimistic Update: Immediate UI feedback, rollback on error

**Component Hierarchy**:
```
TaskPortal (Root)
├── TaskHeader
│   ├── Notification Bell (unread count)
│   ├── User Menu (profile, settings, logout)
│   └── Quick Filters (my tasks, team tasks, all tasks)
├── TaskSidebar
│   ├── Department Filter
│   ├── Status Filter
│   ├── Priority Filter
│   └── Saved Views
└── TaskContent
    ├── TaskList (Grid View)
    │   ├── TaskCard (List Item)
    │   │   ├── Task Metadata
    │   │   ├── Action Buttons
    │   │   └── Priority Indicator
    │   └── Pagination Controls
    └── TaskDetail (Detail View)
        ├── Task Header (name, process, status)
        ├── Form Section (dynamic form rendering)
        ├── Process Context (variables, diagram position)
        ├── History Timeline (previous approvers, comments)
        └── Action Panel (complete, reject, delegate)
```

#### 6.1.2 Step-by-Step Task Management Tasks

**Task 1: Task List API Integration** (2 days)
- Flowable API endpoint: GET /runtime/tasks
- Query parameters: assignee, candidateGroups, processDefinitionKey, sorting
- Response mapping: Transform Flowable task DTO to UI task model
- Pagination: Implement server-side pagination with page size 20
- Error handling: Network failures, unauthorized access, empty states

**Task 2: Role-Based Task Filtering** (2 days)
- JWT claims extraction: Parse department, roles, groups from token
- Filter construction: Build candidateGroups from user's Keycloak groups
- DOA filtering: Show approval tasks matching user's DOA level
- Department filter: Auto-filter by user's department with option to view all
- Saved filters: Persist user preferences in localStorage

**Task 3: Task Claiming Logic** (1 day)
- Claim eligibility check: Verify user in candidateGroups before claim
- API call: POST /runtime/tasks/:id/claim with assignee
- Optimistic update: Update UI immediately, rollback on failure
- Conflict handling: Task claimed by another user, show error message
- Notification: Toast message on successful claim

**Task 4: Task Card Component** (2 days)
- Visual design: Card layout with priority color coding
- Metadata display: Task name, process name, creation date, due date
- Status badges: Open, Claimed, In Progress, Completed
- Hover actions: Quick claim, quick complete, view details
- Responsive design: Mobile-friendly card stacking

**Task 5: Task Detail View** (3 days)
- Layout: Split view with form on left, context on right
- Form rendering: Dynamic form generation from Flowable form definition
- Process context: Show current step in process diagram with highlight
- Variable display: Read-only table of process variables
- History timeline: Vertical timeline with user avatars, timestamps, comments

**Task 6: Pagination and Sorting** (1 day)
- Server-side pagination: Use Flowable start/size parameters
- Sort options: Creation date, due date, priority, process name
- Sort direction: Ascending/descending toggle
- Page controls: Previous, next, jump to page, page size selector
- State persistence: Remember sort/page across navigation

**Task 7: Search and Filtering UI** (2 days)
- Search bar: Full-text search across task name, process name, description
- Filter panel: Collapsible sidebar with multiple filter categories
- Active filters: Chip display with remove option
- Clear all: Reset filters to default state
- Filter persistence: Save to URL query parameters for shareability

#### 6.1.3 Task List Data Flow

**Data Flow: Task List Load**

1. User navigates to /tasks
   - Action: TaskPortal component mounts
   - Trigger: useEffect with empty dependency array

2. Extract JWT claims
   - Action: Parse Keycloak token from localStorage
   - Extract: userId, department, roles, groups, doaLevel
   - Store: User context in React Context or Zustand store

3. Build task query
   - candidateGroups: Map user's Keycloak groups to Flowable groups
   - processDefinitionKey: Filter by department if restricted
   - sorting: Default to createTime descending
   - pagination: Start at 0, size 20

4. Fetch tasks from Flowable API
   - API call: GET /runtime/tasks with query parameters
   - Loading state: Show skeleton loader
   - Success: Transform and display tasks
   - Error: Show error message with retry button

5. Render task list
   - Component: TaskList receives tasks array
   - Iteration: Map tasks to TaskCard components
   - Empty state: Show "No tasks available" message

**Data Flow: Task Claim**

1. User clicks "Claim" button on task card
   - Validation: Check user in candidateGroups
   - Optimistic update: Mark task as claimed in UI
   - API call: POST /runtime/tasks/:taskId/claim

2. Server processes claim
   - Success: Task assigned to user
   - Failure: Task already claimed or user not authorized

3. Handle response
   - Success: Show toast notification, update task list
   - Failure: Rollback optimistic update, show error message

---

### 6.2 Approval Workflow UI with DOA Logic

#### 6.2.1 Component Architecture

**ApprovalPanel** (Specialized Task Detail)
- Responsibility: Show approval-specific UI with DOA context
- DOA Display: Show user's approval authority, required approval level
- Approval Actions: Approve, reject, escalate to higher DOA
- Escalation Logic: Auto-route to next DOA level if amount exceeds authority

**DOA Indicator** (Visual Component)
- Responsibility: Display DOA level and approval limits
- Visual Design: Progress bar showing user's authority vs request amount
- Color Coding: Green (within authority), yellow (near limit), red (exceeds)
- Escalation Hint: Show next approver if escalation needed

#### 6.2.2 Step-by-Step Approval UI Tasks

**Task 1: DOA-Based Task Visibility** (2 days)
- JWT claim: Extract doaLevel from token
- Task filter: Show approval tasks where required_doa_level less than or equal to user's level
- Amount validation: Parse request amount from process variables
- Visual indicator: Show DOA match in task card
- Edge case: Handle missing DOA information gracefully

**Task 2: Approval Form Component** (3 days)
- Layout: Two-column form with request details and approval section
- Request summary: Amount, requester, department, justification
- DOA context: User's approval limit, request amount comparison
- Approval actions: Radio buttons (Approve, Reject, Escalate)
- Comment field: Required for rejection, optional for approval
- Attachment support: Upload supporting documents for approval decision

**Task 3: Approval Decision Logic** (2 days)
- Approve action: POST /runtime/tasks/:id/complete with approved=true
- Reject action: POST /runtime/tasks/:id/complete with approved=false, comment required
- Escalate action: Create new user task for next DOA level approver
- Process variables: Set approval status, approver ID, approval timestamp
- Audit trail: Record decision in task history

**Task 4: DOA Escalation UI** (2 days)
- Escalation detection: Compare request amount to user's DOA limit
- Automatic routing: Show next approver based on DOA hierarchy
- Manual escalation: Allow user to escalate even if within authority
- Escalation reason: Text field for justification
- Notification: Trigger notification to next approver

**Task 5: Approval History Display** (1 day)
- Timeline component: Vertical timeline with approval chain
- Approver cards: Avatar, name, role, DOA level, decision, timestamp
- Decision details: Show comments, attachments, escalation reason
- Current step: Highlight current approver in chain
- Download option: Export approval history as PDF

**Task 6: Conditional Field Visibility** (1 day)
- Rule engine: Show/hide form fields based on approval type
- DOA fields: Show only for financial approvals
- Department fields: Show only for relevant departments
- Dynamic validation: Change required fields based on approval path

#### 6.2.3 DOA Approval Data Flow

**Data Flow: DOA-Based Task Assignment**

1. User submits request with amount 5000 USD
   - Process variable: requestAmount=5000
   - Process logic: Determine required DOA level

2. Backend task router evaluates DOA
   - Amount 5000 requires Level 2 (1K - 10K range)
   - candidateGroups: doa_approver_level2, doa_approver_level3, doa_approver_level4
   - Task created: Visible to Level 2+ approvers

3. Level 2 approver (Department Head) opens task
   - UI displays: Request amount 5000, User's limit 10K
   - DOA indicator: Green (within authority)
   - Actions enabled: Approve, Reject, Escalate

4. Approver clicks Approve
   - Validation: User's doaLevel >= required level
   - Process variable: approvedBy=userId, approvalLevel=2, approvalTimestamp
   - Task completion: POST /runtime/tasks/:id/complete
   - Process continues: Move to next step (e.g., procurement)

**Data Flow: DOA Escalation**

1. Level 1 approver (Manager, limit 1K) opens task for 5000 USD request
   - DOA indicator: Red (exceeds authority)
   - Actions: Approve button disabled, Escalate button enabled
   - Message: "This request exceeds your approval authority. Please escalate to Department Head."

2. Approver clicks Escalate
   - Modal: Escalation reason textarea
   - API call: POST /runtime/tasks/:id/escalate with reason
   - Backend: Create new task for Level 2 approvers
   - Notification: Email/push to Level 2 approvers
   - Current task: Mark as escalated, assignee unchanged

---

### 6.3 Manager Delegation UI

#### 6.3.1 Component Architecture

**DelegationPanel** (Manager Tool)
- Responsibility: Allow managers to delegate tasks to team members
- Access Control: Show only if user has manager role
- Delegation Types: Specific task, bulk delegation, temporary assignment
- Audit Trail: Track all delegation actions

**DelegateSelector** (User Picker)
- Responsibility: Select team member for delegation
- User filtering: Show only direct reports and team members
- Availability indicator: Show user's current task load
- Search: Autocomplete by name or email

#### 6.3.2 Step-by-Step Delegation Tasks

**Task 1: Delegation Authorization** (1 day)
- JWT claims: Check for manager role or department_manager role
- Team member fetch: GET /api/users?managerId=currentUserId
- Permission validation: Can only delegate to team members or same department
- UI visibility: Show delegation button only if authorized

**Task 2: Delegation Modal Component** (2 days)
- Modal layout: Overlay with delegate selector and reason field
- User search: Autocomplete input with avatar and name
- Task load indicator: Show delegatee's current task count
- Delegation reason: Text field for tracking
- Notification option: Checkbox to notify delegatee

**Task 3: Delegation API Integration** (1 day)
- Flowable API: POST /runtime/tasks/:id/delegate
- Payload: assignee (delegatee userId), owner (current user)
- Process variables: Set delegatedBy, delegationReason, delegationTimestamp
- Optimistic update: Update UI immediately
- Error handling: Handle already-delegated tasks

**Task 4: Bulk Delegation UI** (2 days)
- Multi-select: Checkbox selection for multiple tasks
- Bulk action: Delegate all selected tasks to one user
- Confirmation dialog: Show list of tasks to delegate
- Progress indicator: Show delegation progress for each task
- Partial success: Handle cases where some delegations fail

**Task 5: Delegation History View** (1 day)
- New route: /tasks/delegated
- Table view: Show all delegated tasks with delegatee, date, reason
- Filter: By delegatee, date range, task type
- Recall option: Un-delegate task back to manager
- Export: Download delegation report

**Task 6: Temporary Assignment** (1 day)
- Duration picker: Date range for temporary delegation
- Auto-recall: Backend job returns task to manager after duration
- Reminder: Notification to manager before task returns
- Extension option: Extend temporary delegation period

#### 6.3.3 Delegation Data Flow

**Data Flow: Task Delegation**

1. Manager opens task detail view
   - UI shows: Delegate button next to Complete button
   - Authorization: Verify user has manager role

2. Manager clicks Delegate
   - Modal opens: Delegate selector component
   - API call: GET /api/users?department=Finance&role=employee
   - Display: List of eligible team members

3. Manager selects team member and provides reason
   - Validation: Ensure team member selected and reason provided
   - API call: POST /runtime/tasks/:taskId/delegate
   - Payload: assignee=teamMemberId, owner=managerId, reason="text"

4. Backend processes delegation
   - Flowable: Update task assignee, set owner field
   - Process variable: delegatedBy, delegationTimestamp
   - Notification: Trigger notification to delegatee

5. UI updates
   - Task list: Move task from manager's list to delegatee's list
   - Toast: "Task delegated to John Doe successfully"
   - History: Record delegation in audit trail

---

### 6.4 Touchpoint Notification Display Strategy

#### 6.4.1 Touchpoint Concept

**Definition**: A touchpoint is any interaction point where the system requires user attention or provides status updates.

**Touchpoint Types**:
1. Action Required: User must claim/complete task
2. Informational: Status update, no action needed
3. Approval Pending: Waiting for another user
4. Delegation: Task assigned by manager
5. Escalation: Task escalated due to DOA
6. Completion: Process completed successfully

#### 6.4.2 Notification Architecture

**NotificationCenter** (Component)
- Responsibility: Display all user notifications in one place
- Layout: Dropdown panel from header bell icon
- Grouping: By process, by type, by date
- Actions: Mark as read, delete, view related task

**NotificationCard** (Item Component)
- Responsibility: Display single notification with context
- Visual design: Icon based on type, priority color coding
- Content: Title, description, timestamp, related task link
- Actions: Quick action buttons based on notification type

**NotificationBadge** (Indicator)
- Responsibility: Show unread count in header
- Visual design: Red badge with number
- Update: Real-time update via WebSocket or polling
- Animation: Pulse animation on new notification

#### 6.4.3 Step-by-Step Notification Tasks

**Task 1: Notification Data Model** (1 day)
- Define TypeScript interfaces: Notification, NotificationType, NotificationPriority
- Schema design: id, userId, type, title, message, taskId, processId, isRead, timestamp
- Backend API: GET /api/notifications?userId=&unreadOnly=true
- Pagination: Support infinite scroll for notification list

**Task 2: Notification Center Component** (2 days)
- Dropdown panel: Overlay positioned below bell icon
- Header: "Notifications" title with "Mark all as read" button
- List: Scrollable list of notification cards
- Empty state: "No new notifications" message
- Footer: "View all notifications" link to dedicated page

**Task 3: Real-Time Notification Delivery** (2 days)
- WebSocket connection: Connect on user login
- Message format: JSON with notification object
- Toast notification: Show brief toast for high-priority notifications
- Badge update: Increment unread count
- Sound/vibration: Optional based on user preferences

**Task 4: Notification Actions** (2 days)
- Mark as read: PUT /api/notifications/:id/read
- Delete: DELETE /api/notifications/:id
- Quick action: Navigate directly to related task
- Bulk actions: Mark all as read, delete all read
- Settings: User preference for notification types to receive

**Task 5: Touchpoint Mapping to Notifications** (2 days)
- Request submission: "Your asset request has been submitted. Tracking ID: AR-12345"
- Manager approval: "New asset request requires your approval. Requester: John Doe"
- Finance approval: "Asset request AR-12345 requires financial approval. Amount: 5000 USD"
- Status update: "Your asset request AR-12345 has been approved by Jane Smith"
- Delegation: "Task 'Review Purchase Request' has been delegated to you by Manager"
- Escalation: "Request AR-12345 has been escalated to Department Head due to amount"
- Completion: "Asset request AR-12345 completed. Asset ID: A-98765 assigned to you"

**Task 6: Notification Preferences** (1 day)
- Settings page: /settings/notifications
- Channel selection: Email, push, in-app for each notification type
- Frequency: Immediate, hourly digest, daily digest
- Quiet hours: No notifications during specified time
- Department filter: Only notifications for specific departments

#### 6.4.4 Touchpoint Notification Data Flow

**Data Flow: Request Submission Touchpoint**

1. User submits asset request form
   - API call: POST /api/process-instances/start
   - Payload: processDefinitionKey=asset-request, variables={...}

2. Backend starts process instance
   - Flowable: Create process instance, create first user task
   - Task router: Assign to requester's manager based on JWT managerId claim
   - Notification: Trigger NotificationDelegate service task

3. NotificationDelegate executes
   - Template: PR_SUBMITTED
   - Recipients: Requester (confirmation), Manager (action required)
   - Channel: In-app notification, email
   - Content: "Your asset request AR-12345 has been submitted and assigned to Manager Jane Smith"

4. Frontend receives notification
   - WebSocket message: New notification event
   - NotificationCenter: Add notification to list
   - Badge: Increment unread count
   - Toast: Show brief toast message

5. User views notification
   - Click: Mark as read, navigate to task detail
   - Task detail: Show form and process context

---

### 6.5 Backend Integration Points

#### 6.5.1 Flowable Task API

**Task List**:
- Endpoint: GET /runtime/tasks
- Query Parameters: assignee, candidateUser, candidateGroups, processDefinitionKey, sorting, start, size
- Response: List of Task DTOs with id, name, assignee, createTime, dueDate, processInstanceId
- Filtering: Support complex AND/OR filtering with JSON payload

**Task Claim**:
- Endpoint: POST /runtime/tasks/:taskId/claim
- Payload: assignee (userId)
- Response: Updated task object or error
- Conflict: HTTP 409 if already claimed

**Task Complete**:
- Endpoint: POST /runtime/tasks/:taskId/complete
- Payload: variables (process variables to set)
- Response: 204 No Content on success
- Validation: Backend validates required variables

**Task Delegation**:
- Endpoint: POST /runtime/tasks/:taskId/delegate
- Payload: assignee (delegatee userId)
- Response: Updated task with owner field
- Process Variables: Set delegatedBy, delegationTimestamp

#### 6.5.2 Task Router Integration

**Keycloak JWT Claims**:
- Claims: sub (userId), department, roles, groups, doaLevel, managerId
- Extraction: Spring Security JwtAuthenticationConverter
- Validation: Verify token signature, expiration, issuer

**Task Assignment Logic**:
- candidateGroups: Map from user's Keycloak groups
- Example: User in /HR Department/Managers group → candidateGroups=hr_manager, manager, doa_approver_level1
- DOA filtering: Filter tasks by doa_level variable vs user's doaLevel claim

**Manager Lookup**:
- JWT claim: managerId points to manager's Keycloak user ID
- Task assignment: Set candidateUsers=[managerId] for approval tasks
- Fallback: If managerId not set, assign to department POC

#### 6.5.3 Notification Service Integration

**Notification API**:
- Endpoint: GET /api/notifications
- Query Parameters: userId, unreadOnly, type, startDate, endDate, limit
- Response: List of Notification objects
- Real-time: WebSocket subscription at /ws/notifications

**Notification Creation**:
- Trigger: NotificationDelegate in BPMN process
- Payload: notificationType, recipients (user IDs or roles), taskId, processInstanceId
- Backend: Create notification records, push to WebSocket subscribers

**Notification Templates**:
- Storage: Database table notification_templates
- Variables: Inject process variables into templates using expression syntax
- Localization: Support multiple languages based on user preference

---

### 6.6 Testing Strategy Summary

#### 6.6.1 Unit Testing

**Task List Component**:
- Test data transformation from Flowable DTO to UI model
- Test filtering logic: by status, department, assignee
- Test pagination calculations
- Mock Flowable API responses

**Approval Panel Component**:
- Test DOA level comparison logic
- Test approve/reject/escalate button states
- Test comment validation
- Mock task data with various DOA levels

**Delegation Component**:
- Test team member filtering
- Test delegation payload construction
- Test error handling for failed delegation
- Mock user list API

#### 6.6.2 Integration Testing

**Task Claiming Flow**:
- Create test task with candidateGroups
- Login as user in candidate group
- Claim task, verify assignment
- Attempt claim by unauthorized user, verify rejection

**Approval Workflow**:
- Create test process with approval tasks
- Login as approver with specific DOA level
- Approve request within authority
- Attempt approval beyond authority, verify escalation

**Delegation Flow**:
- Login as manager
- Delegate task to team member
- Login as team member, verify task appears
- Complete task, verify manager notified

#### 6.6.3 End-to-End Testing

**Complete Approval Journey**:
1. User submits asset request
2. Manager receives notification
3. Manager claims task
4. Manager reviews and approves
5. Finance receives DOA approval task
6. Finance approves
7. User receives completion notification
8. Verify audit trail complete

**Delegation Journey**:
1. Manager receives high-priority task
2. Manager delegates to team member with reason
3. Team member receives notification
4. Team member claims and completes
5. Manager receives completion notification
6. Verify delegation history recorded

---

### 6.7 User Experience Considerations

#### 6.7.1 Progressive Disclosure

**Task List**:
- Show essential columns first: Task name, process, assignee, due date
- Collapsible details: Expand card to show full description, variables
- Advanced filters: Hide in collapsible sidebar, show simple filters first

**Task Detail**:
- Tabs: Task info, form, process diagram, history
- Lazy load: Process diagram loaded only when tab opened
- Accordion: Collapse process variables section by default

#### 6.7.2 Loading States and Skeletons

**Task List Loading**:
- Skeleton cards: Show card outline with animated gradient
- Progressive rendering: Render tasks as they load
- Optimistic updates: Show claimed task immediately, rollback on error

**Form Loading**:
- Form skeleton: Show input field placeholders
- Field-level loading: Load dynamic fields progressively
- Save indicators: Show spinner on save button during submission

#### 6.7.3 Error Handling and Recovery

**Network Errors**:
- Retry button: Allow user to retry failed operations
- Offline indicator: Show banner when network unavailable
- Queue actions: Queue task claims/completions for retry when online

**Validation Errors**:
- Inline errors: Show field-level errors next to inputs
- Error summary: List all errors at top of form
- Focus management: Auto-focus first error field

**Authorization Errors**:
- Clear messages: "You do not have permission to claim this task"
- Suggested action: "Please contact your manager for access"
- Redirect: Auto-redirect to allowed task list

#### 6.7.4 Mobile Responsiveness

**Task List Mobile**:
- Card stacking: One card per row on mobile
- Swipe actions: Swipe card to reveal quick actions
- Bottom sheet: Actions appear in bottom sheet modal

**Task Detail Mobile**:
- Vertical layout: Stack form fields vertically
- Fixed action bar: Keep Complete/Reject buttons fixed at bottom
- Collapsible sections: Minimize space usage

#### 6.7.5 Accessibility

**Keyboard Navigation**:
- Tab order: Logical tab order through task list and forms
- Arrow keys: Navigate between task cards with up/down arrows
- Escape: Close modals and dropdowns with escape key

**Screen Reader Support**:
- ARIA labels: All interactive elements labeled
- Live regions: Announce task list updates
- Status messages: Announce success/error messages

**Visual Design**:
- Color contrast: WCAG AA compliance for all text
- Focus indicators: Visible focus outline on all interactive elements
- Icon labels: Text labels for all icon-only buttons

---

### 6.8 Implementation Checklist (3-Week Timeline)

#### Week 1: Task Management Foundation
- [ ] Create TaskPortal container component
- [ ] Implement TaskList grid component
- [ ] Integrate Flowable task list API
- [ ] Build JWT claims extraction utility
- [ ] Implement role-based task filtering
- [ ] Create TaskCard component with metadata
- [ ] Add task claiming functionality
- [ ] Implement pagination and sorting
- [ ] Build search and filter UI
- [ ] Unit tests for task list components

#### Week 2: Approval Workflow and DOA
- [ ] Create ApprovalPanel component
- [ ] Implement DOA indicator component
- [ ] Build approval decision form
- [ ] Add DOA escalation logic
- [ ] Create approval history timeline
- [ ] Implement conditional field visibility
- [ ] Add attachment upload for approval documents
- [ ] Integration tests for approval workflow
- [ ] Test DOA level validation
- [ ] Edge case testing for escalation scenarios

#### Week 3: Delegation and Notifications
- [ ] Create DelegationPanel component
- [ ] Implement DelegateSelector user picker
- [ ] Build delegation authorization logic
- [ ] Add bulk delegation functionality
- [ ] Create delegation history view
- [ ] Implement temporary assignment
- [ ] Build NotificationCenter component
- [ ] Add real-time notification delivery
- [ ] Create notification preferences page
- [ ] Map touchpoints to notification templates
- [ ] E2E testing for complete user journeys
- [ ] Accessibility audit and fixes
- [ ] Performance optimization
- [ ] Documentation and user guide

---

## Cross-Phase Integration Considerations

### Integration Points Between Phase 4 and Phase 6

**Process Definition and Task Forms**:
- Phase 4: BPMN editor defines user tasks with form keys
- Phase 6: Task portal renders forms based on form keys
- Integration: Ensure form keys in BPMN match available form templates

**Service Delegate Configuration**:
- Phase 4: Visual REST delegate configuration in BPMN
- Phase 6: Task completion may trigger REST service delegates
- Integration: Test that configured REST calls execute correctly during task completion

**Notification Delegate Configuration**:
- Phase 4: Visual notification delegate configuration
- Phase 6: Notifications appear in NotificationCenter
- Integration: Map notification types configured in BPMN to UI notification display

**DOA Configuration**:
- Phase 4: Set doa_level process variables in BPMN
- Phase 6: DOA approval UI reads doa_level to determine visibility
- Integration: Ensure consistent DOA level numbering between BPMN and UI

### Shared Data Models

**Task Model**:
```typescript
interface Task {
  id: string
  name: string
  processInstanceId: string
  processDefinitionKey: string
  assignee: string | null
  candidateGroups: string[]
  createTime: Date
  dueDate: Date | null
  priority: number
  formKey: string | null
  variables: Record<string, any>
}
```

**Notification Model**:
```typescript
interface Notification {
  id: string
  userId: string
  type: NotificationType
  priority: NotificationPriority
  title: string
  message: string
  taskId: string | null
  processInstanceId: string | null
  isRead: boolean
  createdAt: Date
}
```

**Approval Context Model**:
```typescript
interface ApprovalContext {
  taskId: string
  requestAmount: number
  requesterUserId: string
  requesterDepartment: string
  currentApproverDoaLevel: number
  requiredDoaLevel: number
  approvalHistory: ApprovalDecision[]
  canApprove: boolean
  canEscalate: boolean
}
```

---

## Conclusion

This implementation plan provides a comprehensive architectural roadmap for Phase 4 and Phase 6 frontend development. Key success factors:

1. **Incremental Delivery**: MVP approach for Phase 4 allows early feedback
2. **Backend Independence**: Mock data fallbacks enable parallel development
3. **User-Centric Design**: Focus on progressive disclosure and accessibility
4. **Testing Rigor**: Comprehensive testing strategy ensures quality
5. **Integration Clarity**: Clear integration points between phases

Estimated total effort: 7 weeks (4 weeks Phase 4 + 3 weeks Phase 6) with one frontend developer.

Success metrics:
- 90%+ no-code compliance for delegate configuration (Phase 4)
- Zero manual XML editing required for common patterns (Phase 4)
- Sub-3s task list load time (Phase 6)
- 100% role-based task visibility accuracy (Phase 6)
- Full WCAG 2.1 AA accessibility compliance (Both phases)

---

**Document Status**: Final
**Next Steps**: Review with frontend team, allocate resources, begin Phase 4 Week 1 tasks
**Related Tickets**: To be created in project management system
