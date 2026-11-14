package ecole_221.sn.ecole.modules.admin.domain.port.repository;

import java.util.List;
import java.util.Map;

public interface AdminRepositoryPort {
    void assignGate(String gate, String guard);
    Map<String, String> getGateAssignment();
    void addRealtimeLog(String message);
    List<String> getRealtimeLogs();
}
