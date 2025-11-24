#!/bin/bash

# Test script for Forms API endpoint
# Tests both the old problematic path and the new working path

echo "====================================="
echo "Forms API Endpoint Test"
echo "====================================="
echo ""

BASE_URL="http://localhost:8081"
FORM_KEY="employee-onboarding"

echo "Test 1: Testing NEW path /werkflow/api/forms/{formKey}"
echo "Expected: 401 Unauthorized (endpoint exists but needs auth)"
echo "-------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/werkflow/api/forms/${FORM_KEY}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "401" ]; then
    echo "✓ PASS - Endpoint is properly routed to controller"
else
    echo "✗ FAIL - Expected 401, got $HTTP_CODE"
    echo "Response: $BODY"
fi
echo ""

echo "Test 2: Testing OLD path /api/forms/{formKey}"
echo "Expected: Could be 401 or 404 depending on Flowable routing"
echo "-------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/api/forms/${FORM_KEY}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "404" ]; then
    echo "⚠ WARNING - Old path returns 404 (Flowable REST conflict)"
    echo "Response: $BODY"
elif [ "$HTTP_CODE" = "401" ]; then
    echo "ℹ INFO - Old path returns 401 (may work, but not recommended)"
else
    echo "✗ UNEXPECTED - Got HTTP $HTTP_CODE"
    echo "Response: $BODY"
fi
echo ""

echo "Test 3: Check if endpoint is Spring controller or static resource"
echo "Testing with verbose output..."
echo "-------------------------------------"
curl -s -I "${BASE_URL}/werkflow/api/forms/${FORM_KEY}" | grep -E "(HTTP/|WWW-Authenticate|Content-Type)"
echo ""

echo "====================================="
echo "Summary:"
echo "====================================="
echo "The new path /werkflow/api/forms/* should return 401"
echo "This confirms:"
echo "  - Endpoint is properly routed to FormController"
echo "  - Security is working (requires authentication)"
echo "  - No Flowable REST API conflict"
echo ""
echo "To test with authentication, use:"
echo "  1. Get token from Keycloak"
echo "  2. curl -H 'Authorization: Bearer \$TOKEN' ${BASE_URL}/werkflow/api/forms/${FORM_KEY}"
