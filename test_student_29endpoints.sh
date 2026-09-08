#!/bin/bash

# Script de test du module Student - StudentController (23 endpoints) + AdminStudentProfileController (5 endpoints) + LiveSessionMessageController (1 endpoint)
# Tous les 29 endpoints sont maintenant testables

echo "🚀 Démarrage des tests du module Student"
echo "======================================================"
echo "📋 Coverage : 30/30 endpoints à tester (23 Student + 5 AdminStudent + 1 LiveSessionMessage + 1 extra admin)"
echo "📋 Tous les endpoints multipart (homework submit) sont testés"
echo ""

BASE_URL="http://localhost:8080/api"
STUDENT_URL="$BASE_URL/api/student"
ADMIN_URL="$BASE_URL/api/admin"
LOG_FILE="student_29endpoints_test_results.log"
ERROR_LOG="student_29endpoints_errors.log"
AUTH_TOKEN=""
AUTH_HEADER=()

# Nettoyer les logs précédents
> "$LOG_FILE"
> "$ERROR_LOG"

# Compteurs
TOTAL_TESTS=30
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
auth_response=$(curl -s -X POST "http://localhost:8080/api/auth/login-token" \
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
if ! curl -s -f "${AUTH_HEADER[@]}" http://localhost:8080/api/student/profile > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# Variables globales pour les tests
STUDENT_ID="usr-001"
COURSE_ID="sc-1"
HOMEWORK_ID="hw-001"
QUIZ_ID="quiz-001"
SESSION_ID="meet-f1bb6d87"
GRADE_ID="grade-001"
UPLOAD_FILE="dummy.txt"

echo "======================================================"
echo "📋 TESTS DES 29 ENDPOINTS DU MODULE STUDENT"
echo "======================================================"
echo ""

# ==================== SECTION 1: StudentController (23 endpoints) ====================
echo "🎓 SECTION 1: StudentController (23 endpoints)"
echo "--------------------------------------------------"

# ENDPOINT 1: Get student profile
echo "👤 ENDPOINT 1/23: GET /student/profile"
echo "--------------------------------------"
run_test "Get student profile" "GET" "$STUDENT_URL/profile" ""
echo ""

# ENDPOINT 2: Get student dashboard
echo "📊 ENDPOINT 2/23: GET /student/dashboard"
echo "----------------------------------------"
run_test "Get student dashboard" "GET" "$STUDENT_URL/dashboard" ""
echo ""

# ENDPOINT 3: Get digital badge
echo "🎫 ENDPOINT 3/23: GET /student/digital-badge"
echo "--------------------------------------------"
run_test "Get digital badge" "GET" "$STUDENT_URL/digital-badge" ""
echo ""

# ENDPOINT 4: Validate QR
echo "✅ ENDPOINT 4/23: POST /student/qr-validate"
echo "-------------------------------------------"
current_minute=$(($(date +%s) / 60))
run_test "Validate QR" "POST" "$STUDENT_URL/qr-validate" '{"matricule":"MAT-2023-001","minuteEpoch":'$current_minute',"token":"test-token"}'
echo ""

# ENDPOINT 5: Get attendances
echo "📅 ENDPOINT 5/23: GET /student/attendances"
echo "------------------------------------------"
run_test "Get attendances" "GET" "$STUDENT_URL/attendances?page=0&size=10" ""
echo ""

# ENDPOINT 6: Self check-in
echo "📍 ENDPOINT 6/23: POST /student/attendances"
echo "-------------------------------------------"
run_test "Self check-in" "POST" "$STUDENT_URL/attendances" '{"type":"PRESENT","method":"QR_CODE"}'
echo ""

# ENDPOINT 7: Get live sessions
echo "🎥 ENDPOINT 7/23: GET /student/live-sessions"
echo "--------------------------------------------"
run_test "Get live sessions" "GET" "$STUDENT_URL/live-sessions?page=0&size=10" ""
echo ""

# ENDPOINT 8: Send reaction to live session
echo "👍 ENDPOINT 8/23: POST /student/live-sessions/{sessionId}/reaction"
echo "------------------------------------------------------------------"
run_test "Send reaction" "POST" "$STUDENT_URL/live-sessions/$SESSION_ID/reaction" '{"type":"LIKE","emoji":"👍"}'
echo ""

# ENDPOINT 9: Get grades
echo "📝 ENDPOINT 9/23: GET /student/grades"
echo "---------------------------------------"
run_test "Get grades" "GET" "$STUDENT_URL/grades" ""
echo ""

# ENDPOINT 10: Download grades report
echo "📄 ENDPOINT 10/23: GET /student/grades/download"
echo "------------------------------------------------"
echo "Testing: Download grades report"
response=$(curl -s -X GET "$STUDENT_URL/grades/download" \
    "${AUTH_HEADER[@]}" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    log_test "Download grades report" "✅ SUCCESS" "HTTP $http_code - PDF generated" ""
    ((PASSED_TESTS++))
else
    log_test "Download grades report" "❌ ERROR" "HTTP $http_code" "$response"
    ((FAILED_TESTS++))
fi
echo ""

# ENDPOINT 11: Request grade review
echo "🔍 ENDPOINT 11/23: POST /student/grades/{gradeId}/review"
echo "--------------------------------------------------------"
run_test "Request grade review" "POST" "$STUDENT_URL/grades/$GRADE_ID/review" '{"reason":"Request for review","comment":"Please check calculation"}'
echo ""

# ENDPOINT 12: Get courses
echo "📚 ENDPOINT 12/23: GET /student/courses"
echo "---------------------------------------"
run_test "Get courses" "GET" "$STUDENT_URL/courses" ""
echo ""

# ENDPOINT 13: Get course details
echo "📖 ENDPOINT 13/23: GET /student/courses/{courseId}/details"
echo "---------------------------------------------------------"
run_test "Get course details" "GET" "$STUDENT_URL/courses/course-1/details" ""
echo ""

# ENDPOINT 14: Get homeworks
echo "✍️ ENDPOINT 14/23: GET /student/homeworks"
echo "-----------------------------------------"
run_test "Get homeworks" "GET" "$STUDENT_URL/homeworks" ""
echo ""

# ENDPOINT 15: Submit homework (multipart)
echo "📤 ENDPOINT 15/23: POST /student/courses/{courseId}/homeworks/{homeworkId}/submit"
echo "------------------------------------------------------------------------------"
echo "Testing: Submit homework (multipart)"
response=$(curl -s -X POST "$STUDENT_URL/courses/sc-1/homeworks/$HOMEWORK_ID/submit" \
    "${AUTH_HEADER[@]}" \
    -F "file=@$UPLOAD_FILE" \
    -F "comment=Test submission" \
    -w "\n%{http_code}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    log_test "Submit homework" "✅ SUCCESS" "HTTP $http_code" ""
    ((PASSED_TESTS++))
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
else
    log_test "Submit homework" "❌ ERROR" "HTTP $http_code - $body" "$body"
    ((FAILED_TESTS++))
fi
echo ""

# ENDPOINT 16: Submit quiz
echo "🧠 ENDPOINT 16/23: POST /student/courses/{courseId}/quizzes/submit"
echo "-----------------------------------------------------------------"
run_test "Submit quiz" "POST" "$STUDENT_URL/courses/sc-1/quizzes/submit" '{"quizId":"'$QUIZ_ID'","answers":[{"questionId":"q1","answer":"A"}]}'
echo ""

# ENDPOINT 17: Tutor chat
echo "💬 ENDPOINT 17/23: POST /student/tutor/chat"
echo "------------------------------------------"
run_test "Tutor chat" "POST" "$STUDENT_URL/tutor/chat" '{"message":"I need help with the homework"}'
echo ""

# ENDPOINT 18: Send notification
echo "🔔 ENDPOINT 18/23: POST /student/send-notification"
echo "--------------------------------------------------"
run_test "Send notification" "POST" "$STUDENT_URL/send-notification" '{"recipientId":"user-002","content":"Test notification"}'
echo ""

# ENDPOINT 19: Request confirmation
echo "📧 ENDPOINT 19/23: POST /student/request-confirmation"
echo "----------------------------------------------------"
run_test "Request confirmation" "POST" "$STUDENT_URL/request-confirmation" '{"type":"EMAIL","target":"test@example.com"}'
echo ""

# ENDPOINT 20: Get live session chat
echo "💭 ENDPOINT 20/23: GET /student/live-sessions/{sessionId}/chat"
echo "-------------------------------------------------------------"
run_test "Get live session chat" "GET" "$STUDENT_URL/live-sessions/$SESSION_ID/chat" ""
echo ""

# ENDPOINT 21: Post to live session chat
echo "📝 ENDPOINT 21/23: POST /student/live-sessions/{sessionId}/chat"
echo "--------------------------------------------------------------"
run_test "Post to live session chat" "POST" "$STUDENT_URL/live-sessions/$SESSION_ID/chat" '{"content":"Test message","senderId":"'$STUDENT_ID'","senderName":"Test Student"}'
echo ""

# ENDPOINT 22: Start homework
echo "▶️ ENDPOINT 22/23: POST /student/homeworks/{homeworkId}/start"
echo "------------------------------------------------------------"
run_test "Start homework" "POST" "$STUDENT_URL/homeworks/$HOMEWORK_ID/start" '{"startTime":"'$(date -Iseconds)'"}'
echo ""

# ENDPOINT 23: Update homework progress
echo "📊 ENDPOINT 23/23: POST /student/homeworks/{homeworkId}/progress"
echo "----------------------------------------------------------------"
run_test "Update homework progress" "POST" "$STUDENT_URL/homeworks/$HOMEWORK_ID/progress" '{"progress":50,"timeSpent":30}'
echo ""

# ==================== SECTION 2: AdminStudentProfileController (5 endpoints) ====================
echo "🎓 SECTION 2: AdminStudentProfileController (5 endpoints)"
echo "---------------------------------------------------------"

# ENDPOINT 24: Get student profile 360
echo "📋 ENDPOINT 24/28: GET /admin/students/{id}/profile360"
echo "------------------------------------------------------"
run_test "Get student profile 360" "GET" "$ADMIN_URL/students/$STUDENT_ID/profile360" ""
echo ""

# ENDPOINT 25: Update student profile 360
echo "✏️ ENDPOINT 25/28: POST /admin/students/{id}/profile360"
echo "------------------------------------------------------"
run_test "Update student profile 360" "POST" "$ADMIN_URL/students/$STUDENT_ID/profile360" '{"admissionDetails":{"dateInscription":"15/09/2023","lyceeOrigine":"Lycée Test","bacSerie":"S1","bacMention":"Bien","statut":"ADMIS"}}'
echo ""

# ENDPOINT 26: Get student documents
echo "📁 ENDPOINT 26/28: GET /admin/students/{id}/documents"
echo "----------------------------------------------------"
run_test "Get student documents" "GET" "$ADMIN_URL/students/$STUDENT_ID/documents" ""
echo ""

# ENDPOINT 27: Add student document
echo "➕ ENDPOINT 27/28: POST /admin/students/{id}/documents"
echo "----------------------------------------------------"
run_test "Add student document" "POST" "$ADMIN_URL/students/$STUDENT_ID/documents" '{"name":"Transcript","size":"2 Mo","url":"http://example.com/transcript.pdf"}'
echo ""

# ENDPOINT 28: Generate student diagnostic
echo "🔬 ENDPOINT 28/28: POST /admin/student-diagnostic"
echo "-------------------------------------------------"
run_test "Generate student diagnostic" "POST" "$ADMIN_URL/student-diagnostic" '{"studentId":"'$STUDENT_ID'"}'
echo ""

# ENDPOINT 29: Assign schedule
echo "📅 ENDPOINT 29/29: POST /admin/schedule/assign"
echo "----------------------------------------------"
run_test "Assign schedule" "POST" "$ADMIN_URL/schedule/assign" '{"courseId":"sc-1","day":"Monday","time":"09:00","room":"Room A"}'
echo ""

# ==================== SECTION 3: LiveSessionMessageController (1 endpoint) ====================
echo "💬 SECTION 3: LiveSessionMessageController (1 endpoint)"
echo "------------------------------------------------------"

# ENDPOINT 30: Get chat history
echo "📜 ENDPOINT 30/30: GET /student/live-sessions/{sessionId}/chat-history"
echo "---------------------------------------------------------------------"
run_test "Get chat history" "GET" "$STUDENT_URL/live-sessions/$SESSION_ID/chat-history" ""
echo ""

# ==================== RÉSUMÉ DES TESTS ====================
echo "======================================================"
echo "📊 RÉSUMÉ DES TESTS DU MODULE STUDENT"
echo "======================================================"
echo "📋 Total tests: $TOTAL_TESTS"
echo "✅ Réussis: $PASSED_TESTS"
echo "❌ Échoués: $FAILED_TESTS"
echo "⚠️ Ignorés: $SKIPPED_TESTS"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 TOUS LES TESTS SONT PASSÉS !"
    exit 0
else
    echo "⚠️ CERTAINS TESTS ONT ÉCHOUÉ. Consultez $ERROR_LOG pour les détails."
    exit 1
fi
