package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import ecole_221.sn.ecole.modules.notifications.domain.exception.NotificationNotFoundException;
import ecole_221.sn.ecole.modules.notifications.domain.model.Notification;
import ecole_221.sn.ecole.modules.notifications.domain.model.NotificationFactory;
import ecole_221.sn.ecole.modules.notifications.domain.port.repository.NotificationRepositoryPort;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.NotificationType;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.ReadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Notifications.
 * Implémente NotificationRepositoryPort en utilisant les repositories JPA existants.
 */
// @Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationRepositoryAdapter.class);
    
    private final NotificationJpaRepository notificationJpaRepository;
    
    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }
    
    @Override
    public Notification saveNotification(Notification notification) {
        logger.debug("Sauvegarde de la notification: {}", notification.getId());
        NotificationJpaEntity entity = toNotificationEntity(notification);
        NotificationJpaEntity savedEntity = notificationJpaRepository.save(entity);
        return toNotification(savedEntity);
    }
    
    @Override
    public Optional<Notification> findNotificationById(String id) {
        logger.debug("Récupération de la notification: {}", id);
        return notificationJpaRepository.findById(id)
            .map(this::toNotification);
    }
    
    @Override
    public List<Notification> findAllNotifications() {
        logger.debug("Récupération de toutes les notifications depuis JPA");
        return notificationJpaRepository.findAll().stream()
            .map(this::toNotification)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteNotification(String id) {
        logger.debug("Suppression de la notification: {}", id);
        if (!notificationJpaRepository.existsById(id)) {
            throw new NotificationNotFoundException(id);
        }
        notificationJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Notification> findNotificationsByRecipientId(String recipientId) {
        logger.debug("Récupération des notifications par destinataire: {}", recipientId);
        return notificationJpaRepository.findByRecipientId(recipientId).stream()
            .map(this::toNotification)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Notification> findUnreadNotificationsByRecipientId(String recipientId) {
        logger.debug("Récupération des notifications non lues par destinataire: {}", recipientId);
        return notificationJpaRepository.findUnreadByRecipientId(recipientId).stream()
            .map(this::toNotification)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Notification> findNotificationsByReadStatus(ReadStatus readStatus) {
        logger.debug("Récupération des notifications par statut: {}", readStatus);
        // Temporarily disabled due to missing property in JPA entity
        // return notificationJpaRepository.findByReadStatus(readStatus.value()).stream()
        //     .map(this::toNotification)
        //     .collect(Collectors.toList());
        return List.of();
    }
    
    @Override
    public void markAsRead(String id) {
        logger.debug("Marquage de la notification comme lue: {}", id);
        NotificationJpaEntity entity = notificationJpaRepository.findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
        entity.setIsRead(true);
        entity.setReadAt(java.time.LocalDateTime.now());
        notificationJpaRepository.save(entity);
    }
    
    @Override
    public void markAsUnread(String id) {
        logger.debug("Marquage de la notification comme non lue: {}", id);
        NotificationJpaEntity entity = notificationJpaRepository.findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
        entity.setIsRead(false);
        entity.setReadAt(null);
        notificationJpaRepository.save(entity);
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Notification toNotification(NotificationJpaEntity entity) {
        return NotificationFactory.reconstruct(
            entity.getId(),
            entity.getRecipientId(),
            entity.getTitle(),
            entity.getMessage(),
            new NotificationType(entity.getNotificationType()),
            entity.getIsRead() != null && entity.getIsRead() ? ReadStatus.READ : ReadStatus.UNREAD,
            entity.getCreatedAt()
        );
    }
    
    private NotificationJpaEntity toNotificationEntity(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(notification.getId());
        entity.setRecipientId(notification.getRecipientId());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getContent());
        entity.setNotificationType(notification.getType().value());
        entity.setIsRead(notification.isRead());
        entity.setCreatedAt(notification.getCreatedAt());
        if (notification.isRead()) {
            entity.setReadAt(java.time.LocalDateTime.now());
        }
        return entity;
    }
}