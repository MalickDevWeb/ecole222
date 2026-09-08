#!/bin/bash

# Script de test pour le module Media - 1 endpoint
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
echo "🧪 TEST MODULE MEDIA - 1 ENDPOINT"
echo "=========================================="

# Créer un fichier test temporaire
echo "This is a test file content" > /tmp/test_upload.txt

# Test 1: POST /media/upload
echo -e "\n📝 Test 1: POST /media/upload"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/media/upload" \
  -F "file=@/tmp/test_upload.txt" \
  -F "userId=test-user-001")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    print_result 0 "POST /media/upload - Upload fichier"
    echo "Response: $body" | head -c 200
else
    print_result 1 "POST /media/upload - HTTP $http_code"
fi

# Nettoyage
rm -f /tmp/test_upload.txt

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS MEDIA"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "MEDIA MODULE TEST RESULTS - $(date)" > media_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> media_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS MEDIA SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS MEDIA ONT ÉCHOUÉ${NC}"
    exit 1
fi