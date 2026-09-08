#!/bin/bash
function test_endpoint {
    local ENDPOINT=$1
    local TOKEN=$2
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api$ENDPOINT")
    echo "$ENDPOINT : $HTTP_CODE"
}

# --- STUDENT ---
echo "--- TESTING STUDENT ENDPOINTS ---"
STUDENT_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"etudiant221@gmail.com","password":"ecole221"}' http://127.0.0.1:8080/api/auth/login | jq -r '.token')
test_endpoint "/student/profile" $STUDENT_TOKEN
test_endpoint "/student/dashboard" $STUDENT_TOKEN
test_endpoint "/student/digital-badge" $STUDENT_TOKEN
test_endpoint "/student/attendances?page=0&size=10" $STUDENT_TOKEN
test_endpoint "/student/live-sessions?page=0&size=10" $STUDENT_TOKEN
test_endpoint "/student/live-sessions/sess-1/chat-history" $STUDENT_TOKEN
echo ""

# --- ADMIN ---
echo "--- TESTING ADMIN ENDPOINTS ---"
ADMIN_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"admin221@gmail.com","password":"ecole221"}' http://127.0.0.1:8080/api/auth/login | jq -r '.token')
test_endpoint "/planning/rooms" $ADMIN_TOKEN
test_endpoint "/planning/filieres" $ADMIN_TOKEN
test_endpoint "/planning/classes" $ADMIN_TOKEN
test_endpoint "/planning/slots" $ADMIN_TOKEN
test_endpoint "/admin/realtime-logs" $ADMIN_TOKEN
echo ""

# --- VIGIL ---
echo "--- TESTING VIGIL ENDPOINTS ---"
VIGIL_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"vigil@ecole221.sn","password":"vigil123"}' http://127.0.0.1:8080/api/auth/login | jq -r '.token')
test_endpoint "/security/checkpoint-status" $VIGIL_TOKEN
test_endpoint "/vigil/check-ins" $VIGIL_TOKEN

