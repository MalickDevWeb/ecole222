#!/bin/bash

# Script de test pour le module PLANNING avec de vraies requêtes HTTP
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
echo "🧪 TEST MODULE PLANNING - 12 ENDPOINTS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Vérifier que le cookie existe
if [ ! -f "$COOKIE_FILE" ]; then
    echo -e "${RED}❌ Fichier cookie non trouvé: $COOKIE_FILE${NC}"
    exit 1
fi

# Test 1: GET /planning/rooms - Liste des salles
echo -e "\n${YELLOW}Test 1: GET /planning/rooms${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/rooms" \
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

# Test 2: GET /planning/rooms/{roomId} - Détails d'une salle
echo -e "\n${YELLOW}Test 2: GET /planning/rooms/ROOM-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/rooms/ROOM-001" \
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

# Test 3: POST /planning/rooms - Créer/Mettre à jour une salle
echo -e "\n${YELLOW}Test 3: POST /planning/rooms${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/planning/rooms" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "ROOM-TEST-$(date +%s)", "name": "Salle Test", "capacity": 30, "building": "A", "location": "Batiment A"}')
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

# Test 4: GET /planning/slots - Liste des créneaux horaires
echo -e "\n${YELLOW}Test 4: GET /planning/slots${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/slots" \
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

# Test 5: GET /planning/slots/{slotId} - Détails d'un créneau horaire
echo -e "\n${YELLOW}Test 5: GET /planning/slots/SLOT-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/slots/SLOT-001" \
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

# Test 6: POST /planning/slots - Créer/Mettre à jour un créneau horaire
echo -e "\n${YELLOW}Test 6: POST /planning/slots${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/planning/slots" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "SLOT-TEST-$(date +%s)", "dayOfWeek": "LUNDI", "startTime": "08:00", "endTime": "10:00", "type": "CM"}')
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

# Test 7: GET /planning/filieres - Liste des filières
echo -e "\n${YELLOW}Test 7: GET /planning/filieres${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/filieres" \
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

# Test 8: GET /planning/filieres/{filiereId} - Détails d'une filière
echo -e "\n${YELLOW}Test 8: GET /planning/filieres/FIL-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/filieres/FIL-001" \
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

# Test 9: POST /planning/filieres - Créer/Mettre à jour une filière
echo -e "\n${YELLOW}Test 9: POST /planning/filieres${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/planning/filieres" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "FIL-TEST-$(date +%s)", "name": "Filière Test", "code": "TEST", "level": "L1"}')
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

# Test 10: GET /planning/classes - Liste des classes
echo -e "\n${YELLOW}Test 10: GET /planning/classes${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/classes?page=0&size=10" \
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

# Test 11: GET /planning/classes/{classeId} - Détails d'une classe
echo -e "\n${YELLOW}Test 11: GET /planning/classes/CLS-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/planning/classes/CLS-001" \
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

# Test 12: POST /planning/classes - Créer/Mettre à jour une classe
echo -e "\n${YELLOW}Test 12: POST /planning/classes${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/planning/classes" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "CLS-TEST-$(date +%s)", "name": "Classe Test", "code": "TEST-001", "filiereId": "FIL-TEST", "level": "L1", "capacity": 30, "currentSize": 0}')
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

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS PLANNING"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Tests échoués: $FAILED_TESTS${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde des résultats
echo "PLANNING MODULE TEST RESULTS - $(date)" > planning_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> planning_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS PLANNING SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS PLANNING ONT ÉCHOUÉ${NC}"
    exit 1
fi