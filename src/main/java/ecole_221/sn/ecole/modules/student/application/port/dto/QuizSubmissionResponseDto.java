package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO pour la réponse après soumission d'un quiz.
 */
@Schema(description = "Résultat de la soumission d'un quiz")
public record QuizSubmissionResponseDto(
    @Schema(description = "Succès de l'opération")
    boolean success,
    
    @Schema(description = "Identifiant du cours", example = "course-1")
    String courseId,
    
    @Schema(description = "Score obtenu", example = "8")
    int score,
    
    @Schema(description = "Score total possible", example = "10")
    int total,
    
    @Schema(description = "Message de retour")
    String message
) {
    public QuizSubmissionResponseDto {
        // Validation can be added here
    }
    
    /**
     * Calcule le pourcentage de réussite.
     */
    public double getPercentage() {
        return total > 0 ? (double) score / total * 100 : 0;
    }
}
