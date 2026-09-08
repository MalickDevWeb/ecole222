package ecole_221.sn.ecole.modules.notifications.domain.port.repository;

import ecole_221.sn.ecole.modules.notifications.domain.model.Notification;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.ReadStatus;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des notifications.
 * Définit les opérations de persistance pour l'agrégat Notification.
 */
public interface NotificationRepositoryPort {
    
    /**
     * Sauvegarde une notification (création ou modification).
     */
    Notification saveNotification(Notification notification);
    
    /**
     * Récupère une notification par son identifiant.
     */
    Optional<Notification> findNotificationById(String id);
    
    /**
     * Récupère toutes les notifications.
     */
    List<Notification> findAllNotifications();
    
    /**
     * Supprime une notification par son ID.
     */
    void deleteNotification(String id);
    
    /**
     * Récupère les notifications par destinataire.
     */
    List<Notification> findNotificationsByRecipientId(String recipientId);
    
    /**
     * Récupère les notifications non lues par destinataire.
     */
    List<Notification> findUnreadNotificationsByRecipientId(String recipientId);
    
    /**
     * Récupère les notifications par statut de lecture.
     */
    List<Notification> findNotificationsByReadStatus(ReadStatus readStatus);
    
    /**
     * Marque une notification comme lue.
     */
    void markAsRead(String id);
    
    /**
     * Marque une notification comme non lue.
     */
    void markAsUnread(String id);
}