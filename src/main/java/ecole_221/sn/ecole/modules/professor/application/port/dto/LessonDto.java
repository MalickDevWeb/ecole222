package ecole_221.sn.ecole.modules.professor.application.port.dto;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * DTO représentant une leçon/cours.
 * Standardisé avec le contrat BACKEND_DATA_CONTRACT.md
 */
@Schema(description = "Leçon ou séance de cours")
public record LessonDto(
    @Schema(description = "Identifiant de la leçon", example = "lesson-1")
    String id,
    
    @Schema(description = "Titre de la leçon", example = "Introduction à Spring Boot")
    String titre,
    
    @Schema(description = "Durée en minutes", example = "90")
    int duration,
    
    @Schema(description = "Date de la leçon")
    LocalDate date,
    
    @Schema(description = "Type de session")
    SessionType type,
    
    @Schema(description = "Identifiant du cours parent")
    String courseId
) {
    public LessonDto {
        // Validation can be added here
    }
}
