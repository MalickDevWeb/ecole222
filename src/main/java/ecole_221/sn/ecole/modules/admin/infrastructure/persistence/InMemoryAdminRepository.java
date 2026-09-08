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
    private final Map<String, StatutFrais> studentStatuses = new ConcurrentHashMap<>();
    private final List<String> realtimeLogs = new CopyOnWriteArrayList<>();

    public InMemoryAdminRepository() {
        gateAssignment.put("gate", "Porte Principale");
        gateAssignment.put("guard", "Agent Mamadou Diallo");
        
        studentStatuses.put("usr-001", StatutFrais.EN_REGLE);
        studentStatuses.put("std-001", StatutFrais.EN_REGLE);
        studentStatuses.put("std-002", StatutFrais.EN_RETARD);

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
        studentStatuses.put(studentId, status);
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
        return studentStatuses.getOrDefault(studentId, StatutFrais.EN_REGLE);
    }
}
