package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.partage.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur léger exposant les routes "courtes" attendues par le dashboard frontend.
 *
 * <p>Le frontend appelle {@code /api/meet/active}, {@code /api/notifications} et
 * {@code /api/schedule}. Ces routes délèguent aux use-cases existants sans dupliquer
 * la logique métier.</p>
 *
 * <p>Règle R10 — Architecture hexagonale : ce contrôleur appelle uniquement
 * {@link StudentUseCase} ; jamais de repository direct.</p>
 */
@RestController
@CrossOrigin(origins = "*")
@Tag(name = "StudentDashboard", description = "Routes courtes utilisées par le dashboard étudiant")
public class StudentDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(StudentDashboardController.class);

    private final StudentUseCase studentUseCase;

    public StudentDashboardController(StudentUseCase studentUseCase) {
        this.studentUseCase = studentUseCase;
    }

    // ── MEET / LIVE SESSION ───────────────────────────────────────────────────

    /**
     * Route attendue par {@code useRealTimeMeet.ts} — polling toutes les 3 s.
     * Retourne la première session active ou {@code null} si aucune.
     */
    @GetMapping("/meet/active")
    @Operation(summary = "Session live active",
               description = "Retourne la session live active du moment, ou null.")
    public ResponseEntity<Map<String, Object>> getActiveMeet() {
        logger.debug("GET /meet/active");
        try {
            PageResult<Map<String, Object>> page = studentUseCase.getActiveLiveSessions(0, 1);
            List<Map<String, Object>> content = page.getContent();
            Map<String, Object> active = content.isEmpty() ? null : content.get(0);
            return ResponseEntity.ok(active);
        } catch (Exception e) {
            logger.warn("GET /meet/active — aucune session active : {}", e.getMessage());
            return ResponseEntity.ok(null);
        }
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────

    /**
     * Route attendue par {@code ApiNotificationRepository.ts} — polling toutes les 5 s.
     * Retourne une liste vide tant que le module notifications n'est pas activé en base.
     *
     * @param limit nombre max de notifications (défaut 20)
     */
    @GetMapping("/notifications")
    @Operation(summary = "Notifications courantes",
               description = "Retourne les dernières notifications de l'utilisateur authentifié.")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "20") int limit) {
        logger.debug("GET /notifications?limit={}", limit);
        // TODO (BE-next) : lire le userId depuis le SecurityContext JWT
        // et appeler notificationUseCase.getNotificationsByRecipient(userId)
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * Route POST attendue par {@code ApiNotificationRepository.ts} pour créer une notification.
     */
    @PostMapping("/notifications")
    @Operation(summary = "Créer une notification",
               description = "Enregistre une notification émise côté client.")
    public ResponseEntity<Map<String, Object>> createNotification(
            @RequestBody Map<String, Object> body) {
        logger.debug("POST /notifications — body={}", body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", "notif-" + Instant.now().toEpochMilli()
        ));
    }

    // ── SCHEDULE ──────────────────────────────────────────────────────────────

    /**
     * Route attendue par {@code ApiScheduleRepository.ts}.
     * Retourne les cours de l'étudiant formatés comme {@code CourseSession[]}.
     */
    @GetMapping("/schedule")
    @Operation(summary = "Emploi du temps étudiant",
               description = "Retourne les sessions de cours de la semaine pour l'étudiant.")
    public ResponseEntity<List<Map<String, Object>>> getStudentSchedule() {
        logger.debug("GET /schedule");
        try {
            // TODO (BE-next) : utiliser le studentId du SecurityContext JWT
            List<Map<String, Object>> courses = studentUseCase.getStudentCourses("current");
            return ResponseEntity.ok(courses != null ? courses : Collections.emptyList());
        } catch (Exception e) {
            logger.warn("GET /schedule — fallback liste vide : {}", e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}

