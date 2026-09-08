package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * DTO représentant un devoir étudiant.
 */
@Schema(description = "Devoir à rendre par l'étudiant")
public record HomeworkDto(
    @Schema(description = "Identifiant du devoir", example = "hw-001")
    String id,
    
    @Schema(description = "Statut du devoir", example = "a_rendre")
    String statut,
    
    @Schema(description = "Titre du devoir", example = "TP Docker")
    String titre,
    
    @Schema(description = "Date limite de rendage")
    LocalDate deadline,
    
    @Schema(description = "Priorité", example = "haute")
    String prio,

    @Schema(description = "Identifiant du cours associé", example = "course-1")
    String courseId,

    @Schema(description = "Description du devoir")
    String desc,

    @Schema(description = "Progression en pourcentage", example = "50")
    Integer progress
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
