package ecole_221.sn.ecole.modules.collaboration.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un groupe de travail.
 */
@Schema(description = "Groupe de travail")
public record WorkgroupDto(
    @Schema(description = "Identifiant du groupe", example = "wg-001")
    String id,
    
    @Schema(description = "Nom du groupe", example = "Projet Équipe A")
    String name,
    
    @Schema(description = "Description du groupe")
    String description,
    
    @Schema(description = "Identifiant du créateur", example = "usr-001")
    String creatorId,
    
    @Schema(description = "Nombre de membres", example = "5")
    int memberCount
) {
    public WorkgroupDto {
        // Validation can be added here
    }
}