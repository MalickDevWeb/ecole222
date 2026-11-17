package ecole_221.sn.ecole.modules.notifications.application.service;

import ecole_221.sn.ecole.modules.notifications.application.port.dto.NotificationDto;
import ecole_221.sn.ecole.modules.notifications.application.port.usecase.NotificationUseCase;
import ecole_221.sn.ecole.modules.notifications.domain.model.Notification;
import ecole_221.sn.ecole.modules.notifications.domain.model.NotificationFactory;
import ecole_221.sn.ecole.modules.notifications.domain.port.repository.NotificationRepositoryPort;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.NotificationType;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.ReadStatus;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationUseCaseImpl implements NotificationUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationUseCaseImpl.class);
    
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationJpaRepository notificationRepository;
    private final NotificationTemplateJpaRepository templateRepository;
    
    public NotificationUseCaseImpl(NotificationRepositoryPort notificationRepositoryPort,
                                   NotificationJpaRepository notificationRepository,
                                   NotificationTemplateJpaRepository templateRepository) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
    }
    
    // ==================== GESTION DES NOTIFICATIONS ====================
    
    @Override
    public List<NotificationDto> getAllNotifications() {
        logger.info("Récupération de toutes les notifications");
        return notificationRepositoryPort.findAllNotifications().stream()
            .map(this::toNotificationDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<NotificationDto> getNotificationsByRecipient(String recipientId) {
        logger.info("Récupération des notifications par destinataire: {}", recipientId);
        return notificationRepositoryPort.findNotificationsByRecipientId(recipientId).stream()
            .map(this::toNotificationDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<NotificationDto> getUnreadNotifications(String recipientId) {
        logger.info("Récupération des notifications non lues par destinataire: {}", recipientId);
        return notificationRepositoryPort.findUnreadNotificationsByRecipientId(recipientId).stream()
            .map(this::toNotificationDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public NotificationDto createNotification(NotificationDto notificationDto) {
        logger.info("Création notification: {}", notificationDto.title());
        Notification notification = toNotification(notificationDto);
        Notification savedNotification = notificationRepositoryPort.saveNotification(notification);
        return toNotificationDto(savedNotification);
    }
    
    @Override
    public NotificationDto markAsRead(String id) {
        logger.info("Marquage de la notification comme lue: {}", id);
        notificationRepositoryPort.markAsRead(id);
        return notificationRepositoryPort.findNotificationById(id)
            .map(this::toNotificationDto)
            .orElse(null);
    }
    
    @Override
    public void markAllAsRead(String recipientId) {
        logger.info("Marquage de toutes les notifications comme lues pour: {}", recipientId);
        List<Notification> unreadNotifications = notificationRepositoryPort.findUnreadNotificationsByRecipientId(recipientId);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
            notificationRepositoryPort.saveNotification(notification);
        }
    }
    
    @Override
    public void deleteNotification(String id) {
        logger.info("Suppression de la notification: {}", id);
        notificationRepositoryPort.deleteNotification(id);
    }
    
    // ==================== GESTION DES TEMPLATES (compatibilité JPA) ====================
    
    @Override
    public List<NotificationTemplateJpaEntity> getAllTemplates() {
        return templateRepository.findAll();
    }
    
    @Override
    public NotificationTemplateJpaEntity getTemplateByKey(String templateKey) {
        return templateRepository.findByTemplateKey(templateKey).orElse(null);
    }
    
    @Override
    public NotificationTemplateJpaEntity createTemplate(NotificationTemplateJpaEntity template) {
        return templateRepository.save(template);
    }
    
    @Override
    public NotificationTemplateJpaEntity updateTemplate(String id, NotificationTemplateJpaEntity template) {
        // APP-QA-04 : modèle inexistant → 404 ; champs éditables bornés
        NotificationTemplateJpaEntity existant = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Modèle de notification introuvable."));
        if (template == null) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Aucune donnée reçue.");
        }
        java.util.function.BiFunction<String, Integer, java.util.function.Function<String, String>> borne = (libelle, max) -> v -> {
            if (v == null) return null;
            if (v.isBlank()) throw ecole_221.sn.ecole.partage.ChampsRequete.refus(libelle + " ne peut pas être vide.");
            if (v.strip().length() > max) throw ecole_221.sn.ecole.partage.ChampsRequete.refus(libelle + " ne doit pas dépasser " + max + " caractères.");
            return v.strip();
        };
        if (template.getTemplateName() != null) existant.setTemplateName(borne.apply("Le nom du modèle", 200).apply(template.getTemplateName()));
        if (template.getSubject() != null) existant.setSubject(borne.apply("Le sujet", 200).apply(template.getSubject()));
        if (template.getBodyTemplate() != null) existant.setBodyTemplate(borne.apply("Le texte du modèle", 10_000).apply(template.getBodyTemplate()));
        if (template.getCategory() != null) existant.setCategory(borne.apply("La catégorie", 50).apply(template.getCategory()));
        if (template.getDescription() != null) existant.setDescription(borne.apply("La description", 1_000).apply(template.getDescription()));
        if (template.getTemplateType() != null) {
            if (!java.util.List.of("email", "sms", "push", "in_app").contains(template.getTemplateType())) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le type doit être l'une des valeurs : email, sms, push, in_app.");
            }
            existant.setTemplateType(template.getTemplateType());
        }
        if (template.getDefaultPriority() != null) {
            if (!java.util.List.of("low", "normal", "high", "urgent").contains(template.getDefaultPriority())) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("La priorité doit être l'une des valeurs : low, normal, high, urgent.");
            }
            existant.setDefaultPriority(template.getDefaultPriority());
        }
        if (template.getIsActive() != null) existant.setIsActive(template.getIsActive());
        return templateRepository.save(existant);
    }
    
    // ==================== ENVOI DE NOTIFICATIONS (compatibilité JPA) ====================
    
    @Override
    public NotificationJpaEntity sendNotification(String recipientId, String recipientRole, String title,
                                                  String message, String category, String priority) {
        NotificationJpaEntity notification = new NotificationJpaEntity(
            UUID.randomUUID().toString(),
            recipientId,
            recipientRole,
            title,
            message,
            "info",
            category,
            priority
        );
        notification.setRecipientType("user");
        notification.setIsRead(false);
        NotificationJpaEntity savedNotification = notificationRepository.save(notification);
        return savedNotification;
    }
    
    @Override
    public NotificationJpaEntity sendFromTemplate(String templateKey, String recipientId, Map<String, Object> variables) {
        NotificationTemplateJpaEntity template = templateRepository.findByTemplateKey(templateKey).orElse(null);
        if (template == null) {
            logger.error("Template non trouvé: {}", templateKey);
            return null;
        }
        
        // Remplacement des variables dans le template
        String body = template.getBodyTemplate();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            body = body.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
        }
        
        NotificationJpaEntity notification = new NotificationJpaEntity(
            UUID.randomUUID().toString(),
            recipientId,
            "ALL",
            template.getSubject(),
            body,
            template.getTemplateType(),
            template.getCategory(),
            template.getDefaultPriority()
        );
        
        notification.setActionRequired(template.getActionRequired());
        notification.setActionType(template.getDefaultActionType());
        notification.setActionLink(template.getDefaultActionLink());
        notification.setIsRead(false);
        
        return notificationRepository.save(notification);
    }
    
    @Override
    public void sendBulkNotification(List<String> recipientIds, String title, String message, String category) {
        for (String recipientId : recipientIds) {
            sendNotification(recipientId, "ALL", title, message, category, "normal");
        }
        logger.info("Notification groupée envoyée à {} destinataires", recipientIds.size());
    }
    
    // ==================== HUB DE NOTIFICATIONS (compatibilité JPA) ====================
    
    @Override
    public Map<String, Object> getNotificationHub(String recipientId) {
        Map<String, Object> hub = new HashMap<>();
        
        List<NotificationJpaEntity> allNotifications = notificationRepository.findByRecipientId(recipientId);
        List<NotificationJpaEntity> unreadNotifications = notificationRepository.findUnreadByRecipientId(recipientId);
        
        hub.put("total", allNotifications.size());
        hub.put("unread", unreadNotifications.size());
        hub.put("notifications", allNotifications.stream()
            .sorted(Comparator.comparing(NotificationJpaEntity::getCreatedAt).reversed())
            .collect(Collectors.toList()));
        
        // Par catégorie
        Map<String, Long> byCategory = allNotifications.stream()
            .collect(Collectors.groupingBy(NotificationJpaEntity::getCategory, Collectors.counting()));
        hub.put("byCategory", byCategory);
        
        return hub;
    }
    
    @Override
    public Long getUnreadCount(String recipientId) {
        return notificationRepository.countUnreadByRecipientId(recipientId);
    }
    
    @Override
    public List<NotificationJpaEntity> getNotificationsByCategory(String category) {
        return notificationRepository.findByCategory(category);
    }
    
    // ==================== MAPPERS ====================
    
    private NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getRecipientId(),
            notification.getTitle(),
            notification.getContent(),
            notification.getType().value(),
            notification.getReadStatus().value(),
            notification.getCreatedAt().toString()
        );
    }

    private Notification toNotification(NotificationDto dto) {
        String id = (dto.id() != null && !dto.id().isBlank()) ? dto.id() : UUID.randomUUID().toString();
        String type = "INFO";
        if (dto.type() != null) {
            String t = dto.type().toUpperCase();
            if (t.contains("WARN")) type = "WARNING";
            else if (t.contains("ERR")) type = "ERROR";
            else if (t.contains("SUCC")) type = "SUCCESS";
        }
        ReadStatus readStatus = ReadStatus.UNREAD;
        if (dto.readStatus() != null && dto.readStatus().equalsIgnoreCase("READ")) {
            readStatus = ReadStatus.READ;
        }
        LocalDateTime createdAt;
        try {
            createdAt = (dto.createdAt() != null && !dto.createdAt().isBlank()) ? LocalDateTime.parse(dto.createdAt()) : LocalDateTime.now();
        } catch (Exception e) {
            createdAt = LocalDateTime.now();
        }
        return NotificationFactory.reconstruct(
            id,
            dto.recipientId(),
            dto.title(),
            dto.content(),
            new NotificationType(type),
            readStatus,
            createdAt
        );
    }
}