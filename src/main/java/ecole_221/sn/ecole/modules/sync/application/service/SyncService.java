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

    @Override
    public List<SyncResult> synchronize(List<SyncAction> actions) {
        List<SyncResult> results = new ArrayList<>();
        if (actions == null || actions.isEmpty()) {
            return results;
        }

        for (SyncAction action : actions) {
            try {
                processSingleAction(action);
                results.add(SyncResultFactory.success(action.getId(), "Synchronisé avec succès"));
            } catch (Exception e) {
                results.add(SyncResultFactory.failure(action.getId(), e.getMessage()));
            }
        }
        return results;
    }

    private void processSingleAction(SyncAction action) {
        Map<String, Object> payload = action.getPayload();
        if (payload == null) {
            throw new IllegalArgumentException("Action sans données.");
        }
        String actionType = action.getType().name();
        switch (actionType) {
            case "ATTENDANCE_SELF" -> studentUseCase.submitSelfAttendance(
                    // Avant : « usr-001 » par défaut → la présence était attribuée à un autre élève.
                    // Désormais : l'élève CONNECTÉ ; un studentId différent dans la charge est refusé.
                    etudiantConnecte(payload.get("studentId")),
                    obligatoire(payload, "type", "le type de pointage"),
                    (String) payload.get("method")
            );
            case "BADGE_SCAN" -> securityUseCase.processBadgeScan(
                    (String) payload.get("badgeId")
            );
            case "GRADE_SAVE" -> {
                Number cc = (Number) payload.get("cc");
                Number exam = (Number) payload.get("examen");
                professorUseCase.saveGrade(
                        (String) payload.get("studentId"),
                        (String) payload.get("courseId"),
                        cc != null ? cc.doubleValue() : null,
                        exam != null ? exam.doubleValue() : null
                );
            }
            case "LOG_ADD" -> adminUseCase.addLog(
                    (String) payload.get("message")
            );
            case "ATTENDANCE_SUBMIT" -> professorUseCase.submitAttendance(
                    // Avant : date « 2026-07-20 » et cours « course-1 » inventés si absents → action refusée.
                    dateIso(obligatoire(payload, "date", "la date")),
                    obligatoire(payload, "studentId", "l'étudiant"),
                    (String) payload.get("studentName"),
                    obligatoire(payload, "status", "le statut"),
                    obligatoire(payload, "courseId", "le cours")
            );
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
