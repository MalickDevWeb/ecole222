package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête de création d'un module.
 */
@Schema(description = "Requête pour créer un module")
public record CreateModuleRequestDto(
    @Schema(description = "Nom du module", example = "Backend Spring Boot")
    @NotBlank(message = "Le nom du module est obligatoire")
    String name
) {
    public CreateModuleRequestDto {
        // Validation is handled by annotations
    }
}
