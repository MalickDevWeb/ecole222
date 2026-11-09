package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Filtré par école (@TenantId sur l'entité). */
@Repository
public interface InscriptionClasseJpaRepository extends JpaRepository<InscriptionClasseJpaEntity, String> {
    Optional<InscriptionClasseJpaEntity> findFirstByUserId(String userId);
    List<InscriptionClasseJpaEntity> findByUserId(String userId);
}
