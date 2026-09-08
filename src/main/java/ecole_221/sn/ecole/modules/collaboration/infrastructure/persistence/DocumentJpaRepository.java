package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {
    List<DocumentJpaEntity> findByGroupId(String groupId);
}
