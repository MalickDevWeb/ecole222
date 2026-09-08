package ecole_221.sn.ecole.modules.collaboration.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une tâche.
 */
@Schema(description = "Tâche")
public record TaskDto(
    @Schema(description = "Identifiant de la tâche", example = "task-001")
    String id,
    
    @Schema(description = "Titre de la tâche", example = "Préparer le rapport")
    String title,
    
    @Schema(description = "Description de la tâche")
    String description,
    
    @Schema(description = "Identifiant du groupe", example = "wg-001")
    String workgroupId,
    
    @Schema(description = "Identifiant de l'assigné", example = "usr-002")
    String assigneeId,
    
    @Schema(description = "Statut de la tâche", example = "EN_COURS")
    String status,
    
    @Schema(description = "Priorité", example = "HIGH")
    String priority
) {
    public TaskDto {
        // Validation can be added here
    }
}