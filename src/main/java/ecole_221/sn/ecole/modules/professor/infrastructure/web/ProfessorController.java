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
            return ResponseEntity.ok(ProfessorMapper.toCoursePageResponse(pageResult));
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

        // Aucun modèle d'inscription élève ↔ classe n'existe encore : on renvoie les étudiants ACTIFS de l'école
        // du professeur (jamais ceux d'une autre école), sans prétendre qu'ils sont inscrits dans la classe du cours.
        List<StudentDto> etudiants = comptesEcole.membres("ETUDIANT").stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getValue().getStatus()))
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
        try {
            Map<String, Object> homeworkMap = ProfessorMapper.toHomeworkMap(request);
            professorUseCase.saveHomework(courseId, 
                request.titre(), request.desc(), 
                request.prio().name(), request.deadlineStr());
            logger.info("Homework created successfully for course {}", courseId);
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Devoir créé avec succès"));
        } catch (Exception e) {
            logger.error("Error creating homework for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/lessons")
    @Operation(summary = "Leçons d'un cours", description = "Retourne la liste des leçons d'un cours.")
    public ResponseEntity<PageResponseDto<LessonDto>> getLessons(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("GET /professor/courses/{}/lessons - page={}, size={}", courseId, page, size);
        try {
            PageResult<Map<String, Object>> pageResult =
                professorUseCase.getLessons(courseId, page, size);
            logger.debug("Retrieved {} lessons for course {}", pageResult.getTotalElements(), courseId);
            return ResponseEntity.ok(ProfessorMapper.toLessonPageResponse(pageResult));
        } catch (Exception e) {
            logger.error("Error fetching lessons for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/lessons")
    @Operation(summary = "Créer une leçon", description = "Ajoute une nouvelle leçon à un cours (titre, durée en minutes, date).")
    public ResponseEntity<Map<String, Object>> createLesson(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Valid @RequestBody CreateLessonRequestDto request) {
        
        logger.info("POST /professor/courses/{}/lessons - creating lesson", courseId);
        logger.debug("Lesson request: title={}, duration={}, date={}", 
            request.title(), request.duration(), request.date());
        try {
            Map<String, Object> lessonMap = ProfessorMapper.toLessonMap(request);
            professorUseCase.saveLesson(courseId, 
                request.title(), request.duration(), request.date());
            logger.info("Lesson created successfully for course {}", courseId);
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Leçon créée avec succès"));
        } catch (Exception e) {
            logger.error("Error creating lesson for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/professor/courses/{courseId}/modules")
    @Operation(summary = "Modules d'un cours", description = "Retourne la liste des modules d'un cours.")
    public ResponseEntity<PageResponseDto<ModuleDto>> getModules(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("GET /professor/courses/{}/modules - page={}, size={}", courseId, page, size);
        try {
            PageResult<Map<String, Object>> pageResult =
                professorUseCase.getModules(courseId, page, size);
            logger.debug("Retrieved {} modules for course {}", pageResult.getTotalElements(), courseId);
            return ResponseEntity.ok(ProfessorMapper.toModulePageResponse(pageResult));
        } catch (Exception e) {
            logger.error("Error fetching modules for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/modules")
    @Operation(summary = "Créer un module", description = "Ajoute un nouveau module à un cours.")
    public ResponseEntity<Map<String, Object>> createModule(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Valid @RequestBody CreateModuleRequestDto request) {
        
        logger.info("POST /professor/courses/{}/modules - creating module", courseId);
        logger.debug("Module request: name={}", request.name());
        try {
            Map<String, Object> moduleMap = ProfessorMapper.toModuleMap(request);
            professorUseCase.saveModule(courseId, request.name());
            logger.info("Module created successfully for course {}", courseId);
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Module créé avec succès"));
        } catch (Exception e) {
            logger.error("Error creating module for course {}: {}", courseId, e.getMessage());
            throw e;
        }
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
        try {
            professorUseCase.saveReminder(
                courseId, 
                (String) body.get("content"), 
                (Boolean) body.get("isUrgent"),
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
    @Operation(summary = "Quiz d'un module", description = "Retourne les quiz d'un module.")
    public ResponseEntity<List<Map<String, Object>>> getQuizzes(
            @Parameter(description = "Identifiant du module") @PathVariable("moduleId") String moduleId) {
        logger.info("GET /professor/modules/{}/quizzes", moduleId);
        try {
            PageResult<Map<String, Object>> pageResult = professorUseCase.getQuizzes(moduleId, 0, 10);
            logger.debug("Retrieved {} quizzes for module {}", pageResult.getTotalElements(), moduleId);
            return ResponseEntity.ok(pageResult.getContent());
        } catch (Exception e) {
            logger.error("Error fetching quizzes for module {}: {}", moduleId, e.getMessage());
            throw e;
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
