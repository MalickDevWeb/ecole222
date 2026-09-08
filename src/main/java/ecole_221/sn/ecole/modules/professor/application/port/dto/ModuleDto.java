package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un module de cours.
 * Standardisé avec le contrat BACKEND_DATA_CONTRACT.md
 */
@Schema(description = "Module de cours")
public record ModuleDto(
    @Schema(description = "Identifiant du module", example = "module-1")
    String id,
    
    @Schema(description = "Titre du module", example = "Backend Spring Boot")
    String titre,
    
    @Schema(description = "Description du module")
    String description,
    
    @Schema(description = "Nombre d'heures", example = "40")
    int hours,
    
    @Schema(description = "Identifiant du cours parent")
    String courseId
) {
    public ModuleDto {
        // Validation can be added here
    }
}
