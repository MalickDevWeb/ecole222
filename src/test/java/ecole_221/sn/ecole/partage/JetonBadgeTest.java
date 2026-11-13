package ecole_221.sn.ecole.partage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Badge QR dynamique signé par le serveur (2026-11-13). */
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
    }

    @Test
    void ancienAlgorithmeEtAutreCleRefuses() {
        long m = JetonBadge.minuteCourante();
        // ancien jeton FNV-1a 32 bits (8 caractères) : refusé
        assertThat(jetons.valide("ETU-001", m, "1A2B3C4D")).isFalse();
        // même algorithme, autre clé : refusé
        assertThat(jetons.valide("ETU-001", m, new JetonBadge("autre", "").jeton("ETU-001", m))).isFalse();
        // la clé dépend du secret du serveur
        assertThat(new JetonBadge("", "secret-jwt-de-test-assez-long-0123456789").jeton("ETU-001", m))
                .isEqualTo(jetons.jeton("ETU-001", m));
    }

    @Test
    void contenuMalFormeLuCommeInvalide() {
        assertThat(jetons.lire("ETU-001")).isEmpty();
        assertThat(jetons.lire("STU_SECURE:ETU-001").orElseThrow().valide()).isFalse();
        assertThat(jetons.lire("STU_SECURE:ETU-001:abc:XYZ").orElseThrow().valide()).isFalse();
    }
}
