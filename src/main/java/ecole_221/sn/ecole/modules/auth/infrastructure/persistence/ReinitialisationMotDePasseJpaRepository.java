package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReinitialisationMotDePasseJpaRepository extends JpaRepository<ReinitialisationMotDePasseJpaEntity, String> {
    Optional<ReinitialisationMotDePasseJpaEntity> findByJetonHache(String jetonHache);
    List<ReinitialisationMotDePasseJpaEntity> findByUserIdAndUtiliseLeIsNull(String userId);
    /** V20 : liens d'une école en attente de l'accord d'un second administrateur. */
    List<ReinitialisationMotDePasseJpaEntity> findByOrganizationIdAndAccordRequisTrueAndAccordeParIsNullAndUtiliseLeIsNullAndAnnuleLeIsNull(String organizationId);
}
