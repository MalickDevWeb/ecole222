#!/bin/bash
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"prof@ecole221.sn","password":"password123"}' http://127.0.0.1:8080/api/auth/login | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Testing /student/profile"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/profile"
echo ""

echo "Testing /student/dashboard"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/dashboard"
echo ""

echo "Testing /student/live-sessions"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/live-sessions?page=0&size=10"
echo ""

echo "Testing /student/digital-badge"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/digital-badge"
echo ""

echo "Testing /student/live-sessions/live-session-101/chat-history"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/live-sessions/live-session-101/chat-history"
echo ""

echo "Testing /prof/today-schedule"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/prof/today-schedule"
echo ""

