package ecole_221.sn.ecole.modules.planning.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une salle de cours.
 */
@Schema(description = "Salle de cours")
public record RoomDto(
    @Schema(description = "Identifiant de la salle", example = "room-001")
    String id,
    
    @Schema(description = "Nom de la salle", example = "Salle A101")
    String name,
    
    @Schema(description = "Capacité de la salle", example = "30")
    int capacity,
    
    @Schema(description = "Équipements disponibles", example = "projecteur,tableau blanc")
    String equipment,
    
    @Schema(description = "Localisation", example = "Bâtiment A, 1er étage")
    String location
) {
    public RoomDto {
        // Validation can be added here
    }
}