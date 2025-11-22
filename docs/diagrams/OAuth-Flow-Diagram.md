# OAuth2 Flow Diagrams for Werkflow

## Complete OAuth2 Flow with Dual URLs

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           USER'S BROWSER (Host Machine)                  │
│                         http://localhost:4000                            │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1. User clicks "Sign In"
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     Admin Portal (Next.js Server-Side)                   │
│                                                                          │
│  2. Generates authorization URL using KEYCLOAK_ISSUER_BROWSER           │
│     URL: http://localhost:8090/realms/werkflow/protocol/openid-         │
│          connect/auth?client_id=...&redirect_uri=...                    │
│                                                                          │
│  3. Sends 302 redirect to browser                                       │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 4. Browser follows redirect
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    BROWSER → KEYCLOAK LOGIN PAGE                         │
│                    http://localhost:8090                                 │
│                                                                          │
│  ┌────────────────────────────────────────────────┐                     │
│  │  Werkflow Login                                │                     │
│  │  Username: [____________]                      │                     │
│  │  Password: [____________]                      │                     │
│  │  [ Sign In ]                                   │                     │
│  └────────────────────────────────────────────────┘                     │
│                                                                          │
│  5. User enters credentials                                             │
│  6. Keycloak validates credentials                                      │
│  7. Keycloak creates session                                            │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 8. Redirect to callback URL
                                    │    with authorization code
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         BROWSER → CALLBACK URL                           │
│  http://localhost:4000/api/auth/callback?code=abc123&state=xyz         │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 9. Browser requests callback
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              Admin Portal Container (Server-Side Token Exchange)         │
│                                                                          │
│  10. Receives authorization code from browser                           │
│                                                                          │
│  11. Makes server-to-server request to exchange code for tokens         │
│      Uses KEYCLOAK_ISSUER (internal Docker network)                     │
│                                                                          │
│      POST http://keycloak:8080/realms/werkflow/protocol/                │
│           openid-connect/token                                          │
│      Body:                                                              │
│        grant_type=authorization_code                                    │
│        code=abc123                                                      │
│        client_id=werkflow-admin-portal                                  │
│        client_secret=<secret>                                           │
│        redirect_uri=http://localhost:4000/api/auth/callback            │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 12. Token request via Docker network
                                    │     (admin-portal container → keycloak container)
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              KEYCLOAK CONTAINER (Internal Docker Network)                │
│                    keycloak:8080                                         │
│                                                                          │
│  13. Validates authorization code                                       │
│  14. Validates client credentials                                       │
│  15. Generates tokens:                                                  │
│      {                                                                  │
│        "access_token": "eyJhbGc...",                                    │
│        "refresh_token": "eyJhbGc...",                                   │
│        "id_token": "eyJhbGc...",                                        │
│        "expires_in": 300,                                               │
│        "token_type": "Bearer"                                           │
│      }                                                                  │
│                                                                          │
│  16. Returns tokens to admin-portal container                           │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 17. Tokens returned via Docker network
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              Admin Portal Container (Token Validation)                   │
│                                                                          │
│  18. Receives tokens                                                    │
│                                                                          │
│  19. Validates ID token signature                                       │
│      Fetches JWKS from:                                                 │
│      http://keycloak:8080/realms/werkflow/protocol/                    │
│           openid-connect/certs                                          │
│                                                                          │
│  20. Creates session with user data from ID token                       │
│                                                                          │
│  21. Sets session cookie                                                │
│                                                                          │
│  22. Redirects browser to dashboard                                     │
│      Response: 302 → http://localhost:4000/studio/dashboard            │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 23. Browser follows redirect
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATED USER SESSION                       │
│                    http://localhost:4000/studio/dashboard               │
│                                                                          │
│  User is now logged in with session cookie                              │
│  Access token stored in session for API calls                           │
└──────────────────────────────────────────────────────────────────────────┘
```

## Network Path Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                         HOST MACHINE                              │
│                                                                   │
│  ┌─────────────────┐                                             │
│  │  User's Browser │                                             │
│  │                 │                                             │
│  │  Can access:    │                                             │
│  │  localhost:4000 │◄────┐                                       │
│  │  localhost:8090 │     │                                       │
│  └─────────────────┘     │                                       │
│            │             │                                       │
│            │             │ Port Mapping                          │
│            │             │ 4000:4000                             │
│            │             │ 8090:8080                             │
└────────────┼─────────────┼───────────────────────────────────────┘
             │             │
             │             │
             ▼             │
┌───────────────────────────────────────────────────────────────────┐
│               DOCKER NETWORK: werkflow-network                    │
│                                                                   │
│  ┌─────────────────────────┐       ┌──────────────────────────┐ │
│  │   admin-portal          │       │      keycloak            │ │
│  │   container             │       │      container           │ │
│  │                         │       │                          │ │
│  │   Internal: 4000        │       │   Internal: 8080 ────────┤─┘
│  │   External: 4000 ───────┼───────┤   External: 8090         │
│  │                         │       │                          │
│  │   Can access:           │       │   Can accept from:       │
│  │   - keycloak:8080 ──────┼──────►│   - admin-portal         │
│  │   - localhost:4000      │       │   - hr-portal            │
│  │   - NOT localhost:8090  │       │   - Any Host header      │
│  │     (different network) │       │                          │
│  │                         │       │   Responds with URLs     │
│  │   Uses for:             │       │   matching Host header   │
│  │   - Token exchange      │       │   (KC_HOSTNAME_STRICT=   │
│  │   - Token validation    │       │    false)                │
│  │   - OIDC discovery      │       │                          │
│  │   - Userinfo endpoint   │       │                          │
│  └─────────────────────────┘       └──────────────────────────┘ │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

## Environment Variable Usage

```
┌────────────────────────────────────────────────────────────────────┐
│                   ENVIRONMENT VARIABLES                            │
└────────────────────────────────────────────────────────────────────┘

                            admin-portal

                 ┌──────────────────────────────────┐
                 │  KEYCLOAK_ISSUER                 │
                 │  http://keycloak:8080/realms/    │
                 │  werkflow                        │
                 └──────────────────────────────────┘
                            │
                            ├─► Used for:
                            │   - Token endpoint (server-side)
                            │   - Userinfo endpoint (server-side)
                            │   - OIDC discovery (server-side)
                            │   - Token validation (server-side)
                            │
                            │   All server-side operations in
                            │   admin-portal container use this
                            │   to reach keycloak via internal
                            │   Docker network


                 ┌──────────────────────────────────┐
                 │  KEYCLOAK_ISSUER_BROWSER         │
                 │  http://localhost:8090/realms/   │
                 │  werkflow                        │
                 └──────────────────────────────────┘
                            │
                            ├─► Used for:
                            │   - Authorization URL (browser redirect)
                            │   - Login page redirect
                            │
                            │   Generated by admin-portal but
                            │   executed by user's browser on
                            │   host machine
