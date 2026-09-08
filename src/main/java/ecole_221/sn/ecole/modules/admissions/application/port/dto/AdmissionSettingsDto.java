package ecole_221.sn.ecole.modules.admissions.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant les paramètres d'admission.
 */
@Schema(description = "Paramètres d'admission")
public record AdmissionSettingsDto(
    @Schema(description = "Identifiant des paramètres", example = "settings-001")
    String id,
    
    @Schema(description = "Date de début des candidatures", example = "2024-06-01")
    String startDate,
    
    @Schema(description = "Date de fin des candidatures", example = "2024-08-31")
    String endDate,
    
    @Schema(description = "Note minimale requise", example = "10.0")
    Double minNote,
    
    @Schema(description = "Nombre de places disponibles", example = "100")
    int placesDisponibles,
    
    @Schema(description = "Documents requis", example = "bulletin, lettre, photo")
    String documentsRequis,
    
    @Schema(description = "Statut des admissions", example = "OUVERT")
    String statut
) {
    public AdmissionSettingsDto {
        // Validation can be added here
    }
}