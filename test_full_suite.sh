#!/bin/bash
BASE="http://127.0.0.1:8080/api/api"
PASS=0
FAIL=0

function test_endpoint {
    local METHOD=${1:-GET}
    local ENDPOINT=$2
    local COOKIE_JAR=$3
    local BODY=$4
    local EXPECTED=${5:-200}

    if [ -n "$BODY" ]; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X $METHOD -b "$COOKIE_JAR" -H "Content-Type: application/json" -d "$BODY" "$BASE$ENDPOINT")
    else
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X $METHOD -b "$COOKIE_JAR" "$BASE$ENDPOINT")
    fi

    if [ "$HTTP_CODE" -eq "$EXPECTED" ] || [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
        echo "  ✅ $METHOD $ENDPOINT : $HTTP_CODE"
        PASS=$((PASS+1))
    else
        echo "  ❌ $METHOD $ENDPOINT : $HTTP_CODE"
        FAIL=$((FAIL+1))
    fi
}

echo "============================================"
echo "   🎓 TEST ÉTUDIANT (student@ecole221.sn)"
echo "============================================"
curl -s -c cookies_student.txt -X POST -H "Content-Type: application/json" -d '{"email":"etudiant221@gmail.com","password":"ecole221"}' $BASE/auth/login > /dev/null
test_endpoint GET "/student/profile" cookies_student.txt
test_endpoint GET "/student/dashboard" cookies_student.txt
test_endpoint GET "/student/digital-badge" cookies_student.txt
test_endpoint GET "/student/attendances?page=0&size=10" cookies_student.txt
test_endpoint POST "/student/attendances" cookies_student.txt '{"date":"2026-07-20","status":"PRESENT","courseId":"course-1"}'
test_endpoint GET "/student/live-sessions?page=0&size=10" cookies_student.txt
test_endpoint GET "/student/live-sessions/sess-1/chat-history" cookies_student.txt
test_endpoint POST "/student/live-sessions/sess-1/reaction" cookies_student.txt '{"type":"LIKE"}'
test_endpoint GET "/student/grades" cookies_student.txt
test_endpoint POST "/student/grades/g-1/review" cookies_student.txt '{"reason":"Erreur de saisie"}'
test_endpoint GET "/student/courses" cookies_student.txt
test_endpoint GET "/student/homeworks" cookies_student.txt
test_endpoint POST "/student/courses/course-1/quizzes/submit" cookies_student.txt '{"answers":[{"selectedOption":1,"correctOption":1}]}'
test_endpoint GET "/student/courses/course-1/details" cookies_student.txt

echo ""
echo "============================================"
echo "   👨‍🏫 TEST PROFESSEUR (prof@ecole221.sn)"
echo "============================================"
curl -s -c cookies_prof.txt -X POST -H "Content-Type: application/json" -d '{"email":"professeur221@gmail.com","password":"ecole221"}' $BASE/auth/login > /dev/null
test_endpoint GET "/professor/courses?page=0&size=10" cookies_prof.txt
test_endpoint GET "/professor/courses/course-1/homeworks?page=0&size=10" cookies_prof.txt
test_endpoint POST "/professor/courses/course-1/homeworks" cookies_prof.txt '{"title":"DM1","description":"Exercices chapitre 3","priority":"HIGH","deadline":"2026-07-30"}'
test_endpoint GET "/professor/courses/course-1/lessons?page=0&size=10" cookies_prof.txt
test_endpoint POST "/professor/courses/course-1/lessons" cookies_prof.txt '{"title":"Intro JPA","duration":90,"date":"2026-07-25"}'
test_endpoint GET "/professor/courses/course-1/modules?page=0&size=10" cookies_prof.txt
test_endpoint POST "/professor/courses/course-1/modules" cookies_prof.txt '{"name":"Module Spring Boot"}'
test_endpoint GET "/professor/courses/course-1/quizzes?page=0&size=10" cookies_prof.txt
test_endpoint POST "/professor/courses/course-1/quizzes" cookies_prof.txt '{"title":"Quiz JPA","duration":30}'
test_endpoint GET "/professor/schedule" cookies_prof.txt
test_endpoint POST "/prof/attendance/submit" cookies_prof.txt '{"date":"2026-07-20","studentId":"usr-001","studentName":"Mamadou Sall","status":"PRESENT","courseId":"course-1"}'
test_endpoint GET "/professor/courses/course-1/students" cookies_prof.txt
test_endpoint POST "/professor/courses/course-1/grades/usr-001" cookies_prof.txt '{"cc":16.5,"exam":18.0}'
test_endpoint POST "/professor/modules/module-1/quizzes" cookies_prof.txt '{"title":"Quiz Module","duration":20}'
test_endpoint GET "/prof/classes/M1%20GL/students" cookies_prof.txt
test_endpoint POST "/professor/schedule/sess-1/cancel" cookies_prof.txt '{"reason":"Maladie"}'
test_endpoint POST "/professor/schedule/sess-1/reschedule" cookies_prof.txt '{"day":"Mardi","time":"10:00","room":"Salle 103"}'

