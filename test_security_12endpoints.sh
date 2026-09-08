#!/bin/bash

# Script de test pour le module SECURITY avec de vraies requêtes HTTP
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
echo "🧪 TEST MODULE SECURITY - 12 ENDPOINTS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Vérifier que le cookie existe
if [ ! -f "$COOKIE_FILE" ]; then
    echo -e "${RED}❌ Fichier cookie non trouvé: $COOKIE_FILE${NC}"
    exit 1
fi

# Test 1: GET /security/vigil-profiles - Liste des profils de vigilance
echo -e "\n${YELLOW}Test 1: GET /security/vigil-profiles${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/security/vigil-profiles" \
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

# Test 2: GET /security/vigil-profiles/{id} - Profil de vigilance par ID
echo -e "\n${YELLOW}Test 2: GET /security/vigil-profiles/VIG-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/security/vigil-profiles/VIG-001" \
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

# Test 3: GET /security/vigil-profiles/user/{userId} - Profils par utilisateur
echo -e "\n${YELLOW}Test 3: GET /security/vigil-profiles/user/USR-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/security/vigil-profiles/user/USR-001" \
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

# Test 4: GET /security/vigil-profiles/role/{role} - Profils par rôle
echo -e "\n${YELLOW}Test 4: GET /security/vigil-profiles/role/VIGIL${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/security/vigil-profiles/role/VIGIL" \
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

# Test 5: POST /security/vigil-profiles - Créer profil de vigilance
echo -e "\n${YELLOW}Test 5: POST /security/vigil-profiles${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
VIGIL_CREATED_AT=$(date +'%Y-%m-%dT%H:%M:%S')
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/security/vigil-profiles" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d "{\"id\": \"VIG-TEST-$(date +%s)\", \"name\": \"Test Vigil\", \"role\": \"VIGIL\", \"userId\": \"USR-TEST\", \"status\": \"ACTIVE\", \"createdAt\": \"$VIGIL_CREATED_AT\"}")
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

# Test 6: GET /security/checkpoint-status - Statut des checkpoints
echo -e "\n${YELLOW}Test 6: GET /security/checkpoint-status${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/security/checkpoint-status" \
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

# Test 7: POST /security/scan-logs - Scan de badge
echo -e "\n${YELLOW}Test 7: POST /security/scan-logs${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/security/scan-logs" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"badgeId": "STU-001"}')
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

# Test 8: GET /vigil/check-ins - Liste des check-ins vigil
echo -e "\n${YELLOW}Test 8: GET /vigil/check-ins${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/vigil/check-ins" \
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

# Test 9: GET /vigil/profile - Profil vigil
echo -e "\n${YELLOW}Test 9: GET /vigil/profile${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/vigil/profile" \
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

# Test 10: POST /vigil/scan - Scan vigil de badge
echo -e "\n${YELLOW}Test 10: POST /vigil/scan${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vigil/scan" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"badgeId": "STU_SECURE:2026-GC-001:1234567890:abc123"}')
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

# Test 11: GET /vigil/last-scan - Dernier scan
echo -e "\n${YELLOW}Test 11: GET /vigil/last-scan${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/vigil/last-scan" \
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

# Test 12: POST /vigil/scan - Scan avec matricule manuel
echo -e "\n${YELLOW}Test 12: POST /vigil/scan (matricule manuel)${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vigil/scan" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"badgeId": "2026-GC-001"}')
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

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS SECURITY"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Tests échoués: $FAILED_TESTS${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde des résultats
echo "SECURITY MODULE TEST RESULTS - $(date)" > security_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> security_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS SECURITY SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS SECURITY ONT ÉCHOUÉ${NC}"
    exit 1
fi