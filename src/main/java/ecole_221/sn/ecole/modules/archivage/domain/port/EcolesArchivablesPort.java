package ecole_221.sn.ecole.modules.archivage.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port vers les écoles (table organizations) pour l'archivage. Chaque changement de statut applique la règle de
 * l'agrégat Organization, puis n'est écrit que si l'école a encore le statut lu (« … WHERE status = lu ») : une
 * réactivation concurrente ne peut pas être écrasée, et inversement.
 */
public interface EcolesArchivablesPort {

    record EcoleArchivable(String id, String nom, String slug, String statut, LocalDateTime suspendueLe) {
    }

    Optional<EcoleArchivable> lire(String organizationId);

    /** Gel : SUSPENDED → ARCHIVED (Organization.archive()). false si l'école n'est plus suspendue. */
    boolean geler(String organizationId);

    /** Échec avant la purge : ARCHIVED → SUSPENDED, suspended_at = maintenant (Organization.unarchive(), règle D1). */
    boolean rendreSuspendue(String organizationId);

    /** Restauration : ARCHIVED → ACTIVE, suspended_at effacé (Organization.restaurer()). */
    boolean restaurer(String organizationId);

    /** Écoles SUSPENDED depuis au plus tard {@code limite} (seuil atteint), sans opération d'archivage vivante. */
    List<EcoleArchivable> suspenduesAvant(LocalDateTime limite);
}
