import { auth } from './auth';

export default auth((req) => {
  // req.auth contains the authenticated user session
});

export const config = {
  // Protect all routes except static files and public pages
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|login|error).*)'],
};
