#!/bin/bash

# Script de test complet des endpoints backend
BASE_URL="http://localhost:8080/api"

echo "=== TEST COMPLET DES ENDPOINTS BACKEND ==="
echo "Testé le: $(date)"
echo ""

# Couleurs pour le résultat
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction de test
test_endpoint() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -n "Testing: $description ... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$url" -H "Content-Type: application/json")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$url" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL$url" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "PATCH" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL$url" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$url" -H "Content-Type: application/json")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✓ OK ($http_code)${NC}"
        return 0
    elif [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        echo -e "${YELLOW}⚠ AUTH ($http_code)${NC}"
        return 1
    else
        echo -e "${RED}✗ FAIL ($http_code)${NC}"
        echo "Error: $body"
        return 1
    fi
}

# Compteur
total=0
passed=0
failed=0

# AUTH
echo "=== AUTHENTICATION ==="
test_endpoint "POST" "/auth/login" '{"email":"professeur221@gmail.com","password":"password123"}' "Login" && ((passed++)) || ((failed++))
((total++))
test_endpoint "POST" "/auth/logout" '{}' "Logout" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/auth/me" '' "Get User Profile" && ((passed++)) || ((failed++))
((total++))
echo ""

# STUDENT
echo "=== STUDENT ==="
test_endpoint "GET" "/student/profile" '' "Student Profile" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/student/dashboard" '' "Student Dashboard" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/student/digital-badge" '' "Digital Badge" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/student/attendances" '' "Attendances" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/student/live-sessions" '' "Live Sessions" && ((passed++)) || ((failed++))
((total++))
test_endpoint "POST" "/student/qr-validate" '{"matricule":"2026-GC-001","minuteEpoch":1234567890,"token":"test"}' "QR Validate" && ((passed++)) || ((failed++))
((total++))
echo ""

# PROFESSOR
echo "=== PROFESSOR ==="
test_endpoint "GET" "/professor/courses" '' "Professor Courses" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/professor/courses/course-1/students" '' "Course Students" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/professor/courses/course-1/grades" '' "Course Grades" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/professor/courses/course-1/homeworks" '' "Course Homeworks" && ((passed++)) || ((failed++))
((total++))
echo ""

# ADMIN
echo "=== ADMIN ==="
test_endpoint "GET" "/admin/users" '' "Admin Users" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admin/stats" '' "Admin Stats" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admin/realtime-logs" '' "Realtime Logs" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admin/gates/assign" '' "Gates Assign" && ((passed++)) || ((failed++))
((total++))
echo ""

# COLLABORATION
echo "=== COLLABORATION ==="
test_endpoint "GET" "/collaboration/workgroups" '' "Workgroups" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/collaboration/meets" '' "Meets" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/collaboration/admin-meets" '' "Admin Meets" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/collaboration/workgroups/group-001/documents" '' "Group Documents" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/collaboration/workgroups/group-001/tasks" '' "Group Tasks" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/collaboration/workgroups/group-001/messages" '' "Group Messages" && ((passed++)) || ((failed++))
((total++))
echo ""

# DASHBOARD
echo "=== DASHBOARD ==="
test_endpoint "GET" "/dashboard/kpis" '' "Dashboard KPIs" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/dashboard/alerts" '' "Dashboard Alerts" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/dashboard/activities" '' "Dashboard Activities" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/dashboard/complete" '' "Dashboard Complete" && ((passed++)) || ((failed++))
((total++))
echo ""

# FINANCE
echo "=== FINANCE ==="
test_endpoint "GET" "/finance/students" '' "Finance Students" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/finance/expenses" '' "Finance Expenses" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/finance/transactions" '' "Finance Transactions" && ((passed++)) || ((failed++))
((total++))
echo ""

# SECURITY
echo "=== SECURITY ==="
test_endpoint "GET" "/security/checkpoint-status" '' "Checkpoint Status" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/vigil/check-ins" '' "Vigil Check-ins" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/vigil/profile" '' "Vigil Profile" && ((passed++)) || ((failed++))
((total++))
echo ""

# ADMISSIONS
echo "=== ADMISSIONS ==="
test_endpoint "GET" "/admissions/candidates" '' "Admissions Candidates" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admissions/settings" '' "Admissions Settings" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admissions/kanban" '' "Admissions Kanban" && ((passed++)) || ((failed++))
((total++))
test_endpoint "GET" "/admissions/statistics" '' "Admissions Statistics" && ((passed++)) || ((failed++))
((total++))
echo ""

# RÉSUMÉ
echo "=== RÉSUMÉ ==="
echo "Total tests: $total"
echo -e "${GREEN}Réussis: $passed${NC}"
echo -e "${RED}Échoués: $failed${NC}"
echo -e "${YELLOW}Taux de réussite: $((passed * 100 / total))%${NC}"