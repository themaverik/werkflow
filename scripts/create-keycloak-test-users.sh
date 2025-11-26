#!/bin/bash

# Keycloak Test Users Creation Script
# Creates 10 test users across all departments

set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8090}"
REALM="werkflow"
ADMIN_USER="admin"
ADMIN_PASS="admin"

echo "=== Keycloak Test User Creation Script ==="
echo "Keycloak URL: $KEYCLOAK_URL"
echo "Realm: $REALM"
echo ""

# Get admin token
echo "1. Authenticating as admin..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "ERROR: Failed to authenticate"
  exit 1
fi
echo "✓ Authentication successful"
echo ""

# Function to create user
create_user() {
  local USERNAME=$1
  local EMAIL=$2
  local FIRST_NAME=$3
  local LAST_NAME=$4
  local DEPARTMENT=$5
  local DOA_LEVEL=$6

  echo "Creating user: $USERNAME ($EMAIL)..."

  # Create user
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "username": "'"$USERNAME"'",
      "email": "'"$EMAIL"'",
      "firstName": "'"$FIRST_NAME"'",
      "lastName": "'"$LAST_NAME"'",
      "enabled": true,
      "emailVerified": true,
      "attributes": {
        "department": ["'"$DEPARTMENT"'"],
        "doa_level": ["'"$DOA_LEVEL"'"]
      },
      "credentials": [{
        "type": "password",
        "value": "password123",
        "temporary": true
      }]
    }' > /dev/null

  echo "✓ User created: $USERNAME"
}

# Function to assign role
assign_role() {
  local USERNAME=$1
  local ROLE_NAME=$2

  # Get user ID
  USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$USERNAME" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

  if [ -z "$USER_ID" ] || [ "$USER_ID" == "null" ]; then
    echo "ERROR: Could not find user $USERNAME"
    return
  fi

  # Get role ID
  ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$ROLE_NAME" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.id')

  if [ -z "$ROLE_ID" ] || [ "$ROLE_ID" == "null" ]; then
    echo "WARNING: Role $ROLE_NAME not found"
    return
  fi

  # Assign role
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/role-mappings/realm" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '[{
      "id": "'"$ROLE_ID"'",
      "name": "'"$ROLE_NAME"'"
    }]' > /dev/null

  echo "✓ Role assigned: $ROLE_NAME → $USERNAME"
}

echo "2. Creating test users..."
echo ""

# Admin Users
create_user "admin.user" "admin@company.com" "Admin" "User" "Admin" 4
assign_role "admin.user" "admin"
assign_role "admin.user" "super_admin"
echo ""

# HR Users
create_user "hr.employee" "hr.employee@company.com" "HR" "Employee" "HR" 0
assign_role "hr.employee" "employee"
assign_role "hr.employee" "asset_request_requester"
echo ""

create_user "hr.manager" "hr.manager@company.com" "HR" "Manager" "HR" 1
assign_role "hr.manager" "hr_manager"
echo ""

create_user "hr.head" "hr.head@company.com" "HR" "Head" "HR" 2
assign_role "hr.head" "hr_head"
echo ""

# IT Users
create_user "it.employee" "it.employee@company.com" "IT" "Employee" "IT" 0
assign_role "it.employee" "employee"
assign_role "it.employee" "asset_request_requester"
echo ""

create_user "it.manager" "it.manager@company.com" "IT" "Manager" "IT" 1
assign_role "it.manager" "it_manager"
echo ""

# Finance Users
create_user "finance.manager" "finance.manager@company.com" "Finance" "Manager" "Finance" 2
assign_role "finance.manager" "finance_manager"
echo ""

create_user "finance.head" "finance.head@company.com" "Finance" "Head" "Finance" 4
assign_role "finance.head" "finance_head"
echo ""

# Procurement Users
create_user "procurement.manager" "procurement.manager@company.com" "Procurement" "Manager" "Procurement" 1
assign_role "procurement.manager" "procurement_manager"
echo ""

create_user "procurement.admin" "procurement.admin@company.com" "Procurement" "Admin" "Procurement" 2
assign_role "procurement.admin" "procurement_head"
assign_role "procurement.admin" "admin"
echo ""

echo "=== Test Users Created Successfully ==="
echo ""
echo "Users created:"
echo "  admin.user / password123 (Admin, Super Admin)"
echo "  hr.employee / password123 (HR Employee)"
echo "  hr.manager / password123 (HR Manager, DOA Level 1)"
echo "  hr.head / password123 (HR Head, DOA Level 2)"
echo "  it.employee / password123 (IT Employee)"
echo "  it.manager / password123 (IT Manager, DOA Level 1)"
echo "  finance.manager / password123 (Finance Manager, DOA Level 2)"
echo "  finance.head / password123 (Finance Head/CFO, DOA Level 4)"
echo "  procurement.manager / password123 (Procurement Manager, DOA Level 1)"
echo "  procurement.admin / password123 (Procurement Admin, DOA Level 2)"
echo ""
echo "All users must change password on first login (temporary=true)"
