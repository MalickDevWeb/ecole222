package ecole_221.sn.ecole.modules.security.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un profil de vigilance.
 */
@Schema(description = "Profil de vigilance")
public record VigilProfileDto(
    @Schema(description = "Identifiant du profil", example = "vigil-001")
    String id,
    
    @Schema(description = "Nom du profil", example = "Jean Dupont")
    String name,
    
    @Schema(description = "Rôle", example = "ADMIN")
    String role,
    
    @Schema(description = "Identifiant de l'utilisateur", example = "usr-001")
    String userId,
    
    @Schema(description = "Statut", example = "active")
    String status,
    
    @Schema(description = "Date de création", example = "2024-07-28T15:30:00")
    String createdAt
) {
    public VigilProfileDto {
        // Validation can be added here
    }
}