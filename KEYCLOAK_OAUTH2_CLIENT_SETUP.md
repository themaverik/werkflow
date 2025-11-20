# Keycloak OAuth2 Client Setup Guide

## Problem

The admin portal is returning a "Configuration" error when attempting to login because the OAuth2 client has not been created in Keycloak yet.

## Solution

You need to create an OAuth2 client in Keycloak for the `werkflow-admin-portal`. Here's how:

---

## Step 1: Access Keycloak Admin Console

1. Open browser and go to: `http://localhost:8090/admin/master/console`
2. Login with credentials:
   - Username: `admin`
   - Password: `admin123`

---

## Step 2: Navigate to Werkflow Realm

1. In the top-left corner, you'll see a dropdown showing "Master"
2. Click on it to open the realm selector
3. Select **"werkflow"** realm

---

## Step 3: Create OAuth2 Client for Admin Portal

1. In the left sidebar, click **"Clients"**
2. Click the **"Create client"** button (top-right)
3. Fill in the details:
   - **Client ID**: `werkflow-admin-portal`
   - Leave everything else as default
4. Click **"Next"**

---

## Step 4: Configure Client Authentication

1. Toggle **"Client authentication"** to **ON** (blue)
   - This creates a client secret for secure OAuth2 flow
2. Click **"Next"**

---

## Step 5: Configure Redirect URIs

1. Under **"Valid redirect URIs"**, click **"Add URI"**
2. Add these URIs:
   - `http://localhost:4000/api/auth/callback/keycloak`
   - `http://localhost:4000/login`
3. Under **"Valid post logout redirect URIs"**, click **"Add URI"**
4. Add this URI:
   - `http://localhost:4000`
5. Click **"Save"**

---

## Step 6: Get the Client Secret

1. Go to the **"Credentials"** tab
2. You'll see **"Client secret"** displayed
3. **Copy this value** - you need it for the next step

Example output:
```
Client ID: werkflow-admin-portal
Client secret: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

## Step 7: Update Docker Configuration

Now you need to set the client secret in your environment:

### Option A: Using Environment Variable (Recommended for Development)

Run this command in your terminal **before** restarting Docker:

```bash
export KEYCLOAK_ADMIN_PORTAL_SECRET="<YOUR_CLIENT_SECRET_HERE>"
```

Replace `<YOUR_CLIENT_SECRET_HERE>` with the secret you copied in Step 6.

Then restart the admin portal:
```bash
docker-compose restart admin-portal
```

### Option B: Direct Update to docker-compose.yml

Edit `infrastructure/docker/docker-compose.yml` and find this line:

```yaml
KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_ADMIN_PORTAL_SECRET:-change-me-in-production}
```

Replace it with:

```yaml
KEYCLOAK_CLIENT_SECRET: your-actual-secret-here
```

Then restart:
```bash
docker-compose restart admin-portal
```

---

## Step 8: Create OAuth2 Client for HR Portal (Same Process)

Repeat the same steps above but with:
- **Client ID**: `werkflow-hr-portal`
- **Valid redirect URIs**:
  - `http://localhost:4001/api/auth/callback/keycloak`
  - `http://localhost:4001/login`
- **Valid post logout redirect URIs**:
  - `http://localhost:4001`

Get the client secret and set it using:
```bash
export KEYCLOAK_HR_PORTAL_SECRET="<YOUR_CLIENT_SECRET_HERE>"
docker-compose restart hr-portal
```

---

## Step 9: Create HR_ADMIN Role

1. In left sidebar, click **"Roles"**
2. Click **"Create role"**
3. Enter name: `HR_ADMIN`
4. Click **"Save"**

---

## Step 10: Create Test User

1. In left sidebar, click **"Users"**
2. Click **"Create user"**
3. Fill in:
   - **Username**: `admin`
   - **Email**: `admin@werkflow.local`
   - **First name**: `Admin`
   - **Last name**: `User`
4. Click **"Create"**

5. Go to the **"Credentials"** tab:
   - Click **"Set password"**
   - Enter: `admin123`
   - Toggle **"Temporary"** to **OFF**
   - Click **"Set password"**

6. Go to the **"Role mapping"** tab:
   - Click **"Assign role"**
   - Search for `HR_ADMIN`
   - Select it and click **"Assign"**

---

## Test the Login Flow

1. Go to: `http://localhost:4000/login`
2. You should see a Keycloak login button or redirect
3. Login with credentials:
   - Username: `admin`
   - Password: `admin123`
4. You should be redirected to: `http://localhost:4000/studio/processes`
5. You should see the BPMN processes list

---

## Troubleshooting

### Still Getting "Configuration" Error

**Problem**: Error persists after creating the client

**Solution**:
1. Verify client secret was copied correctly
2. Make sure environment variable is set (if using Option A)
3. Restart the container: `docker-compose restart admin-portal`
4. Check logs: `docker logs werkflow-admin-portal | grep -i keycloak`

### Login Redirects Back to Login Page

**Problem**: After entering credentials, it redirects back to login

**Solution**:
1. Verify the redirect URIs are correctly configured in Keycloak
2. Check the exact spelling: `http://localhost:4000/api/auth/callback/keycloak`
3. Make sure `HR_ADMIN` role is assigned to the user
4. Clear browser cookies for localhost:4000

### Cannot Access Keycloak Admin Console

**Problem**: `http://localhost:8090/admin/master/console` is unreachable

**Solution**:
```bash
# Check Keycloak container status
docker ps | grep keycloak

# If unhealthy, restart it
docker-compose restart keycloak

# Wait 30 seconds for it to start, then try again
```

---

## What Each Redirect URI Does

| URI | Purpose |
|-----|---------|
| `http://localhost:4000/api/auth/callback/keycloak` | NextAuth OAuth callback endpoint |
| `http://localhost:4000/login` | Redirect after logout |
| `http://localhost:4000` | Post-logout redirect |

---

## Next Steps

After completing these steps:

1. Try logging in at `http://localhost:4000/login`
2. Navigate to `http://localhost:4000/studio/processes`
3. View BPMN process designs
4. Explore the admin portal features

---

## Additional Resources

- Keycloak Admin Console: `http://localhost:8090/admin/master/console`
- NextAuth Keycloak Provider Docs: `https://next-auth.js.org/providers/keycloak`
- werkflow Realm: `http://localhost:8090/realms/werkflow`
