package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.partage.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "StudentDashboard", description = "Routes courtes utilisées par le dashboard étudiant")
public class StudentDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(StudentDashboardController.class);

    private final StudentUseCase studentUseCase;
    private final ecole_221.sn.ecole.modules.notifications.application.NotificationsUtilisateurService notificationsUtilisateur;

    public StudentDashboardController(StudentUseCase studentUseCase,
            ecole_221.sn.ecole.modules.notifications.application.NotificationsUtilisateurService notificationsUtilisateur) {
        this.studentUseCase = studentUseCase;
        this.notificationsUtilisateur = notificationsUtilisateur;
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
     * Notifications du compte connecté (cloche de l'élève), plus récentes d'abord.
     * Avant (2026-11-13) : liste toujours vide.
     */
    @GetMapping("/notifications")
    @Operation(summary = "Mes notifications",
               description = "Retourne les dernières notifications du compte connecté.")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationsUtilisateur.mesNotifications(moi(), limit));
    }

    /**
     * Prévient les élèves d'une classe (cours modifié ou annulé). Réservé à l'administration et aux professeurs.
     * Avant (2026-11-13) : ne faisait rien, renvoyait un identifiant inventé, et était ouvert à tout compte.
     */
    @PostMapping("/notifications")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','ADMINISTRATOR','SUPER_ADMIN','PLATFORM_ADMIN','PROFESSEUR','PROFESSOR','TEACHER')")
    @Operation(summary = "Notifier les élèves d'une classe",
               description = "Enregistre une notification pour chaque élève inscrit dans la classe indiquée (classeId).")
    public ResponseEntity<Map<String, Object>> createNotification(
            @RequestBody Map<String, Object> body) {
        int prevenus = notificationsUtilisateur.notifierClasse(texte(body, "classeId", "La classe"),
                texte(body, "title", "Le titre"), texte(body, "desc", "Le message"), texte(body, "icon", "L'icône"));
        return ResponseEntity.ok(Map.of("success", true, "destinataires", prevenus));
    }

    private static String texte(Map<String, Object> body, String cle, String libelle) {
        Object v = body == null ? null : body.get(cle);
        if (v != null && !(v instanceof String)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    libelle + " doit être un texte.");
        }
        return (String) v;
    }

    private static String moi() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u && u.getId() != null) {
            return u.getId();
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
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
        // 2026-11-14 : avant, cours d'un élève nommé « current » (liste toujours vide) et erreurs avalées.
        // Désormais : séances des classes de l'élève CONNECTÉ, planifiées par l'administration.
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        if (emploiDuTemps == null) return ResponseEntity.ok(Collections.emptyList());
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (var s : emploiDuTemps.seances(u.getId())) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", s.id());
            m.put("nom", s.cours());
            m.put("jour", s.jourCode());
            m.put("jourComplet", s.jour());
            m.put("dateStr", "");
            m.put("heureDebut", s.heureDebut());
            m.put("heureFin", s.heureFin());
            m.put("heureStr", s.horaire());
            m.put("type", s.type());
            m.put("salle", s.salle());
            m.put("professeur", s.professeur());
            m.put("description", "");
            m.put("status", "reporte".equals(s.statut()) ? "a_venir" : s.statut());   // reporté : jour et horaire déjà mis à jour
            m.put("cancellationReason", s.motifAnnulation());
            m.put("classe", s.classe());
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    /** Emploi du temps réel de l'élève (2026-11-14) ; facultatif pour les tests sans contexte Spring. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.student.application.service.EmploiDuTempsEleveService emploiDuTemps;
}

