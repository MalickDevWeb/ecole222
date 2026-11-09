package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryAdminRepository implements AdminRepositoryPort {

    private final Map<String, String> gateAssignment = new ConcurrentHashMap<>();
    private final List<String> realtimeLogs = new CopyOnWriteArrayList<>();
    /** Statuts de frais persistés par école (plus de statuts inventés pour usr-001 / std-00x). */
    private final StatutFraisJpaRepository statutsFrais;

    public InMemoryAdminRepository(StatutFraisJpaRepository statutsFrais) {
        this.statutsFrais = statutsFrais;
        gateAssignment.put("gate", "Porte Principale");
        // plus de gardien inventé : « Non assigné » tant qu'aucune affectation n'est saisie

        realtimeLogs.add("[" + LocalDateTime.now() + "] Système de contrôle initialisé.");
    }

    @Override
    public void assignGate(String gate, String guard) {
        gateAssignment.put("gate", gate);
        gateAssignment.put("guard", guard);
    }

    @Override
    public Map<String, String> getGateAssignment() {
        return new HashMap<>(gateAssignment);
    }

    @Override
    public void updateStudentStatus(String studentId, StatutFrais status) {
        StatutFraisJpaEntity ligne = statutsFrais.findById(studentId)
                .orElseGet(() -> new StatutFraisJpaEntity(studentId, status.name()));
        ligne.setStatut(status.name());
        statutsFrais.save(ligne);
    }

    @Override
    public void addRealtimeLog(String message) {
        realtimeLogs.add(0, "[" + LocalDateTime.now() + "] " + message);
    }

    @Override
    public List<String> getRealtimeLogs() {
        return new ArrayList<>(realtimeLogs);
    }

    @Override
    public StatutFrais getStudentStatus(String studentId) {
        // null = non renseigné : l'application ne présume JAMAIS qu'un élève est « En Règle »
        return statutsFrais.findById(studentId).map(l -> StatutFrais.valueOf(l.getStatut())).orElse(null);
    }
}
