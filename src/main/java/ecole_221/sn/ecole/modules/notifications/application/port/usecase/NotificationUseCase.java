package ecole_221.sn.ecole.modules.notifications.application.port.usecase;

import ecole_221.sn.ecole.modules.notifications.application.port.dto.NotificationDto;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationTemplateJpaEntity;
import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Notifications.
 * Gère les notifications et les templates de notification.
 */
public interface NotificationUseCase {
    
    // ==================== GESTION DES NOTIFICATIONS ====================
    
    /**
     * Récupère toutes les notifications.
     */
    List<NotificationDto> getAllNotifications();
    
    /**
     * Récupère les notifications par destinataire.
     */
    List<NotificationDto> getNotificationsByRecipient(String recipientId);
    
    /**
     * Récupère les notifications non lues par destinataire.
     */
    List<NotificationDto> getUnreadNotifications(String recipientId);
    
    /**
     * Crée une nouvelle notification.
     */
    NotificationDto createNotification(NotificationDto notification);
    
    /**
     * Marque une notification comme lue.
     */
    NotificationDto markAsRead(String id);
    
    /**
     * Marque toutes les notifications comme lues.
     */
    void markAllAsRead(String recipientId);
    
    /**
     * Supprime une notification.
     */
    void deleteNotification(String id);
    
    // ==================== GESTION DES TEMPLATES (compatibilité JPA) ====================
    
    List<NotificationTemplateJpaEntity> getAllTemplates();
    NotificationTemplateJpaEntity getTemplateByKey(String templateKey);
    NotificationTemplateJpaEntity createTemplate(NotificationTemplateJpaEntity template);
    NotificationTemplateJpaEntity updateTemplate(String id, NotificationTemplateJpaEntity template);
    
    // ==================== ENVOI DE NOTIFICATIONS (compatibilité JPA) ====================
    
    NotificationJpaEntity sendNotification(String recipientId, String recipientRole, String title, 
                                         String message, String category, String priority);
    NotificationJpaEntity sendFromTemplate(String templateKey, String recipientId, Map<String, Object> variables);
    void sendBulkNotification(List<String> recipientIds, String title, String message, String category);
    
    // ==================== HUB DE NOTIFICATIONS (compatibilité JPA) ====================
    
    Map<String, Object> getNotificationHub(String recipientId);
    Long getUnreadCount(String recipientId);
    List<NotificationJpaEntity> getNotificationsByCategory(String category);
}