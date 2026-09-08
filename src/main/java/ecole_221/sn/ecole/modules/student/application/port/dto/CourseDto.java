package ecole_221.sn.ecole.modules.student.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO représentant un cours étudiant.
 */
@Schema(description = "Cours de l'étudiant")
public record CourseDto(
    @Schema(description = "Identifiant du cours", example = "course-1")
    String id,
    
    @Schema(description = "Titre du cours", example = "Architecture des Microservices")
    String titre,
    
    @Schema(description = "Coefficient", example = "3")
    int coefficient,
    
    @Schema(description = "Progression en pourcentage", example = "42")
    int progress,
    
    @Schema(description = "Statut du cours")
    CourseStatus statut,
    
    @Schema(description = "Unités/Modules du cours")
    List<String> unites
) {
    public CourseDto {
        // Validation can be added here
    }
}
