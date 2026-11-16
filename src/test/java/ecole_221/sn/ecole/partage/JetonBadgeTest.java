package ecole_221.sn.ecole.partage;

import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** Badge QR dynamique signé par le serveur (Ed25519, 2026-11-14). */
class JetonBadgeTest {

    private final JetonBadge jetons = new JetonBadge("", "secret-jwt-de-test-assez-long-0123456789");

    @Test
    void badgeDuServeurValideDansLaMinute() {
        long m = JetonBadge.minuteCourante();
        var lu = jetons.lire(jetons.contenu("ETU-001", m)).orElseThrow();
        assertThat(lu.valide()).isTrue();
        assertThat(lu.matricule()).isEqualTo("ETU-001");
        assertThat(jetons.valide("ETU-001", m - 1, jetons.jeton("ETU-001", m - 1))).isTrue();
    }

    @Test
    void badgeExpireOuDUnAutreMatriculeRefuse() {
        long m = JetonBadge.minuteCourante();
        assertThat(jetons.valide("ETU-001", m - 2, jetons.jeton("ETU-001", m - 2))).isFalse();
        assertThat(jetons.valide("ETU-002", m, jetons.jeton("ETU-001", m))).isFalse();
        // scan hors ligne : la fraîcheur est jugée à la minute du scan
        assertThat(jetons.lire(jetons.contenu("ETU-001", m - 30), m - 30).orElseThrow().valide()).isTrue();
        assertThat(jetons.lire(jetons.contenu("ETU-001", m - 30), m).orElseThrow().valide()).isFalse();
    }

    @Test
    void ancienAlgorithmeAutreCleEtSignatureAltereeRefuses() {
        long m = JetonBadge.minuteCourante();
        assertThat(jetons.valide("ETU-001", m, "1A2B3C4D")).isFalse();                                     // ancien FNV-1a
        assertThat(jetons.valide("ETU-001", m, new JetonBadge("autre", "").jeton("ETU-001", m))).isFalse(); // autre clé
        String j = jetons.jeton("ETU-001", m);
        String altere = j.substring(0, 40) + (j.charAt(40) == 'A' ? 'B' : 'A') + j.substring(41);
        assertThat(jetons.valide("ETU-001", m, altere)).isFalse();
        assertThat(jetons.valide("ETU-001", m, j + "A")).isFalse();
    }

    @Test
    void clePubliqueStableEtSuffisantePourVerifierHorsLigne() throws Exception {
        // même secret → même clé (redémarrage, plusieurs instances)
        assertThat(new JetonBadge("", "secret-jwt-de-test-assez-long-0123456789").clePubliqueBase64()).isEqualTo(jetons.clePubliqueBase64());
        assertThat(JetonBadge.aleatoire().clePubliqueBase64()).isNotEqualTo(jetons.clePubliqueBase64());
        // le terminal du vigile vérifie avec la seule clé publique (32 octets), comme le fera le navigateur
        byte[] brute = Base64.getUrlDecoder().decode(jetons.clePubliqueBase64());
        assertThat(brute).hasSize(32);
        byte[] spki = new byte[44];
        System.arraycopy(new byte[] {0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00}, 0, spki, 0, 12);
        System.arraycopy(brute, 0, spki, 12, 32);
        var publique = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(spki));
        long m = JetonBadge.minuteCourante();
        Signature s = Signature.getInstance("Ed25519");
        s.initVerify(publique);
        s.update(("ETU-001:" + m).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(s.verify(Base64.getUrlDecoder().decode(jetons.jeton("ETU-001", m)))).isTrue();
    }

    @Test
    void contenuMalFormeLuCommeInvalide() {
        assertThat(jetons.lire("ETU-001")).isEmpty();
        assertThat(jetons.lire("STU_SECURE:ETU-001").orElseThrow().valide()).isFalse();
        assertThat(jetons.lire("STU_SECURE:ETU-001:abc:XYZ").orElseThrow().valide()).isFalse();
    }

    // ==================== QR d'entrée du vigile (pointage, 16/11) ====================

    @Test
    void qrEntreeDuServeurValideDansLaMinutePourSonEcole() {
        long m = JetonBadge.minuteCourante();
        var lu = jetons.lireEntree(jetons.contenuEntree("org-a", m)).orElseThrow();
        assertThat(lu.valide()).isTrue();
        assertThat(lu.ecoleId()).isEqualTo("org-a");
        assertThat(jetons.lireEntree(jetons.contenuEntree("org-a", m - 1)).orElseThrow().valide()).isTrue();
    }

    @Test
    void qrEntreeExpireFalsifieOuEcoleChangeeRefuse() {
        long m = JetonBadge.minuteCourante();
        assertThat(jetons.lireEntree(jetons.contenuEntree("org-a", m - 2)).orElseThrow().valide()).isFalse();
        assertThat(jetons.lireEntree(new JetonBadge("autre", "").contenuEntree("org-a", m)).orElseThrow().valide()).isFalse();
        // signature d'une école recollée sur une autre
        String vrai = jetons.contenuEntree("org-a", m);
        String sig = vrai.substring(vrai.lastIndexOf(':') + 1);
        assertThat(jetons.lireEntree(JetonBadge.PREFIXE_ENTREE + "org-b:" + m + ":" + sig).orElseThrow().valide()).isFalse();
        assertThat(jetons.lireEntree("ENTREE_SECURE:org-a").orElseThrow().valide()).isFalse();
        assertThat(jetons.lireEntree("ENTREE_SECURE:org-a:abc:XYZ").orElseThrow().valide()).isFalse();
        assertThat(jetons.lireEntree("221-VIGILE-PORTAL-MAIN")).isEmpty();
    }

    @Test
    void badgeEleveEtQrEntreeNeSontPasInterchangeables() {
        long m = JetonBadge.minuteCourante();
        // la signature d'un badge élève ne vaut pas QR d'entrée, et inversement
        String sigBadge = jetons.jeton("org-a", m);
        assertThat(jetons.lireEntree(JetonBadge.PREFIXE_ENTREE + "org-a:" + m + ":" + sigBadge).orElseThrow().valide()).isFalse();
        String entree = jetons.contenuEntree("org-a", m);
        assertThat(jetons.valide("org-a", m, entree.substring(entree.lastIndexOf(':') + 1))).isFalse();
        assertThat(jetons.lire(entree)).isEmpty();
    }
}
