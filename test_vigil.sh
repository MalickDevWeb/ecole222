#!/bin/bash

# Script de test complet pour les endpoints Security/Vigil
# Base URL
BASE_URL="http://127.0.0.1:8080/api/api"

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Compteurs
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Fonction pour tester un endpoint
test_endpoint() {
    local description="$1"
    local method="$2"
    local endpoint="$3"
    local auth="$4"
    local data="$5"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Test $TOTAL_TESTS: $description ... "
    
    # Construire la commande curl
    local cmd="curl -s -o /dev/null -w '%{http_code}' -X $method"
    
    if [ "$auth" == "yes" ] && [ -n "$VIGIL_TOKEN" ]; then
        cmd="$cmd -H 'Authorization: Bearer $VIGIL_TOKEN'"
    fi
    
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

echo "=========================================="
echo "  Tests des Endpoints Security/Vigil"
echo "=========================================="
echo ""

# Connexion pour obtenir le token
echo "Connexion du vigile..."
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d '{"email":"etudiant221@gmail.com","password":"ecole221"}' http://127.0.0.1:8080/api/api/auth/login)
VIGIL_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

# Si le token est null, on continue sans authentification pour les endpoints publics
if [ -z "$VIGIL_TOKEN" ] || [ "$VIGIL_TOKEN" == "null" ]; then
    echo -e "${YELLOW}Attention: Token non disponible, certains tests nécessitant l'authentification pourraient échouer${NC}"
    VIGIL_TOKEN=""
else
    echo -e "${GREEN}Token obtenu avec succès${NC}"
fi
echo ""

# ==================== ENDPOINTS PROFILS DE VIGILANCE ====================
echo "=== ENDPOINTS PROFILS DE VIGILANCE ==="
echo ""

test_endpoint "GET /security/vigil-profiles - Récupérer tous les profils" \
    "GET" "/security/vigil-profiles" "no" ""

test_endpoint "GET /security/vigil-profiles/{id} - Récupérer profil par ID" \
    "GET" "/security/vigil-profiles/VIG-221" "no" ""

test_endpoint "GET /security/vigil-profiles/user/{userId} - Récupérer profils par utilisateur" \
    "GET" "/security/vigil-profiles/user/vigil221@gmail.com" "no" ""

test_endpoint "GET /security/vigil-profiles/role/{role} - Récupérer profils par rôle" \
    "GET" "/security/vigil-profiles/role/VIGIL" "no" ""

test_endpoint "POST /security/vigil-profiles - Créer un profil" \
    "POST" "/security/vigil-profiles" "no" \
    '{"id":"VIG-TEST","name":"Test Vigile","role":"VIGIL","userId":"test@example.com","status":"active","createdAt":"2024-01-01T00:00:00"}'

test_endpoint "PUT /security/vigil-profiles/{id} - Mettre à jour un profil" \
    "PUT" "/security/vigil-profiles/VIG-TEST" "no" \
    '{"id":"VIG-TEST","name":"Test Vigile Updated","role":"VIGIL","userId":"test@example.com","status":"active","createdAt":"2024-01-01T00:00:00"}'

test_endpoint "DELETE /security/vigil-profiles/{id} - Supprimer un profil" \
    "DELETE" "/security/vigil-profiles/VIG-TEST" "no" ""

echo ""

# ==================== ENDPOINTS OPÉRATIONS DE SÉCURITÉ ====================
echo "=== ENDPOINTS OPÉRATIONS DE SÉCURITÉ ==="
echo ""

test_endpoint "GET /security/checkpoint-status - Statut checkpoints" \
    "GET" "/security/checkpoint-status" "no" ""

test_endpoint "POST /security/scan-logs - Scan badge" \
    "POST" "/security/scan-logs" "no" \
    '{"badgeId":"STU-SN-2024-001"}'

echo ""

# ==================== ENDPOINTS VIGIL ====================
echo "=== ENDPOINTS VIGIL ==="
echo ""

test_endpoint "GET /vigil/check-ins - Check-ins vigile" \
    "GET" "/vigil/check-ins" "no" ""

test_endpoint "GET /vigil/profile - Profil vigile" \
    "GET" "/vigil/profile" "no" ""

test_endpoint "POST /vigil/scan - Scan vigile badge (format QR)" \
    "POST" "/vigil/scan" "no" \
    '{"badgeId":"STU_SECURE:SN-2024-001:1785087394:abc123"}'

test_endpoint "POST /vigil/scan - Scan vigile badge (format manuel)" \
    "POST" "/vigil/scan" "no" \
    '{"badgeId":"SN-2024-001"}'

test_endpoint "GET /vigil/last-scan - Dernier scan" \
    "GET" "/vigil/last-scan" "no" ""

echo ""
echo "=========================================="
echo "  RÉSUMÉ DES TESTS"
echo "=========================================="
echo -e "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Réussis: $PASSED_TESTS${NC}"
echo -e "${RED}Échoués: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}Tous les tests sont passés!${NC}"
    exit 0
else
    echo -e "${RED}Certains tests ont échoué${NC}"
    exit 1
fi