```

## Request Flow Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│              BROWSER REQUEST (Authorization)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Origin: User's browser on host machine                        │
│  URL Generation: Uses KEYCLOAK_ISSUER_BROWSER                  │
│                                                                 │
│  http://localhost:8090/realms/werkflow/protocol/openid-        │
│       connect/auth?client_id=...                               │
│                                                                 │
│  Network Path:                                                 │
│  Browser → localhost:8090 → Docker port mapping →              │
│  keycloak:8080 (container)                                     │
│                                                                 │
│  ✅ Works because browser can resolve localhost                │
│  ✅ Port 8090 is mapped to keycloak container's port 8080      │
└─────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│              SERVER REQUEST (Token Exchange)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Origin: admin-portal container                                │
│  URL Generation: Uses KEYCLOAK_ISSUER                          │
│                                                                 │
│  http://keycloak:8080/realms/werkflow/protocol/openid-         │
│       connect/token                                            │
│                                                                 │
│  Network Path:                                                 │
│  admin-portal container → keycloak container                   │
│  (same Docker network)                                         │
│                                                                 │
│  ✅ Works because containers share werkflow-network            │
│  ✅ No external network hop required                           │
│  ✅ Fast, secure internal communication                        │
└─────────────────────────────────────────────────────────────────┘
```

## Configuration Profiles

### Development (Local, Not in Docker)

```
┌─────────────────────────────────────────────────┐
│  Developer's Machine                            │
│                                                 │
│  ┌─────────────────┐     ┌─────────────────┐   │
│  │  Next.js        │     │  Browser        │   │
│  │  (npm run dev)  │     │                 │   │
│  │                 │     │                 │   │
│  │  localhost:4000 │     │  localhost:4000 │   │
│  └────────┬────────┘     └────────┬────────┘   │
│           │                       │            │
│           └───────────┬───────────┘            │
│                       │                        │
└───────────────────────┼────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│  Docker                                         │
│  ┌───────────────┐                              │
│  │  Keycloak     │                              │
│  │  8090:8080    │                              │
│  └───────────────┘                              │
└─────────────────────────────────────────────────┘

.env.local:
  KEYCLOAK_ISSUER=http://localhost:8090/realms/werkflow
  KEYCLOAK_ISSUER_BROWSER=http://localhost:8090/realms/werkflow

Both use localhost:8090 because both Next.js and browser
are on the host machine.
```

