package ecole_221.sn.ecole.modules.platform.billing.domain.port.repository;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepositoryPort {
    Plan save(Plan plan);
    Optional<Plan> findById(String id);
    List<Plan> findAll();
    void deleteById(String id);
}
