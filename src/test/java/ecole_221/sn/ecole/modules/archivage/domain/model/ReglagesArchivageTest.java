package ecole_221.sn.ecole.modules.archivage.domain.model;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bornes des réglages (étude, § 10.6) : seuil 7 à 365 jours, conservation 15 à 365 jours ; 400 hors bornes. */
class ReglagesArchivageTest {

    @Test
    void defautsEtBornesAcceptees() {
        assertThat(new ReglagesArchivage(ReglagesArchivage.SEUIL_DEFAUT, ReglagesArchivage.CONSERVATION_DEFAUT))
                .isEqualTo(new ReglagesArchivage(30, 30));
        new ReglagesArchivage(7, 15);
        new ReglagesArchivage(365, 365);
    }

    @Test
    void seuilHorsBornesRefuseAvecMessageClair() {
        for (int jours : new int[]{0, 1, 6, 366, -5}) {
            assertThatThrownBy(() -> ReglagesArchivage.verifierSeuil(jours)).isInstanceOf(ArchivageException.class)
                    .hasMessage("Le seuil d'archivage doit être compris entre 7 et 365 jours.")
                    .extracting(e -> ((ArchivageException) e).nature()).isEqualTo(ArchivageException.Nature.INVALIDE);
        }
    }

    @Test
    void conservationHorsBornesRefuseeAvecMessageClair() {
        for (int jours : new int[]{0, 14, 366}) {
            assertThatThrownBy(() -> new ReglagesArchivage(30, jours)).isInstanceOf(ArchivageException.class)
                    .hasMessage("La durée de conservation doit être comprise entre 15 et 365 jours.");
        }
    }
}
