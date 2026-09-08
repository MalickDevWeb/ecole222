#!/bin/bash

# Script de test pour le module FINANCE avec de vraies requêtes HTTP
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
echo "🧪 TEST MODULE FINANCE - 19 ENDPOINTS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Vérifier que le cookie existe
if [ ! -f "$COOKIE_FILE" ]; then
    echo -e "${RED}❌ Fichier cookie non trouvé: $COOKIE_FILE${NC}"
    exit 1
fi

# Test 1: GET /finance/students - Liste des finances étudiantes
echo -e "\n${YELLOW}Test 1: GET /finance/students${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/students" \
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

# Test 2: GET /finance/students/{id} - Détails finance étudiant
echo -e "\n${YELLOW}Test 2: GET /finance/students/STU-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/students/STU-001" \
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

# Test 3: GET /finance/students/classe/{classe} - Finances par classe
echo -e "\n${YELLOW}Test 3: GET /finance/students/classe/L1-GL${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/students/classe/L1-GL" \
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

# Test 4: GET /finance/students/debt - Étudiants avec dette
echo -e "\n${YELLOW}Test 4: GET /finance/students/debt${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/students/debt" \
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

# Test 5: POST /finance/students - Créer finance étudiant
echo -e "\n${YELLOW}Test 5: POST /finance/students${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/finance/students" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "finance-stu-$(date +%s)", "studentId": "STU-TEST", "totalFees": 500000, "paidAmount": 250000, "status": "PARTIAL"}')
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

# Test 6: PATCH /finance/students/{id}/status - Mettre à jour statut étudiant
echo -e "\n${YELLOW}Test 6: PATCH /finance/students/STU-001/status${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/finance/students/STU-001/status" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"status": "PAID"}')
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

# Test 7: GET /finance/transactions - Liste des transactions
echo -e "\n${YELLOW}Test 7: GET /finance/transactions${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/transactions" \
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

# Test 8: GET /finance/transactions/student/{studentId} - Transactions étudiant
echo -e "\n${YELLOW}Test 8: GET /finance/transactions/student/STU-001${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/transactions/student/STU-001" \
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

# Test 9: GET /finance/transactions/recent - Transactions récentes
echo -e "\n${YELLOW}Test 9: GET /finance/transactions/recent${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/transactions/recent" \
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

# Test 10: POST /finance/transactions - Créer transaction
echo -e "\n${YELLOW}Test 10: POST /finance/transactions${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/finance/transactions" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d '{"id": "trans-$(date +%s)", "studentId": "STU-001", "amount": 50000, "type": "PAYMENT", "description": "Test payment"}')
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

# Test 11: GET /finance/dashboard - Dashboard financier
echo -e "\n${YELLOW}Test 11: GET /finance/dashboard${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/dashboard" \
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

# Test 12: GET /finance/total-debt - Dette totale
echo -e "\n${YELLOW}Test 12: GET /finance/total-debt${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/total-debt" \
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

# Test 13: GET /finance/total-collected - Total collecté
echo -e "\n${YELLOW}Test 13: GET /finance/total-collected${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/total-collected" \
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

# Test 14: GET /finance/expenses-by-category - Dépenses par catégorie
echo -e "\n${YELLOW}Test 14: GET /finance/expenses-by-category${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/expenses-by-category" \
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

# Test 15: GET /finance/payment-trends - Tendances de paiement
echo -e "\n${YELLOW}Test 15: GET /finance/payment-trends${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/payment-trends" \
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

# Test 16: GET /finance/expenses - Liste des dépenses
echo -e "\n${YELLOW}Test 16: GET /finance/expenses${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/expenses" \
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

# Test 17: POST /finance/expenses - Créer dépense
echo -e "\n${YELLOW}Test 17: POST /finance/expenses${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
EXPENSE_DATE=$(date +'%Y-%m-%dT%H:%M:%S')
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/finance/expenses" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_FILE" \
  -d "{\"id\": \"exp-$(date +%s)\", \"category\": \"RH\", \"amount\": 100000, \"label\": \"Test expense\", \"description\": \"Test expense\", \"date\": \"$EXPENSE_DATE\"}")
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

# Test 18: GET /finance/budget - Budget
echo -e "\n${YELLOW}Test 18: GET /finance/budget${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/budget" \
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

# Test 19: GET /finance/reports - Rapports financiers
echo -e "\n${YELLOW}Test 19: GET /finance/reports${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/finance/reports" \
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
echo "📊 RÉSUMÉ DES TESTS FINANCE"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Tests échoués: $FAILED_TESTS${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde des résultats
echo "FINANCE MODULE TEST RESULTS - $(date)" > finance_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> finance_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS FINANCE SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS FINANCE ONT ÉCHOUÉ${NC}"
    exit 1
fi