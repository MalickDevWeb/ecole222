package ecole_221.sn.ecole.modules.archivage.application.format;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chiffrement des archives : AES-256-GCM par segments, clé de données enveloppée par la clé maîtresse. */
class ChiffrementFluxTest {

    private static byte[] octets(int n) {
        byte[] b = new byte[n];
        new Random(n).nextBytes(b);
        return b;
    }

    private static byte[] dechiffrer(byte[] chiffre, byte[] cle) throws IOException {
        try (InputStream in = ChiffrementFlux.dechiffrer(new ByteArrayInputStream(chiffre), cle)) {
            return in.readAllBytes();
        }
    }

    @Test
    void allerRetourExactPourToutesLesTailles() throws IOException {
        byte[] cle = ChiffrementFlux.nouvelleCle();
        int seg = ChiffrementFlux.SEGMENT;
        for (int taille : new int[]{0, 1, 1000, seg - 1, seg, seg + 1, 2 * seg, 2 * seg + 17}) {
            byte[] clair = octets(taille);
            byte[] chiffre = ChiffrementFlux.chiffrerTout(clair, cle);
            assertThat(dechiffrer(chiffre, cle)).as("taille %d", taille).isEqualTo(clair);
            // en-tête + une étiquette de 16 octets par segment (au moins un segment, même vide)
            int segments = Math.max(1, (taille + seg - 1) / seg);
            assertThat(chiffre.length).isEqualTo(ChiffrementFlux.TAILLE_ENTETE + taille + segments * ChiffrementFlux.ETIQUETTE);
        }
    }

    @Test
    void leClairNApparaitPasDansLeFichier() throws IOException {
        byte[] clair = "parent.exemple@ecole.test".repeat(50).getBytes();
        byte[] chiffre = ChiffrementFlux.chiffrerTout(clair, ChiffrementFlux.nouvelleCle());
        assertThat(new String(chiffre, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("parent.exemple");
    }

    @Test
    void unOctetModifieEstDetecte() throws IOException {
        byte[] cle = ChiffrementFlux.nouvelleCle();
        byte[] chiffre = ChiffrementFlux.chiffrerTout(octets(5000), cle);
        chiffre[ChiffrementFlux.TAILLE_ENTETE + 100] ^= 1;
        assertThatThrownBy(() -> dechiffrer(chiffre, cle)).isInstanceOf(ArchivageException.class)
                .hasMessageContaining("altéré");
    }

    @Test
    void uneArchiveTronqueeALaFrontiereDUnSegmentEstDetectee() throws IOException {
        byte[] cle = ChiffrementFlux.nouvelleCle();
        int seg = ChiffrementFlux.SEGMENT;
        byte[] chiffre = ChiffrementFlux.chiffrerTout(octets(2 * seg + 10), cle);
        byte[] tronque = Arrays.copyOf(chiffre, ChiffrementFlux.TAILLE_ENTETE + 2 * (seg + ChiffrementFlux.ETIQUETTE));
        assertThatThrownBy(() -> dechiffrer(tronque, cle)).isInstanceOf(ArchivageException.class);
        byte[] sansDernier = Arrays.copyOf(chiffre, ChiffrementFlux.TAILLE_ENTETE + seg + ChiffrementFlux.ETIQUETTE);
        assertThatThrownBy(() -> dechiffrer(sansDernier, cle)).isInstanceOf(ArchivageException.class);
    }

    @Test
    void desOctetsAjoutesALaFinSontDetectes() throws IOException {
        byte[] cle = ChiffrementFlux.nouvelleCle();
        byte[] chiffre = ChiffrementFlux.chiffrerTout(octets(300), cle);
        byte[] allonge = Arrays.copyOf(chiffre, chiffre.length + 20);
        assertThatThrownBy(() -> dechiffrer(allonge, cle)).isInstanceOf(ArchivageException.class);
    }

    @Test
    void uneAutreCleNeDechiffrePas() throws IOException {
        byte[] chiffre = ChiffrementFlux.chiffrerTout(octets(300), ChiffrementFlux.nouvelleCle());
        assertThatThrownBy(() -> dechiffrer(chiffre, ChiffrementFlux.nouvelleCle())).isInstanceOf(ArchivageException.class)
                .hasMessageContaining("mauvaise clé");
    }

    @Test
    void unFichierQuiNEstPasUneArchiveEstRefuse() {
        assertThatThrownBy(() -> dechiffrer("pas une archive du tout".getBytes(), ChiffrementFlux.nouvelleCle()))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("pas une archive");
    }

    @Test
    void cleDeDonneesEnveloppeeParLaCleMaitresseEtLieeALArchive() {
        byte[] maitresse = ChiffrementFlux.nouvelleCle();
        byte[] donnees = ChiffrementFlux.nouvelleCle();
        String enveloppe = ChiffrementFlux.envelopper(maitresse, donnees, "arc-1");
        assertThat(enveloppe).doesNotContain(java.util.Base64.getEncoder().encodeToString(donnees));
        assertThat(ChiffrementFlux.desenvelopper(maitresse, enveloppe, "arc-1")).isEqualTo(donnees);
        assertThatThrownBy(() -> ChiffrementFlux.desenvelopper(ChiffrementFlux.nouvelleCle(), enveloppe, "arc-1"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("ARCHIVES_CLE");
        assertThatThrownBy(() -> ChiffrementFlux.desenvelopper(maitresse, enveloppe, "arc-2"))
                .isInstanceOf(ArchivageException.class);
    }

    @Test
    void identifiantDeCleStableEtSansLaCle() {
        byte[] maitresse = ChiffrementFlux.nouvelleCle();
        assertThat(ChiffrementFlux.identifiant(maitresse)).hasSize(16).isEqualTo(ChiffrementFlux.identifiant(maitresse.clone()));
        assertThat(ChiffrementFlux.identifiant(maitresse)).isNotEqualTo(ChiffrementFlux.identifiant(ChiffrementFlux.nouvelleCle()));
    }
}
