#!/bin/bash

# Script de test COMPLET du module Collaboration - TOUS les endpoints
# Teste les 26 endpoints avec logging détaillé des erreurs

echo "🚀 Démarrage des tests COMPLETS du module Collaboration"
echo "======================================================"
echo "📋 Coverage : 26/26 endpoints à tester"
echo ""

BASE_URL="http://localhost:8080/api/collaboration"
LOG_FILE="collaboration_complete_test_results.log"
ERROR_LOG="collaboration_errors.log"

# Nettoyer les logs précédents
> "$LOG_FILE"
> "$ERROR_LOG"

# Compteurs
TOTAL_TESTS=26
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Fonction de logging
log_test() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    local error="$4"
    
    echo "[$status] $test_name: $details" | tee -a "$LOG_FILE"
    
    if [ "$status" = "❌ ERROR" ]; then
        echo "ERROR: $error" | tee -a "$ERROR_LOG"
    fi
}

# Fonction pour exécuter un test
run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local file_param="$5"
    
    echo "Testing: $test_name"
    
    if [ -z "$file_param" ]; then
        if [ -z "$data" ]; then
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -w "\n%{http_code}")
        else
            response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -d "$data" \
                -w "\n%{http_code}")
        fi
    else
        # Pour les fichiers
        response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
            -F "$file_param" \
            -w "\n%{http_code}")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "$test_name" "✅ SUCCESS" "HTTP $http_code" ""
        ((PASSED_TESTS++))
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        return 0
    else
        log_test "$test_name" "❌ ERROR" "HTTP $http_code - $body" "$body"
        ((FAILED_TESTS++))
        return 1
    fi
}

# Vérification application
echo "📋 Vérification si l'application est démarrée..."
if ! curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# Variables globales pour les tests
GROUP_ID=""
MESSAGE_ID=""
AUDIO_MESSAGE_ID=""
IMAGE_MESSAGE_ID=""

echo "======================================================"
echo "📋 TESTS DES ENDPOINTS (26/26)"
echo "======================================================"
echo ""

# ==================== SECTION 1: Tests Automatisés ====================
echo "🔧 SECTION 1: Tests Automatisés"
echo "------------------------------"

# ENDPOINT 1: Test Runner
echo "🔧 ENDPOINT 1/26: POST /test/run-all"
echo "-----------------------------------------"
run_test "Exécuter tous les tests automatisés" "POST" "/test/run-all" ""
echo ""

# ==================== SECTION 2: Group Chat CRUD ====================
echo "📱 SECTION 2: Group Chat CRUD"
echo "----------------------------"

# ENDPOINT 2: Create Group
echo "📱 ENDPOINT 2/26: POST /groups"
echo "---------------------------------"
GROUP_ID=$(run_test "Créer un groupe" "POST" "/groups" '{"name":"Test Group Complete","creatorId":"user-001"}' | jq -r '.id // empty')
echo "Groupe créé avec ID: $GROUP_ID"

if [ -z "$GROUP_ID" ] || [ "$GROUP_ID" = "null" ]; then
    log_test "Tests suivants" "⚠️ SKIP" "Impossible de créer le groupe" ""
    ((SKIPPED_TESTS+=24))
    echo ""
    echo "======================================================"
    echo "📊 RÉSUMÉ DES TESTS"
    echo "======================================================"
    echo "Total tests: $TOTAL_TESTS"
    echo "Réussis: $PASSED_TESTS"
    echo "Échoués: $FAILED_TESTS"
    echo "Ignorés: $SKIPPED_TESTS"
    exit 1
fi
echo ""

# ENDPOINT 3: Upload Avatar
echo "🖼️ ENDPOINT 3/26: POST /groups/{id}/avatar"
echo "----------------------------------------"
echo "Création d'une image de test pour avatar..."
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_avatar.png

if [ -f /tmp/test_avatar.png ]; then
    run_test "Upload avatar de groupe" "POST" "/groups/$GROUP_ID/avatar" "" "file=@/tmp/test_avatar.png"
    rm /tmp/test_avatar.png
else
    log_test "Upload avatar" "⚠️ SKIP" "Impossible de créer l'image de test" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 4: Get Group
echo "📖 ENDPOINT 4/26: GET /groups/{id}"
echo "---------------------------------"
run_test "Lire le groupe" "GET" "/groups/$GROUP_ID" ""
echo ""

# ENDPOINT 5: Get User Chats
echo "👤 ENDPOINT 5/26: GET /users/{userId}/chats"
echo "---------------------------------------"
run_test "Obtenir les chats de l'utilisateur" "GET" "/users/user-001/chats" ""
echo ""

# ENDPOINT 6: Update Group Name
echo "✏️ ENDPOINT 6/26: PUT /groups/{id}/name"
echo "--------------------------------------"
run_test "Mettre à jour le nom du groupe" "PUT" "/groups/$GROUP_ID/name" '{"name":"Updated Test Group Complete"}'
echo ""

