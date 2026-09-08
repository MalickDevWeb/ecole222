package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ParentEnfantJpaRepository extends JpaRepository<ParentEnfantJpaEntity, String> {
    List<ParentEnfantJpaEntity> findByParentId(String parentId);
    List<ParentEnfantJpaEntity> findByEnfantId(String enfantId);
    Optional<ParentEnfantJpaEntity> findByParentIdAndEnfantId(String parentId, String enfantId);
    void deleteByParentIdAndEnfantId(String parentId, String enfantId);
}