echo ""
echo "============================================"
echo "   🔒 TEST ADMIN (admin@ecole221.sn)"
echo "============================================"
curl -s -c cookies_admin.txt -X POST -H "Content-Type: application/json" -d '{"email":"admin221@gmail.com","password":"ecole221"}' $BASE/auth/login > /dev/null
test_endpoint GET "/planning/rooms" cookies_admin.txt
test_endpoint GET "/planning/filieres" cookies_admin.txt
test_endpoint GET "/planning/classes" cookies_admin.txt
test_endpoint GET "/planning/slots" cookies_admin.txt
test_endpoint GET "/admin/realtime-logs" cookies_admin.txt
test_endpoint POST "/admin/gates/assign" cookies_admin.txt '{"gate":"Gate A","vigilId":"usr-004"}'
test_endpoint PUT "/planning/rooms" cookies_admin.txt '[{"id":"room-1","name":"Salle 101","capacity":30}]'
test_endpoint PUT "/planning/filieres" cookies_admin.txt '[{"id":"fil-1","name":"Genie Logiciel"}]'
test_endpoint PUT "/planning/classes" cookies_admin.txt '[{"id":"class-1","name":"M1 GL"}]'
test_endpoint PUT "/planning/slots" cookies_admin.txt '[{"id":"slot-1","day":"Lundi"}]'
test_endpoint PATCH "/admin/students/usr-001/status" cookies_admin.txt '{"statutFrais":"PAYE"}'
test_endpoint GET "/candidatures/settings" cookies_admin.txt
test_endpoint POST "/admin/candidatures/settings" cookies_admin.txt '{"ouvert":true}'

echo ""
echo "============================================"
echo "   👮 TEST VIGIL (vigil@ecole221.sn)"
echo "============================================"
curl -s -c cookies_vigil.txt -X POST -H "Content-Type: application/json" -d '{"email":"vigile221@gmail.com","password":"ecole221"}' $BASE/auth/login > /dev/null
test_endpoint GET "/security/checkpoint-status" cookies_vigil.txt
test_endpoint GET "/vigil/check-ins" cookies_vigil.txt
test_endpoint POST "/security/scan-logs" cookies_vigil.txt '{"studentId":"usr-001","checkpointId":"gate-A","timestamp":"2026-07-20T08:00:00"}'
test_endpoint GET "/collaboration/workgroups" cookies_vigil.txt

echo ""
echo "============================================"
echo "   🤝 TEST COLLABORATION & NOTIFS"
echo "============================================"
test_endpoint POST "/collaboration/workgroups" cookies_student.txt '{"name":"Projet Annuel"}'
test_endpoint GET "/collaboration/workgroups/grp-1/documents" cookies_student.txt
test_endpoint POST "/collaboration/workgroups/grp-1/documents" cookies_student.txt '{"name":"Cahier de charges"}'
test_endpoint GET "/collaboration/workgroups/grp-1/tasks" cookies_student.txt
test_endpoint POST "/collaboration/workgroups/grp-1/tasks" cookies_student.txt '{"title":"Faire le MCD"}'
test_endpoint GET "/collaboration/workgroups/grp-1/messages" cookies_student.txt
test_endpoint POST "/collaboration/workgroups/grp-1/messages" cookies_student.txt '{"content":"Bonjour équipe"}'
test_endpoint GET "/collaboration/meets" cookies_student.txt
test_endpoint POST "/collaboration/meets" cookies_student.txt '{"title":"Point hebdo"}'
test_endpoint GET "/collaboration/admin-meets" cookies_admin.txt
test_endpoint GET "/api/notifications" cookies_student.txt
test_endpoint POST "/sync" cookies_student.txt '[]'
test_endpoint GET "/auth/me" cookies_student.txt
test_endpoint POST "/auth/logout" cookies_student.txt

echo ""
echo "============================================"
echo "   📊 RÉSULTATS FINAUX"
echo "============================================"
echo "  ✅ Passés  : $PASS"
echo "  ❌ Échoués : $FAIL"
echo "  Total     : $((PASS+FAIL))"
