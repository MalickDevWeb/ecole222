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
    String etablissementPrecedent,
    String adresse,
    String anneeBac,
    String mentionBac,
    /** Pièces justificatives : liens renvoyés par POST /media/upload (obligatoires, http(s) uniquement). */
    String photoIdentiteUrl,
    String relevesNotesUrl,
    String diplomeUrl,
    /** 16/11 : pièces exigées par les règles d'admission de l'école (code de la pièce → lien /media/upload). */
    java.util.Map<String, String> piecesSupplementaires
) {
    /** Compatibilité : dossier sans pièce exigée par une règle. */
    public EnrollChildPayloadDto(String prenom, String nom, String dateNaissance, String lieuNaissance, String nationalite,
                                 String sexe, String email, String telephone, String filiere, String niveau,
                                 String diplomePrecedent, String etablissementPrecedent, String adresse, String anneeBac,
                                 String mentionBac, String photoIdentiteUrl, String relevesNotesUrl, String diplomeUrl) {
        this(prenom, nom, dateNaissance, lieuNaissance, nationalite, sexe, email, telephone, filiere, niveau, diplomePrecedent,
                etablissementPrecedent, adresse, anneeBac, mentionBac, photoIdentiteUrl, relevesNotesUrl, diplomeUrl, null);
    }
}
