# Fix: `/studio/processes` Returning 404 Error

## Problem
When you visit `http://localhost:4000/studio/processes`, you get a **404 error** instead of seeing the processes list.

---

## Root Causes & Solutions

### Root Cause 1: Missing or Invalid Authentication Token

**Symptoms**:
- Redirects to login page
- Returns 404 after login attempts
- No authenticated session

**Solutions**:

**Option A: Clear Cache & Re-authenticate**
```bash
# 1. Clear browser cookies/cache
# - Chrome: DevTools → Application → Clear Site Data
# - Safari: Settings → Privacy → Manage Website Data
# - Firefox: Settings → Privacy → Clear Data

# 2. Restart the browser session
# 3. Go to http://localhost:4000
# 4. Click "Process Studio"
# 5. Complete full login flow
```

**Option B: Use Keycloak Admin Console**
```bash
# 1. Open Keycloak: http://localhost:8090
# 2. Login with: admin / admin123
# 3. Go to: Realm: werkflow
# 4. Click: Users → Find your user
# 5. Verify user exists and is active
```

---

### Root Cause 2: Missing HR_ADMIN Role

**Symptoms**:
- Access Denied page appears
- Shows message: "You don't have permission to access the Studio"
- Lists your current roles (missing HR_ADMIN)

**Solutions**:

**Assign HR_ADMIN Role (Admin Only)**:
```bash
# 1. Open Keycloak: http://localhost:8090
# 2. Login: admin / admin123
# 3. Select Realm: werkflow
# 4. Go to: Users → Find your user
# 5. Click: Role Mappings tab
# 6. Under "Client Roles", select: werkflow-admin-portal
# 7. Add from "Available Roles": HR_ADMIN
# 8. Save changes
```

**Verify Role Assignment**:
```bash
# In browser DevTools console:
const session = await fetch('/api/auth/session').then(r => r.json())
console.log('Your roles:', session.user.roles)
# Should include: ["HR_ADMIN"]
```

---

### Root Cause 3: Engine Service Not Running

**Symptoms**:
- Processes page loads but shows: "No processes deployed yet"
- Or shows loading spinner indefinitely
- Network error in browser console

**Solutions**:

**Check Service Status**:
```bash
# 1. Check if services are running
docker ps | grep werkflow

# Should show:
# werkflow-engine      ✅ Running
# werkflow-admin       ✅ Running
# werkflow-hr          ✅ Running
```

**Restart Services**:
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker

# Check status
docker-compose ps

# If not running:
docker-compose up -d

# Monitor startup
docker-compose logs -f engine-service
```

**Test API Directly**:
```bash
# Test if engine service responds
curl -s http://localhost:8081/api/flowable/process-definitions | jq .

# Should return: [] (empty array if no processes)
# Or: [{ id, key, name, version, ... }]

# If fails, check logs:
docker logs werkflow-engine --tail 50
```

---

### Root Cause 4: Authentication Server (Keycloak) Issues

**Symptoms**:
- Can't login at all
- Login page returns 500 error
- Keycloak unavailable

**Solutions**:

**Check Keycloak Status**:
```bash
# 1. Test Keycloak endpoint
curl -s http://localhost:8090 | head -20

# 2. Check Keycloak logs
docker logs werkflow-keycloak --tail 50

# 3. Check Keycloak PostgreSQL
docker logs werkflow-keycloak-db --tail 20
```

**Restart Keycloak & PostgreSQL**:
```bash
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker

# Restart Keycloak dependencies
docker-compose restart keycloak-postgres keycloak

# Wait 30-60 seconds for initialization
sleep 60

# Verify health
curl -s http://localhost:8090/health/ready | jq .
```

---

## Step-by-Step Troubleshooting

### Step 1: Test Frontend is Running
```bash
# Should return HTML (not error)
curl -I http://localhost:4000
# Look for: HTTP/1.1 200 OK
```

### Step 2: Test Authentication
```bash
# Go to: http://localhost:4000/api/auth/session
# If redirected to login: Auth not working
# If returns JSON: Auth working, check roles
```

### Step 3: Check Your Roles
```javascript
// In browser console:
fetch('/api/auth/session')
  .then(r => r.json())
  .then(session => {
    console.log('User:', session.user.name)
    console.log('Roles:', session.user.roles)
    console.log('Has HR_ADMIN:', session.user.roles?.includes('HR_ADMIN'))
  })
```

### Step 4: Test Backend API
```bash
# Should return process list (empty or with data)
curl http://localhost:8081/api/flowable/process-definitions

# If fails:
# - Check engine service is running: docker ps
# - Check logs: docker logs werkflow-engine
# - Restart: docker-compose restart engine-service
```

### Step 5: Check Browser Console
```javascript
// Open DevTools → Console
// Look for errors like:
// - "Failed to fetch /studio/processes"
// - "401 Unauthorized"
// - "403 Forbidden"
// - "Network error"

