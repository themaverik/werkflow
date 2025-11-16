# HR Portal

Employee-facing portal for HR operations in the werkflow enterprise platform.

## Overview

The HR Portal provides employees and HR managers with access to HR-related workflows and operations.

## Features (Planned - Phase 2, Week 9-12)

### For Employees
- Personal dashboard
- Leave request submission
- Leave balance tracking
- Performance review participation
- Onboarding workflows
- Document uploads
- Profile management

### For HR Managers
- Employee management
- Leave approval workflows
- Performance review management
- Onboarding tracking
- HR analytics dashboard
- Department management

### For Department Managers
- Team member overview
- Leave approval
- Performance review initiation
- Team onboarding

## Technology Stack

- Next.js 14
- React 18
- TypeScript
- Tailwind CSS
- shadcn/ui components
- NextAuth v5
- React Hook Form
- Zod validation

## Port

- **4001** - HTTP server

## Status

**TODO**: To be implemented in Phase 2, Week 9-12

## Development

```bash
# Install dependencies
npm install

# Copy environment file
cp .env.local.example .env.local

# Update environment variables in .env.local

# Run development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

## Environment Variables

See `.env.local.example` for required configuration.
