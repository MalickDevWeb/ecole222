package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO pour la requête de création d'une leçon.
 */
@Schema(description = "Requête pour créer une leçon")
public record CreateLessonRequestDto(
    @Schema(description = "Titre de la leçon", example = "Introduction à Spring Boot")
    @NotBlank(message = "Le titre est obligatoire")
    String title,
    
    @Schema(description = "Durée en minutes", example = "90")
    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    Integer duration,
    
    @Schema(description = "Date de la leçon (format: yyyy-MM-dd)", example = "2026-07-25")
    @NotBlank(message = "La date est obligatoire")
    String date
) {
    public CreateLessonRequestDto {
        // Validation is handled by annotations
    }
}
