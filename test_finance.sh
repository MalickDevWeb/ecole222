#!/bin/bash

# Script de test du module Finance - FinanceController (17 endpoints) + ExpenseController (10 endpoints)
# Total: 27 endpoints à tester

echo "=========================================="
echo "  Tests des Endpoints Finance"
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

# ==================== ENDPOINTS FINANCES ÉTUDIANTS (8 endpoints) ====================
echo "=== ENDPOINTS FINANCES ÉTUDIANTS (8 endpoints) ==="
echo ""

test_endpoint "GET /finance/students - Liste finances étudiantes" \
    "GET" "/finance/students" ""

test_endpoint "GET /finance/students/{id} - Détails finance étudiant" \
    "GET" "/finance/students/fin-001" ""

test_endpoint "GET /finance/students/classe/{classe} - Finances par classe" \
    "GET" "/finance/students/classe/L1%20GL" ""

test_endpoint "GET /finance/students/debt - Étudiants avec dette" \
    "GET" "/finance/students/debt" ""

test_endpoint "POST /finance/students - Créer finance étudiant" \
    "POST" "/finance/students" \
    '{"id":"fin-test","studentId":"std-test","studentName":"Test Student","classe":"L1 GL","statutFrais":"En%20Règle","montantDu":450000,"montantPaye":0,"montantRestant":450000}'

test_endpoint "PUT /finance/students/{id} - Mettre à jour finance étudiant" \
    "PUT" "/finance/students/fin-test" \
    '{"id":"fin-test","studentId":"std-test","studentName":"Test Student Updated","classe":"L1 GL","statutFrais":"En%20Règle","montantDu":450000,"montantPaye":100000,"montantRestant":350000}'

test_endpoint "PATCH /finance/students/{id}/status - Mettre à jour statut" \
    "PATCH" "/finance/students/fin-test/status" \
    '{"status":"En%20Avance"}'

test_endpoint "DELETE /finance/students/{id} - Supprimer finance étudiant" \
    "DELETE" "/finance/students/fin-test" ""

echo ""

# ==================== ENDPOINTS TRANSACTIONS (4 endpoints) ====================
echo "=== ENDPOINTS TRANSACTIONS (4 endpoints) ==="
echo ""

test_endpoint "GET /finance/transactions - Liste transactions" \
    "GET" "/finance/transactions" ""

test_endpoint "GET /finance/transactions/student/{id} - Transactions étudiant" \
    "GET" "/finance/transactions/student/std-001" ""

test_endpoint "GET /finance/transactions/recent - Transactions récentes" \
    "GET" "/finance/transactions/recent" ""

test_endpoint "POST /finance/transactions - Créer transaction" \
    "POST" "/finance/transactions" \
    '{"id":"trans-test","studentId":"std-001","amount":50000,"type":"PAIEMENT","date":"2026-08-12T12:00:00","description":"Paiement frais"}'

echo ""

# ==================== ENDPOINTS ANALYTIQUES (5 endpoints) ====================
echo "=== ENDPOINTS ANALYTIQUES (5 endpoints) ==="
echo ""

test_endpoint "GET /finance/dashboard - Dashboard financier" \
    "GET" "/finance/dashboard" ""

test_endpoint "GET /finance/total-debt - Dette totale" \
    "GET" "/finance/total-debt" ""

test_endpoint "GET /finance/total-collected - Total collecté" \
    "GET" "/finance/total-collected" ""

test_endpoint "GET /finance/expenses-by-category - Dépenses par catégorie" \
    "GET" "/finance/expenses-by-category" ""

test_endpoint "GET /finance/payment-trends - Tendances de paiement" \
    "GET" "/finance/payment-trends" ""

echo ""

# ==================== ENDPOINTS DÉPENSES (10 endpoints) ====================
echo "=== ENDPOINTS DÉPENSES (10 endpoints) ==="
echo ""

test_endpoint "POST /finance/expenses - Créer dépense" \
    "POST" "/finance/expenses" \
    '{"label":"Test Expense","amount":10000,"category":"MATÉRIEL","description":"Test expense"}'

test_endpoint "GET /finance/expenses/{id} - Récupérer dépense" \
    "GET" "/finance/expenses/1" ""

test_endpoint "GET /finance/expenses - Liste dépenses" \
    "GET" "/finance/expenses" ""

test_endpoint "PUT /finance/expenses/{id} - Mettre à jour dépense" \
    "PUT" "/finance/expenses/1" \
    '{"label":"Test Expense Updated","description":"Updated description"}'

test_endpoint "PATCH /finance/expenses/{id}/status - Mettre à jour statut" \
    "PATCH" "/finance/expenses/1/status" \
    '{"status":"PAYÉ"}'

test_endpoint "GET /finance/expenses/category/{category} - Dépenses par catégorie" \
    "GET" "/finance/expenses/category/MATÉRIEL" ""

test_endpoint "GET /finance/expenses/status/{status} - Dépenses par statut" \
    "GET" "/finance/expenses/status/EN_ATTENTE" ""

echo "Test $((TOTAL_TESTS + 1)): GET /finance/expenses/between - Dépenses entre dates ... ${YELLOW}SKIPPED${NC} (Requires date parameters)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

echo "Test $((TOTAL_TESTS + 1)): GET /finance/expenses/recent - Dépenses récentes ... ${YELLOW}SKIPPED${NC} (Requires date parameter)"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
SKIPPED_TESTS=$((SKIPPED_TESTS + 1))

test_endpoint "GET /finance/expenses/total/{category} - Total payé par catégorie" \
    "GET" "/finance/expenses/total/MATÉRIEL" ""

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