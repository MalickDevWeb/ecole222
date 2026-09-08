#!/bin/bash

# Script de test pour le module Admissions - 15 endpoints
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
echo "🧪 TEST MODULE ADMISSIONS - 15 ENDPOINTS"
echo "=========================================="

# Section 1: Candidats CRUD (8 endpoints)
echo -e "\n📝 Section 1: Candidats CRUD"

# Test 1: GET /admissions/candidates
echo -e "\n📝 Test 1: GET /admissions/candidates"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/candidates")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/candidates - Liste candidats"
else
    print_result 1 "GET /admissions/candidates - HTTP $http_code"
fi

# Test 2: POST /admissions/candidates
echo -e "\n📝 Test 2: POST /admissions/candidates"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admissions/candidates" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Doe",
    "prenom": "John",
    "email": "john.doe@example.com",
    "telephone": "221771234567",
    "dateNaissance": "2000-01-15",
    "filiere": "Génie Civil",
    "step": "SUBMISSION",
    "status": "PENDING"
  }')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    CANDIDATE_ID=$(echo "$body" | jq -r '.id // empty')
    print_result 0 "POST /admissions/candidates - Création candidat"
else
    print_result 1 "POST /admissions/candidates - HTTP $http_code"
    CANDIDATE_ID="test-001"
fi

# Test 3: GET /admissions/candidates/{id}
echo -e "\n📝 Test 3: GET /admissions/candidates/{id}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/candidates/$CANDIDATE_ID")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/candidates/{id} - Détails candidat"
else
    print_result 1 "GET /admissions/candidates/{id} - HTTP $http_code"
fi

# Test 4: GET /admissions/candidates/step/{step}
echo -e "\n📝 Test 4: GET /admissions/candidates/step/{step}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/candidates/step/SUBMISSION")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/candidates/step/{step} - Candidats par étape"
else
    print_result 1 "GET /admissions/candidates/step/{step} - HTTP $http_code"
fi

# Test 5: GET /admissions/candidates/status/{status}
echo -e "\n📝 Test 5: GET /admissions/candidates/status/{status}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/candidates/status/PENDING")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/candidates/status/{status} - Candidats par statut"
else
    print_result 1 "GET /admissions/candidates/status/{status} - HTTP $http_code"
fi

# Test 6: PUT /admissions/candidates/{id}
echo -e "\n📝 Test 6: PUT /admissions/candidates/{id}"
response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/admissions/candidates/$CANDIDATE_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Doe",
    "prenom": "John Updated",
    "email": "john.doe.updated@example.com",
    "telephone": "221771234567",
    "dateNaissance": "2000-01-15",
    "filiere": "Génie Civil"
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PUT /admissions/candidates/{id} - Mise à jour candidat"
else
    print_result 1 "PUT /admissions/candidates/{id} - HTTP $http_code"
fi

# Test 7: PATCH /admissions/candidates/{id}/step
echo -e "\n📝 Test 7: PATCH /admissions/candidates/{id}/step"
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/admissions/candidates/$CANDIDATE_ID/step" \
  -H "Content-Type: application/json" \
  -d '{"step": "REVIEW"}')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PATCH /admissions/candidates/{id}/step - Mise à jour étape"
else
    print_result 1 "PATCH /admissions/candidates/{id}/step - HTTP $http_code"
fi

# Test 8: PATCH /admissions/candidates/{id}/status
echo -e "\n📝 Test 8: PATCH /admissions/candidates/{id}/status"
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/admissions/candidates/$CANDIDATE_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED", "rejectionReason": ""}')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PATCH /admissions/candidates/{id}/status - Mise à jour statut"
else
    print_result 1 "PATCH /admissions/candidates/{id}/status - HTTP $http_code"
fi

# Section 2: Paramètres (3 endpoints)
echo -e "\n📝 Section 2: Paramètres"

# Test 9: GET /admissions/settings
echo -e "\n📝 Test 9: GET /admissions/settings"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/settings")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/settings - Paramètres admissions"
else
    print_result 1 "GET /admissions/settings - HTTP $http_code"
fi

# Test 10: PUT /admissions/settings
echo -e "\n📝 Test 10: PUT /admissions/settings"
response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/admissions/settings" \
  -H "Content-Type: application/json" \
  -d '{
    "session": "2026-2027",
    "deadline": "2026-12-31",
    "maxCandidates": 500,
    "isOpen": true
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PUT /admissions/settings - Mise à jour paramètres"
else
    print_result 1 "PUT /admissions/settings - HTTP $http_code"
fi

# Test 11: PATCH /admissions/settings/toggle
echo -e "\n📝 Test 11: PATCH /admissions/settings/toggle"
response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/admissions/settings/toggle" \
  -H "Content-Type: application/json" \
  -d '{"isOpen": true}')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PATCH /admissions/settings/toggle - Toggle candidatures"
else
    print_result 1 "PATCH /admissions/settings/toggle - HTTP $http_code"
fi

# Section 3: Workflow et Statistiques (4 endpoints)
echo -e "\n📝 Section 3: Workflow et Statistiques"

# Test 12: GET /admissions/kanban
echo -e "\n📝 Test 12: GET /admissions/kanban"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/kanban")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/kanban - Données Kanban"
else
    print_result 1 "GET /admissions/kanban - HTTP $http_code"
fi

# Test 13: GET /admissions/statistics
echo -e "\n📝 Test 13: GET /admissions/statistics"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/statistics")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/statistics - Statistiques candidats"
else
    print_result 1 "GET /admissions/statistics - HTTP $http_code"
fi

# Test 14: GET /admissions/top-candidates
echo -e "\n📝 Test 14: GET /admissions/top-candidates"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admissions/top-candidates?limit=10")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /admissions/top-candidates - Meilleurs candidats"
else
    print_result 1 "GET /admissions/top-candidates - HTTP $http_code"
fi

# Test 15: POST /admissions/automation/process
echo -e "\n📝 Test 15: POST /admissions/automation/process"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admissions/automation/process")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "POST /admissions/automation/process - Traitement admissions"
else
    print_result 1 "POST /admissions/automation/process - HTTP $http_code"
fi

# Test DELETE /admissions/candidates/{id} (bonus test)
echo -e "\n📝 Test Bonus: DELETE /admissions/candidates/{id}"
response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/admissions/candidates/$CANDIDATE_ID")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ] || [ "$http_code" = "204" ]; then
    print_result 0 "DELETE /admissions/candidates/{id} - Suppression candidat"
else
    print_result 1 "DELETE /admissions/candidates/{id} - HTTP $http_code"
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS ADMISSIONS"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "ADMISSIONS MODULE TEST RESULTS - $(date)" > admissions_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> admissions_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS ADMISSIONS SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS ADMISSIONS ONT ÉCHOUÉ${NC}"
    exit 1
fi