package ecole_221.sn.ecole.modules.pointage.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Pointages de l'école courante (filtre d'école appliqué par Hibernate, @TenantId). */
public interface PointageJpaRepository extends JpaRepository<PointageJpaEntity, String> {
    Optional<PointageJpaEntity> findFirstByUtilisateurIdOrderByHorodatageDesc(String utilisateurId);
    List<PointageJpaEntity> findTop60ByUtilisateurIdOrderByHorodatageDesc(String utilisateurId);
}
