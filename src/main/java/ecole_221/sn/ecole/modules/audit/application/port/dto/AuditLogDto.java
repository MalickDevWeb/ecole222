package ecole_221.sn.ecole.modules.audit.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un log d'audit.
 */
@Schema(description = "Log d'audit")
public record AuditLogDto(
    @Schema(description = "Identifiant du log", example = "audit-001")
    String id,
    
    @Schema(description = "Utilisateur concerné", example = "usr-001")
    String userId,
    
    @Schema(description = "Action effectuée", example = "LOGIN")
    String action,
    
    @Schema(description = "Description de l'action", example = "Connexion utilisateur")
    String description,
    
    @Schema(description = "Timestamp de l'action", example = "2024-07-28T15:30:00")
    String timestamp,
    
    @Schema(description = "Adresse IP", example = "192.168.1.1")
    String ipAddress,
    
    @Schema(description = "User agent", example = "Mozilla/5.0")
    String userAgent,

    @Schema(description = "Nom de l'auteur au moment de l'action", example = "Awa Diop")
    String actorName,

    @Schema(description = "Rôle de l'auteur au moment de l'action", example = "SECRETAIRE")
    String actorRole
) {
    /** Entrée sans nom ni rôle d'auteur (écriture explicite par POST /audit/logs). */
    public AuditLogDto(String id, String userId, String action, String description, String timestamp, String ipAddress,
                       String userAgent) {
        this(id, userId, action, description, timestamp, ipAddress, userAgent, null, null);
    }
}