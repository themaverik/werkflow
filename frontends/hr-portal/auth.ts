import NextAuth from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import { authConfig } from './auth.config';

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
export const { auth, signIn, signOut, handlers } = NextAuth({
  ...authConfig,
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,

      // CRITICAL: Use KEYCLOAK_ISSUER_PUBLIC for issuer validation
      // This must match what Keycloak puts in the "iss" claim of tokens
      // Keycloak returns "http://localhost:8090/realms/werkflow" via KC_HOSTNAME config
      issuer: process.env.KEYCLOAK_ISSUER_PUBLIC || process.env.KEYCLOAK_ISSUER_INTERNAL,

      // Override authorization URL for browser redirects
      // Users click login -> browser redirects to this URL (must be accessible from browser)
      authorization: {
        params: { scope: "openid email profile" },
        url: `${process.env.KEYCLOAK_ISSUER_BROWSER || process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/auth`,
      },

      // Override token URL to use internal network for server-side token exchange
      // After browser redirect with code, server exchanges code for token here
      token: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/token`,

      // Override userinfo URL to use internal network for server-side user data fetch
      // Server fetches user profile data after getting token
      userinfo: `${process.env.KEYCLOAK_ISSUER_INTERNAL}/protocol/openid-connect/userinfo`,
    }),
  ],
});
