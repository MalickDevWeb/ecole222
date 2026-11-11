package ecole_221.sn.ecole.modules.parent.domain.repository;

import ecole_221.sn.ecole.modules.parent.domain.model.CandidatureParent;

import java.util.List;
import java.util.Map;

/** Dossiers de candidature déposés par les parents, dans l'école courante. */
public interface CandidaturesParentPort {

    /** Lève une erreur (403) si l'école n'accepte pas de candidatures aujourd'hui (APP-BE-15). */
    default void exigerOuverture() { }

    /** Une candidature existe-t-elle déjà avec cet e-mail dans l'école courante ? */
    boolean emailDejaCandidat(String email);

    /**
     * Enregistre le dossier dans le circuit des admissions et renvoie son identifiant.
     * {@code complement} contient les informations sans colonne dédiée (naissance, parcours, adresse).
     */
    String deposer(String parentId, String prenom, String nom, String email, String telephone,
                   String formation, Map<String, String> complement);

    List<CandidatureParent> listerParParent(String parentId);
}
