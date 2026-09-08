#!/bin/bash
ADMIN_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"admin@ecole221.sn","password":"password123"}' http://127.0.0.1:8080/api/auth/login | jq -r '.token')
curl -s -o /dev/null -w "%{http_code}\n" -X PATCH -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"statutFrais":"PAYE"}' http://127.0.0.1:8080/api/admin/students/usr-001/status
