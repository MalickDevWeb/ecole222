package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, String> {
    List<MessageJpaEntity> findByGroupId(String groupId);
}
