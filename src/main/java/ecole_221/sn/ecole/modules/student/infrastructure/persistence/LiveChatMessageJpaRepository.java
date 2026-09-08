package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveChatMessageJpaRepository extends JpaRepository<LiveChatMessageJpaEntity, String> {
    List<LiveChatMessageJpaEntity> findBySessionId(String sessionId);
    List<LiveChatMessageJpaEntity> findBySessionIdOrderByTimestampAsc(String sessionId);
}