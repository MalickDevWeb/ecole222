package ecole_221.sn.ecole.modules.admissions.domain.valueobject;

/**
 * Value Object représentant le statut d'un candidat.
 * Immutabilité garantie par le mot-clé enum.
 */
public enum CandidateStatus {
    EN_ATTENTE,
    ACCEPTED,
    REJECTED,
    EN_COURS,
    FINALISE
}