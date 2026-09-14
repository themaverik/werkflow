# Werkflow Frontend

Next.js 14 React application with visual BPMN workflow designer and dynamic form builder.

## 🚀 Technology Stack

- **Next.js 14** - React framework with App Router
- **React 18** - UI library
- **TypeScript 5** - Type safety
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - Re-usable components built with Radix UI
- **bpmn-js** - BPMN 2.0 rendering and editing
- **Form.io** - Dynamic form builder
- **React Query** - Server state management
- **Axios** - HTTP client
- **NextAuth v5** - Authentication (Keycloak integration)

## 📋 Prerequisites

- **Node.js 20+**
- **npm or pnpm** or yarn
- **Backend API** running on http://localhost:8080/api
- **Keycloak** running on http://localhost:8090

## 🏃 Getting Started

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Configure Environment Variables

Copy the example env file:

```bash
cp .env.local.example .env.local
```

Edit `.env.local` with your configuration:

```env
# Backend API
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# NextAuth
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-here

# Keycloak
KEYCLOAK_CLIENT_ID=werkflow-frontend
KEYCLOAK_CLIENT_SECRET=your-client-secret
KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
```

### 3. Run Development Server

```bash
npm run dev
```

Open http://localhost:3000 in your browser.

### 4. Build for Production

```bash
npm run build
npm start
```

## 📁 Project Structure

```
frontend/
├── app/                        # Next.js App Router
│   ├── (auth)/                 # Authentication pages (future)
│   ├── (studio)/               # Process & Form Designer (future)
│   ├── (portal)/               # Task Management (future)
│   ├── layout.tsx              # Root layout
│   ├── page.tsx                # Home page
│   ├── providers.tsx           # React Query provider
│   └── globals.css             # Global styles
├── components/
│   ├── ui/                     # shadcn/ui components
│   │   ├── button.tsx
│   │   └── card.tsx
│   ├── bpmn/                   # BPMN designer (future)
│   ├── forms/                  # Form builder (future)
│   └── tasks/                  # Task management (future)
├── lib/
│   ├── api/                    # API client functions
│   │   ├── client.ts           # Axios instance
│   │   ├── workflows.ts        # Workflow APIs
│   │   └── flowable.ts         # Flowable deployment APIs
│   ├── hooks/                  # Custom React hooks (future)
│   └── utils.ts                # Utilities
├── public/                     # Static assets
├── .env.local.example          # Environment variables example
├── next.config.mjs             # Next.js configuration
├── tailwind.config.ts          # Tailwind CSS configuration
├── tsconfig.json               # TypeScript configuration
└── package.json                # Dependencies
```

## 🎨 Available Pages

### Current (Phase 1)
- **/** - Landing page with navigation to main sections

### Coming Soon (Phase 2-4)
- **/studio/processes** - BPMN process designer
- **/studio/processes/new** - Create new BPMN process
- **/studio/processes/[id]** - Edit existing process
- **/studio/forms** - Form builder
- **/studio/forms/new** - Create new form
- **/studio/forms/[id]** - Edit existing form
- **/portal/tasks** - My tasks list
- **/portal/tasks/[id]** - Task detail and completion
- **/portal/processes** - Process instances

## 🔌 API Integration

The frontend integrates with the Spring Boot backend via REST APIs:

### Workflow APIs (`lib/api/workflows.ts`)
- `startProcess()` - Start a new workflow process
- `getProcessInstance()` - Get process details
- `getTasksByAssignee()` - Get user's tasks
- `completeTask()` - Complete a task
- And more...

### Flowable Deployment APIs (`lib/api/flowable.ts`)
- `deployBpmn()` - Deploy BPMN process definition
- `getProcessDefinitions()` - List all process definitions
- `deployForm()` - Deploy form definition
- `getFormDefinition()` - Get form by key

All API calls use React Query for caching and state management.

## 🧪 Testing

```bash
# Type check
npm run type-check

# Lint
npm run lint

# Unit tests (coming soon)
npm test

# E2E tests (coming soon)
npm run test:e2e
```

## 🚀 Deployment

### Vercel (Recommended)

```bash
npm install -g vercel
vercel deploy --prod
```

### Docker

```dockerfile
# Build
docker build -t werkflow-frontend .

# Run
docker run -p 3000:3000 werkflow-frontend
```

### Environment Variables for Production

Set these in your deployment platform:

- `NEXT_PUBLIC_API_URL` - Backend API URL
- `NEXTAUTH_URL` - Frontend URL
- `NEXTAUTH_SECRET` - Secret for NextAuth
- `KEYCLOAK_CLIENT_ID` - Keycloak client ID
- `KEYCLOAK_CLIENT_SECRET` - Keycloak client secret
- `KEYCLOAK_ISSUER` - Keycloak issuer URL

## 📚 Development Guidelines

### Component Structure
- Use **Server Components** by default
- Use **Client Components** only when needed (add `'use client'`)
- Keep components small and focused

### API Calls
Always use React Query:

```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['resource', id],
  queryFn: () => fetchResource(id)
})
```

### Styling
- Use Tailwind CSS utility classes
- Use shadcn/ui components when possible
- Keep custom CSS minimal

### Type Safety
- Use TypeScript for all files
- Define interfaces for API responses
- Use type inference where possible

## 🛣️ Roadmap

See [ROADMAP.md](../../ROADMAP.md) for detailed implementation plan.

### Phase 1: Foundation ✅ (Current)
- [x] Next.js 14 setup with TypeScript
- [x] Tailwind CSS + shadcn/ui
- [x] React Query configuration
- [x] API client with Axios
- [x] Basic layout and routing

### Phase 2: BPMN Designer (Weeks 3-4)
- [ ] Integrate bpmn-js
- [ ] Process designer UI
- [ ] Process deployment
- [ ] Properties panel

### Phase 3: Form Builder (Weeks 5-6)
- [ ] Integrate Form.io
- [ ] Form builder UI
- [ ] Form renderer
- [ ] Form deployment

### Phase 4: Runtime Portal (Weeks 7-8)
- [ ] Task list page
- [ ] Task completion
- [ ] Process timeline
- [ ] Search and filters

## 🔧 Troubleshooting

**Port 3000 already in use:**
```bash
# Kill process on port 3000
lsof -ti:3000 | xargs kill -9

# Or run on different port
PORT=3001 npm run dev
```

**API connection errors:**
- Verify backend is running on http://localhost:8080
- Check CORS settings in backend
- Verify `NEXT_PUBLIC_API_URL` in `.env.local`

**Build errors:**
```bash
# Clear Next.js cache
rm -rf .next

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install
```

## 📖 Learn More

- [Next.js Documentation](https://nextjs.org/docs)
- [React Query Documentation](https://tanstack.com/query/latest)
- [bpmn-js Documentation](https://bpmn.io/toolkit/bpmn-js/)
- [Form.io Documentation](https://help.form.io/)
- [shadcn/ui](https://ui.shadcn.com/)
- [Tailwind CSS](https://tailwindcss.com/docs)

## 📄 License

Proprietary - All rights reserved
