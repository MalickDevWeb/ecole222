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
        String actionType = action.getType().name();
        switch (actionType) {
            case "ATTENDANCE_SELF" -> studentUseCase.submitSelfAttendance(
                    (String) payload.getOrDefault("studentId", "usr-001"),
                    (String) payload.get("type"),
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
                    (String) payload.getOrDefault("date", "2026-07-20"),
                    (String) payload.get("studentId"),
                    (String) payload.get("studentName"),
                    (String) payload.get("status"),
                    (String) payload.getOrDefault("courseId", "course-1")
            );
            default -> throw new IllegalArgumentException("Type d'action de synchronisation inconnu: " + actionType);
        }
    }
}
