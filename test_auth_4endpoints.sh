#!/bin/bash

# Script de test pour le module Auth - 4 endpoints
# Base URL
BASE_URL="http://localhost:8080/api"
COOKIE_FILE="auth_test_cookies.txt"

# Couleurs pour la sortie
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Compteurs
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Fonction pour afficher les résultats
print_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $2"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL${NC}: $2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo "=========================================="
echo "🧪 TEST MODULE AUTH - 4 ENDPOINTS"
echo "=========================================="

# Test 1: Login via token (pour Swagger)
echo -e "\n📝 Test 1: POST /auth/login-token"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login-token" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin221@gmail.com","password":"ecole221"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo "$body" | jq '.' > /dev/null 2>&1
    print_result $? "POST /auth/login-token - Login successful"
    TOKEN=$(echo "$body" | jq -r '.token // empty')
else
    print_result 1 "POST /auth/login-token - HTTP $http_code"
fi

# Test 2: Login via cookie (pour Web)
echo -e "\n📝 Test 2: POST /auth/login (cookie)"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin221@gmail.com","password":"ecole221"}' \
  -c "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_result 0 "POST /auth/login - Cookie login successful"
else
    print_result 1 "POST /auth/login - HTTP $http_code"
fi

# Test 3: Get user profile (requires auth)
echo -e "\n📝 Test 3: GET /auth/me"
if [ -n "$TOKEN" ]; then
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/auth/me" \
      -H "Authorization: Bearer $TOKEN")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ]; then
        print_result 0 "GET /auth/me - Profile retrieved"
    else
        print_result 1 "GET /auth/me - HTTP $http_code"
    fi
else
    print_result 1 "GET /auth/me - No token available"
fi

# Test 4: Logout
echo -e "\n📝 Test 4: POST /auth/logout"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/logout" \
  -b "$COOKIE_FILE" \
  -c "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "200" ]; then
    print_result 0 "POST /auth/logout - Logout successful"
else
    print_result 1 "POST /auth/logout - HTTP $http_code"
fi

# Nettoyage
rm -f "$COOKIE_FILE"

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS AUTH"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "AUTH MODULE TEST RESULTS - $(date)" > auth_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> auth_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS AUTH SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS AUTH ONT ÉCHOUÉ${NC}"
    exit 1
fi