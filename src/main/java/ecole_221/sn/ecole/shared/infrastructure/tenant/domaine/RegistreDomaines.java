package ecole_221.sn.ecole.shared.infrastructure.tenant.domaine;

import java.util.Optional;

/**
 * Domaines propres DÉCLARÉS par les administrateurs des écoles (décision du Tech Lead du 15/11 : pointage DNS ET
 * déclaration). Port lu par le résolveur ; l'implémentation lit la table globale {@code domaines_ecoles} (V17).
 */
public interface RegistreDomaines {

    /** Statuts d'un domaine déclaré. */
    String EN_ATTENTE_POINTAGE = "EN_ATTENTE_POINTAGE";
    String ACTIF = "ACTIF";
    String DESACTIVE = "DESACTIVE";

    /** Domaine déclaré (hostname en A-label minuscule) : école et statut. */
    record DomaineDeclare(String hostname, String organizationId, String statut) {
    }

    Optional<DomaineDeclare> parHote(String hostnameNormalise);
}
