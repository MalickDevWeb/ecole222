package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreneauPlanningJpaRepository extends JpaRepository<CreneauPlanningJpaEntity, String> {
}
