package ecole_221.sn.ecole.modules.notifications.infrastructure.web;

import ecole_221.sn.ecole.modules.notifications.application.port.dto.NotificationDto;
import ecole_221.sn.ecole.modules.notifications.application.port.usecase.NotificationUseCase;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/v1/notifications", "/api/v1/notifications"})
@CrossOrigin(origins = "*")
@Tag(name = "Notifications", description = "Centre de notifications - envoi, templates, hub")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationUseCase notificationUseCase;
    
    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }
    
    // ==================== ENDPOINTS NOTIFICATIONS ====================
    
    @GetMapping
    @Operation(summary = "Liste des notifications", description = "Retourne toutes les notifications")
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        logger.info("GET /notifications - Récupération notifications");
        return ResponseEntity.ok(notificationUseCase.getAllNotifications());
    }
    
    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Notifications destinataire", description = "Retourne les notifications d'un destinataire")
    public ResponseEntity<List<NotificationDto>> getNotificationsByRecipient(@PathVariable String recipientId) {
        logger.info("GET /notifications/recipient/{} - Récupération notifications destinataire", recipientId);
        return ResponseEntity.ok(notificationUseCase.getNotificationsByRecipient(recipientId));
    }
    
    @GetMapping("/recipient/{recipientId}/unread")
    @Operation(summary = "Notifications non lues", description = "Retourne les notifications non lues d'un destinataire")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(@PathVariable String recipientId) {
        logger.info("GET /notifications/recipient/{}/unread - Récupération notifications non lues", recipientId);
        return ResponseEntity.ok(notificationUseCase.getUnreadNotifications(recipientId));
    }
    
    @PostMapping
    @Operation(summary = "Créer notification", description = "Crée une nouvelle notification")
    public ResponseEntity<NotificationDto> createNotification(@RequestBody NotificationDto notification) {
        logger.info("POST /notifications - Création notification");
        return ResponseEntity.ok(notificationUseCase.createNotification(notification));
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer comme lue", description = "Marque une notification comme lue")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable String id) {
        logger.info("PATCH /notifications/{}/read - Marquer comme lue", id);
        return ResponseEntity.ok(notificationUseCase.markAsRead(id));
    }
    
    @PatchMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Marquer tout comme lu", description = "Marque toutes les notifications d'un destinataire comme lues")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable String recipientId) {
        logger.info("PATCH /notifications/recipient/{}/read-all - Marquer tout comme lu", recipientId);
        notificationUseCase.markAllAsRead(recipientId);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer notification", description = "Supprime une notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        logger.info("DELETE /notifications/{} - Suppression notification", id);
        notificationUseCase.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
    
    // ==================== ENDPOINTS TEMPLATES (compatibilité JPA) ====================
    
    @GetMapping("/templates")
    @Operation(summary = "Liste des templates", description = "Retourne tous les templates de notification")
    public ResponseEntity<List<NotificationTemplateJpaEntity>> getAllTemplates() {
        logger.info("GET /notifications/templates - Récupération templates");
        return ResponseEntity.ok(notificationUseCase.getAllTemplates());
    }
    
    @GetMapping("/templates/{templateKey}")
    @Operation(summary = "Template par clé", description = "Retourne un template spécifique")
    public ResponseEntity<NotificationTemplateJpaEntity> getTemplateByKey(@PathVariable String templateKey) {
        logger.info("GET /notifications/templates/{} - Récupération template", templateKey);
        NotificationTemplateJpaEntity template = notificationUseCase.getTemplateByKey(templateKey);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }
    
    @PostMapping("/templates")
    @Operation(summary = "Créer template", description = "Crée un nouveau template de notification")
    public ResponseEntity<NotificationTemplateJpaEntity> createTemplate(@RequestBody NotificationTemplateJpaEntity template) {
        logger.info("POST /notifications/templates - Création template");
        return ResponseEntity.ok(notificationUseCase.createTemplate(template));
    }
    
    @PutMapping("/templates/{id}")
    @Operation(summary = "Mettre à jour template", description = "Met à jour un template de notification")
    public ResponseEntity<NotificationTemplateJpaEntity> updateTemplate(@PathVariable String id, 
                                                                     @RequestBody NotificationTemplateJpaEntity template) {
        logger.info("PUT /notifications/templates/{} - Mise à jour template", id);
        return ResponseEntity.ok(notificationUseCase.updateTemplate(id, template));
    }
    
    // ==================== ENDPOINTS ENVOI (compatibilité JPA) ====================
    
    @PostMapping("/send")
    @Operation(summary = "Envoyer notification", description = "Envoie une notification à un destinataire")
    public ResponseEntity<NotificationJpaEntity> sendNotification(@RequestBody Map<String, String> body) {
        logger.info("POST /notifications/send - Envoi notification");
        return ResponseEntity.ok(notificationUseCase.sendNotification(
            body.get("recipientId"),
            body.get("recipientRole"),
            body.get("title"),
            body.get("message"),
            body.get("category"),
            body.get("priority")
        ));
    }
    
    @PostMapping("/send-template")
    @Operation(summary = "Envoyer depuis template", description = "Envoie une notification basée sur un template")
    public ResponseEntity<NotificationJpaEntity> sendFromTemplate(@RequestBody Map<String, Object> body) {
        logger.info("POST /notifications/send-template - Envoi depuis template");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        return ResponseEntity.ok(notificationUseCase.sendFromTemplate(
            body.get("templateKey").toString(),
            body.get("recipientId").toString(),
            variables
        ));
    }
    
    @PostMapping("/send-bulk")
    @Operation(summary = "Envoi groupé", description = "Envoie une notification à plusieurs destinataires")
    public ResponseEntity<Map<String, Object>> sendBulkNotification(@RequestBody Map<String, Object> body) {
        logger.info("POST /notifications/send-bulk - Envoi groupé");
        @SuppressWarnings("unchecked")
        List<String> recipientIds = (List<String>) body.get("recipientIds");
        notificationUseCase.sendBulkNotification(
            recipientIds,
            body.get("title").toString(),
            body.get("message").toString(),
            body.get("category").toString()
        );
        return ResponseEntity.ok(Map.of("success", true, "count", recipientIds.size()));
    }
    
    // ==================== ENDPOINTS HUB (compatibilité JPA) ====================
    
    @GetMapping("/hub/{recipientId}")
    @Operation(summary = "Hub notifications", description = "Retourne le hub de notifications d'un destinataire")
    public ResponseEntity<Map<String, Object>> getNotificationHub(@PathVariable String recipientId) {
        logger.info("GET /notifications/hub/{} - Récupération hub notifications", recipientId);
        return ResponseEntity.ok(notificationUseCase.getNotificationHub(recipientId));
    }
    
    @GetMapping("/recipient/{recipientId}/unread-count")
    @Operation(summary = "Compteur non lus", description = "Retourne le nombre de notifications non lues")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String recipientId) {
        logger.info("GET /notifications/recipient/{}/unread-count - Récupération compteur non lus", recipientId);
        return ResponseEntity.ok(notificationUseCase.getUnreadCount(recipientId));
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "Notifications par catégorie", description = "Retourne les notifications par catégorie")
    public ResponseEntity<List<NotificationJpaEntity>> getNotificationsByCategory(@PathVariable String category) {
        logger.info("GET /notifications/category/{} - Récupération notifications par catégorie", category);
        return ResponseEntity.ok(notificationUseCase.getNotificationsByCategory(category));
    }
}