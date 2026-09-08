package ecole_221.sn.ecole.modules.student.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO pour la réponse après soumission d'une réaction.
 */
@Schema(description = "Réponse après soumission d'une réaction en live")
public record ReactionResponseDto(
    @Schema(description = "Succès de l'opération")
    boolean success,
    
    @Schema(description = "Identifiant de la session")
    String sessionId,
    
    @Schema(description = "Type de réaction")
    ReactionType reaction
) {
    public ReactionResponseDto {
        // Validation can be added here
    }
}
