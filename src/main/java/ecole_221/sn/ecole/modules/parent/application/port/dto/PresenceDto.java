package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant une présence.
 */
public record PresenceDto(
    String id,
    String date,
    String statut,  // "PRESENT", "ABSENT", "RETARD"
    String cours,
    String professeur,
    String heure
) {
    public PresenceDto {
        // Validation can be added here
    }
}