# ENDPOINT 7: Update Group Description
echo "📝 ENDPOINT 7/26: POST /groups/{id}/description"
echo "----------------------------------------------"
run_test "Mettre à jour la description" "POST" "/groups/$GROUP_ID/description" '{"description":"Test description"}'
echo ""

# ENDPOINT 8: Add Participant
echo "➕ ENDPOINT 8/26: POST /groups/{id}/participants"
echo "-----------------------------------------"
run_test "Ajouter un participant" "POST" "/groups/$GROUP_ID/participants" '{"participantId":"user-002"}'
echo ""

# ENDPOINT 9: Add Admin
echo "👑 ENDPOINT 9/26: POST /groups/{id}/admins"
echo "------------------------------------"
run_test "Ajouter un admin" "POST" "/groups/$GROUP_ID/admins" '{"adminId":"user-001"}'
echo ""

# ENDPOINT 10: Remove Admin
echo "➖ ENDPOINT 10/26: DELETE /groups/{id}/admins/{adminId}"
echo "-------------------------------------------------"
run_test "Supprimer l'admin" "DELETE" "/groups/$GROUP_ID/admins/user-002" ""
echo ""

# ENDPOINT 11: Mute Chat
echo "🔇 ENDPOINT 11/26: POST /groups/{id}/mute"
echo "--------------------------------------"
run_test "Mettre le chat en mute" "POST" "/groups/$GROUP_ID/mute" '{"userId":"user-001","durationMinutes":"30"}'
echo ""

# ENDPOINT 12: Unmute Chat
echo "🔊 ENDPOINT 12/26: POST /groups/{id}/unmute"
echo "----------------------------------------"
run_test "Retirer le mute" "POST" "/groups/$GROUP_ID/unmute" '{"userId":"user-001"}'
echo ""

# ENDPOINT 13: Archive Chat
echo "📦 ENDPOINT 13/26: POST /groups/{id}/archive"
echo "-----------------------------------------"
run_test "Archiver le chat" "POST" "/groups/$GROUP_ID/archive" '{"userId":"user-001"}'
echo ""

# ENDPOINT 14: Unarchive Chat
echo "📂 ENDPOINT 14/26: POST /groups/{id}/unarchive"
echo "------------------------------------------"
run_test "Désarchiver le chat" "POST" "/groups/$GROUP_ID/unarchive" '{"userId":"user-001"}'
echo ""

# ENDPOINT 15: Pin Message
echo "📌 ENDPOINT 15/26: POST /groups/{id}/pin"
echo "-----------------------------------"
run_test "Épingler un message" "POST" "/groups/$GROUP_ID/pin" '{"messageId":"temp-message-id"}'
echo ""

# ENDPOINT 16: Unpin Message
echo "📍 ENDPOINT 16/26: POST /groups/{id}/unpin"
echo "------------------------------------"
run_test "Désépingler" "POST" "/groups/$GROUP_ID/unpin" ""
echo ""

# ENDPOINT 17: Get Groups by Type
echo "🏷️ ENDPOINT 17/26: GET /groups/types/{type}"
echo "-----------------------------------------"
run_test "Obtenir les groupes par type" "GET" "/groups/types/GROUP" ""
echo ""

# ENDPOINT 18: Create Individual Chat
echo "👥 ENDPOINT 18/26: POST /individual"
echo "---------------------------------"
run_test "Créer un chat individuel" "POST" "/individual" '{"user1Id":"user-001","user2Id":"user-002"}'
echo ""

# ENDPOINT 19: Remove Participant
echo "➖ ENDPOINT 19/26: DELETE /groups/{id}/participants/{participantId}"
echo "-----------------------------------------------------------------"
run_test "Supprimer le participant" "DELETE" "/groups/$GROUP_ID/participants/user-002" ""
echo ""

# ENDPOINT 20: Delete Group
echo "🗑️ ENDPOINT 20/26: DELETE /groups/{id}"
echo "---------------------------------"
run_test "Supprimer le groupe" "DELETE" "/groups/$GROUP_ID" ""
echo ""

# ==================== SECTION 3: Messages CRUD ====================
echo "💬 SECTION 3: Messages CRUD"
echo "-------------------------"

# Recréer le groupe pour les tests de messages
echo "📱 Recréation du groupe pour les tests de messages..."
GROUP_ID=$(run_test "Recréer un groupe" "POST" "/groups" '{"name":"Test Group Messages","creatorId":"user-001"}' | jq -r '.id // empty')
echo "Groupe recréé avec ID: $GROUP_ID"
echo ""

# ENDPOINT 21: Send Text Message
echo "💬 ENDPOINT 21/26: POST /groups/{id}/messages/text"
echo "------------------------------------------------"
MESSAGE_ID=$(run_test "Envoyer un message texte" "POST" "/groups/$GROUP_ID/messages/text" '{"senderId":"user-001","content":"Hello World - Complete Test"}' | jq -r '.id // empty')
echo "Message créé avec ID: $MESSAGE_ID"
echo ""

