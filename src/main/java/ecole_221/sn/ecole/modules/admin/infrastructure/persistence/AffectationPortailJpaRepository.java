package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Filtré par école (@TenantId) : une seule affectation par école. */
@Repository
public interface AffectationPortailJpaRepository extends JpaRepository<AffectationPortailJpaEntity, String> {
}
