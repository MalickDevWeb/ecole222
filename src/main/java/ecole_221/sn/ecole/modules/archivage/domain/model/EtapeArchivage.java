package ecole_221.sn.ecole.modules.archivage.domain.model;

/**
 * Étapes d'un archivage en cours. Jusqu'à VERIFICATION comprise, rien n'est supprimé : un échec rend l'école à son
 * statut d'avant. PURGE est le point de non-retour : une coupure reprend la purge, jamais l'inverse.
 */
public enum EtapeArchivage {
    GEL, EXPORT, VERIFICATION, PURGE, TERMINEE;

    public boolean avantPurge() {
        return this == GEL || this == EXPORT || this == VERIFICATION;
    }
}
