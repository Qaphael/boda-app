#!/bin/bash
echo "=== Test sync with real Firebase token ==="
TOKEN="fake-test-token"
curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"phone":"700111222","name":"Test User"}' http://localhost:3002/api/users/sync
echo ""

echo "=== Check if Firebase is initialized ==="
docker exec boda-gulu-api node -e "const admin = require('firebase-admin'); console.log('apps:', admin.apps.length)"
