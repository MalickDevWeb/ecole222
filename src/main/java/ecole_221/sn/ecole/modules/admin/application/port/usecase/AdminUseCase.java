package ecole_221.sn.ecole.modules.admin.application.port.usecase;

import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import java.util.List;
import java.util.Map;

public interface AdminUseCase {
    void assignGate(String gate, String guard);
    Map<String, String> getGateAssignment();
    void updateStudentStatus(String studentId, StatutFrais status);
    List<String> getRealtimeLogs();
    void addLog(String log);
    void updateCandidaturesSettings(boolean enabled);
}
