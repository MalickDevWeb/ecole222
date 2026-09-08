package ecole_221.sn.ecole.modules.student.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la requête de soumission d'une réaction.
 */
@Schema(description = "Requête pour soumettre une réaction en live")
public record ReactionRequestDto(
    @Schema(description = "Type de réaction", example = "LIKE")
    @NotNull(message = "Le type de réaction est obligatoire")
    ReactionType type
) {
    public ReactionRequestDto {
        // Validation is handled by annotations
    }
}
