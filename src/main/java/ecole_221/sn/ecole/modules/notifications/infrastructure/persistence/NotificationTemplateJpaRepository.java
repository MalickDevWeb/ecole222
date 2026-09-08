package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateJpaEntity, String> {
    Optional<NotificationTemplateJpaEntity> findByTemplateKey(String templateKey);
    List<NotificationTemplateJpaEntity> findByCategory(String category);
    List<NotificationTemplateJpaEntity> findByTemplateType(String templateType);
    List<NotificationTemplateJpaEntity> findByIsActiveTrue();
    
    @Query("SELECT t FROM NotificationTemplateJpaEntity t WHERE t.isActive = true AND t.category = :category")
    List<NotificationTemplateJpaEntity> findActiveByCategory(@Param("category") String category);
    
    @Query("SELECT t FROM NotificationTemplateJpaEntity t WHERE t.templateType = :type AND t.isActive = true")
    List<NotificationTemplateJpaEntity> findActiveByType(@Param("type") String type);
}