#!/bin/bash

# Script de test pour le module Email - 5 endpoints
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
echo "🧪 TEST MODULE EMAIL - 5 ENDPOINTS"
echo "=========================================="

# Test 1: POST /email/send
echo -e "\n📝 Test 1: POST /email/send"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/email/send" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "subject": "Test Email",
    "content": "This is a test email content"
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /email/send - Envoi email simple"
else
    print_result 1 "POST /email/send - HTTP $http_code"
fi

# Test 2: POST /email/send-template
echo -e "\n📝 Test 2: POST /email/send-template"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/email/send-template" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "templateId": "template-001",
    "variables": {
      "name": "John Doe",
      "confirmationCode": "123456"
    }
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /email/send-template - Envoi email template"
else
    print_result 1 "POST /email/send-template - HTTP $http_code"
fi

# Test 3: POST /email/send-confirmation
echo -e "\n📝 Test 3: POST /email/send-confirmation"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/email/send-confirmation" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "type": "registration",
    "data": {
      "name": "John Doe",
      "confirmationCode": "123456"
    }
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /email/send-confirmation - Envoi email confirmation"
else
    print_result 1 "POST /email/send-confirmation - HTTP $http_code"
fi

# Test 4: POST /email/send-reminder
echo -e "\n📝 Test 4: POST /email/send-reminder"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X POST "$BASE_URL/email/send-reminder" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "reminderType": "homework",
    "details": {
      "subject": "Mathematics",
      "dueDate": "2026-08-15"
    }
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /email/send-reminder - Envoi email rappel"
else
    print_result 1 "POST /email/send-reminder - HTTP $http_code"
fi

# Test 5: GET /email/status
echo -e "\n📝 Test 5: GET /email/status"
response=$(curl -s -w -b "admin_cookies_new.txt" "\n%{http_code}" -X GET "$BASE_URL/email/status")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /email/status - Statut service email"
else
    print_result 1 "GET /email/status - HTTP $http_code"
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS EMAIL"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "EMAIL MODULE TEST RESULTS - $(date)" > email_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> email_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS EMAIL SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS EMAIL ONT ÉCHOUÉ${NC}"
    exit 1
fi