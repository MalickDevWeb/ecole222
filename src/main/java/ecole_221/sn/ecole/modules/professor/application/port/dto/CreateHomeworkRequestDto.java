package ecole_221.sn.ecole.modules.professor.application.port.dto;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO pour la requête de création d'un devoir.
 */
@Schema(description = "Requête pour créer un devoir")
public record CreateHomeworkRequestDto(
    @Schema(description = "Titre du devoir", example = "TP Docker")
    @NotBlank(message = "Le titre est obligatoire")
    String titre,
    
    @Schema(description = "Description du devoir")
    String desc,
    
    @Schema(description = "Priorité du devoir")
    @NotNull(message = "La priorité est obligatoire")
    Priority prio,
    
    @Schema(description = "Date limite (format: yyyy-MM-dd)", example = "2026-07-30")
    @NotBlank(message = "La date limite est obligatoire")
    String deadlineStr
) {
    public CreateHomeworkRequestDto {
        // Validation is handled by annotations
    }
}
