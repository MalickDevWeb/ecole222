package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Filtré par école (@TenantId). */
@Repository
public interface ProfilEleveJpaRepository extends JpaRepository<ProfilEleveJpaEntity, String> {
    Optional<ProfilEleveJpaEntity> findFirstByUserId(String userId);
}
