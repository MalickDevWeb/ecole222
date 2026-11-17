package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Requêtes limitées à l'école courante par @TenantId. */
public interface MessageParentJpaRepository extends JpaRepository<MessageParentJpaEntity, String> {
    List<MessageParentJpaEntity> findByParentIdOrderByEnvoyeLeDesc(String parentId);
    List<MessageParentJpaEntity> findByInterlocuteurIdOrderByEnvoyeLeDesc(String interlocuteurId);
}
