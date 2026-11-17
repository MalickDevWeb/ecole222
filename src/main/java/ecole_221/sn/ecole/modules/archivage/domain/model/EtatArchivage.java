package ecole_221.sn.ecole.modules.archivage.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un archivage d'école (décisions du Tech Lead du 16/11) :
 * ACTIVE → SUSPENDED → ARCHIVAGE_EN_COURS → ARCHIVEE → SUPPRESSION_A_CONFIRMER → SUPPRIMEE_DEFINITIVEMENT ;
 * restauration : RESTAURATION_EN_COURS → RESTAUREE (l'école redevient ACTIVE).
 *
 * <p>Ces états vivent dans {@code archivages_ecoles}, pas dans {@code organizations.status} : l'école reste ARCHIVED
 * du gel jusqu'à la restauration, statut que tout le code existant refuse déjà (403, connexion, WebSocket) et que la
 * contrainte {@code organizations_status_check} accepte sans migration risquée.</p>
 */
public enum EtatArchivage {
    ARCHIVAGE_EN_COURS("Archivage en cours"),
    ARCHIVEE("Archivée"),
    SUPPRESSION_A_CONFIRMER("Suppression à confirmer"),
    RESTAURATION_EN_COURS("Restauration en cours"),
    RESTAUREE("Restaurée"),
    SUPPRIMEE_DEFINITIVEMENT("Supprimée définitivement"),
    ECHEC("Échec");

    /** États « vivants » : une seule opération de ce type par école (index unique partiel de V24). */
    public static final Set<EtatArchivage> VIVANTS =
            EnumSet.of(ARCHIVAGE_EN_COURS, ARCHIVEE, SUPPRESSION_A_CONFIRMER, RESTAURATION_EN_COURS);

    private final String libelle;

    EtatArchivage(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    public boolean vivant() {
        return VIVANTS.contains(this);
    }
}
