package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * École d'APPARTENANCE d'un compte (15/11, défauts F1 et F10) : utilisée à la connexion, pour le lien de
 * réinitialisation du mot de passe et au CONNECT WebSocket.
 *
 * <p>Ordre : appartenance active (non expirée) dans une école ACTIVE, puis dans une école en attente de validation,
 * puis appartenance non active. Une école suspendue, archivée ou rejetée n'est jamais retenue ; elle est signalée
 * ({@link Choix#fermee()}) pour expliquer un refus. Avant : la première appartenance trouvée (memberships.get(0)).</p>
 */
public final class EcoleDuCompte {

    private EcoleDuCompte() {
    }

    /**
     * @param retenue école retenue (null si aucune école ouverte)
     * @param fermee  première école du compte refusée par son statut (null si aucune)
     */
    public record Choix(Organization retenue, Organization fermee) {
    }

    public static boolean ouverte(Organization ecole) {
        return ecole.isActive() || OrganizationStatus.PENDING.equals(ecole.getStatus());
    }

    public static Choix choisir(List<OrganizationUser> appartenances, Function<String, Optional<Organization>> ecoleParId) {
        Organization meilleure = null;
        int rangMeilleur = Integer.MAX_VALUE;
        Organization fermee = null;
        for (OrganizationUser m : appartenances == null ? List.<OrganizationUser>of() : appartenances) {
            if (m == null || m.getOrganizationId() == null) {
                continue;
            }
            Organization ecole = ecoleParId.apply(m.getOrganizationId()).orElse(null);
            if (ecole == null) {
                continue;
            }
            if (!ouverte(ecole)) {
                if (fermee == null) {
                    fermee = ecole;
                }
                continue;
            }
            boolean membreActif = m.isActive() && !m.isExpired();
            int rang = (membreActif ? 0 : 2) + (ecole.isActive() ? 0 : 1);
            if (rang < rangMeilleur) {
                meilleure = ecole;
                rangMeilleur = rang;
            }
        }
        return new Choix(meilleure, fermee);
    }
}
