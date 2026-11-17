package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Domaines propres déclarés (V17). */
public interface DomaineEcoleJpaRepository extends JpaRepository<DomaineEcoleJpaEntity, String> {

    Optional<DomaineEcoleJpaEntity> findByHostname(String hostname);

    List<DomaineEcoleJpaEntity> findByOrganizationIdOrderByCreatedAtAsc(String organizationId);

    long countByOrganizationId(String organizationId);
}
