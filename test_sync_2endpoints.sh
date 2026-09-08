#!/bin/bash

# Script de test pour le module Sync - 2 endpoints
# Base URL
BASE_URL="http://localhost:8080/api"

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
echo "🧪 TEST MODULE SYNC - 2 ENDPOINTS"
echo "=========================================="

# Test 1: POST /sync
echo -e "\n📝 Test 1: POST /sync"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/sync" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "actionType": "CREATE",
      "entityType": "User",
      "entityId": "user-001",
      "data": {"name": "Test User"}
    },
    {
      "actionType": "UPDATE",
      "entityType": "User",
      "entityId": "user-002",
      "data": {"name": "Updated User"}
    }
  ]')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /sync - Synchronisation batch"
else
    print_result 1 "POST /sync - HTTP $http_code"
fi

# Test 2: GET /sync/check
echo -e "\n📝 Test 2: GET /sync/check"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X GET "$BASE_URL/sync/check")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /sync/check - Vérification version"
else
    print_result 1 "GET /sync/check - HTTP $http_code"
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS SYNC"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "SYNC MODULE TEST RESULTS - $(date)" > sync_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> sync_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS SYNC SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS SYNC ONT ÉCHOUÉ${NC}"
    exit 1
fi