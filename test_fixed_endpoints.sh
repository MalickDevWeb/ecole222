#!/bin/bash

# Script de test des 5 endpoints corrigés
echo "🚀 Test des 5 endpoints corrigés"
echo "================================"
echo ""

BASE_URL="http://localhost:8080/api"
GROUPCHAT_URL="$BASE_URL/api/collaboration/groupchat"

# Vérification application
echo "📋 Vérification si l'application est démarrée..."
if ! curl -s -f http://localhost:8080/api/collaboration/workgroups > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# Variables pour les tests
GROUP_ID=""
MESSAGE_ID=""

# Test 1: Créer un groupe
echo "➕ Test 1: Créer un groupe"
response=$(curl -s -X POST "$GROUPCHAT_URL/groups" -H "Content-Type: application/json" -d '{"name":"Test Fixed Endpoints","creatorId":"user-001"}')
GROUP_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "✅ Groupe créé avec succès: $GROUP_ID"
else
    echo "❌ Erreur création groupe: $response"
    exit 1
fi
echo ""

# Test 2: Upload avatar (multipart fix)
echo "📷 Test 2: Upload avatar (multipart fix)"
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_avatar.png
response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/avatar" \
    -F "file=@/tmp/test_avatar.png" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "✅ Upload avatar réussi: HTTP $http_code"
else
    echo "❌ Upload avatar échoué: HTTP $http_code - $body"
fi
echo ""

# Test 3: Envoyer message texte (pour créer un message)
echo "💬 Test 3: Envoyer message texte (pour créer un message)"
response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/text" \
    -H "Content-Type: application/json" \
    -d '{"senderId":"user-001","content":"Test message"}')
MESSAGE_ID=$(echo "$response" | jq -r '.id // empty')
if [ -n "$MESSAGE_ID" ] && [ "$MESSAGE_ID" != "null" ]; then
    echo "✅ Message texte créé: $MESSAGE_ID"
else
    echo "❌ Erreur création message: $response"
fi
echo ""

# Test 4: Marquer message comme lu (HTTP 500 fix)
echo "📖 Test 4: Marquer message comme lu (HTTP 500 fix)"
response=$(curl -s -X POST "$GROUPCHAT_URL/messages/$MESSAGE_ID/read" \
    -H "Content-Type: application/json" \
    -d '{"userId":"user-001","chatId":"'$GROUP_ID'"}' \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "✅ Marquage comme lu réussi: HTTP $http_code"
else
    echo "❌ Marquage comme lu échoué: HTTP $http_code - $body"
fi
echo ""

# Test 5: Supprimer message (HTTP 500 fix)
echo "🗑️ Test 5: Supprimer message (HTTP 500 fix)"
response=$(curl -s -X DELETE "$GROUPCHAT_URL/messages/$MESSAGE_ID" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "✅ Suppression message réussie: HTTP $http_code"
else
    echo "❌ Suppression message échouée: HTTP $http_code - $body"
fi
echo ""

# Test 6: Envoyer message audio (multipart fix)
echo "🎵 Test 6: Envoyer message audio (multipart fix)"
echo "test audio data" > /tmp/test_audio.mp3
response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/audio" \
    -F "audio=@/tmp/test_audio.mp3" \
    -F "senderId=user-001" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "✅ Message audio envoyé: HTTP $http_code"
else
    echo "❌ Message audio échoué: HTTP $http_code - $body"
fi
echo ""

# Test 7: Envoyer message image (multipart fix)
echo "🖼️ Test 7: Envoyer message image (multipart fix)"
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.jpg
response=$(curl -s -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/image" \
    -F "image=@/tmp/test_image.jpg" \
    -F "senderId=user-001" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "✅ Message image envoyé: HTTP $http_code"
else
    echo "❌ Message image échoué: HTTP $http_code - $body"
fi
echo ""

echo "================================"
echo "🎯 Tests terminés"
echo "================================"