package ecole_221.sn.ecole.modules.planning.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une filière.
 */
@Schema(description = "Filière")
public record FiliereDto(
    @Schema(description = "Identifiant de la filière", example = "fil-001")
    String id,
    
    @Schema(description = "Nom de la filière", example = "Génie Logiciel")
    String name,
    
    @Schema(description = "Code de la filière", example = "GL")
    String code,
    
    @Schema(description = "Description de la filière")
    String description,
    
    @Schema(description = "Niveau d'études", example = "Licence, Master")
    String level
) {
    public FiliereDto {
        // Validation can be added here
    }
}