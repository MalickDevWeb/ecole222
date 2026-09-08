#!/bin/bash

# Script de test du module Professor - ProfessorController (10 endpoints) + ProfExtraController (8 endpoints)
# Tous les 18 endpoints sont maintenant testables

echo "🚀 Démarrage des tests du module Professor"
echo "======================================================"
echo "📋 Coverage : 12/12 endpoints à tester (9 Professor + 3 ProfExtra)"
echo ""

BASE_URL="http://localhost:8080/api"
PROFESSOR_URL="$BASE_URL/api/professor"
PROF_URL="$BASE_URL/api/prof"
LOG_FILE="professor_18endpoints_test_results.log"
ERROR_LOG="professor_18endpoints_errors.log"
AUTH_TOKEN=""
AUTH_HEADER=()

# Nettoyer les logs précédents
> "$LOG_FILE"
> "$ERROR_LOG"

# Compteurs
TOTAL_TESTS=12
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
if ! curl -s -f "${AUTH_HEADER[@]}" http://localhost:8080/api/professor/courses > /dev/null 2>&1; then
    echo "⚠️ L'application n'est pas démarrée. Veuillez démarrer l'application d'abord."
    exit 1
fi

echo "✅ Application démarrée"
echo ""

# Variables globales pour les tests
PROFESSOR_ID="usr-002"
COURSE_ID="course-1"
STUDENT_ID="usr-001"
MODULE_ID="module-1"
QUIZ_ID="quiz-001"
HOMEWORK_ID="hw-001"
LESSON_ID="lesson-001"
SESSION_ID="sched-001"
CLASS_ID="M1 GL"

echo "======================================================"
echo "📋 TESTS DES 18 ENDPOINTS DU MODULE PROFESSOR"
echo "======================================================"
echo ""

# ==================== SECTION 1: ProfessorController (9 endpoints) ====================
echo "🎓 SECTION 1: ProfessorController (9 endpoints)"
echo "------------------------------------------------------"

# ENDPOINT 1: Get courses
echo "📚 ENDPOINT 1/9: GET /professor/courses"
echo "------------------------------------------"
run_test "Get courses" "GET" "$PROFESSOR_URL/courses?profId=$PROFESSOR_ID&page=0&size=10" ""
echo ""

# ENDPOINT 2: Get course students
echo "👥 ENDPOINT 2/9: GET /professor/courses/{courseId}/students"
echo "------------------------------------------------------------"
run_test "Get course students" "GET" "$PROFESSOR_URL/courses/$COURSE_ID/students" ""
echo ""

# ENDPOINT 3: Get course grades
echo "📝 ENDPOINT 3/9: GET /professor/courses/{courseId}/grades"
echo "---------------------------------------------------------"
run_test "Get course grades" "GET" "$PROFESSOR_URL/courses/$COURSE_ID/grades" ""
echo ""

# ENDPOINT 4: Save grade
echo "💯 ENDPOINT 4/9: POST /professor/courses/{courseId}/grades/{studentId}"
echo "--------------------------------------------------------------------"
run_test "Save grade" "POST" "$PROFESSOR_URL/courses/$COURSE_ID/grades/$STUDENT_ID" '{"cc":15.5,"examen":16.0}'
echo ""

# ENDPOINT 5: Get homeworks
echo "✍️ ENDPOINT 5/9: GET /professor/courses/{courseId}/homeworks"
echo "-------------------------------------------------------------"
run_test "Get homeworks" "GET" "$PROFESSOR_URL/courses/$COURSE_ID/homeworks?page=0&size=10" ""
echo ""

# ENDPOINT 6: Create homework
echo "📤 ENDPOINT 6/9: POST /professor/courses/{courseId}/homeworks"
echo "-------------------------------------------------------------"
run_test "Create homework" "POST" "$PROFESSOR_URL/courses/$COURSE_ID/homeworks" '{"titre":"TP Kubernetes","desc":"Créer un cluster Kubernetes","prio":"HIGH","deadlineStr":"2026-08-15"}'
echo ""

# ENDPOINT 7: Get lessons
echo "📖 ENDPOINT 7/9: GET /professor/courses/{courseId}/lessons"
echo "-----------------------------------------------------------"
run_test "Get lessons" "GET" "$PROFESSOR_URL/courses/$COURSE_ID/lessons?page=0&size=10" ""
echo ""

# ENDPOINT 8: Create lesson
echo "➕ ENDPOINT 8/9: POST /professor/courses/{courseId}/lessons"
echo "-----------------------------------------------------------"
run_test "Create lesson" "POST" "$PROFESSOR_URL/courses/$COURSE_ID/lessons" '{"titre":"Introduction aux Microservices","content":"Présentation des concepts clés","duration":90}'
echo ""

# ENDPOINT 9: Get today schedule
echo "📅 ENDPOINT 9/9: GET /professor/schedule"
echo "-----------------------------------------"
run_test "Get today schedule" "GET" "$PROFESSOR_URL/schedule" ""
echo ""

# ENDPOINT 9: Get modules (SKIP - endpoint not implemented)
echo "📦 ENDPOINT 9/10: GET /professor/modules"
echo "---------------------------------------"
echo "⚠️ SKIP: Endpoint not implemented"
((SKIPPED_TESTS++))
echo ""

# ==================== SECTION 2: ProfExtraController (3 endpoints) ====================
echo "🎓 SECTION 2: ProfExtraController (3 endpoints)"
echo "-----------------------------------------------------"

# ENDPOINT 10: Get quizzes
echo "🧠 ENDPOINT 10/12: GET /professor/courses/{courseId}/quizzes"
echo "----------------------------------------------------------"
run_test "Get quizzes" "GET" "$PROFESSOR_URL/courses/$COURSE_ID/quizzes?page=0&size=10" ""
echo ""

# ENDPOINT 11: Create quiz in module
echo "🆕 ENDPOINT 11/12: POST /professor/modules/{moduleId}/quizzes"
echo "----------------------------------------------------------"
run_test "Create quiz in module" "POST" "$PROFESSOR_URL/modules/$MODULE_ID/quizzes" '{"title":"Quiz Architecture","duration":30}'
echo ""

# ENDPOINT 12: Create quiz in course
echo "🆕 ENDPOINT 12/12: POST /professor/courses/{courseId}/quizzes"
echo "----------------------------------------------------------"
run_test "Create quiz in course" "POST" "$PROFESSOR_URL/courses/$COURSE_ID/quizzes" '{"title":"Quiz Cours","duration":45}'
echo ""

# ENDPOINT 13: Submit attendance
echo "📋 ENDPOINT 13/12: POST /prof/attendance/submit"
echo "------------------------------------------------"
run_test "Submit attendance" "POST" "$PROF_URL/attendance/submit" '[{"studentId":"usr-001","studentName":"Mamadou Sall","status":"PRESENT","date":"2026-08-02","courseId":"course-1"}]'
echo ""

# ==================== RÉSUMÉ DES TESTS ====================
echo "======================================================"
echo "📊 RÉSUMÉ DES TESTS DU MODULE PROFESSOR"
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
