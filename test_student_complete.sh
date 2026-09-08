#!/bin/bash

# Script de test COMPLET du module Student - TOUS les endpoints
# Teste les 23 endpoints avec logging détaillé des erreurs

echo "🚀 Démarrage des tests COMPLETS du module Student"
echo "=================================================="
echo "📋 Coverage : 23/23 endpoints à tester"
echo ""

BASE_URL="http://localhost:8080/api/student"
AUTH_URL="http://localhost:8080/api/auth"
LOG_FILE="student_complete_test_results.log"
ERROR_LOG="student_errors.log"

# Nettoyer les logs précédents
> "$LOG_FILE"
> "$ERROR_LOG"

# Compteurs
TOTAL_TESTS=23
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
    local auth_token="$6"
    
    echo "Testing: $test_name"
    
    local headers="-H \"Content-Type: application/json\""
    if [ -n "$auth_token" ]; then
        headers="$headers -H \"Authorization: Bearer $auth_token\""
    fi
    
    if [ -z "$file_param" ]; then
        if [ -z "$data" ]; then
            response=$(eval curl -s -X "$method" "$BASE_URL$endpoint" \
                $headers \
                -w "\n%{http_code}")
        else
            response=$(eval curl -s -X "$method" "$BASE_URL$endpoint" \
                $headers \
                -d "$data" \
                -w "\n%{http_code}")
        fi
    else
        # Pour les fichiers
        response=$(eval curl -s -X "$method" "$BASE_URL$endpoint" \
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

# Authentification pour obtenir un token
echo "🔐 Authentification étudiant..."
AUTH_RESPONSE=$(curl -s -X POST "$AUTH_URL/login-token" \
    -H "Content-Type: application/json" \
    -d '{"email":"etudiant221@gmail.com","password":"ecole221"}')

AUTH_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.token // empty')

if [ -z "$AUTH_TOKEN" ] || [ "$AUTH_TOKEN" = "null" ]; then
    echo "⚠️ Impossible d'obtenir le token d'authentification. Tests sans auth."
    AUTH_TOKEN=""
else
    echo "✅ Token obtenu: ${AUTH_TOKEN:0:20}..."
fi
echo ""

# Variables globales pour les tests
COURSE_ID="course-1"
HOMEWORK_ID="hw-1"
SESSION_ID="sess-1"
GRADE_ID="g1"

echo "=================================================="
echo "📋 TESTS DES ENDPOINTS (23/23)"
echo "=================================================="
echo ""

# ==================== SECTION 1: Profil & Dashboard ====================
echo "👤 SECTION 1: Profil & Dashboard"
echo "---------------------------------"

# ENDPOINT 1: Get Profile
echo "👤 ENDPOINT 1/23: GET /profile"
echo "-------------------------------"
run_test "Profil étudiant" "GET" "/profile" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 2: Get Dashboard
echo "📊 ENDPOINT 2/23: GET /dashboard"
echo "---------------------------------"
run_test "Dashboard étudiant" "GET" "/dashboard" "" "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 2: Badge Numérique & QR ====================
echo "🎫 SECTION 2: Badge Numérique & QR"
echo "---------------------------------"

# ENDPOINT 3: Get Digital Badge
echo "🎫 ENDPOINT 3/23: GET /digital-badge"
echo "------------------------------------"
run_test "Badge numérique" "GET" "/digital-badge" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 4: Validate QR (public)
echo "✅ ENDPOINT 4/23: POST /qr-validate"
echo "-----------------------------------"
run_test "Validation QR" "POST" "/qr-validate" '{"matricule":"SN-2026-1234","minuteEpoch":1234567890,"token":"test-token"}' "" ""
echo ""

# ==================== SECTION 3: Présences ====================
echo "📅 SECTION 3: Présences"
echo "-----------------------"

# ENDPOINT 5: Get Attendances
echo "📅 ENDPOINT 5/23: GET /attendances"
echo "-----------------------------------"
run_test "Liste des présences" "GET" "/attendances?page=0&size=10" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 6: Self Check-in
echo "✍️ ENDPOINT 6/23: POST /attendances"
echo "------------------------------------"
run_test "Auto check-in" "POST" "/attendances" '{"type":"PRESENT","method":"MANUAL"}' "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 4: Sessions Live ====================
echo "📺 SECTION 4: Sessions Live"
echo "---------------------------"

# ENDPOINT 7: Get Live Sessions
echo "📺 ENDPOINT 7/23: GET /live-sessions"
echo "-------------------------------------"
run_test "Sessions live actives" "GET" "/live-sessions?page=0&size=10" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 8: Send Reaction
echo "👍 ENDPOINT 8/23: POST /live-sessions/{sessionId}/reaction"
echo "----------------------------------------------------------"
run_test "Envoyer une réaction" "POST" "/live-sessions/$SESSION_ID/reaction" '{"type":"LIKE"}' "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 9: Get Chat Live
echo "💬 ENDPOINT 9/23: GET /live-sessions/{sessionId}/chat"
echo "-----------------------------------------------------"
run_test "Chat live" "GET" "/live-sessions/$SESSION_ID/chat" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 10: Post Chat Live
echo "💬 ENDPOINT 10/23: POST /live-sessions/{sessionId}/chat"
echo "------------------------------------------------------"
run_test "Envoyer message chat" "POST" "/live-sessions/$SESSION_ID/chat" '{"user":"Mamadou Sall","text":"Question sur le chapitre 1"}' "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 5: Notes ====================
echo "📝 SECTION 5: Notes"
echo "------------------"

# ENDPOINT 11: Get Grades
echo "📝 ENDPOINT 11/23: GET /grades"
echo "------------------------------"
run_test "Liste des notes" "GET" "/grades" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 12: Download Grades PDF
echo "📄 ENDPOINT 12/23: GET /grades/download"
echo "----------------------------------------"
run_test "Télécharger relevé de notes PDF" "GET" "/grades/download" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 13: Request Grade Review
echo "🔍 ENDPOINT 13/23: POST /grades/{gradeId}/review"
echo "------------------------------------------------"
run_test "Demander révision de note" "POST" "/grades/$GRADE_ID/review" '{"reason":"Je demande une révision de cette note"}' "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 6: Cours ====================
echo "📚 SECTION 6: Cours"
echo "------------------"

# ENDPOINT 14: Get Courses
echo "📚 ENDPOINT 14/23: GET /courses"
echo "------------------------------"
run_test "Liste des cours" "GET" "/courses" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 15: Get Course Details
echo "📖 ENDPOINT 15/23: GET /courses/{courseId}/details"
echo "---------------------------------------------------"
run_test "Détails du cours" "GET" "/courses/$COURSE_ID/details" "" "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 7: Devoirs ====================
echo "📋 SECTION 7: Devoirs"
echo "--------------------"

# ENDPOINT 16: Get Homeworks
echo "📋 ENDPOINT 16/23: GET /homeworks"
echo "---------------------------------"
run_test "Liste des devoirs" "GET" "/homeworks" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 17: Submit Homework (multipart)
echo "📤 ENDPOINT 17/23: POST /courses/{courseId}/homeworks/{homeworkId}/submit"
echo "-------------------------------------------------------------------------"
echo "Création d'un fichier de test pour devoir..."
echo "Test homework content" > /tmp/test_homework.txt

if [ -f /tmp/test_homework.txt ]; then
    run_test "Soumettre un devoir" "POST" "/courses/$COURSE_ID/homeworks/$HOMEWORK_ID/submit" "" "file=@/tmp/test_homework.txt" "$AUTH_TOKEN"
    rm /tmp/test_homework.txt
else
    log_test "Soumettre devoir" "⚠️ SKIP" "Impossible de créer le fichier de test" ""
    ((SKIPPED_TESTS++))
fi
echo ""

# ENDPOINT 18: Start Homework
echo "▶️ ENDPOINT 18/23: POST /homeworks/{homeworkId}/start"
echo "-----------------------------------------------------"
run_test "Commencer un devoir" "POST" "/homeworks/$HOMEWORK_ID/start" "" "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 19: Update Homework Progress
echo "📊 ENDPOINT 19/23: POST /homeworks/{homeworkId}/progress"
echo "---------------------------------------------------------"
run_test "Mettre à jour progression devoir" "POST" "/homeworks/$HOMEWORK_ID/progress" '{"progress":50}' "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 8: Quiz & Tuteur IA ====================
echo "🎯 SECTION 8: Quiz & Tuteur IA"
echo "------------------------------"

# ENDPOINT 20: Submit Quiz
echo "🎯 ENDPOINT 20/23: POST /courses/{courseId}/quizzes/submit"
echo "----------------------------------------------------------"
run_test "Soumettre un quiz" "POST" "/courses/$COURSE_ID/quizzes/submit" '{"answers":[{"questionId":"q1","answer":"A"}]}' "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 21: Chat with Tutor
echo "🤖 ENDPOINT 21/23: POST /tutor/chat"
echo "-----------------------------------"
run_test "Chat avec tuteur IA" "POST" "/tutor/chat" '{"message":"Explique-moi l architecture hexagonale"}' "" "$AUTH_TOKEN"
echo ""

# ==================== SECTION 9: Emails ====================
echo "📧 SECTION 9: Emails"
echo "-------------------"

# ENDPOINT 22: Send Notification
echo "📧 ENDPOINT 22/23: POST /send-notification"
echo "------------------------------------------"
run_test "Envoyer notification" "POST" "/send-notification" '{"to":"professeur@ecole221.sn","subject":"Question sur le cours","message":"Pouvez-vous m expliquer le chapitre 2?"}' "" "$AUTH_TOKEN"
echo ""

# ENDPOINT 23: Request Confirmation
echo "📧 ENDPOINT 23/23: POST /request-confirmation"
echo "---------------------------------------------"
run_test "Demander confirmation" "POST" "/request-confirmation" '{"type":"INSCRIPTION","message":"Je confirme mon inscription"}' "" "$AUTH_TOKEN"
echo ""

# ==================== RÉSUMÉ ====================
echo "=================================================="
echo "📊 RÉSUMÉ DES TESTS"
echo "=================================================="
echo "Total tests: $TOTAL_TESTS"
echo "Réussis: $PASSED_TESTS"
echo "Échoués: $FAILED_TESTS"
echo "Ignorés: $SKIPPED_TESTS"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 TOUS LES TESTS SONT PASSÉS!"
else
    echo "⚠️ CERTAINS TESTS ONT ÉCHOUÉ. Voir $ERROR_LOG pour les détails."
fi

echo ""
echo "📄 Rapport détaillé: $LOG_FILE"
echo "❌ Erreurs: $ERROR_LOG"

exit $FAILED_TESTS
