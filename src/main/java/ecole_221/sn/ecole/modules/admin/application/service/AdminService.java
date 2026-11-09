package ecole_221.sn.ecole.modules.admin.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;

import java.util.List;
import java.util.Map;

@Service
public class AdminService implements AdminUseCase {

    private final AdminRepositoryPort repositoryPort;
    private final ParametresCandidaturesService parametresCandidatures;

    public AdminService(AdminRepositoryPort repositoryPort, ParametresCandidaturesService parametresCandidatures) {
        this.repositoryPort = repositoryPort;
        this.parametresCandidatures = parametresCandidatures;
    }

    @Override
    public void assignGate(String gate, String guard) {
        repositoryPort.assignGate(gate, guard);
        repositoryPort.addRealtimeLog("Portail '" + gate + "' assigné au vigil '" + guard + "'");
    }

    @Override
    public Map<String, String> getGateAssignment() {
        return repositoryPort.getGateAssignment();
    }

    @Override
    public void updateStudentStatus(String studentId, StatutFrais status) {
        repositoryPort.updateStudentStatus(studentId, status);
        repositoryPort.addRealtimeLog("Statut financier de l'étudiant '" + studentId + "' mis à jour à '" + status + "'");
    }

    @Override
    public List<String> getRealtimeLogs() {
        return repositoryPort.getRealtimeLogs();
    }

    @Override
    public void addLog(String log) {
        repositoryPort.addRealtimeLog(log);
    }

    @Override
    public void updateCandidaturesSettings(boolean enabled) {
        // Avant : seul un message de journal était écrit (rien n'était enregistré). Désormais la même table
        // que /candidatures/settings, pour l'école courante.
        parametresCandidatures.definirOuverture(enabled);
        repositoryPort.addRealtimeLog("Paramètres de candidature mis à jour: " + (enabled ? "activé" : "désactivé"));
    }
}
