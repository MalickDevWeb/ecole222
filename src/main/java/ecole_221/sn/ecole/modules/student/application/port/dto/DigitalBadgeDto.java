package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un badge numérique étudiant avec QR code.
 */
@Schema(description = "Badge numérique étudiant avec QR code sécurisé")
public record DigitalBadgeDto(
    @Schema(description = "Identifiant du badge", example = "badge-usr-001")
    String id,
    
    @Schema(description = "Identifiant de l'étudiant", example = "usr-001")
    String studentId,
    
    @Schema(description = "Nom complet de l'étudiant", example = "Mamadou Sall")
    String studentName,
    
    @Schema(description = "Matricule étudiant", example = "SN-2026-1234")
    String matricule,
    
    @Schema(description = "Nom de la classe", example = "M1 GL")
    String classe,
    
    @Schema(description = "Payload du QR code (à scanner)")
    String qrPayload,
    
    @Schema(description = "Token HMAC pour validation")
    String token,
    
    @Schema(description = "Minute epoch pour expiration")
    long minuteEpoch,
    
    @Schema(description = "Durée de validité en secondes", example = "60")
    int expiresInSeconds,
    
    @Schema(description = "Statut du badge", example = "ACTIF")
    String status
) {
    public DigitalBadgeDto {
        // Can add validation logic here
    }
}
