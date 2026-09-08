#!/bin/bash

# Simple test for multipart endpoints
echo "🧪 Test simple multipart"
echo "========================"

BASE_URL="http://localhost:8080/api"
GROUPCHAT_URL="$BASE_URL/collaboration/groupchat"

# Test avatar upload
echo "📷 Test 1: Upload avatar"
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_avatar.png

# Create a group first
echo "➕ Creating group..."
response=$(curl -s -X POST "$GROUPCHAT_URL/groups" -H "Content-Type: application/json" -d '{"name":"Test Multipart","creatorId":"user-001"}')
GROUP_ID=$(echo "$response" | jq -r '.id // empty')
echo "Group ID: $GROUP_ID"

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    echo "✅ Group created: $GROUP_ID"
    
    # Test avatar upload with correct multipart format
    echo "📷 Testing avatar upload..."
    curl -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/avatar" \
        -F "file=@/tmp/test_avatar.png" \
        -w "\nHTTP Status: %{http_code}\n"
    
    echo ""
    echo "🎵 Testing audio message..."
    echo "test audio data" > /tmp/test_audio.mp3
    curl -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/audio" \
        -F "audio=@/tmp/test_audio.mp3" \
        -F "senderId=user-001" \
        -w "\nHTTP Status: %{http_code}\n"
    
    echo ""
    echo "🖼️ Testing image message..."
    echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_image.jpg
    curl -X POST "$GROUPCHAT_URL/groups/$GROUP_ID/messages/image" \
        -F "image=@/tmp/test_image.jpg" \
        -F "senderId=user-001" \
        -w "\nHTTP Status: %{http_code}\n"
else
    echo "❌ Failed to create group"
fi

echo ""
echo "========================"
echo "🎯 Test terminé"
