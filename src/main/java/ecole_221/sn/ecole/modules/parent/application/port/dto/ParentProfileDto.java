package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO pour mettre à jour le profil du parent.
 */
public record ParentProfileDto(
    String email,
    String telephone,
    String adresse,
    String profession
) {
    public ParentProfileDto {
        // Validation can be added here
    }
}
