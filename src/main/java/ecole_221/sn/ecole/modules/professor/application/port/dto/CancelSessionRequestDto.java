package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête d'annulation d'une session.
 */
@Schema(description = "Requête pour annuler une session")
public record CancelSessionRequestDto(
    @Schema(description = "Motif de l'annulation", example = "Maladie")
    @NotBlank(message = "Le motif est obligatoire")
    String reason
) {
    public CancelSessionRequestDto {
        // Validation is handled by annotations
    }
}
