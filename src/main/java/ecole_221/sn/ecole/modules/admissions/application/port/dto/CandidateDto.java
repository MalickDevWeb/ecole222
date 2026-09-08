package ecole_221.sn.ecole.modules.admissions.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un candidat.
 */
@Schema(description = "Candidat")
public record CandidateDto(
    @Schema(description = "Identifiant du candidat", example = "cand-001")
    String id,
    
    @Schema(description = "Nom du candidat", example = "Sall")
    String nom,
    
    @Schema(description = "Prénom du candidat", example = "Mamadou")
    String prenom,
    
    @Schema(description = "Email du candidat", example = "mamadou.sall@example.com")
    String email,
    
    @Schema(description = "Téléphone", example = "221771234567")
    String telephone,
    
    @Schema(description = "Statut de la candidature", example = "EN_ATTENTE")
    String statut,
    
    @Schema(description = "Note du candidat", example = "15.5")
    Double note,
    
    @Schema(description = "Filière demandée", example = "Génie Logiciel")
    String filiereDemandee
) {
    public CandidateDto {
        // Validation can be added here
    }
}