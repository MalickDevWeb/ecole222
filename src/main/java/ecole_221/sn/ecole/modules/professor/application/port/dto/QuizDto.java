package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un quiz.
 */
@Schema(description = "Quiz évaluatif")
public record QuizDto(
    @Schema(description = "Identifiant du quiz", example = "quiz-1")
    String id,
    
    @Schema(description = "Titre du quiz", example = "Quiz Spring Boot")
    String title,
    
    @Schema(description = "Durée en minutes", example = "30")
    int duration,
    
    @Schema(description = "Nombre de questions")
    int questionCount,
    
    @Schema(description = "Note maximale", example = "20")
    double maxScore,
    
    @Schema(description = "Identifiant du module parent")
    String moduleId
) {
    public QuizDto {
        // Validation can be added here
    }
}
