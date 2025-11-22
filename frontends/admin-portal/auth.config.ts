import type { NextAuthConfig } from "next-auth"
import Keycloak from "next-auth/providers/keycloak"
import { decodeJwt, JWTPayload } from 'jose'; // Import the decode function from jose

// 1. Define the specific shape of your Keycloak payload
interface KeycloakJWTPayload extends JWTPayload {
  realm_access?: {
    roles: string[];
  };
  resource_access?: {
    [clientId: string]: {
      roles: string[];
    };
  };
  // Add other custom claims here if needed
}

/**
 * NextAuth Configuration for Keycloak OAuth2
 *
 * This configuration handles the dual-URL challenge in Docker environments:
 * - Browser needs to reach Keycloak via localhost:8090 (host-mapped port)
 * - Server-side code needs to reach Keycloak via keycloak:8080 (internal Docker network)
 * - Tokens contain issuer=localhost:8090 (what Keycloak advertises via KC_HOSTNAME)
 *
 * Three-URL Strategy:
 * - KEYCLOAK_ISSUER_INTERNAL: Server-side OIDC discovery (keycloak:8080 in Docker)
 * - KEYCLOAK_ISSUER_PUBLIC: Token validation issuer (localhost:8090 - matches token claims)
 * - KEYCLOAK_ISSUER_BROWSER: Browser redirects (localhost:8090 - user-accessible)
 */
export const authConfig = {
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,

      // CRITICAL: Use KEYCLOAK_ISSUER_PUBLIC for issuer validation
      // This must match what Keycloak puts in the "iss" claim of tokens
      // Keycloak returns "http://localhost:8090/realms/werkflow" via KC_HOSTNAME config
      issuer: process.env.KEYCLOAK_ISSUER_PUBLIC || process.env.KEYCLOAK_ISSUER,

      // Override authorization URL for browser redirects
      // Users click login -> browser redirects to this URL (must be accessible from browser)
      authorization: {
        params: { scope: "openid email profile" },
        url: `${process.env.KEYCLOAK_ISSUER_BROWSER || process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/auth`,
      },

      // Override token URL to use internal network for server-side token exchange
      // After browser redirect with code, server exchanges code for token here
      token: `${process.env.KEYCLOAK_ISSUER_INTERNAL || process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,

      // Override userinfo URL to use internal network for server-side user data fetch
      // Server fetches user profile data after getting token
      userinfo: `${process.env.KEYCLOAK_ISSUER_INTERNAL || process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/userinfo`,
    })
  ],
  callbacks: {
    async jwt({ token, account, profile }) {
      // Initial sign in
      if (account) {
        token.accessToken = account.access_token
        token.refreshToken = account.refresh_token
        token.idToken = account.id_token
        token.expiresAt = account.expires_at

        // Extract roles from Keycloak token
        const realmAccess = (profile as any)?.realm_access
        token.roles = realmAccess?.roles || []

        console.log('Realm access: ', realmAccess)
        console.log('Relam Roles: ', realmAccess?.roles)

        try {
          // Use jose's decodeJwt function
          const decodedToken = decodeJwt(account.access_token || '') as KeycloakJWTPayload;
          console.log('Decoded Token: ', decodedToken)
          
          // Assuming realm roles are in 'realm_access.roles'
          token.roles = decodedToken.realm_access?.roles || [];
          console.log('Decode Roles: ', token.roles)
          
          // If you need to verify the token (not just decode), you would use 
          // jwtVerify(account.access_token, secret) as shown in a previous answer.
          // However, for simply extracting claims inside the callback, decodeJwt is sufficient.

        } catch (error) {
          console.error("Error decoding access token with jose:", error);
        }                
      }

      return token
    },
    async session({ session, token }) {
      // Send properties to the client
      session.accessToken = token.accessToken as string
      session.user.roles = token.roles as string[]

      return session
    },
    authorized({ auth, request: { nextUrl } }) {
      const isLoggedIn = !!auth?.user
      const isOnDashboard = nextUrl.pathname.startsWith('/studio') ||
                           nextUrl.pathname.startsWith('/portal')

      if (isOnDashboard) {
        if (isLoggedIn) return true
        return false // Redirect unauthenticated users to login page
      } else if (isLoggedIn) {
        return true
      }

      return true
    },
  },
  pages: {
    signIn: '/login',
  },
  session: {
    strategy: 'jwt',
  },
  trustHost: true,
} satisfies NextAuthConfig
