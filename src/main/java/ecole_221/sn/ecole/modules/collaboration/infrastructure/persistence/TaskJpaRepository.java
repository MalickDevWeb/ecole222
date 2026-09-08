package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String> {
    List<TaskJpaEntity> findByGroupId(String groupId);
    
    /**
     * Récupère les tâches par assigné.
     */
    List<TaskJpaEntity> findByAssignedTo(String assignedTo);
    
    /**
     * Récupère les tâches par statut.
     */
    List<TaskJpaEntity> findByStatus(String status);
    
    /**
     * Récupère les tâches par priorité.
     */
    List<TaskJpaEntity> findByPriority(String priority);
}
