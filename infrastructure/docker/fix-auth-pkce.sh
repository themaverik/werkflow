#!/bin/bash

# Werkflow Authentication Fix - Configure PKCE
# This script configures PKCE (Proof Key for Code Exchange) in Keycloak
# for the werkflow-admin-portal client

set -e

echo "======================================"
echo "Werkflow Authentication PKCE Fix"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
KEYCLOAK_URL="http://localhost:8090"
REALM="werkflow"
CLIENT_ID="werkflow-admin-portal"
CLIENT_UUID="b13f4946-99d0-4c4d-9c54-7d6ad398ed4a"
ADMIN_USER="admin"
ADMIN_PASS="admin123"

echo "1. Verifying Keycloak is running..."
if ! curl -sf "$KEYCLOAK_URL/health/ready" > /dev/null; then
  echo -e "${RED}ERROR:${NC} Keycloak is not running or not healthy"
  echo "Please start Keycloak: cd /Users/lamteiwahlang/Projects/werkflow/infrastructure/docker && docker compose up -d keycloak"
  exit 1
fi
echo -e "${GREEN}✓${NC} Keycloak is running"
echo ""

echo "2. Getting admin access token..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

if [ -z "$TOKEN" ]; then
  echo -e "${RED}ERROR:${NC} Failed to get access token"
  exit 1
fi
echo -e "${GREEN}✓${NC} Admin access token obtained"
echo ""

echo "3. Getting current client configuration..."
curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" > /tmp/werkflow_client_config.json

if [ ! -s /tmp/werkflow_client_config.json ]; then
  echo -e "${RED}ERROR:${NC} Failed to get client configuration"
  exit 1
fi

echo -e "${GREEN}✓${NC} Client configuration retrieved"
echo ""

echo "4. Checking current PKCE configuration..."
CURRENT_PKCE=$(python3 -c 'import sys, json; c = json.load(open("/tmp/werkflow_client_config.json")); print(c.get("attributes", {}).get("pkce.code.challenge.method", "Not set"))' 2>/dev/null)
echo "   Current PKCE method: $CURRENT_PKCE"
echo ""

if [ "$CURRENT_PKCE" = "S256" ]; then
  echo -e "${YELLOW}INFO:${NC} PKCE is already configured with S256"
  echo "The issue might be elsewhere. Check the full analysis document."
  echo ""
  exit 0
fi

echo "5. Updating client configuration to enable PKCE..."

python3 << 'PYEOF'
import json

with open('/tmp/werkflow_client_config.json', 'r') as f:
    config = json.load(f)

if 'attributes' not in config:
    config['attributes'] = {}

config['attributes']['pkce.code.challenge.method'] = 'S256'

with open('/tmp/werkflow_client_config_updated.json', 'w') as f:
    json.dump(config, f)

print("PKCE configuration prepared")
PYEOF

# Get fresh token
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @/tmp/werkflow_client_config_updated.json)

if [ "$HTTP_CODE" = "204" ] || [ "$HTTP_CODE" = "200" ]; then
  echo -e "${GREEN}✓${NC} Client configuration updated successfully (HTTP $HTTP_CODE)"
else
  echo -e "${RED}ERROR:${NC} Failed to update client configuration (HTTP $HTTP_CODE)"
  exit 1
fi
echo ""

echo "6. Verifying PKCE configuration..."
sleep 1

# Get fresh token for verification
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

NEW_PKCE=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID" \
  -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys, json; c = json.load(sys.stdin); print(c.get("attributes", {}).get("pkce.code.challenge.method", "Not set"))')

if [ "$NEW_PKCE" = "S256" ]; then
  echo -e "${GREEN}✓${NC} PKCE is now configured with S256"
else
  echo -e "${RED}ERROR:${NC} PKCE configuration not applied (current: $NEW_PKCE)"
  exit 1
fi
echo ""

echo "======================================"
echo -e "${GREEN}PKCE Configuration Complete!${NC}"
echo "======================================"
echo ""
echo "Next steps:"
echo "  1. Clear your browser cookies for localhost:4000 and localhost:8090"
echo "  2. Or use incognito/private browsing mode"
echo "  3. Navigate to: http://localhost:4000"
echo "  4. Click 'My Tasks' or any portal link"
echo "  5. Click 'Sign in with Keycloak'"
echo "  6. Login with your Keycloak credentials"
echo "  7. You should be redirected to /portal/tasks successfully"
echo ""
echo "If you still encounter issues:"
echo "  - Check admin-portal logs: docker logs werkflow-admin-portal --tail 100"
echo "  - Check Keycloak logs: docker logs werkflow-keycloak --tail 100"
echo "  - Review: Authentication-Flow-Analysis.md"
echo ""
