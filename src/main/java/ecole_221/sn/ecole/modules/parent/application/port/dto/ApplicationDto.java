package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant une candidature.
 */
public record ApplicationDto(
    String id,
    String enfant,
    String filiere,
    String niveau,
    String dateDepot,
    String statut,  // "EN_ATTENTE", "ACCEPTE", "REFUSE"
    int progress,
    String etape,
    String commentaire
) {
    public ApplicationDto {
        // Validation can be added here
    }
}
