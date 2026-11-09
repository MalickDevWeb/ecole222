package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Filtré par école (@TenantId) : une école ne lit jamais les paramètres d'une autre. */
@Repository
public interface ParametresCandidaturesJpaRepository extends JpaRepository<ParametresCandidaturesJpaEntity, String> {
}
