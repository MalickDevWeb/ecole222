package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Filtré par école (@TenantId). */
@Repository
public interface DocumentEleveJpaRepository extends JpaRepository<DocumentEleveJpaEntity, String> {
    List<DocumentEleveJpaEntity> findByUserIdOrderByCreatedAtAsc(String userId);
}
