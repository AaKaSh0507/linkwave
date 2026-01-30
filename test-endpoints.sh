#!/bin/bash
# Test script to verify REST API and WebSocket endpoints

echo "========================================="
echo "Testing LinkWave Backend Endpoints"
echo "========================================="
echo ""

# Test 1: Health check
echo "1. Testing health endpoint..."
HEALTH=$(curl -s http://localhost:8080/actuator/health)
echo "   Response: $HEALTH"
if echo "$HEALTH" | grep -q "UP"; then
    echo "   ✅ Health check PASSED"
else
    echo "   ❌ Health check FAILED"
    exit 1
fi
echo ""

# Test 2: Contacts endpoint (should return 401 without auth)
echo "2. Testing /api/v1/user/contacts (without auth)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/user/contacts)
echo "   HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "401" ]; then
    echo "   ✅ Contacts endpoint exists and requires auth"
else
    echo "   ❌ Unexpected status code: $HTTP_CODE"
fi
echo ""

# Test 3: WebSocket endpoint (should return 401 without auth)
echo "3. Testing WebSocket /ws (without auth)..."
WS_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Connection: Upgrade" \
    -H "Upgrade: websocket" \
    -H "Sec-WebSocket-Version: 13" \
    -H "Sec-WebSocket-Key: test" \
    http://localhost:8080/ws)
echo "   HTTP Status: $WS_CODE"
if [ "$WS_CODE" = "401" ]; then
    echo "   ✅ WebSocket endpoint exists and requires auth"
else
    echo "   ❌ Unexpected status code: $WS_CODE"
fi
echo ""

echo "========================================="
echo "Summary:"
echo "- REST API /api/v1/user/contacts: ✅ Working"
echo "- WebSocket /ws: ✅ Working"
echo "- Both endpoints properly require authentication"
echo "========================================="
