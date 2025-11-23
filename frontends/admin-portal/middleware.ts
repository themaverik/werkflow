import { auth } from "@/auth"
import { NextResponse } from 'next/server'

export default auth((req) => {
  const isLoggedIn = !!req.auth
  const { pathname, search } = req.nextUrl

  // Check if accessing protected studio routes without login
  if (pathname.startsWith('/studio/')) {
    const actualPath = pathname.replace(/^\/studio/, '') || '/'

    const isProtectedRoute =
      actualPath.startsWith('/processes') ||
      actualPath.startsWith('/forms') ||
      actualPath.startsWith('/workflows') ||
      actualPath.startsWith('/services')

    // If not logged in, redirect to login (preserving the original /studio/ URL)
    if (isProtectedRoute && !isLoggedIn) {
      const loginUrl = new URL('/login', req.url)
      loginUrl.searchParams.set('callbackUrl', pathname + search)
      return NextResponse.redirect(loginUrl)
    }

    // For authenticated requests, redirect to the actual route (without /studio/ prefix)
    // This is needed because routes exist at /processes, not /studio/processes
    const redirectUrl = new URL(actualPath + search, req.url)
    return NextResponse.redirect(redirectUrl)
  }

  // Check if accessing protected portal routes without login
  if (pathname.startsWith('/portal/')) {
    const actualPath = pathname.replace(/^\/portal/, '') || '/'

    const isProtectedRoute =
      actualPath.startsWith('/tasks') ||
      actualPath.startsWith('/monitoring') ||
      actualPath.startsWith('/analytics')

    // If not logged in, redirect to login (preserving the original /portal/ URL)
    if (isProtectedRoute && !isLoggedIn) {
      const loginUrl = new URL('/login', req.url)
      loginUrl.searchParams.set('callbackUrl', pathname + search)
      return NextResponse.redirect(loginUrl)
    }

    // For authenticated requests, redirect to the actual route (without /portal/ prefix)
    const redirectUrl = new URL(actualPath + search, req.url)
    return NextResponse.redirect(redirectUrl)
  }

  // For direct access to routes (without /studio/ or /portal/ prefix)
  const isProtectedRoute =
    pathname.startsWith('/processes') ||
    pathname.startsWith('/forms') ||
    pathname.startsWith('/workflows') ||
    pathname.startsWith('/services') ||
    pathname.startsWith('/tasks') ||
    pathname.startsWith('/monitoring') ||
    pathname.startsWith('/analytics')

  // If accessing protected route without being logged in, redirect to login
  if (isProtectedRoute && !isLoggedIn) {
    const loginUrl = new URL('/login', req.url)
    loginUrl.searchParams.set('callbackUrl', pathname + search)
    return NextResponse.redirect(loginUrl)
  }

  return undefined
})

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
}
