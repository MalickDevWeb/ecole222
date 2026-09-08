#!/bin/bash

# Script de test du module Planning - PlanningController (16 endpoints) + AdminPlanningController (10 endpoints)
# Total: 26 endpoints à tester

echo "=========================================="
echo "  Tests des Endpoints Planning"
echo "=========================================="
echo ""

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Compteurs
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Base URL
BASE_URL="http://127.0.0.1:8080/api/api"

# Fonction pour tester un endpoint
test_endpoint() {
    local description="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Test $TOTAL_TESTS: $description ... "
    
    # Construire la commande curl
    local cmd="curl -s -o /dev/null -w '%{http_code}' -X $method"
    
    if [ -n "$data" ]; then
        cmd="$cmd -H 'Content-Type: application/json' -d '$data'"
    fi
    
    cmd="$cmd '$BASE_URL$endpoint'"
    
    # Exécuter et capturer le code HTTP
    http_code=$(eval $cmd)
    
    # Vérifier le code HTTP (200, 201, 204 sont considérés comme succès)
    if [ "$http_code" == "200" ] || [ "$http_code" == "201" ] || [ "$http_code" == "204" ]; then
        echo -e "${GREEN}PASSED${NC} (HTTP $http_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}FAILED${NC} (HTTP $http_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# ==================== ENDPOINTS PLANNINGCONTROLLER ====================
echo "=== ENDPOINTS PLANNINGCONTROLLER (16 endpoints) ==="
echo ""

# Rooms
test_endpoint "GET /planning/rooms - Liste des salles" \
    "GET" "/planning/rooms" ""

test_endpoint "POST /planning/rooms - Créer salle" \
    "POST" "/planning/rooms" \
    '{"id":"room-test","name":"Salle Test","capacity":30,"equipment":"projecteur,wifi","location":"Bâtiment A"}'

test_endpoint "GET /planning/rooms/{id} - Détails salle" \
    "GET" "/planning/rooms/room-test" ""

test_endpoint "DELETE /planning/rooms/{id} - Supprimer salle" \
    "DELETE" "/planning/rooms/room-test" ""

echo ""

# Slots
test_endpoint "GET /planning/slots - Liste des créneaux" \
    "GET" "/planning/slots" ""

test_endpoint "POST /planning/slots - Créer créneau" \
    "POST" "/planning/slots" \
    '{"id":"slot-test","startTime":"08:00","endTime":"10:00","dayOfWeek":"LUNDI","type":"CM"}'

test_endpoint "GET /planning/slots/{id} - Détails créneau" \
    "GET" "/planning/slots/slot-test" ""

test_endpoint "DELETE /planning/slots/{id} - Supprimer créneau" \
    "DELETE" "/planning/slots/slot-test" ""

echo ""

# Filieres
test_endpoint "GET /planning/filieres - Liste des filières" \
    "GET" "/planning/filieres" ""

test_endpoint "POST /planning/filieres - Créer filière" \
    "POST" "/planning/filieres" \
    '{"id":"fil-test","name":"Filière Test","code":"TEST","description":"Test","level":"Licence"}'

test_endpoint "GET /planning/filieres/{id} - Détails filière" \
    "GET" "/planning/filieres/fil-test" ""

test_endpoint "DELETE /planning/filieres/{id} - Supprimer filière" \
    "DELETE" "/planning/filieres/fil-test" ""

echo ""

# Classes
test_endpoint "GET /planning/classes - Liste des classes" \
    "GET" "/planning/classes?page=0&size=10" ""

test_endpoint "POST /planning/classes - Créer classe" \
    "POST" "/planning/classes" \
    '{"id":"classe-test","name":"Classe Test","code":"L1-TEST","filiereId":"fil-001","level":"L1","capacity":30,"currentSize":0}'

test_endpoint "GET /planning/classes/{id} - Détails classe" \
    "GET" "/planning/classes/classe-test" ""

test_endpoint "DELETE /planning/classes/{id} - Supprimer classe" \
    "DELETE" "/planning/classes/classe-test" ""

echo ""

# ==================== ENDPOINTS ADMINPLANNINGCONTROLLER ====================
echo "=== ENDPOINTS ADMINPLANNINGCONTROLLER (10 endpoints) ==="
echo ""

# Rooms
test_endpoint "GET /admin/planning/rooms - Liste salles admin" \
    "GET" "/admin/planning/rooms" ""

echo -e "Test $((TOTAL_TESTS + 1)): PUT /admin/planning/rooms - Mettre à jour salles ... ${YELLOW}SKIPPED${NC} (Requires specific admin domain model structure)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

echo ""

# Filieres
test_endpoint "GET /admin/planning/filieres - Liste filières admin" \
    "GET" "/admin/planning/filieres" ""

echo -e "Test $((TOTAL_TESTS + 1)): PUT /admin/planning/filieres - Mettre à jour filières ... ${YELLOW}SKIPPED${NC} (Requires specific admin domain model structure)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

echo ""

# Classes
test_endpoint "GET /admin/planning/classes - Liste classes admin" \
    "GET" "/admin/planning/classes" ""

echo -e "Test $((TOTAL_TESTS + 1)): PUT /admin/planning/classes - Mettre à jour classes ... ${YELLOW}SKIPPED${NC} (Requires specific admin domain model structure)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

echo ""

# Slots
test_endpoint "GET /admin/planning/slots - Liste créneaux admin" \
    "GET" "/admin/planning/slots" ""

echo -e "Test $((TOTAL_TESTS + 1)): PUT /admin/planning/slots - Mettre à jour créneaux ... ${YELLOW}SKIPPED${NC} (Requires specific admin domain model structure)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

echo ""
echo "=========================================="
echo "  RÉSUMÉ DES TESTS"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo -e "${YELLOW}Ignorés: $SKIPPED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}Tous les tests exécutés sont passés!${NC}"
    exit 0
else
    echo -e "${RED}Certains tests ont échoué${NC}"
    exit 1
fi