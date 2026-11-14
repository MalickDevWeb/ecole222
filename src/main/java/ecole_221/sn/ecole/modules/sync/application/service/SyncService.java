package ecole_221.sn.ecole.modules.sync.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.sync.application.port.usecase.SyncUseCase;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncAction;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResult;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResultFactory;
import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SyncService implements SyncUseCase {

    private final StudentUseCase studentUseCase;
    private final ProfessorUseCase professorUseCase;
    private final SecurityUseCase securityUseCase;
    private final AdminUseCase adminUseCase;

    public SyncService(StudentUseCase studentUseCase, ProfessorUseCase professorUseCase,
                       SecurityUseCase securityUseCase, AdminUseCase adminUseCase) {
        this.studentUseCase = studentUseCase;
        this.professorUseCase = professorUseCase;
        this.securityUseCase = securityUseCase;
        this.adminUseCase = adminUseCase;
    }

    /**
     * 2026-11-13 (sécurité) : contrôles de l'espace professeur, réutilisés ici. Avant, /sync était ouvert à tout compte
     * connecté sans aucun contrôle de rôle : un élève pouvait s'attribuer une note (GRADE_SAVE), faire l'appel
     * (ATTENDANCE_SUBMIT), écrire dans le journal de l'administration (LOG_ADD) ou journaliser un passage (BADGE_SCAN).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.professor.infrastructure.web.AccesCoursInterceptor accesCours;

    private static final java.util.Set<String> ROLES_ELEVE = java.util.Set.of("ETUDIANT", "STUDENT");
    private static final java.util.Set<String> ROLES_ADMIN = java.util.Set.of("ADMIN", "ADMINISTRATOR", "SUPER_ADMIN");
    private static final java.util.Set<String> ROLES_ENSEIGNANT = java.util.Set.of("PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN");
    private static final java.util.Set<String> ROLES_CONTROLE_ACCES = java.util.Set.of("VIGIL", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN");

    /** Rôle du compte connecté autorisé pour ce type d'action, sinon refus. */
    static void exigerRole(java.util.Set<String> roles, String action) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean ok = auth != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority().startsWith("ROLE_") ? a.getAuthority().substring(5) : a.getAuthority())
                .anyMatch(roles::contains);
        if (!ok) {
            throw new IllegalArgumentException("Action « " + action + " » refusée : votre rôle ne le permet pas.");
        }
    }

    /** Cours du professeur connecté dans cette école, et élève membre de l'école (mêmes règles que l'espace professeur). */
    private void exigerCoursEtEleve(String courseId, String studentId) {
        if (accesCours == null) {
            throw new IllegalArgumentException("Contrôle du cours indisponible : action refusée.");
        }
        try {
            accesCours.verifierCours(courseId);
            accesCours.verifierEleve(studentId);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw new IllegalArgumentException(e.getReason() == null ? "Action refusée." : e.getReason());
        }
    }

    private static Double noteBornee(Object v, String libelle) {
        if (v == null) return null;
        if (!(v instanceof Number n)) throw new IllegalArgumentException(libelle + " invalide.");
        double d = n.doubleValue();
        if (Double.isNaN(d) || d < 0 || d > 20) throw new IllegalArgumentException(libelle + " doit être comprise entre 0 et 20.");
        return d;
    }

    @Override
    public List<SyncResult> synchronize(List<SyncAction> actions) {
        List<SyncResult> results = new ArrayList<>();
        if (actions == null || actions.isEmpty()) {
            return results;
        }

        for (SyncAction action : actions) {
            try {
                processSingleAction(action);
                journaliser(action);
                results.add(SyncResultFactory.success(action.getId(), "Synchronisé avec succès"));
            } catch (Exception e) {
                results.add(SyncResultFactory.failure(action.getId(), e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 2026-11-14 (ENF-07) : un lot /sync contient plusieurs écritures ; chacune réussie est inscrite au journal d'audit
     * (l'intercepteur HTTP n'en voit qu'une : « POST /sync »). Type d'action et identifiant du lot, sans les données.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit journal;

    private void journaliser(SyncAction action) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (journal == null || auth == null || !(auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            return;
        }
        String nom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
        String role = auth.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5)).findFirst().orElse(null);
        String type = action.getType().name();
        journal.inscrire(new ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit.Entree(
                ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null),
                u.getId(), nom.isEmpty() ? u.getEmail() : nom, role, "UPDATE", "sync/" + type, action.getId(),
                "Synchronisation hors ligne — " + type, null, null, "sync"));
    }

    private void processSingleAction(SyncAction action) {
        Map<String, Object> payload = action.getPayload();
        if (payload == null) {
            throw new IllegalArgumentException("Action sans données.");
        }
        String actionType = action.getType().name();
        switch (actionType) {
            case "ATTENDANCE_SELF" -> {
                exigerRole(ROLES_ELEVE, "pointage");
                studentUseCase.submitSelfAttendance(
                    // Avant : « usr-001 » par défaut → la présence était attribuée à un autre élève.
                    // Désormais : l'élève CONNECTÉ ; un studentId différent dans la charge est refusé.
                    etudiantConnecte(payload.get("studentId")),
                    obligatoire(payload, "type", "le type de pointage"),
                    (String) payload.get("method")
                );
            }
            case "BADGE_SCAN" -> {
                exigerRole(ROLES_CONTROLE_ACCES, "scan de badge");
                securityUseCase.processBadgeScan((String) payload.get("badgeId"));
            }
            case "GRADE_SAVE" -> {
                exigerRole(ROLES_ENSEIGNANT, "saisie de note");
                String studentId = obligatoire(payload, "studentId", "l'élève");
                String courseId = obligatoire(payload, "courseId", "le cours");
                Double cc = noteBornee(payload.get("cc"), "La note de contrôle continu");
                Double exam = noteBornee(payload.get("examen"), "La note d'examen");
                exigerCoursEtEleve(courseId, studentId);
                professorUseCase.saveGrade(studentId, courseId, cc, exam);
            }
            case "LOG_ADD" -> {
                exigerRole(ROLES_ADMIN, "journal");
                adminUseCase.addLog((String) payload.get("message"));
            }
            case "ATTENDANCE_SUBMIT" -> {
                exigerRole(ROLES_ENSEIGNANT, "appel");
                exigerCoursEtEleve(obligatoire(payload, "courseId", "le cours"), obligatoire(payload, "studentId", "l'élève"));
                professorUseCase.submitAttendance(
                    // Avant : date « 2026-07-20 » et cours « course-1 » inventés si absents → action refusée.
                    dateIso(obligatoire(payload, "date", "la date")),
                    obligatoire(payload, "studentId", "l'étudiant"),
                    (String) payload.get("studentName"),
                    obligatoire(payload, "status", "le statut"),
                    obligatoire(payload, "courseId", "le cours")
                );
            }
            default -> throw new IllegalArgumentException("Type d'action de synchronisation inconnu: " + actionType);
        }
    }

    private static String obligatoire(Map<String, Object> payload, String cle, String libelle) {
        Object v = payload.get(cle);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException("Champ obligatoire manquant : " + libelle + ".");
        }
        return v.toString().trim();
    }

    private static String dateIso(String date) {
        try {
            return java.time.LocalDate.parse(date).toString();
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide (format attendu AAAA-MM-JJ) : " + date);
        }
    }

    /** Identifiant de l'élève authentifié ; refuse une charge qui désigne un autre élève. */
    static String etudiantConnecte(Object studentIdDeclare) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            throw new IllegalArgumentException("Pointage refusé : aucun élève authentifié.");
        }
        if (studentIdDeclare != null && !studentIdDeclare.toString().isBlank()
                && !studentIdDeclare.toString().equals(u.getId())) {
            throw new IllegalArgumentException("Pointage refusé : il ne concerne pas l'élève connecté.");
        }
        return u.getId();
    }
}
