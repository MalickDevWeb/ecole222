#!/bin/bash

# Script de test pour le module ADMIN avec de vraies requêtes HTTP
# Base URL: http://localhost:8080/api

BASE_URL="http://localhost:8080/api"
ADMIN_COOKIE_FILE="admin_cookies_new.txt"

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
echo "🧪 TEST MODULE ADMIN - 9 ENDPOINTS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Vérifier que le cookie admin existe
if [ ! -f "$ADMIN_COOKIE_FILE" ]; then
    echo -e "${RED}❌ Fichier cookie admin non trouvé: $ADMIN_COOKIE_FILE${NC}"
    exit 1
fi

# Test 1: POST /admin/gates/assign - Assigner un portail
echo -e "\n${YELLOW}Test 1: POST /admin/gates/assign${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/gates/assign" \
  -H "Content-Type: application/json" \
  -b "$ADMIN_COOKIE_FILE" \
  -d '{"gate": "gate-A", "guard": "VIG-001"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 2: PATCH /admin/students/{id}/status - Modifier statut étudiant
echo -e "\n${YELLOW}Test 2: PATCH /admin/students/usr-001/status${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/admin/students/usr-001/status" \
  -H "Content-Type: application/json" \
  -b "$ADMIN_COOKIE_FILE" \
  -d '{"statutFrais": "En Règle"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 3: GET /admin/realtime-logs - Logs temps réel
echo -e "\n${YELLOW}Test 3: GET /admin/realtime-logs${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admin/realtime-logs" \
  -b "$ADMIN_COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 4: POST /admin/candidatures/settings - Mettre à jour paramètres candidature
echo -e "\n${YELLOW}Test 4: POST /admin/candidatures/settings${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/candidatures/settings" \
  -H "Content-Type: application/json" \
  -b "$ADMIN_COOKIE_FILE" \
  -d '{"enabled": true}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 5: GET /admin/stats - Statistiques globales
echo -e "\n${YELLOW}Test 5: GET /admin/stats${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admin/stats" \
  -b "$ADMIN_COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 6: GET /admin/users - Liste des utilisateurs
echo -e "\n${YELLOW}Test 6: GET /admin/users${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admin/users" \
  -b "$ADMIN_COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 7: POST /admin/students - Ajouter étudiant
echo -e "\n${YELLOW}Test 7: POST /admin/students${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/students" \
  -H "Content-Type: application/json" \
  -b "$ADMIN_COOKIE_FILE" \
  -d '{"name": "Test Student", "matricule": "2026-TEST-001", "promotion_id": "L1-GL"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 8: POST /admin/professors - Ajouter professeur
echo -e "\n${YELLOW}Test 8: POST /admin/professors${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/professors" \
  -H "Content-Type: application/json" \
  -b "$ADMIN_COOKIE_FILE" \
  -d '{"name": "Test Professor", "email": "test.prof@ecole221.sn"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test 9: DELETE /admin/students/{id} - Supprimer étudiant (test avec un ID fictif)
echo -e "\n${YELLOW}Test 9: DELETE /admin/students/test-delete-id${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/admin/students/test-delete-id" \
  -b "$ADMIN_COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - HTTP $http_code"
    echo "Response: $body"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} - HTTP $http_code"
    echo "Response: $body"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS ADMIN"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Tests échoués: $FAILED_TESTS${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde des résultats
echo "ADMIN MODULE TEST RESULTS - $(date)" > admin_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> admin_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS ADMIN SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS ADMIN ONT ÉCHOUÉ${NC}"
    exit 1
fi