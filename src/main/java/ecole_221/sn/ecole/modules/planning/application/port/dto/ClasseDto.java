package ecole_221.sn.ecole.modules.planning.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une classe.
 */
@Schema(description = "Classe")
public record ClasseDto(
    @Schema(description = "Identifiant de la classe", example = "class-001")
    String id,
    
    @Schema(description = "Nom de la classe", example = "L1 GL A")
    String name,
    
    @Schema(description = "Code de la classe", example = "L1-GL-A")
    String code,
    
    @Schema(description = "Identifiant de la filière associée", example = "fil-001")
    String filiereId,
    
    @Schema(description = "Niveau", example = "L1")
    String level,
    
    @Schema(description = "Effectif maximum", example = "30")
    int capacity,
    
    @Schema(description = "Effectif actuel", example = "25")
    int currentSize
) {
    public ClasseDto {
        // Validation can be added here
    }
}