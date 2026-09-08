#!/bin/bash
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"prof@ecole221.sn","password":"password123"}' http://127.0.0.1:8080/api/auth/login | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Testing /professor/courses twice to check Redis caching"
curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses?page=0&size=10" | jq
echo "Second call:"
curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses?page=0&size=10" | jq
