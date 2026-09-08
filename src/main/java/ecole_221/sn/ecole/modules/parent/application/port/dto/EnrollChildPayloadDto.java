package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO pour soumettre un nouveau dossier de candidature.
 */
public record EnrollChildPayloadDto(
    String prenom,
    String nom,
    String dateNaissance,
    String lieuNaissance,
    String nationalite,
    String sexe,
    String email,
    String telephone,
    String filiere,
    String niveau,
    String diplomePrecedent,
    String etablissementPrecedent
) {
    public EnrollChildPayloadDto {
        // Validation can be added here
    }
}
