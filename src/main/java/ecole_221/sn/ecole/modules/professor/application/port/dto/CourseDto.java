package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO représentant un cours du professeur.
 * Standardisé avec le contrat BACKEND_DATA_CONTRACT.md (§4.1)
 */
@Schema(description = "Cours du professeur")
public record CourseDto(
    @Schema(description = "Identifiant du cours", example = "course-1")
    String id,
    
    @Schema(description = "Titre du cours", example = "Architecture des Microservices")
    String titre,
    
    @Schema(description = "Description du cours")
    String description,
    
    @Schema(description = "Code du cours", example = "MIC-101")
    String code,
    
    @Schema(description = "Identifiant de la classe", example = "class-l2-gc")
    String classeId,
    
    @Schema(description = "Nombre de crédits ECTS", example = "6")
    int credits,
    
    @Schema(description = "Nombre d'étudiants inscrits")
    int studentCount,
    
    @Schema(description = "Liste des modules")
    List<ModuleDto> modules
) {
    public CourseDto {
        // Validation can be added here
    }
}
