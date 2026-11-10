package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.dto.*;
import ecole_221.sn.ecole.modules.professor.application.service.ProfessorMapper;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
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
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Professor", description = "Espace professeur — cours, notes, devoirs, leçons, modules, quiz")
public class ProfessorController {
    private static final Logger logger = LoggerFactory.getLogger(ProfessorController.class);

    private final ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase;
    /** Modules, supports et quiz réellement enregistrés (AccesCoursInterceptor contrôle le cours en amont). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.professor.application.service.ContenuPedagogiqueService contenus;

    private ecole_221.sn.ecole.modules.professor.application.service.ContenuPedagogiqueService contenus() {
        if (contenus == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Contenus pédagogiques indisponibles.");
        return contenus;
    }
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classesEcole;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository inscriptions;
    /** Lot B : notification des élèves à la création d'un devoir. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService rendusDevoirs;
    private final ActionLogger actionLogger;
    private final CourseJpaRepository courses;
    private final GradeJpaRepository notes;
    private final AttendanceJpaRepository presences;
    private final ComptesEcoleService comptesEcole;

    public ProfessorController(ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase, ActionLogger actionLogger) {
        this(professorUseCase, actionLogger, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProfessorController(ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase, 
                               ActionLogger actionLogger,
                               CourseJpaRepository courses,
                               GradeJpaRepository notes,
                               AttendanceJpaRepository presences,
                               ComptesEcoleService comptesEcole) {
        logger.info("ProfessorController initialized");
        this.professorUseCase = professorUseCase;
        this.actionLogger = actionLogger;
        this.courses = courses;
        this.notes = notes;
        this.presences = presences;
        this.comptesEcole = comptesEcole;
    }

    @GetMapping("/professor/courses")
    @Operation(summary = "Liste des cours", description = "Retourne les cours assignés à un professeur avec pagination.")
    public ResponseEntity<PageResponseDto<CourseDto>> getCourses(
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        // APP-BE-16 : les cours sont ceux du professeur authentifié
        String profId = ProfExtraController.utilisateurConnecte();
        if (profId == null) {
            return ResponseEntity.status(401).build();
        }
        logger.info("GET /professor/courses - profId={}, page={}, size={}", profId, page, size);
        try {
            PageResult<Course> pageResult =
                professorUseCase.getCourses(profId, page, size);
            
            logger.debug("Retrieved {} courses for professor {}", pageResult.getTotalElements(), profId);
            var page0 = ProfessorMapper.toCoursePageResponse(pageResult);
            // classe et effectif RÉELS (avant : classe absente → « L3-GL » à l'écran, effectif 0)
            var enrichis = page0.content().stream().map(c -> {
                if (c.classeId() == null || classesEcole == null) return c;
                var cl = classesEcole.findById(c.classeId()).orElse(null);
                String nom = cl == null ? null : (cl.getName() != null ? cl.getName() : cl.getNom());
                int effectif = inscriptions == null ? 0 : (int) inscriptions.countByClasseId(c.classeId());
                return c.avecClasse(nom, effectif);
            }).toList();
            return ResponseEntity.ok(new ecole_221.sn.ecole.partage.dto.PageResponseDto<>(enrichis, page0.page(), page0.size(),
                    page0.totalElements(), page0.totalPages(), page0.last()));
        } catch (Exception e) {
            logger.error("Error fetching courses for professor {}: {}", profId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/students")
    @Operation(summary = "Étudiants d'un cours", description = "Étudiants de l'école du professeur, avec leur moyenne et leur dernière présence réelles dans ce cours (null si aucune saisie).")
    public ResponseEntity<List<StudentDto>> getCourseStudents(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId) {
        // Avant : TOUS les étudiants de TOUTES les écoles, avec « PRESENT » et une moyenne de 15 codés en dur.
        String profId = ProfExtraController.utilisateurConnecte();
        if (profId == null) {
            return ResponseEntity.status(401).build();
        }
        if (courses == null || notes == null || presences == null || comptesEcole == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Données des cours indisponibles.");
        }
        String ecole = comptesEcole.ecoleCourante();
        CourseJpaEntity cours = courses.findById(courseId)
                .filter(c -> c.getOrganizationId() == null || ecole.equals(c.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        if (!profId.equals(cours.getProfId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce cours n'est pas attribué à votre compte.");
        }
        logger.info("GET /professor/courses/{}/students - profId={}", courseId, profId);

        Map<String, Double> moyennes = new java.util.HashMap<>();
        for (GradeJpaEntity g : notes.findByCourseId(courseId)) {
            if (g.getStudentId() != null) moyennes.put(g.getStudentId(), g.getAverage());
        }
        // Dernière présence saisie pour ce cours (dates ISO AAAA-MM-JJ : l'ordre lexical suit l'ordre chronologique)
        Map<String, AttendanceJpaEntity> dernieres = new java.util.HashMap<>();
        for (AttendanceJpaEntity a : presences.findByCourseId(courseId)) {
            if (a.getStudentId() == null) continue;
            dernieres.merge(a.getStudentId(), a, (x, y) ->
                    String.valueOf(y.getDate()).compareTo(String.valueOf(x.getDate())) > 0 ? y : x);
        }

        // Élèves AFFECTÉS à la classe du cours (inscriptions_classes). Avant : tous les élèves de l'école apparaissaient
        // dans l'appel et les notes de n'importe quel cours (test navigateur du 2026-11-10).
        java.util.Set<String> eleveDeLaClasse = (inscriptions == null || cours.getClasse() == null) ? null
                : inscriptions.findByClasseId(cours.getClasse()).stream()
                    .map(ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
        List<StudentDto> etudiants = comptesEcole.membres("ETUDIANT").stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getValue().getStatus()))
                .filter(e -> eleveDeLaClasse == null || eleveDeLaClasse.contains(e.getKey().getId()))
                .map(Map.Entry::getKey)
                .map(u -> new StudentDto(
                        u.getId(),
                        u.getMatricule(),
                        u.getNom(),
                        u.getPrenom(),
                        u.getEmail(),
                        dernieres.containsKey(u.getId()) ? dernieres.get(u.getId()).getStatus() : null,
                        moyennes.get(u.getId())))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(etudiants);
    }

    @GetMapping("/professor/courses/{courseId}/grades")
    @Operation(summary = "Notes d'un cours", description = "Retourne la liste des notes de tous les étudiants d'un cours.")
    public ResponseEntity<List<GradeDto>> getCourseGrades(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId) {
        
        logger.info("GET /professor/courses/{}/grades", courseId);
        try {
            List<GradeDto> grades = professorUseCase.getCourseGrades(courseId);
            logger.debug("Retrieved {} grades for course {}", grades.size(), courseId);
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            logger.error("Error fetching grades for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/grades/{studentId}")
    @Operation(summary = "Saisir / modifier une note", description = "Enregistre la note CC et/ou d'examen d'un étudiant pour un cours.")
    public ResponseEntity<GradeDto> saveGrade(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Parameter(description = "Identifiant de l'étudiant") @PathVariable("studentId") String studentId,
            @RequestBody Map<String, Double> body) {
        
        logger.info("POST /professor/courses/{}/grades/{} - saving grade", courseId, studentId);
        logger.debug("Grade body: cc={}, examen={}", body.get("cc"), body.get("examen"));
        validerNotes(body);
        try {
            professorUseCase.saveGrade(studentId, courseId, body.get("cc"), body.get("examen"));
            Map<String, Object> gradeMap = professorUseCase.getGrade(studentId, courseId);
            logger.debug("Grade saved successfully for student {} in course {}", studentId, courseId);
            
            actionLogger.logProfessorAction("SAVE_GRADE", auteur(), "Professeur", courseId, 
                "Note enregistrée pour étudiant " + studentId + " : CC=" + body.get("cc") + ", Examen=" + body.get("examen"));
            
            return ResponseEntity.ok(ProfessorMapper.toGradeDto(gradeMap));
        } catch (Exception e) {
            logger.error("Error saving grade for student {} in course {}: {}", studentId, courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/homeworks")
    @Operation(summary = "Devoirs d'un cours", description = "Retourne la liste des devoirs associés à un cours.")
    public ResponseEntity<PageResponseDto<HomeworkDto>> getHomeworks(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("GET /professor/courses/{}/homeworks - page={}, size={}", courseId, page, size);
        try {
            PageResult<Map<String, Object>> pageResult =
                professorUseCase.getHomeworks(courseId, page, size);
            logger.debug("Retrieved {} homeworks for course {}", pageResult.getTotalElements(), courseId);
            return ResponseEntity.ok(ProfessorMapper.toHomeworkPageResponse(pageResult));
        } catch (Exception e) {
            logger.error("Error fetching homeworks for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/homeworks")
    @Operation(summary = "Créer un devoir", description = "Crée un nouveau devoir pour un cours (titre, description, priorité, deadline).")
    public ResponseEntity<Map<String, Object>> createHomework(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Valid @RequestBody CreateHomeworkRequestDto request) {
        
        logger.info("POST /professor/courses/{}/homeworks - creating homework", courseId);
        logger.debug("Homework request: titre={}, desc={}, prio={}, deadline={}", 
            request.titre(), request.desc(), request.prio(), request.deadlineStr());
        java.time.LocalDate limite = dateValide(request.deadlineStr(), "date limite");
        if (limite.isBefore(java.time.LocalDate.now())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La date limite du devoir est déjà passée.");
        }
        try {
            Map<String, Object> homeworkMap = ProfessorMapper.toHomeworkMap(request);
            professorUseCase.saveHomework(courseId, 
                request.titre(), request.desc(), 
                request.prio().name(), request.deadlineStr());
            logger.info("Homework created successfully for course {}", courseId);
            Map<String, Object> reponse = ProfessorMapper.createSuccessResponse("Devoir créé avec succès");
            // Lot B : élèves de la classe notifiés ; un échec n'annule pas le devoir (résultat renvoyé au professeur)
            if (rendusDevoirs != null) {
                reponse.put("notification", rendusDevoirs.notifierNouveauDevoir(courseId, request.titre(), request.deadlineStr()));
            }
            return ResponseEntity.ok(reponse);
        } catch (Exception e) {
            logger.error("Error creating homework for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/lessons")
    @Operation(summary = "Supports de cours", description = "Supports (leçons) d'un cours : titre, description, pièce jointe, module.")
    public ResponseEntity<List<Map<String, Object>>> getLessons(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(contenus().supports(courseId));
    }

    @PostMapping("/professor/courses/{courseId}/lessons")
    @Operation(summary = "Publier un support de cours", description = "Titre obligatoire ; description, pièce jointe (lien http/https) et module facultatifs.")
    public ResponseEntity<Map<String, Object>> createLesson(@PathVariable("courseId") String courseId,
                                                            @RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> cree = contenus().creerSupport(courseId, corps);
        actionLogger.logProfessorAction("CREATE_LESSON", auteur(), "Professeur", courseId, "Support publié : " + cree.get("title"));
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(cree);
    }

    @GetMapping("/professor/courses/{courseId}/modules")
    @Operation(summary = "Modules d'un cours", description = "Modules (chapitres) d'un cours.")
    public ResponseEntity<List<Map<String, Object>>> getModules(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(contenus().modules(courseId));
    }

    @PostMapping("/professor/courses/{courseId}/modules")
    @Operation(summary = "Créer un module", description = "Titre obligatoire (« title » ou « name »), description facultative.")
    public ResponseEntity<Map<String, Object>> createModule(@PathVariable("courseId") String courseId,
                                                            @RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> cree = contenus().creerModule(courseId, corps);
        actionLogger.logProfessorAction("CREATE_MODULE", auteur(), "Professeur", courseId, "Module créé : " + cree.get("title"));
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(cree);
    }

    @GetMapping("/professor/planning")
    @Operation(summary = "Planning professeur", description = "Retourne le planning du professeur authentifié.")
    public ResponseEntity<List<Map<String, Object>>> getSchedule() {
        // APP-BE-16 : plus de paramètre profId (un professeur ne lit que son propre planning)
        String profId = ProfExtraController.utilisateurConnecte();
        if (profId == null) {
            return ResponseEntity.status(401).build();
        }
        logger.info("GET /professor/planning - profId={}", profId);
        try {
            List<Map<String, Object>> schedule = professorUseCase.getSchedule(profId);
            logger.debug("Retrieved {} schedule sessions for professor {}", schedule.size(), profId);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            logger.error("Error fetching schedule for professor {}: {}", profId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/planning/{sessionId}/cancel")
    @Operation(summary = "Annuler cours", description = "Annule une session de cours.")
    public ResponseEntity<Map<String, Object>> cancelCourse(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @RequestBody Map<String, String> body) {
        logger.info("POST /professor/planning/{}/cancel - reason={}", sessionId, body.get("reason"));
        try {
            ProfExtraController.verifierSeanceDuProfesseur(professorUseCase, sessionId);
            professorUseCase.cancelScheduleSession(sessionId, body.get("reason"));
            actionLogger.logProfessorAction("CANCEL_COURSE", auteur(), "Professeur", sessionId, 
                "Cours annulé: " + body.get("reason"));
            return ResponseEntity.ok(Map.of("success", true, "sessionId", sessionId));
        } catch (Exception e) {
            logger.error("Error cancelling session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/planning/{sessionId}/reschedule")
    @Operation(summary = "Reprogrammer cours", description = "Reprogramme une session de cours.")
    public ResponseEntity<Map<String, Object>> rescheduleCourse(
            @Parameter(description = "Identifiant de la session") @PathVariable("sessionId") String sessionId,
            @RequestBody Map<String, String> body) {
        logger.info("POST /professor/planning/{}/reschedule - day={}, time={}, room={}", 
            sessionId, body.get("day"), body.get("time"), body.get("room"));
        ProfExtraController.reportDirectReserveAdministration();   // lot E : le professeur fait une demande
        try {
            ProfExtraController.verifierSeanceDuProfesseur(professorUseCase, sessionId);
            professorUseCase.rescheduleScheduleSession(sessionId, body.get("day"), body.get("time"), body.get("room"));
            actionLogger.logProfessorAction("RESCHEDULE_COURSE", auteur(), "Professeur", sessionId, 
                "Cours reprogrammé: " + body.get("day") + " " + body.get("time"));
            return ResponseEntity.ok(Map.of("success", true, "sessionId", sessionId));
        } catch (Exception e) {
            logger.error("Error rescheduling session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/reminders")
    @Operation(summary = "Rappels d'un cours", description = "Retourne les rappels d'un cours.")
    public ResponseEntity<List<Map<String, Object>>> getReminders(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId) {
        logger.info("GET /professor/courses/{}/reminders", courseId);
        try {
            List<Map<String, Object>> reminders = professorUseCase.getReminders(courseId);
            logger.debug("Retrieved {} reminders for course {}", reminders.size(), courseId);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            logger.error("Error fetching reminders for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/reminders")
    @Operation(summary = "Créer rappel", description = "Crée un nouveau rappel pour un cours.")
    public ResponseEntity<Map<String, Object>> createReminder(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @RequestBody Map<String, Object> body) {
        logger.info("POST /professor/courses/{}/reminders - content={}, isUrgent={}", 
            courseId, body.get("content"), body.get("isUrgent"));
        Object contenu = body == null ? null : body.get("content");
        if (contenu == null || contenu.toString().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Le texte du rappel est obligatoire.");
        }
        Object urgent = body.get("isUrgent");
        if (urgent != null && !(urgent instanceof Boolean)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "« isUrgent » doit valoir true ou false.");
        }
        try {
            professorUseCase.saveReminder(
                courseId, 
                (String) body.get("content"), 
                Boolean.TRUE.equals(body.get("isUrgent")),
                auteur()
            );
            actionLogger.logProfessorAction("CREATE_REMINDER", auteur(), "Professeur", courseId, 
                "Rappel créé: " + body.get("content"));
            return ResponseEntity.ok(Map.of("success", true, "id", "rem-" + System.currentTimeMillis()));
        } catch (Exception e) {
            logger.error("Error creating reminder for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/modules/{moduleId}/quizzes")
    @Operation(summary = "Quiz d'un module", description = "Quiz QCM d'un module, avec leurs questions.")
    public ResponseEntity<List<Map<String, Object>>> getQuizzes(@PathVariable("moduleId") String moduleId) {
        return ResponseEntity.ok(contenus().quizDuModule(moduleId));
    }

    /** Date AAAA-MM-JJ (éventuellement suivie d'une heure) ; 400 lisible sinon (avant : « vendredi prochain » accepté). */
    static java.time.LocalDate dateValide(String saisie, String libelle) {
        try {
            return java.time.LocalDate.parse(saisie.trim().substring(0, Math.min(10, saisie.trim().length())));
        } catch (RuntimeException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La " + libelle + " est invalide (format attendu AAAA-MM-JJ) : " + saisie);
        }
    }

    /** Auteur réel de l'action pour le journal d'audit (APP-BE-16). */
    private static String auteur() {
        String id = ProfExtraController.utilisateurConnecte();
        return id != null ? id : "anonyme";
    }

    /**
     * Notes attendues : « cc » et/ou « examen », sur 20 (OBS-02, Agent 05).
     * Avant : un corps aux clés inconnues (ex. noteCc) était accepté et enregistrait 0 en silence.
     */
    static void validerNotes(Map<String, Double> body) {
        if (body == null || (body.get("cc") == null && body.get("examen") == null)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Note manquante : champs attendus « cc » et/ou « examen »");
        }
        for (String champ : new String[]{"cc", "examen"}) {
            Double note = body.get(champ);
            if (note != null && (note.isNaN() || note < 0 || note > 20)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Note « " + champ + " » hors de l'intervalle 0–20");
            }
        }
    }
}
