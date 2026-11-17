package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.application.format.TableArchivee;

import java.util.List;
import java.util.Map;

/**
 * Plan d'archivage calculé sur le catalogue réel de la base : tables qui partent (dans l'ordre de la purge, enfants
 * d'abord), tables qui restent.
 */
public record PlanArchivage(List<TablePlan> tables, List<String> gardees) {

    public enum Categorie {
        /** Table portant organization_id : les lignes de l'école partent. */
        DIRECTE,
        /** organization_users : seules les appartenances des comptes rattachés à plusieurs écoles partent. */
        APPARTENANCES,
        /** Table sans organization_id, rattachée à l'école par une table parente (group_chat_*, role_permissions). */
        INDIRECTE
    }

    /**
     * Une table qui part.
     *
     * @param selection condition SQL sur l'alias {@code t} ; chaque « ? » reçoit l'identifiant de l'école
     * @param cle       clé primaire (vide : aucune ; la purge compare alors la ligne entière)
     * @param typesCle  type PostgreSQL de chaque colonne de la clé
     * @param colonnesMedias colonnes d'adresses de fichiers (url, *_url, logo) relevées dans le manifeste
     */
    public record TablePlan(String nom, Categorie categorie, List<TableArchivee.Colonne> colonnes, List<String> cle,
                            Map<String, String> typesCle, String selection, int parametres, List<String> colonnesMedias) {

        public TableArchivee description() {
            return new TableArchivee(nom, categorie.name(), colonnes, cle);
        }

        public Object[] valeurs(String organizationId) {
            Object[] v = new Object[parametres];
            java.util.Arrays.fill(v, organizationId);
            return v;
        }
    }
}
