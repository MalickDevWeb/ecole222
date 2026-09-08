package ecole_221.sn.ecole.modules.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VigilProfileJpaRepository extends JpaRepository<VigilProfileJpaEntity, String> {
    Optional<VigilProfileJpaEntity> findByBadgeId(String badgeId);
    Optional<VigilProfileJpaEntity> findById(String id);
    
    /**
     * Récupère les profils de vigilance par rôle.
     */
    List<VigilProfileJpaEntity> findByRole(String role);
}