package ecole_221.sn.ecole.modules.planning.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un créneau horaire.
 */
@Schema(description = "Créneau horaire")
public record SlotDto(
    @Schema(description = "Identifiant du créneau", example = "slot-001")
    String id,
    
    @Schema(description = "Heure de début", example = "08:00")
    String startTime,
    
    @Schema(description = "Heure de fin", example = "10:00")
    String endTime,
    
    @Schema(description = "Jour de la semaine", example = "LUNDI")
    String dayOfWeek,
    
    @Schema(description = "Type de créneau", example = "CM")
    String type
) {
    public SlotDto {
        // Validation can be added here
    }
}