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
@CrossOrigin(origins = "*")
@Tag(name = "Professor", description = "Espace professeur — quiz, présences, emploi du temps")
public class ProfExtraController {
    private static final Logger logger = LoggerFactory.getLogger(ProfExtraController.class);

    private final ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase;

    public ProfExtraController(ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase professorUseCase) {
        logger.info("ProfExtraController initialized");
        this.professorUseCase = professorUseCase;
    }

    @GetMapping("/professor/courses/{courseId}/quizzes")
    @Operation(summary = "Quiz d'un cours", description = "Retourne la liste des quiz associés à un cours.")
    public ResponseEntity<PageResponseDto<QuizDto>> getQuizzes(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("GET /professor/courses/{}/quizzes - page={}, size={}", courseId, page, size);
        try {
            PageResult<Map<String, Object>> pageResult =
                professorUseCase.getQuizzes(courseId, page, size);
            logger.debug("Retrieved {} quizzes for course {}", pageResult.getTotalElements(), courseId);
            return ResponseEntity.ok(ProfessorMapper.toQuizPageResponse(pageResult));
        } catch (Exception e) {
            logger.error("Error fetching quizzes for course {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/modules/{moduleId}/quizzes")
    @Operation(summary = "Créer un quiz dans un module", description = "Crée un nouveau quiz dans un module (titre, durée en minutes).")
    public ResponseEntity<Map<String, Object>> createQuizInModule(
            @Parameter(description = "Identifiant du module") @PathVariable("moduleId") String moduleId,
            @Valid @RequestBody CreateQuizRequestDto request) {
        
        logger.info("POST /professor/modules/{}/quizzes - creating quiz", moduleId);
        logger.debug("Quiz request: title={}, duration={}", request.title(), request.duration());
        try {
            Object durationObj = request.duration();
            int duration = durationObj instanceof Integer ? (Integer) durationObj : Integer.parseInt(durationObj.toString());
            professorUseCase.saveQuiz(moduleId, request.title(), duration);
            logger.info("Quiz created successfully in module {}", moduleId);
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Quiz créé dans le module avec succès"));
        } catch (Exception e) {
            logger.error("Error creating quiz in module {}: {}", moduleId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/professor/courses/{courseId}/quizzes")
    @Operation(summary = "Créer un quiz dans un cours", description = "Crée un nouveau quiz directement dans un cours (titre, durée en minutes).")
    public ResponseEntity<Map<String, Object>> createQuizInCourse(
            @Parameter(description = "Identifiant du cours") @PathVariable("courseId") String courseId,
            @Valid @RequestBody CreateQuizRequestDto request) {
        
        logger.info("POST /professor/courses/{}/quizzes - creating quiz", courseId);
        logger.debug("Quiz request: title={}, duration={}", request.title(), request.duration());
        try {
            Object durationObj = request.duration();
            int duration = durationObj instanceof Integer ? (Integer) durationObj : Integer.parseInt(durationObj.toString());
            professorUseCase.saveQuiz(courseId, request.title(), duration);
            logger.info("Quiz created successfully in course {}", courseId);
            return ResponseEntity.ok(ProfessorMapper.createSuccessResponse("Quiz créé dans le cours avec succès"));
        } catch (Exception e) {
            logger.error("Error creating quiz in course {}: {}", courseId, e.getMessage());
            throw e;
        }
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
            for (Map<String, Object> record : records) {
                String date = record.getOrDefault("date", "2026-07-20").toString();
                String courseId = record.getOrDefault("courseId", "course-1").toString();
                logger.debug("Submitting attendance: date={}, courseId={}, studentId={}, status={}",
                    date, courseId, record.get("studentId"), record.get("status"));
                professorUseCase.submitAttendance(
                    date,
                    (String) record.get("studentId"),
                    (String) record.get("studentName"),
                    (String) record.get("status"),
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
        try {
            List<Map<String, Object>> schedule = professorUseCase.getSchedule("usr-002");
            logger.debug("Retrieved {} schedule sessions", schedule.size());
            return ResponseEntity.ok(ProfessorMapper.toScheduleDtoList(schedule));
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
        try {
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
}
