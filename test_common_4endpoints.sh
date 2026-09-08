#!/bin/bash

# Script de test pour le module Common - 4 endpoints
# Base URL
BASE_URL="http://localhost:8080/api"
COOKIE_FILE="admin_cookies_new.txt"

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
echo "🧪 TEST MODULE COMMON - 4 ENDPOINTS"
echo "=========================================="

# Test 1: GET /common/health
echo -e "\n📝 Test 1: GET /common/health"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/common/health" -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /common/health - Health check"
else
    print_result 1 "GET /common/health - HTTP $http_code"
fi

# Test 2: GET /common/notifications
echo -e "\n📝 Test 2: GET /common/notifications"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/common/notifications" -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /common/notifications - Liste notifications"
else
    print_result 1 "GET /common/notifications - HTTP $http_code"
fi

# Test 3: GET /common/version
echo -e "\n📝 Test 3: GET /common/version"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/common/version" -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /common/version - Version API"
else
    print_result 1 "GET /common/version - HTTP $http_code"
fi

# Test 4: GET /common/info
echo -e "\n📝 Test 4: GET /common/info"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/common/info" -b "$COOKIE_FILE")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /common/info - Informations système"
else
    print_result 1 "GET /common/info - HTTP $http_code"
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS COMMON"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "COMMON MODULE TEST RESULTS - $(date)" > common_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> common_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS COMMON SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS COMMON ONT ÉCHOUÉ${NC}"
    exit 1
fi