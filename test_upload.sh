#!/bin/bash
PROF_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"prof@ecole221.sn","password":"password123"}' http://127.0.0.1:8080/api/auth/login | jq -r '.token')
echo "Testing Upload..."
echo "dummy file" > dummy.txt
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $PROF_TOKEN" -F "file=@dummy.txt" http://127.0.0.1:8080/api/media/upload
