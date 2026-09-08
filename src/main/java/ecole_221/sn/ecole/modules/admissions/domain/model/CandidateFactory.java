package ecole_221.sn.ecole.modules.admissions.domain.model;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;

import java.util.UUID;

/**
 * Factory pour créer des instances de Candidate.
 * Permet d'accéder au constructeur package-private de l'agrégat racine.
 */
public class CandidateFactory {

    /**
     * Crée un nouveau candidat avec un ID généré et le statut "EN_ATTENTE" par défaut.
     */
    public static Candidate create(String nom, String prenom, String email, String telephone,
                                 String filiereDemandee) {
        String id = UUID.randomUUID().toString();
        return new Candidate(id, nom, prenom, email, telephone, CandidateStatus.EN_ATTENTE, null, filiereDemandee);
    }

    /**
     * Crée un nouveau candidat avec un ID généré et un statut personnalisé.
     */
    public static Candidate create(String nom, String prenom, String email, String telephone,
                                 CandidateStatus statut, Double note, String filiereDemandee) {
        String id = UUID.randomUUID().toString();
        return new Candidate(id, nom, prenom, email, telephone, statut, note, filiereDemandee);
    }

    /**
     * Reconstruit un candidat existant (pour la persistance).
     */
    public static Candidate reconstruct(String id, String nom, String prenom, String email,
                                       String telephone, CandidateStatus statut, Double note,
                                       String filiereDemandee) {
        return new Candidate(id, nom, prenom, email, telephone, statut, note, filiereDemandee);
    }
}