#!/bin/bash

# Script de test pour le module Audit - 16 endpoints
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
echo "🧪 TEST MODULE AUDIT - 16 ENDPOINTS"
echo "=========================================="

# Section 1: Logs d'audit (6 endpoints)
echo -e "\n📝 Section 1: Logs d'audit"

# Test 1: GET /audit/logs
echo -e "\n📝 Test 1: GET /audit/logs"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/logs")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/logs - Liste logs d'audit"
else
    print_result 1 "GET /audit/logs - HTTP $http_code"
fi

# Test 2: GET /audit/logs/recent
echo -e "\n📝 Test 2: GET /audit/logs/recent"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/logs/recent?limit=10")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/logs/recent - Logs récents"
else
    print_result 1 "GET /audit/logs/recent - HTTP $http_code"
fi

# Test 3: GET /audit/logs/actor/{actorId}
echo -e "\n📝 Test 3: GET /audit/logs/actor/{actorId}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/logs/actor/user-001")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/logs/actor/{actorId} - Logs par acteur"
else
    print_result 1 "GET /audit/logs/actor/{actorId} - HTTP $http_code"
fi

# Test 4: GET /audit/logs/module/{module}
echo -e "\n📝 Test 4: GET /audit/logs/module/{module}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/logs/module/auth")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/logs/module/{module} - Logs par module"
else
    print_result 1 "GET /audit/logs/module/{module} - HTTP $http_code"
fi

# Test 5: GET /audit/logs/entity/{entityType}
echo -e "\n📝 Test 5: GET /audit/logs/entity/{entityType}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/logs/entity/User")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/logs/entity/{entityType} - Logs par entité"
else
    print_result 1 "GET /audit/logs/entity/{entityType} - HTTP $http_code"
fi

# Test 6: POST /audit/logs
echo -e "\n📝 Test 6: POST /audit/logs"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/audit/logs" \
  -H "Content-Type: application/json" \
  -d '{
    "actorId": "user-001",
    "action": "TEST_ACTION",
    "entityType": "TestEntity",
    "entityId": "test-001",
    "details": "Test audit log entry",
    "module": "test"
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    print_result 0 "POST /audit/logs - Création log d'audit"
else
    print_result 1 "POST /audit/logs - HTTP $http_code"
fi

# Section 2: Événements de sécurité (7 endpoints)
echo -e "\n📝 Section 2: Événements de sécurité"

# Test 7: GET /audit/security-events
echo -e "\n📝 Test 7: GET /audit/security-events"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-events")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-events - Liste événements sécurité"
else
    print_result 1 "GET /audit/security-events - HTTP $http_code"
fi

# Test 8: GET /audit/security-events/recent
echo -e "\n📝 Test 8: GET /audit/security-events/recent"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-events/recent?limit=10")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-events/recent - Événements récents"
else
    print_result 1 "GET /audit/security-events/recent - HTTP $http_code"
fi

# Test 9: GET /audit/security-events/severity/{severity}
echo -e "\n📝 Test 9: GET /audit/security-events/severity/{severity}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-events/severity/high")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-events/severity/{severity} - Par sévérité"
else
    print_result 1 "GET /audit/security-events/severity/{severity} - HTTP $http_code"
fi

# Test 10: GET /audit/security-events/checkpoint/{checkpointId}
echo -e "\n📝 Test 10: GET /audit/security-events/checkpoint/{checkpointId}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-events/checkpoint/check-001")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-events/checkpoint/{checkpointId} - Par checkpoint"
else
    print_result 1 "GET /audit/security-events/checkpoint/{checkpointId} - HTTP $http_code"
fi

# Test 11: GET /audit/security-events/pending-investigations
echo -e "\n📝 Test 11: GET /audit/security-events/pending-investigations"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-events/pending-investigations")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-events/pending-investigations - Enquêtes en attente"
else
    print_result 1 "GET /audit/security-events/pending-investigations - HTTP $http_code"
fi

# Test 12: POST /audit/security-events
echo -e "\n📝 Test 12: POST /audit/security-events"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/audit/security-events" \
  -H "Content-Type: application/json" \
  -d '{
    "checkpointId": "check-001",
    "severity": "medium",
    "eventType": "TEST_EVENT",
    "description": "Test security event",
    "sourceIp": "127.0.0.1",
    "userId": "user-001"
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    print_result 0 "POST /audit/security-events - Création événement sécurité"
else
    print_result 1 "POST /audit/security-events - HTTP $http_code"
fi

# Test 13: PUT /audit/security-events/{id}
echo -e "\n📝 Test 13: PUT /audit/security-events/{id}"
response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/audit/security-events/event-001" \
  -H "Content-Type: application/json" \
  -d '{
    "checkpointId": "check-001",
    "severity": "high",
    "eventType": "TEST_EVENT_UPDATED",
    "description": "Updated test security event"
  }')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "PUT /audit/security-events/{id} - Mise à jour événement"
else
    print_result 1 "PUT /audit/security-events/{id} - HTTP $http_code"
fi

# Section 3: Analytiques (3 endpoints)
echo -e "\n📝 Section 3: Analytiques"

# Test 14: GET /audit/statistics
echo -e "\n📝 Test 14: GET /audit/statistics"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/statistics")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/statistics - Statistiques d'audit"
else
    print_result 1 "GET /audit/statistics - HTTP $http_code"
fi

# Test 15: GET /audit/security-statistics
echo -e "\n📝 Test 15: GET /audit/security-statistics"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-statistics")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-statistics - Statistiques sécurité"
else
    print_result 1 "GET /audit/security-statistics - HTTP $http_code"
fi

# Test 16: GET /audit/security-trends
echo -e "\n📝 Test 16: GET /audit/security-trends"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/audit/security-trends")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_result 0 "GET /audit/security-trends - Tendances sécurité"
else
    print_result 1 "GET /audit/security-trends - HTTP $http_code"
fi

# Résumé
echo -e "\n=========================================="
echo "📊 RÉSUMÉ DES TESTS AUDIT"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo "=========================================="

# Sauvegarde des résultats
echo "AUDIT MODULE TEST RESULTS - $(date)" > audit_test_results.log
echo "Total: $TOTAL_TESTS, Passed: $PASSED_TESTS, Failed: $FAILED_TESTS" >> audit_test_results.log

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS AUDIT SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS AUDIT ONT ÉCHOUÉ${NC}"
    exit 1
fi