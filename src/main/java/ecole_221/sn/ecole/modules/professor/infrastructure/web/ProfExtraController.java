package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.dto.*;
import ecole_221.sn.ecole.modules.professor.application.service.ProfessorMapper;
import ecole_221.sn.ecole.partage.PageResult;
import ecole_221.sn.ecole.partage.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Professor", description = "Espace professeur — quiz, présences, emploi du temps")
public class ProfExtraController {
    private static final Logger logger = LoggerFactory.getLogger(ProfExtraController.class);

    private final ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase;
    /** Modules, supports et quiz réellement enregistrés (AccesCoursInterceptor contrôle le cours en amont). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.professor.application.service.ContenuPedagogiqueService contenus;

    private ecole_221.sn.ecole.modules.professor.application.service.ContenuPedagogiqueService contenus() {
        if (contenus == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Contenus pédagogiques indisponibles.");
        return contenus;
    }
    /** Contrôle cours / élève de l'école (absent dans les tests unitaires autonomes). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AccesCoursInterceptor accesCours;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classesEcole;
    private static final java.util.Set<String> STATUTS_PRESENCE = java.util.Set.of("PRESENT", "ABSENT", "RETARD", "EXCUSE", "LATE", "EXCUSED");

    public ProfExtraController(ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase) {
        logger.info("ProfExtraController initialized");
        this.professorUseCase = professorUseCase;
    }

    @GetMapping("/professor/courses/{courseId}/quizzes")
    @Operation(summary = "Quiz d'un cours", description = "Tous les quiz QCM du cours (tous modules).")
    public ResponseEntity<List<Map<String, Object>>> getQuizzes(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(contenus().quizDuCours(courseId));
    }

    @PostMapping("/professor/modules/{moduleId}/quizzes")
    @Operation(summary = "Créer un quiz dans un module", description = "Titre et au moins une question (énoncé, ≥ 2 choix, bonne réponse) obligatoires.")
    public ResponseEntity<Map<String, Object>> createQuizInModule(@PathVariable("moduleId") String moduleId,
                                                                  @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(contenus().creerQuiz(moduleId, corps));
    }

    @PostMapping("/professor/courses/{courseId}/quizzes")
    @Operation(summary = "Créer un quiz (ancien chemin)", description = "Un quiz appartient à un module : utiliser POST /professor/modules/{moduleId}/quizzes.")
    public ResponseEntity<Map<String, Object>> createQuiz(@PathVariable("courseId") String courseId,
                                                          @RequestBody(required = false) Map<String, Object> corps) {
        Object moduleId = corps == null ? null : corps.get("moduleId");
        if (moduleId == null || moduleId.toString().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Indiquez le module du quiz (moduleId).");
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(contenus().creerQuizDansCours(courseId, moduleId.toString(), corps));
    }

    @GetMapping("/prof/classes/{classId}/students")
    @Operation(summary = "Étudiants d'une classe", description = "Retourne la liste des étudiants d'une classe donnée.")
    public ResponseEntity<List<StudentDto>> getProfClassStudents(
            @Parameter(description = "Identifiant de la classe") @PathVariable("classId") String classId) {
        
        logger.info("GET /prof/classes/{}/students", classId);
        try {
            List<User> users = professorUseCase.getStudents("M1 GL");
            logger.debug("Retrieved {} students for class {}", users.size(), classId);
            return ResponseEntity.ok(ProfessorMapper.toStudentDtoList(users));
        } catch (Exception e) {
            logger.error("Error fetching students for class {}: {}", classId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/prof/attendance/submit")
    @Operation(summary = "Soumettre les présences", description = "Soumet la feuille de présence. Accepte un objet unique ou une liste.")
    public ResponseEntity<Map<String, Object>> submitAttendance(@RequestBody Object rawBody) {
        logger.info("POST /prof/attendance/submit - submitting attendance");
        try {
            List<Map<String, Object>> records;
            if (rawBody instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tempList = (List<Map<String, Object>>) rawBody;
                records = tempList;
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempMap = (Map<String, Object>) rawBody;
                records = List.of(tempMap);
            }
            
            logger.debug("Processing {} attendance records", records.size());
            // Validation de TOUTE la feuille avant écriture. Avant : date « 2026-07-20 » et cours « course-1 »
            // inventés quand ils manquaient → présences rattachées à un faux jour / un faux cours.
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> r = records.get(i);
                String ligne = records.size() > 1 ? " (ligne " + (i + 1) + ")" : "";
                if (r == null) {
                    throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Présence vide" + ligne + ".");
                }
                for (String[] champ : new String[][]{{"date", "la date"}, {"courseId", "le cours"},
                        {"studentId", "l'étudiant"}, {"status", "le statut"}}) {
                    Object v = r.get(champ[0]);
                    if (v == null || v.toString().isBlank()) {
                        throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Champ obligatoire manquant : " + champ[1] + ligne + ".");
                    }
                }
                java.time.LocalDate jour;
                try {
                    jour = java.time.LocalDate.parse(r.get("date").toString().trim());
                } catch (java.time.format.DateTimeParseException ex) {
                    throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Date invalide (format attendu AAAA-MM-JJ)" + ligne + " : " + r.get("date"));
                }
                // test du 2026-11-10 : statut inventé, date future, élève inexistant et cours d'un collègue acceptés
                if (jour.isAfter(java.time.LocalDate.now())) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "On ne peut pas faire l'appel pour une date future" + ligne + ".");
                }
                String statut = r.get("status").toString().trim().toUpperCase(java.util.Locale.ROOT);
                if (!STATUTS_PRESENCE.contains(statut)) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Statut de présence inconnu" + ligne + " : " + r.get("status")
                            + " (attendu : PRESENT, ABSENT, RETARD ou EXCUSE).");
                }
                if (accesCours != null) {
                    accesCours.verifierCours(r.get("courseId").toString().trim());
                    accesCours.verifierEleve(r.get("studentId").toString().trim());
                }
            }
            for (Map<String, Object> record : records) {
                String date = record.get("date").toString().trim();
                String courseId = record.get("courseId").toString().trim();
                logger.debug("Submitting attendance: date={}, courseId={}, studentId={}, status={}",
                    date, courseId, record.get("studentId"), record.get("status"));
                professorUseCase.submitAttendance(
                    date,
                    record.get("studentId").toString(),
                    record.get("studentName") == null ? null : record.get("studentName").toString(),
                    record.get("status").toString(),
                    courseId
                );
            }
            logger.info("Successfully submitted {} attendance records", records.size());
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Présences soumises avec succès"));
        } catch (Exception e) {
            logger.error("Error submitting attendance: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/schedule")
    @Operation(summary = "Emploi du temps du jour", description = "Retourne les sessions planifiées pour le professeur aujourd'hui.")
    public ResponseEntity<List<ScheduleDto>> getTodaySchedule() {
        logger.info("GET /professor/schedule - fetching today's schedule");
        // APP-BE-16 : l'emploi du temps est celui du professeur authentifié (plus d'identifiant codé en dur)
        String profId = utilisateurConnecte();
        if (profId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            List<Map<String, Object>> schedule = professorUseCase.getSchedule(profId);
            logger.debug("Retrieved {} schedule sessions", schedule.size());
            var seances = ProfessorMapper.toScheduleDtoList(schedule);
            // nom RÉEL de la classe (avant : absent → « L3-GL » / « M1 GL » inventés à l'écran)
            if (classesEcole != null) {
                seances = seances.stream().map(s -> s.classe() == null ? s : s.avecClasse(
                        classesEcole.findById(s.classe()).map(c -> c.getName() != null ? c.getName() : c.getNom()).orElse(s.classe())))
                        .toList();
            }
            return ResponseEntity.ok(seances);
        } catch (Exception e) {
            logger.error("Error fetching schedule: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/schedule/{sessionId}/cancel")
    @Operation(summary = "Annuler une session", description = "Annule une session planifiée avec un motif.")
    public ResponseEntity<Map<String, Object>> cancelSession(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody CancelSessionRequestDto request) {
        
        logger.info("POST /professor/schedule/{}/cancel - reason={}", sessionId, request.reason());
        try {
            verifierSeanceDuProfesseur(professorUseCase, sessionId);
            professorUseCase.cancelScheduleSession(sessionId, request.reason());
            logger.info("Session {} cancelled with reason: {}", sessionId, request.reason());
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse(
                "Session annulée pour motif: " + request.reason()
            ));
        } catch (Exception e) {
            logger.error("Error cancelling session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/schedule/{sessionId}/reschedule")
    @Operation(summary = "Reporter une session", description = "Replanifie une session à un nouveau créneau (day, time, room).")
    public ResponseEntity<Map<String, Object>> rescheduleSession(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody RescheduleSessionRequestDto request) {
        
        logger.info("POST /professor/schedule/{}/reschedule - day={}, time={}, room={}", 
            sessionId, request.day(), request.time(), request.room());
        reportDirectReserveAdministration();
        try {
            verifierSeanceDuProfesseur(professorUseCase, sessionId);
            professorUseCase.rescheduleScheduleSession(sessionId, request.day(), request.time(), request.room());
            logger.info("Session {} rescheduled to {} at {} in {}", 
                sessionId, request.day(), request.time(), request.room());
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse(
                "Session déplacée au " + request.day() + " à " + request.time() + " en " + request.room()
            ));
        } catch (Exception e) {
            logger.error("Error rescheduling session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /** Identifiant de l'utilisateur authentifié, ou null si la requête n'est pas authentifiée. */
    static String utilisateurConnecte() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private static final java.util.Set<String> ROLES_ADMIN =
            java.util.Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");

    /**
     * Lot E (2026-11-10) : le report DIRECT d'une séance est réservé à l'administration ; le professeur passe
     * par POST /professor/schedule/{sessionId}/demandes-report (validation par l'administration).
     */
    static void reportDirectReserveAdministration() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> ROLES_ADMIN.contains(a.getAuthority()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Demandez un report à l'administration");
        }
    }

    /**
     * Un professeur ne peut annuler / reporter que SES séances (relevé par l'Agent 06, APP-SEC-16).
     * Les administrateurs de l'école gardent la main sur toutes les séances.
     */
    static void verifierSeanceDuProfesseur(
            ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase useCase, String sessionId) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> ROLES_ADMIN.contains(a.getAuthority()))) {
            return;
        }
        String profId = utilisateurConnecte();
        if (profId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        boolean sienne = useCase.getSchedule(profId).stream().anyMatch(m -> sessionId.equals(m.get("id")));
        if (!sienne) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Cette séance n'est pas la vôtre");
        }
    }
}
