package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, String> {
}
