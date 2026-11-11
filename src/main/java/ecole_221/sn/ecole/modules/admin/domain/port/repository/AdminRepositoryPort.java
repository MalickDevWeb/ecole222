package ecole_221.sn.ecole.modules.admin.domain.port.repository;

import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import java.util.List;
import java.util.Map;

public interface AdminRepositoryPort {
    void assignGate(String gate, String guard);
    Map<String, String> getGateAssignment();
    void updateStudentStatus(String studentId, StatutFrais status);
    void addRealtimeLog(String message);
    List<String> getRealtimeLogs();
    /** Statut des frais saisi par l'école ; {@code null} si jamais renseigné (aucune valeur présumée). */
    StatutFrais getStudentStatus(String studentId);
}
