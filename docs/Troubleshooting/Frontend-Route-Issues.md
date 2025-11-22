# Frontend Route Issues

Troubleshooting guide for Next.js routing problems in Werkflow frontends.

## Table of Contents

1. [Critical Issue: /studio/ Routes Return 404](#critical-issue-studio-routes-return-404)
2. [General 404 Errors](#general-404-errors)
3. [Redirect Loop Issues](#redirect-loop-issues)
4. [Middleware Configuration](#middleware-configuration)
5. [App Router vs Pages Router](#app-router-vs-pages-router)
6. [Dynamic Routes](#dynamic-routes)

## Critical Issue: /studio/ Routes Return 404

### NEW ISSUE: Admin Portal /studio/ Prefix Routing

**Status**: Known Issue
**Severity**: High
**Impact**: Routes with /studio/ prefix inaccessible

**Affected Routes**:
- `http://localhost:4000/studio/processes` → 404
- `http://localhost:4000/studio/forms` → 404
- `http://localhost:4000/studio/workflows` → 404
- `http://localhost:4000/studio/services` → 404

**Working Routes** (without /studio/ prefix):
- `http://localhost:4000/processes` → Works
- `http://localhost:4000/forms` → Works
- `http://localhost:4000/workflows` → Works

### Root Cause Analysis

**Issue**: Next.js App Router route group design - NOT an authentication issue.

**Root Cause**: Routes are organized in a `(studio)` route group directory. In Next.js App Router, parentheses denote route groups which are for code organization only and do NOT create URL path segments.

**Actual Situation**:
- Routes EXIST at: `app/(studio)/processes/page.tsx`, `app/(studio)/forms/page.tsx`
- URLs RESOLVE to: `/processes`, `/forms`, `/workflows`, `/services` (without /studio/ prefix)
- Requests to `/studio/processes` fail with 404 because that path doesn't exist
- The middleware at `middleware.ts` line 9 checks for paths starting with `/studio`, but these paths don't actually exist

**Why This Happens**:
- Next.js route groups `(parentheses)` are for organizing code, not creating URL segments
- They don't affect routing or URL structure
- `(studio)` is purely organizational and provides no `/studio/` URL prefix
- The middleware was written to expect `/studio/` paths, but they don't exist

**Design Mismatch**:
- The middleware expects routes at `/studio/*` URLs
- The actual routes render at `/*` URLs
- This is a design decision conflict: either make studio an actual directory or update middleware

### Diagnostic Steps

**Step 1: Check File Structure**

```bash
# Check current structure (routes are in route group, not actual directory)
find frontends/admin-portal/app -name "page.tsx" -type f | sort

# Check for (studio) route group
ls -la frontends/admin-portal/app/ | grep studio
# Output: drwxr-xr-x (studio)  <-- This is a route group, not a path segment
```

**Current Structure (in Admin Portal)**:
```
app/
├── (studio)/                        # Route group - doesn't create /studio/ path
│   ├── processes/
│   │   ├── page.tsx                # Maps to /processes (NOT /studio/processes)
│   │   ├── edit/[id]/page.tsx       # Maps to /processes/edit/[id]
│   │   └── new/page.tsx             # Maps to /processes/new
│   ├── forms/
│   │   ├── page.tsx                # Maps to /forms
│   │   ├── edit/[key]/page.tsx      # Maps to /forms/edit/[key]
│   │   ├── new/page.tsx             # Maps to /forms/new
│   │   └── preview/[key]/page.tsx   # Maps to /forms/preview/[key]
│   ├── workflows/
│   │   └── page.tsx                # Maps to /workflows
│   └── services/
│       └── page.tsx                # Maps to /services
├── (portal)/                        # Another route group
│   ├── tasks/page.tsx              # Maps to /tasks
│   ├── monitoring/page.tsx         # Maps to /monitoring
│   └── analytics/page.tsx          # Maps to /analytics
├── page.tsx                         # Maps to / (home)
└── login/page.tsx                  # Maps to /login
```

**Note**: The `(studio)` and `(portal)` parts in parentheses are Route Groups used for code organization only. They do NOT affect the URL structure.

**Step 2: Check Middleware Configuration**

```typescript
// Check frontends/admin-portal/middleware.ts

// Look for rewrites that might add /studio/ prefix
export function middleware(request: NextRequest) {
  // Check for rewrite rules
  // Verify paths are correct
}
```

**Step 3: Check Next.js Config**

```javascript
// Check frontends/admin-portal/next.config.js

// Look for:
// - rewrites() function
// - redirects() function
// - basePath configuration
```

**Step 4: Test Different Paths**

```bash
# Test with prefix
curl -I http://localhost:4000/studio/processes
# Returns: 404

# Test without prefix
curl -I http://localhost:4000/processes
# Returns: 302 (redirect to login) or 200 (if authenticated)
```

### Resolution Options

Choose based on your desired URL structure:

**Option 1: Rename (studio) Route Group to Actual studio Directory** (Recommended for /studio/ URLs)

This changes the route group into a real directory, making routes accessible at `/studio/processes`, `/studio/forms`, etc.

```bash
# From project root
cd frontends/admin-portal

# 1. Rename route group to actual directory
mv app/\(studio\) app/studio

# 2. Update auth.config.ts and middleware.ts to remove /studio check if not needed
# (since all main routes will now be under /studio/)

# 3. Rebuild and restart
docker-compose up -d --build admin-portal
```

**Option 2: Keep Route Groups, Remove /studio/ Prefix from Everywhere** (Current State)

Accept that routes are at `/processes`, `/forms`, etc. (without /studio/) and update references:

```typescript
// Update navigation links throughout the application
// Change all: href="/studio/processes"
// To: href="/processes"

// Update auth.config.ts redirection if needed
// Change: redirect("/studio/processes")
// To: redirect("/processes")

// Update links in components
// Example in navigation:
{
  label: 'Processes',
  href: '/processes',  // Not /studio/processes
}
```

**Option 3: Add Middleware Rewrites** (If you want both /studio/ and / to work)

Make both `/studio/processes` and `/processes` work by rewriting requests:

```typescript
// frontends/admin-portal/middleware.ts
import { auth } from "@/auth"
import { NextResponse } from 'next/server'

export default auth((req) => {
  const { pathname } = req.nextUrl
  const isLoggedIn = !!req.auth

  // Rewrite /studio/* to /* (if /studio/ is used externally)
  if (pathname.startsWith('/studio/')) {
    const rewritePath = pathname.replace(/^\/studio/, '') || '/'
    const rewriteUrl = new URL(rewritePath, req.url)

    // Update protection check for rewritten path
    const isProtectedRoute = rewritePath.startsWith('/processes') ||
                            rewritePath.startsWith('/forms') ||
                            rewritePath.startsWith('/workflows') ||
                            rewritePath.startsWith('/services')

    if (isProtectedRoute && !isLoggedIn) {
      const loginUrl = new URL('/login', req.url)
      loginUrl.searchParams.set('callbackUrl', pathname)
      return NextResponse.redirect(loginUrl)
    }

    return NextResponse.rewrite(rewriteUrl)
  }

  // Existing protection for direct /processes, /forms, etc.
  const isProtectedRoute = pathname.startsWith('/processes') ||
                          pathname.startsWith('/forms') ||
                          pathname.startsWith('/workflows') ||
                          pathname.startsWith('/services')

  if (isProtectedRoute && !isLoggedIn) {
    const loginUrl = new URL('/login', req.url)
    loginUrl.searchParams.set('callbackUrl', pathname)
    return NextResponse.redirect(loginUrl)
  }

  return undefined
})

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)',
}
```

**Option 4: Use Next.js Rewrites in next.config.mjs** (If keeping route group structure)

```javascript
// frontends/admin-portal/next.config.mjs
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/studio/processes',
        destination: '/processes',
      },
      {
        source: '/studio/forms',
        destination: '/forms',
      },
      {
        source: '/studio/workflows',
        destination: '/workflows',
      },
      {
        source: '/studio/services',
        destination: '/services',
      },
    ]
  },
  // ... rest of config
}

export default nextConfig
```

### Testing the Fix

After implementing solution:

```bash
# 1. Rebuild container
docker-compose up -d --build admin-portal

# 2. Test routes
curl -I http://localhost:4000/studio/processes
# Should return 302 or 200 (not 404)

# 3. Test in browser
# Visit http://localhost:4000/studio/processes
# Should load (not 404)

# 4. Check navigation
# Click around studio UI
# Verify all links work
```

### Prevention

To prevent similar routing issues:

1. **Consistent Structure**: Decide on URL structure early (with or without prefix)
2. **Test Routes**: Add route tests to catch 404s in development
3. **Documentation**: Document intended URL structure
4. **Middleware Review**: Review middleware rewrites carefully

## General 404 Errors

### Issue: Route Returns 404 But File Exists

**Symptoms**: Page exists in file system but returns 404

**Diagnostics**:

```bash
# 1. Verify file structure
ls -la frontends/admin-portal/app/your-route/page.tsx

# 2. Check file exports default component
grep "export default" frontends/admin-portal/app/your-route/page.tsx

# 3. Restart Next.js dev server
docker-compose restart admin-portal
```

**Common Causes**:

1. **Missing page.tsx**: Directory exists but no page.tsx file
2. **No Default Export**: Component not exported as default
3. **Build Cache**: Old build cache causing issues
4. **TypeScript Errors**: Compilation errors preventing route from loading

**Solutions**:

```typescript
// Ensure page.tsx has proper structure
export default function YourPage() {
  return <div>Your content</div>
}

// Or for async server components
export default async function YourPage() {
  const data = await fetchData()
  return <div>{data}</div>
}
```

### Issue: Dynamic Route 404

**Symptoms**: Dynamic routes like `/processes/[id]` return 404

**Diagnostics**:

```bash
# Check directory structure
ls -la frontends/admin-portal/app/processes/[id]/

# Should have page.tsx
```

**Solution**:

```
Correct structure:
app/
└── processes/
    └── [id]/
        └── page.tsx

Incorrect (will 404):
app/
└── processes/
    └── id/           # Missing brackets
        └── page.tsx
```

## Redirect Loop Issues

### Issue: Infinite Redirect Loop

**Symptoms**:
- Browser shows "Too many redirects"
- Page keeps redirecting to itself
- Network tab shows multiple 302 redirects

**Common Causes**:

1. **Middleware Loop**: Middleware redirects to same path
2. **Authentication Loop**: Auth middleware misconfigured
3. **NextAuth Callback**: Callback URL points to protected route

**Diagnostic**:

```typescript
// Check middleware.ts
export function middleware(request: NextRequest) {
  // Look for redirects that might loop
  // Ensure proper exclusions
}

// Check auth callbacks
// Ensure callback URLs don't require auth
```

**Solution**:

```typescript
// Fix middleware to exclude certain paths
export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - api/auth/* (auth endpoints)
     * - _next/static (static files)
     * - _next/image (image optimization)
     * - favicon.ico (favicon file)
     */
    '/((?!api/auth|_next/static|_next/image|favicon.ico).*)',
  ],
}
```

## Middleware Configuration

### Checking Middleware for Route Issues

**Location**: `frontends/admin-portal/middleware.ts`

**Common Issues**:

1. **Over-broad Matchers**: Middleware matching too many routes
2. **Incorrect Rewrites**: Rewriting to wrong destinations
3. **Missing Exclusions**: Not excluding static files or API routes

**Diagnostic**:

```typescript
// Add logging to middleware
export function middleware(request: NextRequest) {
  console.log('Middleware processing:', request.nextUrl.pathname)

  // Your middleware logic

  return NextResponse.next()
}
```

**Best Practices**:

```typescript
export const config = {
  matcher: [
    // Include paths that need processing
    '/studio/:path*',
    '/processes/:path*',

    // Exclude paths that don't
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
}
```

## App Router vs Pages Router

### Identifying Router Type

Werkflow uses **App Router** (Next.js 13+).

**App Router Structure**:
```
app/
├── layout.tsx
├── page.tsx
└── studio/
    └── page.tsx
```

**Pages Router Structure** (Not used in Werkflow):
```
pages/
├── _app.tsx
├── index.tsx
└── studio.tsx
```

### App Router Route Conventions

**Routes**: Defined by folders
```
app/studio/processes/page.tsx → /studio/processes
```

**Layouts**: Shared UI for route segments
```
app/studio/layout.tsx → Wraps all /studio/* routes
```

**Loading**: Loading UI
```
app/studio/loading.tsx → Shows while loading
```

**Error**: Error handling
```
app/studio/error.tsx → Error boundary
```

## Dynamic Routes

### Dynamic Segment Syntax

**Single Segment**:
```
app/processes/[id]/page.tsx → /processes/123
```

**Catch-All**:
```
app/docs/[...slug]/page.tsx → /docs/a/b/c
```

**Optional Catch-All**:
```
app/docs/[[...slug]]/page.tsx → /docs or /docs/a/b/c
```

### Accessing Dynamic Params

```typescript
// In page.tsx
export default function Page({ params }: { params: { id: string } }) {
  return <div>Process ID: {params.id}</div>
}

// For catch-all routes
export default function Page({ params }: { params: { slug: string[] } }) {
  return <div>Path: {params.slug.join('/')}</div>
}
```

## See Also

- [Authentication Issues](./Authentication_Issues.md) - Auth-related routing problems
- [OAuth2 Troubleshooting](../OAuth2/OAuth2_Troubleshooting.md) - OAuth redirect issues
- [Next.js Routing Documentation](https://nextjs.org/docs/app/building-your-application/routing)

## Summary

Frontend routing issues in Werkflow typically stem from:

1. **/studio/ Prefix Misconfiguration**: Routes exist without prefix, links use prefix
2. **App Router Structure**: Incorrect directory structure for App Router
3. **Middleware**: Over-broad or incorrect middleware rewrites
4. **Build Cache**: Stale build artifacts
5. **Dynamic Routes**: Incorrect bracket syntax

**Critical Current Issue**: `/studio/` routes return 404 because actual routes exist without the prefix. This is a Next.js configuration issue, not authentication. Choose a resolution option based on desired URL structure.

Most route issues can be resolved by:
- Verifying file structure matches intended URLs
- Checking middleware configuration
- Rebuilding the application
- Testing routes systematically

Always check browser DevTools Network tab and Next.js logs for specific error details.
