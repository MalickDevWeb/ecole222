package ecole_221.sn.ecole.modules.archivage.domain.model;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Machine à états de l'archivage (décisions du Tech Lead du 16/11), sans base ni mock. */
class ArchivageEcoleTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 16, 2, 30);

    private static ArchivageEcole demarre(String statut) {
        return ArchivageEcole.demarrer("org-a", "École A", "ecole-a", statut, T0.minusDays(40), Declenchement.MANUEL,
                "Impayé", "sa-1", "Super Admin", T0);
    }

    private static ArchivageEcole archivee() {
        ArchivageEcole a = demarre("SUSPENDED");
        a.avancer(EtapeArchivage.EXPORT, T0);
        a.enregistrerFichier("local", "arc.e221a", "ab".repeat(32), 100, "24", "cle", "id", Map.of("classes", 2L), 2, 0, T0);
        a.avancer(EtapeArchivage.VERIFICATION, T0);
        a.commencerPurge(T0);
        a.noterTablePurgee("classes", 2, T0);
        a.terminerArchivage(30, Map.of(), T0.plusMinutes(5));
        return a;
    }

    @Test
    void seuleUneEcoleSuspendueOuArchiveeParLAncienneRoutePeutEtreArchivee() {
        assertThat(demarre("SUSPENDED").getEtat()).isEqualTo(EtatArchivage.ARCHIVAGE_EN_COURS);
        assertThat(demarre("SUSPENDED").getEtape()).isEqualTo(EtapeArchivage.GEL);
        assertThat(demarre("ARCHIVED").renduSuspendue()).isFalse();
        for (String statut : List.of("ACTIVE", "PENDING", "REJECTED")) {
            assertThatThrownBy(() -> demarre(statut)).isInstanceOf(ArchivageException.class)
                    .hasMessageContaining("Seule une école suspendue");
        }
    }

    @Test
    void cycleCompletJusquALaSuppressionToujoursConfirmee() {
        ArchivageEcole a = archivee();
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(a.getConservationJusquAu()).isEqualTo(T0.plusMinutes(5).plusDays(30));
        // avant l'échéance : ni « à confirmer », ni suppression
        assertThat(a.echeanceAtteinte(T0.plusDays(29))).isFalse();
        assertThatThrownBy(() -> a.passerASupprimer(T0.plusDays(29))).isInstanceOf(ArchivageException.class);
        assertThatThrownBy(() -> a.supprimerDefinitivement(true, "sa-1", T0.plusDays(29)))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("après la fin de la conservation");
        // échéance : la tâche ne fait que passer « à confirmer »
        a.passerASupprimer(T0.plusDays(31));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.SUPPRESSION_A_CONFIRMER);
        assertThat(a.getCleEnveloppee()).isNotNull();
        // sans confirmation explicite : refus
        assertThatThrownBy(() -> a.supprimerDefinitivement(false, "sa-1", T0.plusDays(32)))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("confirmer");
        a.supprimerDefinitivement(true, "sa-1", T0.plusDays(32));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.SUPPRIMEE_DEFINITIVEMENT);
        assertThat(a.getCleEnveloppee()).isNull();          // archive rendue illisible
        assertThat(a.isFichierASupprimer()).isTrue();
        assertThat(a.getEmpreinteSha256()).isNotNull();     // trace minimale gardée
        assertThat(a.getSupprimeePar()).isEqualTo("sa-1");
    }

    @Test
    void echecAvantPurgeRendLEcoleEtEffaceLaCle() {
        ArchivageEcole a = demarre("SUSPENDED");
        a.avancer(EtapeArchivage.EXPORT, T0);
        a.enregistrerFichier("local", "arc.e221a", "ab".repeat(32), 100, "24", "cle", "id", Map.of(), 0, 0, T0);
        a.avancer(EtapeArchivage.VERIFICATION, T0);
        assertThat(a.echouerAvantPurge("Archive relue différente", T0)).isEqualTo("SUSPENDED");
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ECHEC);
        assertThat(a.getEtat().vivant()).isFalse();          // un nouvel archivage pourra être tenté
        assertThat(a.getCleEnveloppee()).isNull();
        assertThat(a.isFichierASupprimer()).isTrue();
    }

    @Test
    void apresLeDebutDeLaPurgeOnNeRevientPlusEnArriere() {
        ArchivageEcole a = demarre("SUSPENDED");
        assertThatThrownBy(() -> a.commencerPurge(T0)).isInstanceOf(ArchivageException.class);   // pas d'archive vérifiée
        a.avancer(EtapeArchivage.EXPORT, T0);
        a.enregistrerFichier("local", "arc.e221a", "ab".repeat(32), 100, "24", "cle", "id", Map.of(), 0, 0, T0);
        a.avancer(EtapeArchivage.VERIFICATION, T0);
        a.commencerPurge(T0);
        assertThatThrownBy(() -> a.echouerAvantPurge("x", T0)).isInstanceOf(ArchivageException.class);
        assertThatThrownBy(() -> a.avancer(EtapeArchivage.EXPORT, T0)).isInstanceOf(ArchivageException.class);
        a.noterIncidentPurge("coupure", T0);
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ARCHIVAGE_EN_COURS);
        assertThat(a.getTentatives()).isEqualTo(1);
    }

    @Test
    void restaurationReussieOuRefuseeSansPerte() {
        ArchivageEcole a = archivee();
        a.demarrerRestauration("sa-2", T0.plusDays(1));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.RESTAURATION_EN_COURS);
        assertThatThrownBy(() -> a.demarrerRestauration("sa-2", T0)).isInstanceOf(ArchivageException.class);
        a.echouerRestauration("Restauration refusée : 1 conflit", List.of(Map.of("table", "parents")), T0.plusDays(1));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(a.getConflits()).hasSize(1);
        assertThat(a.getCleEnveloppee()).isNotNull();       // archive intacte
        a.demarrerRestauration("sa-2", T0.plusDays(2));
        assertThat(a.getConflits()).isEmpty();
        a.terminerRestauration(T0.plusDays(2));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.RESTAUREE);
        assertThat(a.getCleEnveloppee()).isNull();
        assertThat(a.isFichierASupprimer()).isTrue();
    }

    @Test
    void restaurationPossibleDepuisSuppressionAConfirmer() {
        ArchivageEcole a = archivee();
        a.passerASupprimer(T0.plusDays(40));
        a.demarrerRestauration("sa-1", T0.plusDays(40));
        a.echouerRestauration("panne", null, T0.plusDays(40));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.SUPPRESSION_A_CONFIRMER);
    }

    @Test
    void etatsVivantsDUneSeuleOperationParEcole() {
        assertThat(EtatArchivage.VIVANTS).containsExactlyInAnyOrder(EtatArchivage.ARCHIVAGE_EN_COURS, EtatArchivage.ARCHIVEE,
                EtatArchivage.SUPPRESSION_A_CONFIRMER, EtatArchivage.RESTAURATION_EN_COURS);
    }
}
