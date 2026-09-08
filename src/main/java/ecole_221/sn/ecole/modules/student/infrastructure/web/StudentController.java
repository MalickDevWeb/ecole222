package ecole_221.sn.ecole.modules.student.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecole_221.sn.ecole.modules.student.application.port.dto.*;
import ecole_221.sn.ecole.partage.dto.PageResponseDto;
import ecole_221.sn.ecole.partage.exception.CourseNotFoundException;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.student.application.service.StudentMapper;
import ecole_221.sn.ecole.modules.student.domain.valueobject.ReactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
@CrossOrigin(origins = "*")
@Tag(name = "Student", description = "Espace etudiant — profil, dashboard, presences, sessions live")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    private final StudentUseCase studentUseCase;
    private final ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository courseDetailRepository;
    private final ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository studentDataRepository;
    private final GradesReportService gradesReportService;
    private final ActionLogger actionLogger;
    private final ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase aiUseCase;
    private final ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase emailUseCase;

    public StudentController(
            StudentUseCase studentUseCase,
            ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository courseDetailRepository,
            ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository studentDataRepository,
            GradesReportService gradesReportService,
            ActionLogger actionLogger,
            ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase aiUseCase,
            ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase emailUseCase) {
        this.studentUseCase = studentUseCase;
        this.courseDetailRepository = courseDetailRepository;
        this.studentDataRepository = studentDataRepository;
        this.gradesReportService = gradesReportService;
        this.actionLogger = actionLogger;
        this.aiUseCase = aiUseCase;
        this.emailUseCase = emailUseCase;
    }

    // ==================== PROFILE & DASHBOARD ====================

    @GetMapping("/profile")
    @Operation(summary = "Profil etudiant", description = "Retourne les informations du profil de l'etudiant depuis la BDD.")
    public ResponseEntity<StudentProfileDto> getProfile() {
        logger.info("GET /student/profile - Recuperation du profil etudiant");
        Map<String, Object> profileMap = studentDataRepository.findById("usr-001")
                .map(data -> data.getProfile())
                .orElseGet(() -> studentUseCase.getStudentProfile("usr-001"));
        
        return ResponseEntity.ok(StudentMapper.toProfileDto(profileMap));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord", description = "Retourne les donnees du dashboard depuis la BDD.")
    public ResponseEntity<StudentDashboardDto> getDashboard() {
        logger.info("GET /student/dashboard - Recuperation du dashboard etudiant");
        Map<String, Object> dashboardMap = studentDataRepository.findById("usr-001")
                .map(data -> data.getDashboard())
                .orElseGet(() -> studentUseCase.getStudentDashboard("usr-001"));
        
        return ResponseEntity.ok(StudentMapper.toDashboardDto(dashboardMap));
    }

    // ==================== DIGITAL BADGE ====================

    @GetMapping("/digital-badge")
    @Operation(summary = "Badge numerique dynamique", description = "Retourne le badge QR avec token HMAC rotatif (expire chaque minute).")
    public ResponseEntity<DigitalBadgeDto> getDigitalBadge() {
        logger.info("GET /student/digital-badge - Generation du badge numerique");
        Map<String, Object> badgeMap = studentUseCase.getDigitalBadge("usr-001");
        return ResponseEntity.ok(StudentMapper.toDigitalBadgeDto(badgeMap));
    }

    // ==================== QR VALIDATION ====================

    @PostMapping("/qr-validate")
    @Operation(summary = "Valider un QR etudiant", description = "Verifie la validite d'un token QR scanne (utilise par le vigile).")
    public ResponseEntity<Map<String, Object>> validateQr(@RequestBody Map<String, Object> body) {
        logger.info("POST /student/qr-validate - Validation QR");
        String matricule = String.valueOf(body.get("matricule"));
        long minuteEpoch = Long.parseLong(String.valueOf(body.get("minuteEpoch")));
        String token = String.valueOf(body.get("token"));

        // Validation sans authentification requise pour permettre aux vigiles de scanner
        boolean valid = studentUseCase.validateQrToken(matricule, minuteEpoch, token);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        result.put("matricule", matricule);
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        result.put("message", valid ? "Badge etudiant verifie avec succes" : "Token QR expire ou invalide");
        return ResponseEntity.ok(result);
    }

    // ==================== ATTENDANCES ====================

    @GetMapping("/attendances")
    @Operation(summary = "Liste des presences", description = "Retourne l'historique des presences de l'etudiant.")
    public ResponseEntity<PageResponseDto<AttendanceDto>> getAttendances(
            @Parameter(description = "Numero de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        logger.info("GET /student/attendances - Historique des presences (page={}, size={})", page, size);
        
        return ResponseEntity.ok(StudentMapper.toAttendancePageResponse(
            studentUseCase.getStudentAttendances("usr-001", page, size)
        ));
    }

    @PostMapping("/attendances")
    @Operation(summary = "Auto check-in", description = "Soumet une auto-declaration de presence.")
    public ResponseEntity<Map<String, Object>> selfCheckIn(@Valid @RequestBody AttendanceRequestDto request) {
        logger.info("POST /student/attendances - Auto check-in: type={}, method={}", request.type(), request.method());
        studentUseCase.submitSelfAttendance(
            "usr-001", 
            request.type().name(), 
            request.method()
        );

        actionLogger.logStudentAction("SELF_CHECKIN", "usr-001", "Student", 
            "Auto check-in: " + request.type() + " via " + request.method());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("attendanceId", "att-" + System.currentTimeMillis() % 10000);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("status", "VALIDATED");
        return ResponseEntity.ok(response);
    }

    // ==================== LIVE SESSIONS ====================

    @GetMapping("/live-sessions")
    @Operation(summary = "Sessions live actives", description = "Retourne la liste des sessions de cours en direct disponibles.")
    public ResponseEntity<PageResponseDto<LiveSessionDto>> getLiveSessions(
            @Parameter(description = "Numero de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        logger.info("GET /student/live-sessions - Liste des sessions live (page={}, size={})", page, size);
        
        return ResponseEntity.ok(StudentMapper.toLiveSessionPageResponse(
            studentUseCase.getActiveLiveSessions(page, size)
        ));
    }

    @PostMapping("/live-sessions/{sessionId}/reaction")
    @Operation(summary = "Envoyer une reaction", description = "Envoie une reaction emoji a une session live.")
    public ResponseEntity<ReactionResponseDto> submitReaction(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody ReactionRequestDto request) {
        logger.info("POST /student/live-sessions/{}/reaction - Reaction: {}", sessionId, request.type());
        
        studentUseCase.submitReaction(sessionId, request.type().name());
        
        return ResponseEntity.ok(StudentMapper.toReactionResponseDto(sessionId, request.type()));
    }

    // ==================== GRADES ====================

    @GetMapping("/grades")
    @Operation(summary = "Notes de l'etudiant", description = "Retourne la liste des notes depuis la BDD.")
    public ResponseEntity<List<GradeDto>> getGrades() {
        logger.info("GET /student/grades - Recuperation des notes");
        List<Map<String, Object>> gradesMap = studentUseCase.getStudentGrades("usr-001");
        
        return ResponseEntity.ok(StudentMapper.toGradeDtoList(gradesMap));
    }

    @GetMapping("/grades/download")
    @Operation(summary = "Telecharger le releve PDF", description = "Genere et retourne un releve de notes officiel au format PDF.")
    public ResponseEntity<byte[]> downloadGradesPdf() {
        logger.info("GET /student/grades/download - Telechargement du releve PDF");
        List<Map<String, Object>> gradesMap = studentUseCase.getStudentGrades("usr-001");

        double totalEcts = gradesMap.stream().mapToDouble(g -> ((Number) g.get("ects")).doubleValue()).sum();
        double weightedSum = gradesMap.stream().mapToDouble(g -> {
            double cc = ((Number) g.get("cc")).doubleValue();
            double examen = ((Number) g.get("examen")).doubleValue();
            double ects = ((Number) g.get("ects")).doubleValue();
            double note = (cc * 0.4) + (examen * 0.6);
            return note * ects;
        }).sum();
        double average = totalEcts > 0 ? weightedSum / totalEcts : 0;
        double gpa = average >= 16 ? 4.0 : average >= 14 ? 3.7 : average >= 12 ? 3.3 : average >= 10 ? 3.0 : 0.0;
        int validatedEcts = (int) gradesMap.stream()
                .filter(g -> {
                    double cc = ((Number) g.get("cc")).doubleValue();
                    double examen = ((Number) g.get("examen")).doubleValue();
                    return (cc * 0.4) + (examen * 0.6) >= 10;
                })
                .mapToDouble(g -> ((Number) g.get("ects")).doubleValue())
                .sum();

        byte[] pdf = gradesReportService.generateTranscriptPdf(
                "Mamadou Sall",
                "Genie Logiciel & Systemes d'Information",
                "Licence 3 (L3)",
                average, gpa, validatedEcts,
                gradesMap);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"releve_notes_officiel_ecole221_S2.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/grades/{gradeId}/review")
    @Operation(summary = "Demande de revision de note", description = "L'etudiant soumet une contestation motivee pour une note.")
    public ResponseEntity<Map<String, Object>> requestGradeReview(
            @PathVariable("gradeId") String gradeId,
            @Valid @RequestBody GradeReviewRequestDto request) {
        logger.info("POST /student/grades/{}/review - Demande de revision: reason={}", gradeId, request.reason());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("gradeId", gradeId);
        response.put("reason", request.reason());
        response.put("status", "PENDING_REVIEW");
        return ResponseEntity.ok(response);
    }

    // ==================== COURSES ====================

    @GetMapping("/courses")
    @Operation(summary = "Cours de l'etudiant", description = "Retourne la liste des cours depuis la BDD.")
    public ResponseEntity<List<CourseDto>> getCourses() {
        logger.info("GET /student/courses - Recuperation des cours");
        List<Map<String, Object>> coursesMap = studentUseCase.getStudentCourses("usr-001");
        
        return ResponseEntity.ok(StudentMapper.toCourseDtoList(coursesMap));
    }

    @GetMapping("/courses/{courseId}/details")
    @Operation(summary = "Details du cours", description = "Retourne les details d'un cours.")
    public ResponseEntity<Map<String, Object>> getCourseDetails(@PathVariable("courseId") String courseId) {
        logger.info("GET /student/courses/{}/details - Details du cours", courseId);
        String legacyCourseId = courseId != null && courseId.startsWith("course-")
                ? "c-" + courseId.substring("course-".length())
                : courseId;

        return courseDetailRepository.findById(courseId)
                .or(() -> courseDetailRepository.findById(legacyCourseId))
                .map(detail -> ResponseEntity.ok(detail.getData()))
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    // ==================== HOMEWORKS ====================

    @GetMapping("/homeworks")
    @Operation(summary = "Devoirs de l'etudiant", description = "Retourne la liste des devoirs de l'etudiant.")
    public ResponseEntity<List<HomeworkDto>> getStudentHomeworks() {
        logger.info("GET /student/homeworks - Recuperation des devoirs");
        List<Map<String, Object>> homeworksMap = studentUseCase.getStudentHomeworks("usr-001");
        
        return ResponseEntity.ok(StudentMapper.toHomeworkDtoList(homeworksMap));
    }

    @PostMapping(value = "/courses/{courseId}/homeworks/{homeworkId}/submit", consumes = "multipart/form-data")
    @Operation(summary = "Soumettre un devoir", description = "L'etudiant soumet un fichier pour evaluation.")
    public ResponseEntity<Map<String, Object>> submitHomework(
            @PathVariable("courseId") String courseId,
            @PathVariable("homeworkId") String homeworkId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        logger.info("POST /student/courses/{}/homeworks/{}/submit - Soumission devoir: file={}", courseId, homeworkId, file.getOriginalFilename());

        String fileName = file.getOriginalFilename();
        studentUseCase.submitHomework(homeworkId, fileName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("homeworkId", homeworkId);
        response.put("fileName", fileName);
        response.put("size", file.getSize());
        response.put("status", "Soumis");
        return ResponseEntity.ok(response);
    }

    // ==================== QUIZZES ====================

    @PostMapping("/courses/{courseId}/quizzes/submit")
    @Operation(summary = "Soumettre un quiz", description = "L'etudiant soumet ses reponses au quiz pour evaluation.")
    public ResponseEntity<QuizSubmissionResponseDto> submitQuiz(
            @PathVariable String courseId,
            @RequestBody Map<String, Object> body) {
        logger.info("POST /student/courses/{}/quizzes/submit - Soumission quiz", courseId);

        @SuppressWarnings("unchecked")
        List<Map<String, Integer>> answers = (List<Map<String, Integer>>) body.get("answers");
        int correctCount = 0;
        int total = 0;

        if (answers != null) {
            total = answers.size();
            for (Map<String, Integer> answer : answers) {
                Integer selected = answer.get("selectedOption");
                Integer correct = answer.get("correctOption");
                if (selected != null && correct != null && selected.equals(correct)) {
                    correctCount++;
                }
            }
        }

        return ResponseEntity.ok(StudentMapper.toQuizSubmissionResponseDto(
            courseId, correctCount, total, "Quiz evalue avec succes par le serveur !"
        ));
    }

    // ==================== TUTOR CHAT ====================

    @PostMapping("/tutor/chat")
    @Operation(summary = "Chat avec Tuteur IA", description = "L'etudiant converse avec son tuteur IA.")
    public ResponseEntity<Map<String, Object>> chatWithTutor(@RequestBody Map<String, Object> body) {
        logger.info("POST /student/tutor/chat - Message au tuteur IA");
        String message = (String) body.get("message");
        
        // Utiliser le nouveau service IA
        String aiResponse = aiUseCase.generateTutorResponse(message);
        
        Map<String, Object> chatResponse = new HashMap<>();
        chatResponse.put("text", aiResponse);
        chatResponse.put("provider", aiUseCase.getAIServiceInfo().get("provider"));
        chatResponse.put("available", aiUseCase.isAIServiceAvailable());
        
        return ResponseEntity.ok(chatResponse);
    }
    
    // ==================== EMAIL NOTIFICATIONS ====================
    
    @PostMapping("/send-notification")
    @Operation(summary = "Envoyer une notification email", description = "L'etudiant peut s'envoyer une notification email.")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> body) {
        logger.info("POST /student/send-notification - Envoi de notification email");
        
        String to = (String) body.getOrDefault("to", "etudiant221@gmail.com");
        String subject = (String) body.get("subject");
        String content = (String) body.get("content");
        
        boolean success = emailUseCase.sendStudentNotification(to, subject, content);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Notification envoyée avec succès" : "Échec de l'envoi");
        response.put("provider", emailUseCase.getEmailServiceInfo().get("provider"));
        response.put("available", emailUseCase.isEmailServiceAvailable());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/request-confirmation")
    @Operation(summary = "Demander un email de confirmation", description = "L'etudiant peut demander un email de confirmation.")
    public ResponseEntity<Map<String, Object>> requestConfirmation(@RequestBody Map<String, Object> body) {
        logger.info("POST /student/request-confirmation - Demande de confirmation email");
        
        String to = (String) body.getOrDefault("to", "etudiant221@gmail.com");
        String type = (String) body.getOrDefault("type", "inscription");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.getOrDefault("data", new HashMap<>());
        
        boolean success = emailUseCase.sendConfirmationEmail(to, type, data);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Confirmation envoyée avec succès" : "Échec de l'envoi");
        response.put("provider", emailUseCase.getEmailServiceInfo().get("provider"));
        response.put("available", emailUseCase.isEmailServiceAvailable());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/live-sessions/{sessionId}/chat")
    @Operation(summary = "Chat live session", description = "Retourne les messages du chat d'une session live.")
    public ResponseEntity<List<Map<String, Object>>> getLiveChat(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId) {
        logger.info("GET /student/live-sessions/{}/chat", sessionId);
        List<Map<String, Object>> messages = studentUseCase.getLiveChatMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/live-sessions/{sessionId}/chat")
    @Operation(summary = "Envoyer message chat", description = "Envoie un message dans le chat d'une session live.")
    public ResponseEntity<Map<String, Object>> sendLiveChat(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @RequestBody Map<String, String> body) {
        logger.info("POST /student/live-sessions/{}/chat - user={}, text={}", sessionId, body.get("user"), body.get("text"));
        
        String userId = "usr-001";
        String userName = body.get("user");
        String userRole = "Student";
        String text = body.get("text");
        
        studentUseCase.saveLiveChatMessage(sessionId, userId, userName, userRole, text);
        
        actionLogger.logStudentAction("SEND_CHAT", "usr-001", "Student", 
            "Message envoyé dans session " + sessionId);
        return ResponseEntity.ok(Map.of("success", true, "id", "msg-" + System.currentTimeMillis()));
    }

    @PostMapping("/homeworks/{homeworkId}/start")
    @Operation(summary = "Démarrer devoir", description = "Marque un devoir comme en cours.")
    public ResponseEntity<Map<String, Object>> startHomework(
            @Parameter(description = "Identifiant du devoir") @PathVariable("homeworkId") String homeworkId) {
        logger.info("POST /student/homeworks/{}/start", homeworkId);
        studentUseCase.updateHomeworkProgress(homeworkId, 0);
        actionLogger.logStudentAction("START_HOMEWORK", "usr-001", "Student", 
            "Devoir démarré: " + homeworkId);
        return ResponseEntity.ok(Map.of("success", true, "homeworkId", homeworkId));
    }

    @PostMapping("/homeworks/{homeworkId}/progress")
    @Operation(summary = "Avancement devoir", description = "Met à jour l'avancement d'un devoir.")
    public ResponseEntity<Map<String, Object>> updateHomeworkProgress(
            @Parameter(description = "Identifiant du devoir") @PathVariable("homeworkId") String homeworkId,
            @RequestBody Map<String, Number> body) {
        logger.info("POST /student/homeworks/{}/progress - addedProgress={}", homeworkId, body.get("addedProgress"));
        int progress = body.get("addedProgress") != null ? body.get("addedProgress").intValue() : 0;
        studentUseCase.updateHomeworkProgress(homeworkId, progress);
        actionLogger.logStudentAction("UPDATE_HOMEWORK_PROGRESS", "usr-001", "Student", 
            "Avancement mis à jour pour devoir: " + homeworkId);
        return ResponseEntity.ok(Map.of("success", true, "homeworkId", homeworkId));
    }
}
