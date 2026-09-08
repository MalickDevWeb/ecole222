package ecole_221.sn.ecole.modules.notifications.domain.model;

import ecole_221.sn.ecole.modules.notifications.domain.valueobject.NotificationType;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.ReadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de Notification.
 * Permet d'accéder au constructeur package-private de l'agrégat racine.
 */
public class NotificationFactory {

    // Constructeur privé pour empêcher l'instanciation de la Factory
    private NotificationFactory() {
        throw new UnsupportedOperationException("Classe utilitaire - ne pas instancier");
    }

    /**
     * Crée une nouvelle notification avec un ID généré, le statut UNREAD par défaut et l'horodatage actuel.
     */
    public static Notification create(String recipientId, String title, String content, NotificationType type) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new Notification(id, recipientId, title, content, type, ReadStatus.UNREAD, now);
    }
    
    /**
     * Crée une nouvelle notification avec un ID généré et un statut personnalisé.
     */
    public static Notification create(String recipientId, String title, String content, 
                                    NotificationType type, ReadStatus readStatus) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new Notification(id, recipientId, title, content, type, readStatus, now);
    }
    
    /**
     * Reconstruit une notification existante (pour la persistance).
     */
    public static Notification reconstruct(String id, String recipientId, String title, String content, 
                                         NotificationType type, ReadStatus readStatus, LocalDateTime createdAt) {
        return new Notification(id, recipientId, title, content, type, readStatus, createdAt);
    }
}