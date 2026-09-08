#!/bin/bash

# Script de test complet du module Collaboration
# Teste tous les endpoints CRUD et log les erreurs dans collaboration.log

echo "🚀 Démarrage des tests du module Collaboration"
echo "=============================================="

BASE_URL="http://localhost:8080/api/collaboration"
LOG_FILE="collaboration_test_results.log"

# Nettoyer les logs précédents
> "$LOG_FILE"

# Fonction de logging
log_test() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    echo "[$status] $test_name: $details" | tee -a "$LOG_FILE"
}

# Fonction pour exécuter un test
run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    
    echo "Testing: $test_name"
    
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
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "$test_name" "✅ SUCCESS" "HTTP $http_code"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        return 0
    else
        log_test "$test_name" "❌ ERROR" "HTTP $http_code - $body"
        return 1
    fi
}

# Démarrer l'application si nécessaire
echo "📋 Vérification si l'application est démarrée..."
if ! curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# ==================== TESTS AUTOMATISÉS ====================

echo "🧪 Exécution des tests automatisés..."
run_test "Exécuter tous les tests" "POST" "/test/run-all" ""

echo ""
echo "=============================================="
echo "📝 Tests manuels des endpoints CRUD"
echo "=============================================="
echo ""

# ==================== TESTS GROUP CHAT ====================

echo "📱 Tests GroupChat"
echo "------------------"

# CREATE GROUP
GROUP_ID=$(run_test "Créer un groupe" "POST" "/groups" '{"name":"Test Group","creatorId":"user-001"}' | jq -r '.id // empty')
echo "Groupe créé avec ID: $GROUP_ID"

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    # READ GROUP
    run_test "Lire le groupe" "GET" "/groups/$GROUP_ID" ""
    
    # UPDATE GROUP NAME
    run_test "Mettre à jour le nom" "PUT" "/groups/$GROUP_ID/name" '{"name":"Updated Test Group"}'
    
    # ADD PARTICIPANT
    run_test "Ajouter un participant" "POST" "/groups/$GROUP_ID/participants" '{"participantId":"user-002"}'
    
    # GET USER CHATS
    run_test "Obtenir les chats de l'utilisateur" "GET" "/users/user-001/chats" ""
    
    # DELETE PARTICIPANT
    run_test "Supprimer le participant" "DELETE" "/groups/$GROUP_ID/participants/user-002" ""
else
    log_test "Tests GroupChat" "⚠️ SKIP" "Impossible de créer le groupe"
fi

echo ""

# ==================== TESTS MESSAGES ====================

echo "💬 Tests Messages"
echo "---------------"

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    # SEND TEXT MESSAGE
    MESSAGE_ID=$(run_test "Envoyer un message texte" "POST" "/groups/$GROUP_ID/messages/text" '{"senderId":"user-001","content":"Hello World"}' | jq -r '.id // empty')
    echo "Message créé avec ID: $MESSAGE_ID"
    
    if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
        # GET MESSAGES
        run_test "Obtenir les messages" "GET" "/groups/$GROUP_ID/messages" ""
        
        # MARK AS READ
        run_test "Marquer comme lu" "POST" "/messages/$MESSAGE_ID/read" '{"chatId":"'"$GROUP_ID"'","userId":"user-001"}'
        
        # DELETE MESSAGE
        run_test "Supprimer le message" "DELETE" "/messages/$MESSAGE_ID" ""
    else
        log_test "Tests Messages" "⚠️ SKIP" "Impossible de créer le message"
    fi
else
    log_test "Tests Messages" "⚠️ SKIP" "Groupe non disponible"
fi

echo ""

# ==================== TESTS MEDIA ====================

echo "📎 Tests Media"
echo "-------------"

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    # SEND IMAGE MESSAGE
    echo "Création d'une image de test..."
    echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.png
    
    if [ -f /tmp/test_image.png ]; then
        run_test "Envoyer une image" "POST" "/groups/$GROUP_ID/messages/image" -F "file=@/tmp/test_image.png"
        rm /tmp/test_image.png
    else
        log_test "Upload Image" "⚠️ SKIP" "Impossible de créer l'image de test"
    fi
    
    # SEND AUDIO MESSAGE
    echo "Création d'un audio de test..."
    dd if=/dev/zero of=/tmp/test_audio.mp3 bs=1024 count=100 2>/dev/null
    
    if [ -f /tmp/test_audio.mp3 ]; then
        run_test "Envoyer un audio (60s)" "POST" "/groups/$GROUP_ID/messages/audio" -F "file=@/tmp/test_audio.mp3" -F "duration=60"
        rm /tmp/test_audio.mp3
    else
        log_test "Upload Audio" "⚠️ SKIP" "Impossible de créer l'audio de test"
    fi
else
    log_test "Tests Media" "⚠️ SKIP" "Groupe non disponible"
fi

echo ""

# ==================== NETTOYAGE ====================

echo "🧹 Nettoyage"
echo "-----------"

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    run_test "Supprimer le groupe" "DELETE" "/groups/$GROUP_ID" ""
fi

echo ""
echo "=============================================="
echo "✅ Tests terminés"
echo "=============================================="
echo ""
echo "📋 Résultats détaillés dans: $LOG_FILE"
echo "📋 Logs des erreurs dans: logs/module_tests/collaboration.log"
echo ""
echo "📊 Résumé:"
echo "--------"
grep -c "✅ SUCCESS" "$LOG_FILE" | xargs -I {} echo "Tests réussis: {}"
grep -c "❌ ERROR" "$LOG_FILE" | xargs -I {} echo "Tests échoués: {}"
grep -c "⚠️ SKIP" "$LOG_FILE" | xargs -I {} echo "Tests ignorés: {}"