#!/bin/bash

# Script de test pour les endpoints de collaboration
BASE_URL="http://localhost:8080/api"

echo "=== Test des endpoints de collaboration ==="
echo ""

# Test 1: Récupérer les groupes de travail
echo "1. GET /collaboration/workgroups"
curl -s -X GET "$BASE_URL/collaboration/workgroups" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# Test 2: Créer un nouveau groupe de travail
echo "2. POST /collaboration/workgroups"
curl -s -X POST "$BASE_URL/collaboration/workgroups" \
  -H "Content-Type: application/json" \
  -d '{"name": "Groupe de Test", "description": "Groupe de test temporaire"}' | jq '.'
echo ""
echo ""

# Test 3: Récupérer les documents d'un groupe
echo "3. GET /collaboration/workgroups/group-001/documents"
curl -s -X GET "$BASE_URL/collaboration/workgroups/group-001/documents" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# Test 4: Récupérer les tâches d'un groupe
echo "4. GET /collaboration/workgroups/group-001/tasks"
curl -s -X GET "$BASE_URL/collaboration/workgroups/group-001/tasks" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# Test 5: Récupérer les messages d'un groupe
echo "5. GET /collaboration/workgroups/group-001/messages"
curl -s -X GET "$BASE_URL/collaboration/workgroups/group-001/messages" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# Test 6: Récupérer les réunions
echo "6. GET /collaboration/meets"
curl -s -X GET "$BASE_URL/collaboration/meets" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# Test 7: Créer une nouvelle réunion
echo "7. POST /collaboration/meets"
curl -s -X POST "$BASE_URL/collaboration/meets" \
  -H "Content-Type: application/json" \
  -d '{"title": "Réunion de Test", "link": "https://meet.google.com/test-xyz", "time": "2026-07-29T10:00:00"}' | jq '.'
echo ""
echo ""

# Test 8: Créer une session vocale
echo "8. POST /collaboration/voice-sessions"
curl -s -X POST "$BASE_URL/collaboration/voice-sessions" \
  -H "Content-Type: application/json" \
  -d '{"title": "Session Vocale Test", "groupId": "group-001"}' | jq '.'
echo ""
echo ""

echo "=== Tests terminés ==="