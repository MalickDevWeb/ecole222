package ecole_221.sn.ecole.modules.professor.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la requête de soumission des présences.
 */
@Schema(description = "Requête pour soumettre les présences")
public record SubmitAttendanceRequestDto(
    @Schema(description = "Date de la présence", example = "2026-07-20")
    @NotBlank(message = "La date est obligatoire")
    String date,
    
    @Schema(description = "Identifiant de l'étudiant")
    @NotBlank(message = "L'identifiant de l'étudiant est obligatoire")
    String studentId,
    
    @Schema(description = "Nom de l'étudiant")
    @NotBlank(message = "Le nom de l'étudiant est obligatoire")
    String studentName,
    
    @Schema(description = "Statut de la présence")
    @NotNull(message = "Le statut est obligatoire")
    AttendanceStatus status,
    
    @Schema(description = "Identifiant du cours")
    @NotBlank(message = "L'identifiant du cours est obligatoire")
    String courseId
) {
    public SubmitAttendanceRequestDto {
        // Validation is handled by annotations
    }
}
