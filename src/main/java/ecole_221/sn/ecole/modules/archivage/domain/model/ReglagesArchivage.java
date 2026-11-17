package ecole_221.sn.ecole.modules.archivage.domain.model;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;

/**
 * Réglages de l'archivage, modifiables par le SUPER_ADMIN seul (décisions D1 et D3 du Tech Lead ; bornes de l'étude,
 * § 10.6). Aucun effet rétroactif : l'échéance de conservation est calculée et rangée à la création de l'archive.
 *
 * @param seuilJours        jours de suspension avant l'archivage automatique (7 à 365, 30 par défaut)
 * @param conservationJours jours de conservation d'une archive avant « suppression à confirmer » (15 à 365, 30 par défaut)
 */
public record ReglagesArchivage(int seuilJours, int conservationJours) {

    public static final int SEUIL_DEFAUT = 30;
    public static final int SEUIL_MIN = 7;
    public static final int SEUIL_MAX = 365;
    public static final int CONSERVATION_DEFAUT = 30;
    public static final int CONSERVATION_MIN = 15;
    public static final int CONSERVATION_MAX = 365;

    public ReglagesArchivage {
        verifierSeuil(seuilJours);
        verifierConservation(conservationJours);
    }

    public static void verifierSeuil(long jours) {
        if (jours < SEUIL_MIN || jours > SEUIL_MAX) {
            throw ArchivageException.invalide("Le seuil d'archivage doit être compris entre "
                    + SEUIL_MIN + " et " + SEUIL_MAX + " jours.");
        }
    }

    public static void verifierConservation(long jours) {
        if (jours < CONSERVATION_MIN || jours > CONSERVATION_MAX) {
            throw ArchivageException.invalide("La durée de conservation doit être comprise entre "
                    + CONSERVATION_MIN + " et " + CONSERVATION_MAX + " jours.");
        }
    }
}
