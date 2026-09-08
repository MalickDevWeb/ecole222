#!/bin/bash
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"prof@ecole221.sn","password":"password123"}' http://127.0.0.1:8080/api/auth/login | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Testing /professor/courses"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses?page=0&size=10"
echo ""

echo "Testing /professor/courses/course-1/homeworks"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses/course-1/homeworks?page=0&size=10"
echo ""

echo "Testing /professor/courses/course-1/lessons"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses/course-1/lessons?page=0&size=10"
echo ""

echo "Testing /professor/courses/course-1/modules"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses/course-1/modules?page=0&size=10"
echo ""

echo "Testing /professor/courses/course-1/quizzes"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/professor/courses/course-1/quizzes?page=0&size=10"
echo ""

echo "Testing /student/attendances"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/student/attendances?page=0&size=10"
echo ""

