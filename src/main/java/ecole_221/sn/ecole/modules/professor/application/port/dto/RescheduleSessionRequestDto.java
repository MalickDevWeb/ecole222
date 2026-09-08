package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête de report d'une session.
 */
@Schema(description = "Requête pour reporter une session")
public record RescheduleSessionRequestDto(
    @Schema(description = "Jour de la nouvelle session", example = "Mardi")
    @NotBlank(message = "Le jour est obligatoire")
    String day,
    
    @Schema(description = "Heure de la nouvelle session", example = "10:00")
    @NotBlank(message = "L'heure est obligatoire")
    String time,
    
    @Schema(description = "Salle de la nouvelle session", example = "Salle 103")
    @NotBlank(message = "La salle est obligatoire")
    String room
) {
    public RescheduleSessionRequestDto {
        // Validation is handled by annotations
    }
}