### Development (Full Docker Stack)

```
┌─────────────────────────────────────────────────┐
│  Developer's Machine                            │
│                                                 │
│  ┌─────────────────┐                            │
│  │  Browser        │                            │
│  │  localhost:4000 │                            │
│  │  localhost:8090 │                            │
│  └────────┬────────┘                            │
│           │                                     │
└───────────┼─────────────────────────────────────┘
            │
            │ Port Mappings: 4000:4000, 8090:8080
            ▼
┌─────────────────────────────────────────────────┐
│  Docker Network: werkflow-network               │
│                                                 │
│  ┌──────────────┐      ┌──────────────┐        │
│  │ admin-portal │      │  keycloak    │        │
│  │ 4000:4000    │──────│  8090:8080   │        │
│  └──────────────┘      └──────────────┘        │
└─────────────────────────────────────────────────┘

docker-compose.yml:
  KEYCLOAK_ISSUER: http://keycloak:8080/realms/werkflow
  KEYCLOAK_ISSUER_BROWSER: http://localhost:8090/realms/werkflow

Different URLs because:
- admin-portal container uses internal network
- Browser uses port-mapped external access
```

### Production (Kubernetes)

```
┌─────────────────────────────────────────────────┐
│  Internet                                       │
│  ┌───────────────┐                              │
│  │  User Browser │                              │
│  └───────┬───────┘                              │
└──────────┼──────────────────────────────────────┘
           │
           │ HTTPS
           ▼
┌─────────────────────────────────────────────────┐
│  Ingress / Load Balancer                        │
│  - auth.werkflow.com → keycloak-service         │
│  - app.werkflow.com → admin-portal-service      │
└──────────┬──────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│  Kubernetes Cluster                             │
│                                                 │
│  ┌──────────────┐      ┌──────────────┐        │
│  │ admin-portal │      │  keycloak    │        │
│  │ pod          │──────│  pod         │        │
│  │              │      │              │        │
│  │ Internal:    │      │ Internal:    │        │
│  │ keycloak.svc │      │ keycloak.svc │        │
│  │              │      │              │        │
│  │ External:    │      │ External:    │        │
│  │ auth.werk... │      │ auth.werk... │        │
│  └──────────────┘      └──────────────┘        │
└─────────────────────────────────────────────────┘

Production config:
  KEYCLOAK_ISSUER: http://keycloak.werkflow.svc.cluster.local/realms/werkflow
  KEYCLOAK_ISSUER_BROWSER: https://auth.werkflow.com/realms/werkflow

  KC_HOSTNAME: auth.werkflow.com
  KC_HOSTNAME_STRICT: true
  KC_HOSTNAME_PROTOCOL: https
```

## Security Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    SECURITY BOUNDARIES                         │
└────────────────────────────────────────────────────────────────┘

                        Authentication Flow

┌─────────────────────────────────────────────────────────────────┐
│  1. Browser Redirect (Public Network)                          │
│     Credential Entry                                            │
│                                                                 │
│     Security: HTTPS in production, form-based auth              │
│     Risk: Login page is public (but protected by Keycloak)      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Authorization Code Redirect (Public Network)                │
│     Single-use code, short-lived                                │
│                                                                 │
│     Security: Code useless without client secret                │
│     Risk: Minimal, code can't be replayed or used alone         │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Token Exchange (Internal Docker Network)                    │
│     Client secret transmitted                                   │
│                                                                 │
│     Security: Internal network, not exposed to internet         │
│     Risk: Very low, traffic never leaves Docker network         │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. Token Storage (Server-Side Session)                         │
│     Tokens stored in session, not exposed to browser            │
│                                                                 │
│     Security: Encrypted session cookie with httpOnly flag       │
│     Risk: Very low, tokens never reach browser JavaScript       │
└─────────────────────────────────────────────────────────────────┘
```

This architecture ensures that:
1. Client secrets never reach the browser
2. Access tokens are not exposed to browser JavaScript
3. Token exchange happens over internal network
4. Production uses HTTPS for all external communication
5. Sessions are properly encrypted and secured
