package ecole_221.sn.ecole.modules.audit.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un événement de sécurité.
 */
@Schema(description = "Événement de sécurité")
public record SecurityEventDto(
    @Schema(description = "Identifiant de l'événement", example = "sec-001")
    String id,
    
    @Schema(description = "Type d'événement", example = "LOGIN_ATTEMPT")
    String eventType,
    
    @Schema(description = "Utilisateur concerné", example = "usr-001")
    String userId,
    
    @Schema(description = "Description", example = "Tentative de connexion avec mot de passe incorrect")
    String description,
    
    @Schema(description = "Niveau de gravité", example = "HIGH")
    String severity,
    
    @Schema(description = "Timestamp", example = "2024-07-28T15:30:00")
    String timestamp,
    
    @Schema(description = "Adresse IP source", example = "192.168.1.1")
    String sourceIp
) {
    public SecurityEventDto {
        // Validation can be added here
    }
}