package ecole_221.sn.ecole.modules.notifications.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une notification.
 */
@Schema(description = "Notification")
public record NotificationDto(
    @Schema(description = "Identifiant de la notification", example = "notif-001")
    String id,
    
    @Schema(description = "Identifiant du destinataire", example = "usr-001")
    String recipientId,
    
    @Schema(description = "Titre de la notification", example = "Nouveau message")
    String title,
    
    @Schema(description = "Contenu de la notification")
    String content,
    
    @Schema(description = "Type de notification", example = "INFO")
    String type,
    
    @Schema(description = "Statut de lecture", example = "unread")
    String readStatus,
    
    @Schema(description = "Date de création", example = "2024-07-28T15:30:00")
    String createdAt
) {
    public NotificationDto {
        // Validation can be added here
    }
}