// Check Network tab:
// - Request to /studio/processes
// - Should be 200 OK
// - Look at response
```

---

## Common Error Messages & Fixes

### Error: "Access Denied - You don't have permission to access the Studio"

**Fix**: You need HR_ADMIN role
```bash
# In Keycloak admin console:
# Users → Your User → Role Mappings → Add HR_ADMIN
```

### Error: "403 Forbidden"

**Likely**: Role-based access control blocking request
```bash
# Same as above - need HR_ADMIN role
```

### Error: "404 Not Found"

**Could be**:
1. Route doesn't exist (unlikely - page is in codebase)
2. Authentication redirecting incorrectly
3. Next.js build issue

**Fix**:
```bash
# Restart the admin-portal container
cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
docker-compose restart admin-portal

# Or rebuild
docker-compose up -d --build admin-portal
```

### Error: "Loading..." forever (no data)

**Likely**: API not responding
```bash
# Check engine service
curl http://localhost:8081/api/flowable/process-definitions

# If fails, restart:
docker-compose restart engine-service
```

---

## Working Configuration Checklist

✅ **Authentication**
- [ ] Keycloak running at `http://localhost:8090`
- [ ] Can login to Keycloak admin console: `admin/admin123`
- [ ] Your user exists in "werkflow" realm
- [ ] Your user has "HR_ADMIN" role assigned

✅ **Frontend**
- [ ] Admin Portal running at `http://localhost:4000`
- [ ] Landing page loads (clean, simple design)
- [ ] Can see "Process Studio" button

✅ **Backend Services**
- [ ] Engine Service running at `http://localhost:8081`
- [ ] API endpoint responds: `/api/flowable/process-definitions`
- [ ] PostgreSQL running for data storage
- [ ] No startup errors in logs

✅ **Session & Roles**
- [ ] After login, session created
- [ ] Session contains "HR_ADMIN" role
- [ ] Session token valid (not expired)

---

## Quick Test Script

```bash
#!/bin/bash

echo "1. Testing Frontend..."
curl -I http://localhost:4000 2>/dev/null | head -1

echo ""
echo "2. Testing Keycloak..."
curl -I http://localhost:8090 2>/dev/null | head -1

echo ""
echo "3. Testing Engine API..."
curl -I http://localhost:8081/api/health 2>/dev/null | head -1

echo ""
echo "4. Testing Process Definitions API..."
curl -s http://localhost:8081/api/flowable/process-definitions | jq . 2>/dev/null || echo "Failed to fetch"

echo ""
echo "5. Checking Docker Services..."
docker ps --format "table {{.Names}}\t{{.Status}}" | grep werkflow

echo ""
echo "Done! Check results above."
```

**Run**:
```bash
bash quick-test.sh
```

---

## Still Having Issues?

### Gather Debugging Information

```bash
# 1. Service status
docker-compose ps > /tmp/docker-status.txt

# 2. Engine logs
docker logs werkflow-engine > /tmp/engine-logs.txt

# 3. Keycloak logs
docker logs werkflow-keycloak > /tmp/keycloak-logs.txt

# 4. Browser network trace (in DevTools):
# - Open DevTools → Network tab
# - Refresh page http://localhost:4000/studio/processes
# - Right-click → Save as HAR
# - Save to file

# Then you can inspect what's happening
```

### Common Logs Issues

**Engine Service Error**:
```
[ERROR] Failed to start: database not found
```
Solution: `docker-compose restart postgres engine-service`

**Keycloak Error**:
```
[ERROR] Failed to initialize Keycloak database
```
Solution: `docker-compose down -v && docker-compose up -d`

**Frontend Error**:
```
[ERROR] NEXTAUTH_SECRET not set
```
Solution: Check `.env` files in admin-portal or docker-compose env settings

---

## Reference: Page Flow

```
http://localhost:4000
         ↓
    Landing Page
         ↓
  Click "Process Studio"
         ↓
  Check Authentication
  ├─ Not logged in → Redirect to Keycloak login
  └─ Logged in → Check roles
         ↓
  Check HR_ADMIN Role
  ├─ No HR_ADMIN → Show "Access Denied"
  └─ Has HR_ADMIN → Continue
         ↓
  Redirect to /studio/processes
         ↓
  Studio Layout Loads
         ↓
  Fetch process definitions from backend
  (GET /flowable/process-definitions)
         ↓
  Display Processes
  ├─ No processes → Show empty state
  └─ With processes → Show process cards
```

---

## Further Help

If you're still seeing 404 after checking all the above:

1. **Check the exact URL**: Should be `http://localhost:4000/studio/processes`
   - Not `/studio/process` (singular)
   - Not `/processes` (missing `/studio`)
   - Not with extra characters

2. **Browser DevTools**:
   - Open Console tab
   - Look for JavaScript errors
   - Check Network tab → XHR/Fetch requests

3. **Check Docker logs**:
   ```bash
   docker logs werkflow-admin-portal --tail 100
   ```

4. **Restart everything**:
   ```bash
   cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker
   docker-compose down
   docker-compose up -d
   sleep 60  # Wait for initialization
   # Then try again
   ```

