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
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "Student", description = "Espace etudiant — profil, dashboard, presences, sessions live")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired(required = false)
    private ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MeetJpaRepository meetJpaRepository;

    /** Affectation réelle élève → classe (filtrée par école). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository inscriptions;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classesEcole;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.FiliereJpaRepository filieresEcole;

    /** Lot B : rendus des devoirs de classe (dépôt réel du fichier). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService rendusDevoirs;

    private final StudentUseCase studentUseCase;
    private final ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository courseDetailRepository;
    private final ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository studentDataRepository;
    private final GradesReportService gradesReportService;
    private final ActionLogger actionLogger;
    private final ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase emailUseCase;

    /** Demandes de révision de note transmises à l'administration (APP-QA-04). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.notifications.application.NotificationsUtilisateurService notificationsUtilisateur;

    public StudentController(
            StudentUseCase studentUseCase,
            ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository courseDetailRepository,
            ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository studentDataRepository,
            GradesReportService gradesReportService,
            ActionLogger actionLogger,
            ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase emailUseCase) {
        this.studentUseCase = studentUseCase;
        this.courseDetailRepository = courseDetailRepository;
        this.studentDataRepository = studentDataRepository;
        this.gradesReportService = gradesReportService;
        this.actionLogger = actionLogger;
        this.emailUseCase = emailUseCase;
    }

    // ==================== PROFILE & DASHBOARD ====================

    @GetMapping("/profile")
    @Operation(summary = "Profil etudiant", description = "Retourne les informations du profil de l'etudiant depuis la BDD.")
    public ResponseEntity<StudentProfileDto> getProfile() {
        logger.info("GET /student/profile - Recuperation du profil etudiant");
        return ResponseEntity.ok(StudentMapper.toProfileDto(profilReel()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord", description = "Retourne les donnees du dashboard depuis la BDD.")
    public ResponseEntity<StudentDashboardDto> getDashboard() {
        logger.info("GET /student/dashboard - Recuperation du dashboard etudiant");
        Map<String, Object> dashboardMap = studentDataRepository.findById(etudiant())
                .map(data -> data.getDashboard())
                .orElseGet(() -> studentUseCase.getStudentDashboard(etudiant()));
        
        return ResponseEntity.ok(StudentMapper.toDashboardDto(dashboardMap));
    }

    // ==================== DIGITAL BADGE ====================

    @GetMapping("/digital-badge")
    @Operation(summary = "Badge numerique dynamique", description = "Retourne le badge QR avec token HMAC rotatif (expire chaque minute).")
    public ResponseEntity<DigitalBadgeDto> getDigitalBadge() {
        logger.info("GET /student/digital-badge - Generation du badge numerique");
        Map<String, Object> profil = profilReel();
        String matricule = (String) profil.get("matricule");
        if (matricule == null || matricule.isBlank()) {
            // jamais de matricule inventé : sans matricule attribué, pas de badge d'accès
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Badge indisponible : aucun matricule n'est encore attribué à votre compte.");
        }
        String nom = (String.valueOf(profil.getOrDefault("prenom", "")) + " " + profil.getOrDefault("nom", "")).trim();
        Map<String, Object> badgeMap = studentUseCase.getDigitalBadge(etudiant(), matricule, nom, (String) profil.get("classe"));
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
            studentUseCase.getStudentAttendances(etudiant(), page, size)
        ));
    }

    @PostMapping("/attendances")
    @Operation(summary = "Auto check-in", description = "Soumet une auto-declaration de presence.")
    public ResponseEntity<Map<String, Object>> selfCheckIn(@Valid @RequestBody AttendanceRequestDto request) {
        logger.info("POST /student/attendances - Auto check-in: type={}, method={}", request.type(), request.method());
        String presenceId = studentUseCase.submitSelfAttendance(
            etudiant(), 
            request.type().name(), 
            request.method()
        );

        actionLogger.logStudentAction("SELF_CHECKIN", etudiant(), "Student", 
            "Auto check-in: " + request.type() + " via " + request.method());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("attendanceId", presenceId);   // identifiant réel de la présence enregistrée
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

    // APP-QA-04 : lancer ou supprimer une séance en direct est un acte d'enseignant (avant : tout élève le pouvait)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PROFESSEUR','PROFESSOR','TEACHER','ADMIN','ADMINISTRATOR','SUPER_ADMIN')")
    @PostMapping("/live-sessions")
    @Operation(summary = "Démarrer une session live", description = "Enregistre une session live démarrée par un enseignant.")
    public ResponseEntity<Map<String, Object>> startLiveSession(@RequestBody Map<String, Object> body) {
        String id = "session-" + System.currentTimeMillis();
        // APP-QA-04 : champs contrôlés
        String titre = ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel(body, "title", "Le titre", 200);
        String cours = ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel(body, "courseName", "Le cours", 200);
        String title = titre != null ? titre : (cours != null ? cours : "Session Live");
        String lienMeet = ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel(body, "meetUrl", "Le lien de la séance", 1_000);
        String link = lienMeet != null ? lienMeet : ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel(body, "link", "Le lien de la séance", 1_000);
        if (link == null) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le lien de la séance (meetUrl) est obligatoire.");
        }
        // Seul un lien Google Meet est accepté (les élèves y sont envoyés ; l'écran n'en autorise pas d'autre)
        if (!link.matches("(?i)^https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}(\\?\\S*)?$")) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus(
                    "Le lien de la séance doit être un lien Google Meet, de la forme https://meet.google.com/abc-defg-hij.");
        }
        String heure = ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel(body, "time", "L'heure", 20);
        String time = heure != null ? heure : java.time.LocalTime.now().toString().substring(0, 5);
        
        if (meetJpaRepository != null) {
            ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MeetJpaEntity meet = 
                new ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MeetJpaEntity(
                    id, title, link, time, true
                );
            // Cours et VRAI nom de l'enseignant connecté
            meet.setSubjectName(cours);
            var moi = compteConnecte();
            meet.setTeacherName(((moi.getPrenom() == null ? "" : moi.getPrenom()) + " " + (moi.getNom() == null ? "" : moi.getNom())).trim());
            meetJpaRepository.save(meet);
        }
        logger.info("POST /student/live-sessions - Live session créée: {} (titre: {}, lien: {})", id, title, link);
        return ResponseEntity.ok(Map.of("success", true, "id", id, "title", title, "meetUrl", link != null ? link : ""));
    }

    // APP-QA-04 : lancer ou supprimer une séance en direct est un acte d'enseignant (avant : tout élève le pouvait)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PROFESSEUR','PROFESSOR','TEACHER','ADMIN','ADMINISTRATOR','SUPER_ADMIN')")
    @DeleteMapping("/live-sessions/{id}")
    @Operation(summary = "Arrêter une session live", description = "Supprime ou termine une session live.")
    public ResponseEntity<Map<String, Object>> stopLiveSession(@PathVariable String id) {
        if (meetJpaRepository != null) {
            meetJpaRepository.deleteById(id);
        }
        logger.info("DELETE /student/live-sessions/{} - Session arrêtée", id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session arrêtée avec succès"));
    }

    // ==================== GRADES ====================

    @GetMapping("/grades")
    @Operation(summary = "Notes de l'etudiant", description = "Retourne la liste des notes depuis la BDD.")
    public ResponseEntity<List<GradeDto>> getGrades() {
        logger.info("GET /student/grades - Recuperation des notes");
        List<Map<String, Object>> gradesMap = studentUseCase.getStudentGrades(etudiant());
        
        return ResponseEntity.ok(StudentMapper.toGradeDtoList(gradesMap));
    }

    @GetMapping("/grades/download")
    @Operation(summary = "Telecharger le releve PDF", description = "Genere et retourne un releve de notes officiel au format PDF.")
    public ResponseEntity<byte[]> downloadGradesPdf() {
        logger.info("GET /student/grades/download - Telechargement du releve PDF");
        List<Map<String, Object>> gradesMap = studentUseCase.getStudentGrades(etudiant());

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

        // Relevé au nom de l'étudiant connecté
        var compte = compteConnecte();
        byte[] pdf = gradesReportService.generateTranscriptPdf(
                ((compte.getPrenom() == null ? "" : compte.getPrenom() + " ") + (compte.getNom() == null ? "" : compte.getNom())).trim(),
                "",
                "",
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
        logger.info("POST /student/grades/{}/review - Demande de revision", gradeId);
        // APP-QA-04 : la note doit être l'une des siennes (404 sinon) et la demande est réellement transmise à
        // l'administration
        String eleve = etudiant();
        Map<String, Object> note = studentUseCase.getStudentGrades(eleve).stream()
                .filter(g -> gradeId.equals(String.valueOf(g.get("id"))))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Note introuvable parmi vos notes."));
        if (notificationsUtilisateur == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Les demandes de révision ne peuvent pas être transmises pour le moment.");
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String nom = auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u
                ? ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim() : null;
        notificationsUtilisateur.demanderRevisionNote(eleve, nom, gradeId,
                note.get("module") == null ? null : String.valueOf(note.get("module")), request.reason());

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
        List<Map<String, Object>> coursesMap = studentUseCase.getStudentCourses(etudiant());
        
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
        List<Map<String, Object>> homeworksMap = studentUseCase.getStudentHomeworks(etudiant());
        
        return ResponseEntity.ok(StudentMapper.toHomeworkDtoList(homeworksMap));
    }

    @PostMapping(value = "/courses/{courseId}/homeworks/{homeworkId}/submit", consumes = "multipart/form-data")
    @Operation(summary = "Soumettre un devoir", description = "L'etudiant soumet un fichier pour evaluation.")
    public ResponseEntity<Map<String, Object>> submitHomework(
            @PathVariable("courseId") String courseId,
            @PathVariable("homeworkId") String homeworkId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        logger.info("POST /student/courses/{}/homeworks/{}/submit - Soumission devoir: file={}", courseId, homeworkId, file.getOriginalFilename());
        // Lot B : devoir de classe publié par le professeur → rendu réellement stocké (fichier vérifié), visible du professeur
        if (rendusDevoirs != null && rendusDevoirs.estDevoirDeClasse(homeworkId)) {
            try {
                return ResponseEntity.ok(rendusDevoirs.deposer(etudiant(), courseId, homeworkId,
                        file.getOriginalFilename(), file.getContentType(), file.getBytes()));
            } catch (java.io.IOException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Le fichier envoyé est illisible.");
            }
        }

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

    // POST /courses/{courseId}/quizzes/submit retiré — le score était calculé à partir des bonnes réponses envoyées par l'élève lui-même ; aucun écran ne l'appelait. Les quiz passent par POST /student/quizzes/{quizId}/tentatives (corrigés par le serveur).

    // ==================== TUTEUR IA ====================

    // APP-TL-24 : POST /tutor/chat retiré — l'IA générative est hors du périmètre de cette version
    // (données mobiles, coût en devises, données de mineurs hors du Sénégal : loi n° 2008-12). Le module « ai »
    // reste en place, sans route, pour un éventuel pilote.

    // ==================== EMAIL NOTIFICATIONS ====================
    
    // POST /send-notification retiré — un compte élève envoyait un e-mail à n'importe quelle adresse au nom de l'école (destinataire fourni par la requête) ; aucun écran ne l'appelait.
    
    // POST /request-confirmation retiré — même faille (e-mail vers une adresse fournie par la requête, envoyé pendant la requête) ; aucun écran ne l'appelait.

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
        
        String userId = etudiant();
        // APP-QA-04 : nom affiché = compte connecté ; message obligatoire et borné
        var authChat = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userName = authChat != null && authChat.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User moi
                ? ((moi.getPrenom() == null ? "" : moi.getPrenom()) + " " + (moi.getNom() == null ? "" : moi.getNom())).trim()
                : body.get("user");
        String userRole = "Student";
        String text = body == null ? null : body.get("text");
        if (text == null || text.isBlank()) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le message est vide.");
        }
        if (text.strip().length() > 2_000) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le message ne doit pas dépasser 2000 caractères.");
        }
        text = text.strip();
        
        studentUseCase.saveLiveChatMessage(sessionId, userId, userName, userRole, text);
        
        actionLogger.logStudentAction("SEND_CHAT", etudiant(), "Student", 
            "Message envoyé dans session " + sessionId);
        return ResponseEntity.ok(Map.of("success", true, "id", "msg-" + System.currentTimeMillis()));
    }

    @PostMapping("/homeworks/{homeworkId}/start")
    @Operation(summary = "Démarrer devoir", description = "Marque un devoir comme en cours.")
    public ResponseEntity<Map<String, Object>> startHomework(
            @Parameter(description = "Identifiant du devoir") @PathVariable("homeworkId") String homeworkId) {
        logger.info("POST /student/homeworks/{}/start", homeworkId);
        studentUseCase.updateHomeworkProgress(homeworkId, 0);
        actionLogger.logStudentAction("START_HOMEWORK", etudiant(), "Student", 
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
        actionLogger.logStudentAction("UPDATE_HOMEWORK_PROGRESS", etudiant(), "Student", 
            "Avancement mis à jour pour devoir: " + homeworkId);
        return ResponseEntity.ok(Map.of("success", true, "homeworkId", homeworkId));
    }

    /**
     * Étudiant connecté (APP-SEC-19).
     */
    private static ecole_221.sn.ecole.modules.auth.domain.model.User compteConnecte() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u) {
            return u;
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    /**
     * Profil RÉEL de l'élève connecté : compte (nom, e-mail, matricule…) + classe affectée (niveau, filière).
     * Les informations complémentaires éventuellement présentes dans StudentData complètent les champs vides.
     */
    private Map<String, Object> profilReel() {
        var u = compteConnecte();
        Map<String, Object> p = new java.util.LinkedHashMap<>();
        p.put("id", u.getId());
        p.put("nom", u.getNom());
        p.put("prenom", u.getPrenom());
        p.put("email", u.getEmail());
        p.put("matricule", u.getMatricule());
        p.put("telephone", u.getTelephone());
        p.put("photoUrl", u.getPhotoUrl());
        if (inscriptions != null && classesEcole != null) {
            inscriptions.findFirstByUserId(u.getId())
                    .flatMap(i -> classesEcole.findById(i.getClasseId()))
                    .ifPresent(c -> {
                        p.put("classe", c.getName() != null ? c.getName() : c.getNom());
                        p.put("niveau", c.getLevel());
                        String filiere = c.getFiliereId();
                        if (filiere != null && filieresEcole != null) {
                            filiere = filieresEcole.findById(filiere).map(f -> f.getName() != null ? f.getName() : f.getNom()).orElse(filiere);
                        }
                        p.put("filiere", filiere);
                    });
        }
        studentDataRepository.findById(u.getId()).map(d -> d.getProfile()).ifPresent(extra -> extra.forEach((k, v) -> {
            Object actuel = p.get(k);
            if ((actuel == null || String.valueOf(actuel).isBlank()) && v != null) p.put(k, v);
        }));
        p.replaceAll((k, v) -> v == null ? "" : v);
        return p;
    }

    private static String etudiant() {
        return compteConnecte().getId();
    }
}
