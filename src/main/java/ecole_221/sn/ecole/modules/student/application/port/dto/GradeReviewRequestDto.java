package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la requête de demande de révision de note.
 */
@Schema(description = "Requête pour demander une révision de note")
public record GradeReviewRequestDto(
    @Schema(description = "Raison de la demande de révision")
    @NotBlank(message = "La raison est obligatoire")
    @Size(min = 10, max = 500, message = "La raison doit contenir entre 10 et 500 caractères")
    String reason
) {
    public GradeReviewRequestDto {
        // Validation is handled by annotations
    }
}
