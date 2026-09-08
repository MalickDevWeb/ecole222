package ecole_221.sn.ecole.modules.professor.application.port.dto;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * DTO représentant un devoir.
 */
@Schema(description = "Devoir à rendre")
public record HomeworkDto(
    @Schema(description = "Identifiant du devoir", example = "hw-1")
    String id,
    
    @Schema(description = "Titre du devoir", example = "TP Docker et Conteneurs")
    String title,
    
    @Schema(description = "Description du devoir")
    String description,
    
    @Schema(description = "Priorité du devoir")
    Priority priority,
    
    @Schema(description = "Date limite de rendage")
    LocalDate deadline,
    
    @Schema(description = "Identifiant du cours parent")
    String courseId
) {
    public HomeworkDto {
        // Validation can be added here
    }
    
    /**
     * Vérifie si le devoir est en retard.
     */
    public boolean isOverdue() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }
}
