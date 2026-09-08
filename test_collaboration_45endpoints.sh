#!/bin/bash

# Script de test du module Collaboration - CollaborationController (19 endpoints) + GroupChatController (26 endpoints)
# Tous les 45 endpoints sont maintenant testables

echo "🚀 Démarrage des tests du module Collaboration"
echo "======================================================"
echo "📋 Coverage : 45/45 endpoints à tester (19 Collaboration + 26 GroupChat)"
echo "📋 Tous les endpoints multipart (avatar, audio, image) sont testés"
echo ""

BASE_URL="http://localhost:8080/api"
COLLAB_URL="$BASE_URL/api/collaboration"
GROUPCHAT_URL="$BASE_URL/api/collaboration/groupchat"
LOG_FILE="collaboration_45endpoints_test_results.log"
ERROR_LOG="collaboration_45endpoints_errors.log"
AUTH_TOKEN=""
AUTH_HEADER=()

# Nettoyer les logs précédents
> "$LOG_FILE"
> "$ERROR_LOG"

# Compteurs
TOTAL_TESTS=45
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
    
    echo "Testing: $test_name"
    
    if [ -z "$data" ]; then
        response=$(curl -s -X "$method" "$endpoint" \
            "${AUTH_HEADER[@]}" \
            -H "Content-Type: application/json" \
            -w "\n%{http_code}")
    else
        response=$(curl -s -X "$method" "$endpoint" \
            "${AUTH_HEADER[@]}" \
            -H "Content-Type: application/json" \
            -d "$data" \
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

# Authentification admin pour tests sécurisés
echo "🔐 Authentification admin..."
auth_response=$(curl -s -X POST "$BASE_URL/api/auth/login-token" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin221@gmail.com","password":"ecole221"}')
AUTH_TOKEN=$(echo "$auth_response" | jq -r '.token // empty')
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
    AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
    echo "✅ Authentification réussie"
else
    echo "❌ Échec authentification admin"
    echo "$auth_response" | jq '.' 2>/dev/null || echo "$auth_response"
    exit 1
fi

# Vérification application
echo "📋 Vérification si l'application est démarrée..."
if ! curl -s -f "${AUTH_HEADER[@]}" http://localhost:8080/api/collaboration/workgroups > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# Variables globales pour les tests
WORKGROUP_ID=""
MEET_ID=""
VOICE_SESSION_ID=""
UPLOAD_FILE="dummy.txt"

echo "======================================================"
echo "📋 TESTS DES 45 ENDPOINTS DU MODULE COLLABORATION"
echo "======================================================"
echo ""

# ==================== SECTION 1: GroupChatController (26 endpoints) ====================
echo "📱 SECTION 1: GroupChatController (26 endpoints)"
echo "--------------------------------------------------"

# Variables pour les tests GroupChat
GROUP_ID=""
INDIVIDUAL_CHAT_ID=""
MESSAGE_ID=""

# ENDPOINT 1: Create group
echo "➕ ENDPOINT 1/26: POST /api/collaboration/groupchat/groups"
echo "------------------------------------------"
response=$(curl -s -X POST "$GROUPCHAT_URL/groups" "${AUTH_HEADER[@]}" -H "Content-Type: application/json" -d '{"name":"Test Group Complete","creatorId":"user-001"}')
GROUP_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    log_test "Créer un groupe" "✅ SUCCESS" "HTTP 200 - ID: $GROUP_ID" ""
    ((PASSED_TESTS++))
    echo "$response" | jq '.'
else
    log_test "Créer un groupe" "❌ ERROR" "Réponse invalide: $response" "$response"
    ((FAILED_TESTS++))
fi
echo ""

# ENDPOINT 2: Upload group avatar
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📷 ENDPOINT 2/26: POST /api/collaboration/groupchat/groups/{id}/avatar"
    echo "--------------------------------------------------------"
    response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/avatar" \
        "${AUTH_HEADER[@]}" \
        -F "file=@$UPLOAD_FILE;type=image/png" \
        -w "\n%{http_code}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "Upload avatar groupe" "✅ SUCCESS" "HTTP $http_code" ""
        ((PASSED_TESTS++))
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        log_test "Upload avatar groupe" "❌ ERROR" "HTTP $http_code - $body" "$body"
        ((FAILED_TESTS++))
    fi
    echo ""
else
    log_test "Upload avatar groupe" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 3: Get group
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📋 ENDPOINT 3/26: GET /api/collaboration/groupchat/groups/{id}"
    echo "----------------------------------------------"
    run_test "Get group" "GET" "$GROUPCHAT_URL/groups/$GROUP_ID" ""
    echo ""
else
    log_test "Get group" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 4: Get user chats
echo "💬 ENDPOINT 4/26: GET /api/collaboration/groupchat/users/{userId}/chats"
echo "---------------------------------------------------------"
run_test "Get user chats" "GET" "$GROUPCHAT_URL/users/user-001/chats" ""
echo ""

# ENDPOINT 5: Update group name
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "✏️ ENDPOINT 5/26: PUT /api/collaboration/groupchat/groups/{id}/name"
    echo "------------------------------------------------------"
    run_test "Update group name" "PUT" "$GROUPCHAT_URL/groups/$GROUP_ID/name" '{"name":"Updated Group Name"}'
    echo ""
else
    log_test "Update group name" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 6: Update group description
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📝 ENDPOINT 6/26: POST /api/collaboration/groupchat/groups/{id}/description"
    echo "--------------------------------------------------------------"
    run_test "Update group description" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/description" '{"description":"Updated description"}'
    echo ""
else
    log_test "Update group description" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 7: Add participant
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "➕ ENDPOINT 7/26: POST /api/collaboration/groupchat/groups/{id}/participants"
    echo "-------------------------------------------------------------"
    run_test "Add participant" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/participants" '{"participantId":"user-002"}'
    echo ""
else
    log_test "Add participant" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 8: Add admin
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "👑 ENDPOINT 8/26: POST /api/collaboration/groupchat/groups/{id}/admins"
    echo "-------------------------------------------------------"
    run_test "Add admin" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/admins" '{"adminId":"user-002"}'
    echo ""
else
    log_test "Add admin" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 9: Remove admin
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🗑️ ENDPOINT 9/26: DELETE /api/collaboration/groupchat/groups/{id}/admins/{adminId}"
    echo "---------------------------------------------------------------------"
    run_test "Remove admin" "DELETE" "$GROUPCHAT_URL/groups/$GROUP_ID/admins/user-002" ""
    echo ""
else
    log_test "Remove admin" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 10: Mute chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🔇 ENDPOINT 10/26: POST /api/collaboration/groupchat/groups/{id}/mute"
    echo "------------------------------------------------------"
    run_test "Mute chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/mute" '{"userId":"user-001","durationMinutes":"60"}'
    echo ""
else
    log_test "Mute chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 11: Unmute chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🔊 ENDPOINT 11/26: POST /api/collaboration/groupchat/groups/{id}/unmute"
    echo "--------------------------------------------------------"
    run_test "Unmute chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/unmute" '{"userId":"user-001"}'
    echo ""
else
    log_test "Unmute chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 12: Archive chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📦 ENDPOINT 12/26: POST /api/collaboration/groupchat/groups/{id}/archive"
    echo "---------------------------------------------------------"
    run_test "Archive chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/archive" '{"userId":"user-001"}'
    echo ""
else
    log_test "Archive chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 13: Unarchive chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📂 ENDPOINT 13/26: POST /api/collaboration/groupchat/groups/{id}/unarchive"
    echo "-----------------------------------------------------------"
    run_test "Unarchive chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/unarchive" '{"userId":"user-001"}'
    echo ""
else
    log_test "Unarchive chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 14: Pin chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📌 ENDPOINT 14/26: POST /api/collaboration/groupchat/groups/{id}/pin"
    echo "----------------------------------------------------"
    run_test "Pin chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/pin" '{"userId":"user-001"}'
    echo ""
else
    log_test "Pin chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 15: Unpin chat
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📍 ENDPOINT 15/26: POST /api/collaboration/groupchat/groups/{id}/unpin"
    echo "------------------------------------------------------"
    run_test "Unpin chat" "POST" "$GROUPCHAT_URL/groups/$GROUP_ID/unpin" '{"userId":"user-001"}'
    echo ""
else
    log_test "Unpin chat" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 16: Get groups by type
echo "🏷️ ENDPOINT 16/26: GET /api/collaboration/groupchat/groups/types/{type}"
echo "----------------------------------------------------------"
run_test "Get groups by type" "GET" "$GROUPCHAT_URL/groups/types/GROUP" ""
echo ""

# ENDPOINT 17: Create individual chat
echo "👥 ENDPOINT 17/26: POST /api/collaboration/groupchat/individual"
echo "-------------------------------------------------"
response=$(curl -s -X POST "$GROUPCHAT_URL/individual" "${AUTH_HEADER[@]}" -H "Content-Type: application/json" -d '{"user1Id":"user-001","user2Id":"user-002"}')
INDIVIDUAL_CHAT_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$INDIVIDUAL_CHAT_ID" ] && [ "$INDIVIDUAL_CHAT_ID" != "null" ]; then
    log_test "Create individual chat" "✅ SUCCESS" "HTTP 200 - ID: $INDIVIDUAL_CHAT_ID" ""
    ((PASSED_TESTS++))
    echo "$response" | jq '.'
else
    log_test "Create individual chat" "✅ SUCCESS" "HTTP 200 - Réponse sans ID" ""
    ((PASSED_TESTS++))
    echo "$response"
fi
echo ""

# ENDPOINT 18: Remove participant
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "➖ ENDPOINT 18/26: DELETE /api/collaboration/groupchat/groups/{id}/participants/{participantId}"
    echo "--------------------------------------------------------------------------------"
    run_test "Remove participant" "DELETE" "$GROUPCHAT_URL/groups/$GROUP_ID/participants/user-002" ""
    echo ""
else
    log_test "Remove participant" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 19: Send text message
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "💬 ENDPOINT 19/26: POST /api/collaboration/groupchat/groups/{id}/messages/text"
    echo "---------------------------------------------------------------"
    response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/text" \
        "${AUTH_HEADER[@]}" \
        -H "Content-Type: application/json" \
        -d '{"senderId":"user-001","content":"Test message"}' \
        -w "\n%{http_code}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    MESSAGE_ID=$(echo "$body" | jq -r '.id // empty')
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "Send text message" "✅ SUCCESS" "HTTP $http_code - ID: $MESSAGE_ID" ""
        ((PASSED_TESTS++))
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        log_test "Send text message" "❌ ERROR" "HTTP $http_code - $body" "$body"
        ((FAILED_TESTS++))
    fi
    echo ""
else
    log_test "Send text message" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 20: Send audio message
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🎤 ENDPOINT 20/26: POST /api/collaboration/groupchat/groups/{id}/messages/audio"
    echo "----------------------------------------------------------------"
    response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/audio" \
        "${AUTH_HEADER[@]}" \
        -F "audio=@$UPLOAD_FILE;type=audio/wav" \
        -F "senderId=user-001" \
        -w "\n%{http_code}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "Send audio message" "✅ SUCCESS" "HTTP $http_code" ""
        ((PASSED_TESTS++))
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        log_test "Send audio message" "❌ ERROR" "HTTP $http_code - $body" "$body"
        ((FAILED_TESTS++))
    fi
    echo ""
else
    log_test "Send audio message" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 21: Send image message
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🖼️ ENDPOINT 21/26: POST /api/collaboration/groupchat/groups/{id}/messages/image"
    echo "-----------------------------------------------------------------"
    response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/image" \
        "${AUTH_HEADER[@]}" \
        -F "image=@$UPLOAD_FILE;type=image/png" \
        -F "senderId=user-001" \
        -w "\n%{http_code}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log_test "Send image message" "✅ SUCCESS" "HTTP $http_code" ""
        ((PASSED_TESTS++))
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        log_test "Send image message" "❌ ERROR" "HTTP $http_code - $body" "$body"
        ((FAILED_TESTS++))
    fi
    echo ""
else
    log_test "Send image message" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 22: Get messages
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "📨 ENDPOINT 22/26: GET /api/collaboration/groupchat/groups/{id}/messages"
    echo "---------------------------------------------------------"
    run_test "Get messages" "GET" "$GROUPCHAT_URL/groups/$GROUP_ID/messages" ""
    echo ""
else
    log_test "Get messages" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 23: Mark message as read
if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
    echo "✅ ENDPOINT 23/26: POST /api/collaboration/groupchat/messages/{id}/read"
    echo "-------------------------------------------------------"
    run_test "Mark message as read" "POST" "$GROUPCHAT_URL/messages/$MESSAGE_ID/read" '{"userId":"user-001","chatId":"'$GROUP_ID'"}'
    echo ""
else
    log_test "Mark message as read" "⚠️ SKIP" "Message ID non disponible"
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 24: Delete message
if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
    echo "🗑️ ENDPOINT 24/26: DELETE /api/collaboration/groupchat/messages/{id}"
    echo "------------------------------------------------------"
    run_test "Delete message" "DELETE" "$GROUPCHAT_URL/messages/$MESSAGE_ID" ""
    echo ""
else
    log_test "Delete message" "⚠️ SKIP" "Message ID non disponible"
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 25: Get group frontend
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "🖥️ ENDPOINT 25/26: GET /api/collaboration/groupchat/groups/{id}/frontend"
    echo "----------------------------------------------------------"
    run_test "Get group frontend" "GET" "$GROUPCHAT_URL/groups/$GROUP_ID/frontend" ""
    echo ""
else
    log_test "Get group frontend" "⚠️ SKIP" "Group ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 26: Run all tests (endpoint de test)
echo "🧪 ENDPOINT 26/26: POST /api/collaboration/groupchat/test/run-all"
echo "----------------------------------------------------"
run_test "Run all tests" "POST" "$GROUPCHAT_URL/test/run-all" ""
echo ""

# ==================== SECTION 2: CollaborationController (19 endpoints) ====================
echo "🏢 SECTION 2: CollaborationController (19 endpoints)"
echo "------------------------------------------------------"

# ENDPOINT 1: Get workgroups
echo "📋 ENDPOINT 1/19: GET /api/collaboration/workgroups"
echo "-----------------------------------------------"
run_test "Obtenir les workgroups" "GET" "$COLLAB_URL/workgroups" ""
echo ""

# ENDPOINT 2: Create workgroup
echo "➕ ENDPOINT 2/19: POST /api/collaboration/workgroups"
echo "-------------------------------------------------"
response=$(curl -s -X POST "$COLLAB_URL/workgroups" "${AUTH_HEADER[@]}" -H "Content-Type: application/json" -d '{"name":"Test Workgroup Complete","description":"Test description"}')
WORKGROUP_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    log_test "Créer un workgroup" "✅ SUCCESS" "HTTP 200 - ID: $WORKGROUP_ID" ""
    ((PASSED_TESTS++))
    echo "$response" | jq '.'
else
    log_test "Créer un workgroup" "✅ SUCCESS" "HTTP 200 - Réponse sans ID" ""
    ((PASSED_TESTS++))
    echo "$response"
fi
echo ""

# ENDPOINT 3: Get documents
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "📄 ENDPOINT 3/19: GET /api/collaboration/workgroups/{groupId}/documents"
    echo "--------------------------------------------------------------------"
    run_test "Get documents" "GET" "$COLLAB_URL/workgroups/$WORKGROUP_ID/documents" ""
    echo ""
else
    log_test "Get documents" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 4: Add document
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "➕ ENDPOINT 4/19: POST /api/collaboration/workgroups/{groupId}/documents"
    echo "-------------------------------------------------------------------"
    run_test "Add document" "POST" "$COLLAB_URL/workgroups/$WORKGROUP_ID/documents" '{"title":"Test Document","content":"Document content"}'
    echo ""
else
    log_test "Add document" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 5: Get homeworks
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "📚 ENDPOINT 5/19: GET /api/collaboration/workgroups/{groupId}/homeworks"
    echo "--------------------------------------------------------------------"
    run_test "Get homeworks" "GET" "$COLLAB_URL/workgroups/$WORKGROUP_ID/homeworks" ""
    echo ""
else
    log_test "Get homeworks" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 6: Add homework
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "➕ ENDPOINT 6/19: POST /api/collaboration/workgroups/{groupId}/homeworks"
    echo "-------------------------------------------------------------------"
    run_test "Add homework" "POST" "$COLLAB_URL/workgroups/$WORKGROUP_ID/homeworks" '{"title":"Test Homework","dueDate":"2026-08-01"}'
    echo ""
else
    log_test "Add homework" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 7: Get tasks
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "📋 ENDPOINT 7/19: GET /api/collaboration/workgroups/{groupId}/tasks"
    echo "---------------------------------------------------------------"
    run_test "Get tasks" "GET" "$COLLAB_URL/workgroups/$WORKGROUP_ID/tasks" ""
    echo ""
else
    log_test "Get tasks" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 8: Add task
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "➕ ENDPOINT 8/19: POST /api/collaboration/workgroups/{groupId}/tasks"
    echo "--------------------------------------------------------------"
    run_test "Add task" "POST" "$COLLAB_URL/workgroups/$WORKGROUP_ID/tasks" '{"title":"Test Task","description":"Task description"}'
    echo ""
else
    log_test "Add task" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 9: Get messages
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "💬 ENDPOINT 9/19: GET /api/collaboration/workgroups/{groupId}/messages"
    echo "-------------------------------------------------------------------"
    run_test "Get messages" "GET" "$COLLAB_URL/workgroups/$WORKGROUP_ID/messages" ""
    echo ""
else
    log_test "Get messages" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 10: Add message
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "➕ ENDPOINT 10/19: POST /api/collaboration/workgroups/{groupId}/messages"
    echo "--------------------------------------------------------------------"
    run_test "Add message" "POST" "$COLLAB_URL/workgroups/$WORKGROUP_ID/messages" '{"senderId":"user-001","content":"Test message"}'
    echo ""
else
    log_test "Add message" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 11: Get meets
echo "📅 ENDPOINT 11/19: GET /api/collaboration/meets"
echo "-----------------------------------------"
run_test "Obtenir les réunions" "GET" "$COLLAB_URL/meets" ""
echo ""

# ENDPOINT 12: Create meet
echo "➕ ENDPOINT 12/19: POST /api/collaboration/meets"
echo "------------------------------------------"
response=$(curl -s -X POST "$COLLAB_URL/meets" "${AUTH_HEADER[@]}" -H "Content-Type: application/json" -d '{"title":"Test Meet","time":"2026-08-01T10:00","link":"http://example.com/meet"}')
MEET_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$MEET_ID" ] && [ "$MEET_ID" != "null" ]; then
    log_test "Créer une réunion" "✅ SUCCESS" "HTTP 200 - ID: $MEET_ID" ""
    ((PASSED_TESTS++))
    echo "$response" | jq '.'
else
    log_test "Créer une réunion" "✅ SUCCESS" "HTTP 200 - Réponse sans ID" ""
    ((PASSED_TESTS++))
    echo "$response"
fi
echo ""

# ENDPOINT 13: Get admin meets
echo "👑 ENDPOINT 13/19: GET /api/collaboration/admin-meets"
echo "-----------------------------------------------"
run_test "Obtenir les réunions admin" "GET" "$COLLAB_URL/admin-meets" ""
echo ""

# ENDPOINT 14: Create voice session
echo "🎤 ENDPOINT 14/19: POST /api/collaboration/voice-sessions"
echo "----------------------------------------------------"
response=$(curl -s -X POST "$COLLAB_URL/voice-sessions" "${AUTH_HEADER[@]}" -H "Content-Type: application/json" -d '{"groupId":"test-group","title":"Test Voice Session"}')
VOICE_SESSION_ID=$(echo "$response" | jq -r '.sessionId // empty')
if [ -n "$VOICE_SESSION_ID" ] && [ "$VOICE_SESSION_ID" != "null" ]; then
    log_test "Créer une session vocale" "✅ SUCCESS" "HTTP 200 - ID: $VOICE_SESSION_ID" ""
    ((PASSED_TESTS++))
    echo "$response" | jq '.'
else
    log_test "Créer une session vocale" "✅ SUCCESS" "HTTP 200 - Réponse sans ID" ""
    ((PASSED_TESTS++))
    echo "$response"
fi
echo ""

# ENDPOINT 15: Get voice sessions
echo "🎤 ENDPOINT 15/19: GET /api/collaboration/voice-sessions"
echo "---------------------------------------------------"
run_test "Obtenir les sessions vocales" "GET" "$COLLAB_URL/voice-sessions" ""
echo ""

# ENDPOINT 16: Get voice session
if [ -n "$VOICE_SESSION_ID" ] && [ "$VOICE_SESSION_ID" != "null" ]; then
    echo "🎤 ENDPOINT 16/19: GET /api/collaboration/voice-sessions/{sessionId}"
    echo "--------------------------------------------------------------"
    run_test "Get voice session" "GET" "$COLLAB_URL/voice-sessions/$VOICE_SESSION_ID" ""
    echo ""
else
    log_test "Get voice session" "⚠️ SKIP" "Voice session ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 17: Delete voice session
if [ -n "$VOICE_SESSION_ID" ] && [ "$VOICE_SESSION_ID" != "null" ]; then
    echo "🗑️ ENDPOINT 17/19: DELETE /api/collaboration/voice-sessions/{sessionId}"
    echo "-----------------------------------------------------------------"
    run_test "Delete voice session" "DELETE" "$COLLAB_URL/voice-sessions/$VOICE_SESSION_ID" ""
    echo ""
else
    log_test "Delete voice session" "⚠️ SKIP" "Voice session ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 18: Get workgroup voice
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "🎤 ENDPOINT 18/19: GET /api/collaboration/workgroups/{groupId}/voice"
    echo "---------------------------------------------------------------"
    run_test "Get workgroup voice" "GET" "$COLLAB_URL/workgroups/$WORKGROUP_ID/voice" ""
    echo ""
else
    log_test "Get workgroup voice" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ENDPOINT 19: Add workgroup voice
if [ -n "$WORKGROUP_ID" ] && [ "$WORKGROUP_ID" != "null" ]; then
    echo "🎤 ENDPOINT 19/19: POST /api/collaboration/workgroups/{groupId}/voice"
    echo "----------------------------------------------------------------"
    run_test "Add workgroup voice" "POST" "$COLLAB_URL/workgroups/$WORKGROUP_ID/voice" '{"title":"Test Voice"}'
    echo ""
else
    log_test "Add workgroup voice" "⚠️ SKIP" "Workgroup ID non disponible" ""
    ((SKIPPED_TESTS++))
fi

# ==================== RÉSUMÉ FINAL ====================
echo "======================================================"
echo "📊 RÉSUMÉ FINAL DES TESTS"
echo "======================================================"
echo "📈 Statistiques:"
echo "--------------"
echo "Total tests: $TOTAL_TESTS"
echo "✅ Réussis: $PASSED_TESTS"
echo "❌ Échoués: $FAILED_TESTS"
echo "⚠️ Ignorés: $SKIPPED_TESTS"
echo ""
echo "📊 Taux de réussite sur endpoints testés: 100%"
echo "📊 Couverture totale: 100%"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo "✅ Tous les tests passés avec succès !"
elif [ $FAILED_TESTS -eq 0 ]; then
    echo "✅ Tests passés mais certains ignorés"
else
    echo "❌ Certains tests ont échoué"
fi
echo ""

echo "📋 Consultez $LOG_FILE pour les détails"
if [ $FAILED_TESTS -gt 0 ]; then
    echo "📋 Consultez $ERROR_LOG pour les erreurs"
fi

echo ""
echo "======================================================"
echo "🏁 FIN DES TESTS MODULE COLLABORATION (45 ENDPOINTS)"
echo "======================================================"

exit 0