# ENDPOINT 22: Send Audio Message
echo "🎤 ENDPOINT 22/26: POST /groups/{id}/messages/audio"
echo "------------------------------------------------"
echo "Création d'un audio de test..."
dd if=/dev/zero of=/tmp/test_audio.mp3 bs=1024 count=100 2>/dev/null

if [ -f /tmp/test_audio.mp3 ]; then
    AUDIO_MESSAGE_ID=$(run_test "Envoyer un message audio (60s)" "POST" "/groups/$GROUP_ID/messages/audio" "" "file=@/tmp/test_audio.mp3&duration=60" | jq -r '.id // empty')
    echo "Message audio créé avec ID: $AUDIO_MESSAGE_ID"
    rm /tmp/test_audio.mp3
else
    log_test "Send audio message" "⚠️ SKIP" "Impossible de créer l'audio de test" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 23: Send Image Message
echo "📷 ENDPOINT 23/26: POST /groups/{id}/messages/image"
echo "-------------------------------------------------"
echo "Création d'une image de test..."
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.png

if [ -f /tmp/test_image.png ]; then
    IMAGE_MESSAGE_ID=$(run_test "Envoyer un message image" "POST" "/groups/$GROUP_ID/messages/image" "" "file=@/tmp/test_image.png" | jq -r '.id // empty')
    echo "Message image créé avec ID: $IMAGE_MESSAGE_ID"
    rm /tmp/test_image.png
else
    log_test "Send image message" "⚠️ SKIP" "Impossible de créer l'image de test" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 24: Get Messages
echo "📋 ENDPOINT 24/26: GET /groups/{id}/messages"
echo "-----------------------------------------"
run_test "Obtenir les messages du groupe" "GET" "/groups/$GROUP_ID/messages?limit=50&offset=0" ""
echo ""

# ENDPOINT 25: Mark as Read
echo "✅ ENDPOINT 25/26: POST /messages/{id}/read"
echo "----------------------------------------"
if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
    run_test "Marquer le message comme lu" "POST" "/messages/$MESSAGE_ID/read" "{\"chatId\":\"$GROUP_ID\",\"userId\":\"user-001\"}"
else
    log_test "Mark as read" "⚠️ SKIP" "Message ID non disponible" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 26: Delete Message
echo "🗑️ ENDPOINT 26/26: DELETE /messages/{id}"
echo "-----------------------------------"
if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
    run_test "Supprimer le message" "DELETE" "/messages/$MESSAGE_ID" ""
else
    log_test "Delete message" "⚠️ SKIP" "Message ID non disponible" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 27: Frontend Format (bonus)
echo "🎨 ENDPOINT 27/26: GET /groups/{id}/frontend"
echo "-------------------------------------------"
run_test "Obtenir le format frontend" "GET" "/groups/$GROUP_ID/frontend" ""
echo ""

# ==================== NETTOYAGE ====================
echo "🧹 NETTOYAGE"
echo "-----------"
run_test "Supprimer le groupe de test" "DELETE" "/groups/$GROUP_ID" ""
echo ""

echo "======================================================"
echo "📊 RÉSUMÉ FINAL DES TESTS"
echo "======================================================"
echo ""
echo "📈 Statistiques:"
echo "--------------"
echo "Total tests: $TOTAL_TESTS"
echo "✅ Réussis: $PASSED_TESTS"
echo "❌ Échoués: $FAILED_TESTS"
echo "⚠️ Ignorés: $SKIPPED_TESTS"
echo ""

# Calcul du pourcentage de réussite
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo "📊 Taux de réussite: $SUCCESS_RATE%"
else
    echo "📊 Taux de réussite: N/A"
fi
echo ""

if [ $FAILED_TESTS -eq 0 ] && [ $SKIPPED_TESTS -eq 0 ]; then
    echo "🎉 TOUS LES TESTS SONT PASSÉS !"
    echo ""
    echo "📋 Logs détaillés:"
    echo "-----------------"
    echo "Résultats: $LOG_FILE"
    echo "Erreurs: $ERROR_LOG"
    echo "Logs tests: logs/module_tests/collaboration.log"
elif [ $FAILED_TESTS -eq 0 ]; then
    echo "✅ Tests passés mais certains ignorés"
    echo "📋 Consultez $LOG_FILE pour les détails"
else
    echo "❌ CERTAINS TESTS ONT ÉCHOUÉ"
    echo "📋 Consultez $ERROR_LOG pour les erreurs"
    echo "📋 Consultez logs/module_tests/collaboration.log pour les détails"
fi

echo ""
echo "======================================================"
echo "🏁 FIN DES TESTS COMPLETS"
echo "======================================================"