package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ParentJpaRepository extends JpaRepository<ParentJpaEntity, String> {
    Optional<ParentJpaEntity> findByEmail(String email);
    Optional<ParentJpaEntity> findByUserId(String userId);
    List<ParentJpaEntity> findByNomContainingOrPrenomContaining(String nom, String prenom);
}
