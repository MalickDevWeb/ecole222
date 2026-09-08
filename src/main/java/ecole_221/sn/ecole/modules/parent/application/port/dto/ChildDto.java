package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant un enfant du parent.
 */
public record ChildDto(
    String id,
    String nom,
    String prenom,
    String matricule,
    String filiere,
    String niveau,
    String classe,
    String photoUrl
) {
    public ChildDto {
        // Validation can be added here
    }
}
