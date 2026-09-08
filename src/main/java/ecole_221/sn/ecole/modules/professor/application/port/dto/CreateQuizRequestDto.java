package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO pour la requête de création d'un quiz.
 */
@Schema(description = "Requête pour créer un quiz")
public record CreateQuizRequestDto(
    @Schema(description = "Titre du quiz", example = "Quiz Spring Boot")
    @NotBlank(message = "Le titre est obligatoire")
    String title,
    
    @Schema(description = "Durée en minutes", example = "30")
    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    Integer duration
) {
    public CreateQuizRequestDto {
        // Validation is handled by annotations
    }
}
