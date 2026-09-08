package ecole_221.sn.ecole.modules.student.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO représentant une présence étudiante.
 */
@Schema(description = "Présence d'un étudiant à un cours")
public record AttendanceDto(
    @Schema(description = "Identifiant de la présence", example = "att-12345678")
    String id,
    
    @Schema(description = "Date et heure de la présence")
    LocalDateTime date,
    
    @Schema(description = "Statut de la présence")
    AttendanceStatus status,
    
    @Schema(description = "Identifiant de l'étudiant", example = "usr-001")
    String studentId,
    
    @Schema(description = "Nom de l'étudiant", example = "Mamadou Sall")
    String studentName,
    
    @Schema(description = "Identifiant du cours", example = "course-1")
    String courseId
) {
    public AttendanceDto {
        // Validation can be added here
    }
}
