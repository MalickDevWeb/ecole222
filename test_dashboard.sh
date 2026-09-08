#!/bin/bash

# Script de test du module Dashboard - DashboardController (18 endpoints)
# Total: 18 endpoints à tester

echo "=========================================="
echo "  Tests des Endpoints Dashboard"
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

# ==================== ENDPOINTS KPIS (6 endpoints) ====================
echo "=== ENDPOINTS KPIS (6 endpoints) ==="
echo ""

test_endpoint "GET /dashboard/kpis - Liste des KPIs" \
    "GET" "/dashboard/kpis" ""

test_endpoint "GET /dashboard/kpis/{key} - KPI spécifique" \
    "GET" "/dashboard/kpis/recovery_rate" ""

test_endpoint "GET /dashboard/kpis/category/{cat} - KPIs par catégorie" \
    "GET" "/dashboard/kpis/category/finance" ""

test_endpoint "POST /dashboard/kpis - Créer KPI" \
    "POST" "/dashboard/kpis" \
    '{"id":"kpi-test","key":"test_kpi","name":"Test KPI","value":"100","unit":"%","trend":"up","trendValue":"+5%","category":"test","description":"Test KPI"}'

test_endpoint "PUT /dashboard/kpis/{id} - Mettre à jour KPI" \
    "PUT" "/dashboard/kpis/kpi-test" \
    '{"id":"kpi-test","key":"test_kpi","name":"Test KPI Updated","value":"105","unit":"%","trend":"up","trendValue":"+10%","category":"test","description":"Test KPI Updated"}'

test_endpoint "PATCH /dashboard/kpis/{key}/value - Mettre à jour valeur KPI" \
    "PATCH" "/dashboard/kpis/test_kpi/value" \
    '{"value":"110"}'

echo ""

# ==================== ENDPOINTS ALERTES (6 endpoints) ====================
echo "=== ENDPOINTS ALERTES (6 endpoints) ==="
echo ""

test_endpoint "GET /dashboard/alerts - Liste des alertes" \
    "GET" "/dashboard/alerts" ""

test_endpoint "GET /dashboard/alerts/active - Alertes actives" \
    "GET" "/dashboard/alerts/active" ""

test_endpoint "GET /dashboard/alerts/urgent - Alertes urgentes" \
    "GET" "/dashboard/alerts/urgent" ""

test_endpoint "POST /dashboard/alerts - Créer alerte" \
    "POST" "/dashboard/alerts" \
    '{"id":"alert-test","type":"test","title":"Test Alert","message":"Test alert message","severity":"info","active":true,"triggerTime":"2026-08-12T12:00:00"}'

test_endpoint "PUT /dashboard/alerts/{id} - Mettre à jour alerte" \
    "PUT" "/dashboard/alerts/alert-test" \
    '{"id":"alert-test","type":"test","title":"Test Alert Updated","message":"Test alert message updated","severity":"warning","active":true,"triggerTime":"2026-08-12T12:00:00"}'

test_endpoint "PATCH /dashboard/alerts/{id}/deactivate - Désactiver alerte" \
    "PATCH" "/dashboard/alerts/alert-test/deactivate" ""

echo ""

# ==================== ENDPOINTS ACTIVITÉS (3 endpoints) ====================
echo "=== ENDPOINTS ACTIVITÉS (3 endpoints) ==="
echo ""

test_endpoint "GET /dashboard/activities - Activités récentes" \
    "GET" "/dashboard/activities?limit=10" ""

test_endpoint "GET /dashboard/activities/type/{type} - Activités par type" \
    "GET" "/dashboard/activities/type/LOGIN" ""

test_endpoint "POST /dashboard/activities - Logger activité" \
    "POST" "/dashboard/activities" \
    '{"id":"activity-test","userId":"user-001","userName":"Test User","activityType":"TEST","description":"Test activity","timestamp":"2026-08-12T12:00:00"}'

echo ""

# ==================== ENDPOINTS STATS RÉGIONALES (2 endpoints) ====================
echo "=== ENDPOINTS STATS RÉGIONALES (2 endpoints) ==="
echo ""

test_endpoint "GET /dashboard/regional-stats/{year} - Stats régionales" \
    "GET" "/dashboard/regional-stats/2025-2026" ""

test_endpoint "PUT /dashboard/regional-stats/{id} - Mettre à jour stats régionales" \
    "PUT" "/dashboard/regional-stats/reg-1" \
    '{"id":"reg-1","region":"Dakar Centre","enrollmentRate":70.0,"totalStudents":100,"color":"#B3181C","academicYear":"2025-2026"}'

echo ""

# ==================== ENDPOINT DASHBOARD COMPLET (1 endpoint) ====================
echo "=== ENDPOINT DASHBOARD COMPLET (1 endpoint) ==="
echo ""

test_endpoint "GET /dashboard/complete - Dashboard complet" \
    "GET" "/dashboard/complete" ""

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