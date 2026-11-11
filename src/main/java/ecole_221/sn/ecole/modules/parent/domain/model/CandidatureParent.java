package ecole_221.sn.ecole.modules.parent.domain.model;

import java.time.LocalDateTime;

/**
 * Dossier de candidature déposé par un parent pour son enfant, tel qu'il est enregistré dans le circuit
 * des admissions de l'école. Le statut est la clé d'admission (EN_ATTENTE, ACCEPTED, REJECTED…).
 */
public record CandidatureParent(
        String id,
        String enfant,
        String filiere,
        String niveau,
        LocalDateTime depot,
        String statut,
        String etape,
        String commentaire) {
}
