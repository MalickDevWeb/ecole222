#!/bin/bash

# Script de test pour le module DASHBOARD avec de vraies requêtes HTTP
# Base URL: http://localhost:8080

BASE_URL="http://localhost:8080/api"
COOKIE_FILE="admin_cookies_new.txt"

# Couleurs pour le output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables pour les résultats
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "=========================================="
echo "🧪 TEST MODULE DASHBOARD - 18 ENDPOINTS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Vérifier que le cookie existe
if [ ! -f "$COOKIE_FILE" ]; then
    echo -e "${RED}❌ Fichier cookie non trouvé: $COOKIE_FILE${NC}"
    exit 1
fi

# Test 1: GET /dashboard/kpis - Liste des KPIs
echo -e "\n${YELLOW}Test 1: GET /dashboard/kpis${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/kpis" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 2: GET /dashboard/kpis/{kpiKey} - KPI par clé
echo -e "\n${YELLOW}Test 2: GET /dashboard/kpis/students_count${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/kpis/students_count" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "404" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 3: GET /dashboard/kpis/category/{category} - KPIs par catégorie
echo -e "\n${YELLOW}Test 3: GET /dashboard/kpis/category/academic${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/kpis/category/academic" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 4: POST /dashboard/kpis - Créer KPI
echo -e "\n${YELLOW}Test 4: POST /dashboard/kpis${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/dashboard/kpis" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"key": "test_kpi", "name": "Test KPI", "value": "100", "category": "test", "unit": "count"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 5: PATCH /dashboard/kpis/{kpiKey}/value - Mettre à jour valeur KPI
echo -e "\n${YELLOW}Test 5: PATCH /dashboard/kpis/test_kpi/value${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/dashboard/kpis/test_kpi/value" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"value": "150"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 6: GET /dashboard/alerts - Liste des alertes
echo -e "\n${YELLOW}Test 6: GET /dashboard/alerts${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/alerts" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 7: GET /dashboard/alerts/active - Alertes actives
echo -e "\n${YELLOW}Test 7: GET /dashboard/alerts/active${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/alerts/active" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 8: GET /dashboard/alerts/urgent - Alertes urgentes
echo -e "\n${YELLOW}Test 8: GET /dashboard/alerts/urgent${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/alerts/urgent" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 9: POST /dashboard/alerts - Créer alerte
echo -e "\n${YELLOW}Test 9: POST /dashboard/alerts${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/dashboard/alerts" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "alert-test-$(date +%s)", "title": "Test Alert", "message": "Test alert message", "severity": "medium", "active": true}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 10: PATCH /dashboard/alerts/{id}/deactivate - Désactiver alerte
echo -e "\n${YELLOW}Test 10: PATCH /dashboard/alerts/1/deactivate${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/dashboard/alerts/1/deactivate" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 11: GET /dashboard/activities - Activités récentes
echo -e "\n${YELLOW}Test 11: GET /dashboard/activities${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/activities?limit=10" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 12: GET /dashboard/activities/type/{activityType} - Activités par type
echo -e "\n${YELLOW}Test 12: GET /dashboard/activities/type/LOGIN${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/activities/type/LOGIN" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 13: POST /dashboard/activities - Logger activité
echo -e "\n${YELLOW}Test 13: POST /dashboard/activities${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/dashboard/activities" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "activity-test-$(date +%s)", "activityType": "TEST", "description": "Test activity", "userId": "test-user"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 14: GET /dashboard/regional-stats/{academicYear} - Statistiques régionales
echo -e "\n${YELLOW}Test 14: GET /dashboard/regional-stats/2025-2026${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/regional-stats/2025-2026" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 15: GET /dashboard/system-health - Santé du système
echo -e "\n${YELLOW}Test 15: GET /dashboard/system-health${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/system-health" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "404" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 16: GET /dashboard/performance-metrics - Métriques de performance
echo -e "\n${YELLOW}Test 16: GET /dashboard/performance-metrics${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/performance-metrics" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "404" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 17: GET /dashboard/user-analytics - Analytics utilisateurs
echo -e "\n${YELLOW}Test 17: GET /dashboard/user-analytics${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/user-analytics" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "404" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 18: GET /dashboard/summary - Résumé dashboard
echo -e "\n${YELLOW}Test 18: GET /dashboard/summary${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/dashboard/summary" \
  -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "404" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS DASHBOARD"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Tests échoués: $FAILED_TESTS${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde des résultats
echo "DASHBOARD MODULE TEST RESULTS - $(date)" > dashboard_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> dashboard_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS DASHBOARD SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS DASHBOARD ONT ÉCHOUÉ${NC}"
    exit 1
fi