package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {
    List<NotificationJpaEntity> findByRecipientId(String recipientId);
    List<NotificationJpaEntity> findByRecipientRole(String recipientRole);
    List<NotificationJpaEntity> findByCategory(String category);
    List<NotificationJpaEntity> findByPriority(String priority);
    List<NotificationJpaEntity> findByIsReadFalse();
    
    /**
     * Récupère les notifications non lues par destinataire.
     */
    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.recipientId = :recipientId AND n.isRead = false")
    List<NotificationJpaEntity> findByRecipientIdAndIsReadFalse(@Param("recipientId") String recipientId);
    
    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.recipientId = :recipientId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<NotificationJpaEntity> findUnreadByRecipientId(@Param("recipientId") String recipientId);
    
    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.recipientRole = :role OR n.recipientRole = 'ALL' ORDER BY n.createdAt DESC")
    List<NotificationJpaEntity> findByRoleOrBroadcast(@Param("role") String role);
    
    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<NotificationJpaEntity> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.recipientId = :recipientId AND n.isRead = false")
    Long countUnreadByRecipientId(@Param("recipientId") String recipientId);
}