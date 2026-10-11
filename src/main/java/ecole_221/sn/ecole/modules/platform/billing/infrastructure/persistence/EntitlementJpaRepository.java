package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntitlementJpaRepository extends JpaRepository<EntitlementJpaEntity, String> {
    List<EntitlementJpaEntity> findByPlanId(String planId);
    void deleteByPlanId(String planId);
}
