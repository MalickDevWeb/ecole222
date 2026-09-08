package ecole_221.sn.ecole.modules.student.application.port.dto;

import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la requête de soumission d'une présence.
 */
@Schema(description = "Requête pour soumettre une auto-déclaration de présence")
public record AttendanceRequestDto(
    @Schema(description = "Type de présence (PRESENT/ABSENT)", example = "PRESENT")
    @NotNull(message = "Le type de présence est obligatoire")
    AttendanceStatus type,
    
    @Schema(description = "Méthode de vérification (QR/GPS)", example = "QR")
    @NotBlank(message = "La méthode de vérification est obligatoire")
    String method
) {
    public AttendanceRequestDto {
        // Validation is handled by annotations
    }
}
