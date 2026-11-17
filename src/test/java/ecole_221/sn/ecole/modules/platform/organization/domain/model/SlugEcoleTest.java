package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Identifiant d'école (15/11, défauts F2 et F3, point 9 de la revue DevSecOps). */
class SlugEcoleTest {

    @Test
    void accentsRetiresDepuisLeNom() {
        assertThat(SlugEcole.depuisNom("Lycée de Thiès")).isEqualTo("lycee-de-thies");
        assertThat(SlugEcole.depuisNom("  École Œcuménique  d'Été !")).isEqualTo("ecole-oecumenique-d-ete");
        assertThat(SlugEcole.depuisNom("Institut " + "très ".repeat(20) + "long").length()).isLessThanOrEqualTo(40);
    }

    @Test
    void collisionSuffixePrevisibleEtNomsReserves() {
        Set<String> pris = Set.of("lycee-de-thies", "lycee-de-thies-2");
        assertThat(SlugEcole.libre("lycee-de-thies", pris::contains)).isEqualTo("lycee-de-thies-3");
        // B4 (revue phase 2) : une base réservée ou préfixée par l'École 221 est REFUSÉE, jamais suffixée
        for (String b : new String[]{"admin", "ecole-221", "ecole221", "ecole-221-dakar", "ecole221-paiements", "test"}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> SlugEcole.libre(b, s -> false)).as(b)
                    .isInstanceOf(SlugEcole.BaseReservee.class).hasMessageContaining("réservé");
        }
        assertThat(SlugEcole.libre("lycee-221", s -> s.equals("lycee-221"))).as("collision ordinaire").isEqualTo("lycee-221-2");
        assertThat(SlugEcole.libre("ab", s -> false)).isEqualTo("ecole-ab");
        assertThat(SlugEcole.libre("", s -> false)).isEqualTo("ecole");
        String long40 = "a".repeat(40);
        assertThat(SlugEcole.libre(long40, long40::equals)).hasSize(40).endsWith("-2");
    }

    @Test
    void validationDuSlugFourni() {
        assertThat(SlugEcole.erreur("lycee-de-thies")).isNull();
        assertThat(SlugEcole.erreur("lycée")).contains("invalide").contains("lycee");
        assertThat(SlugEcole.erreur("ab")).contains("3 à 40");
        assertThat(SlugEcole.erreur("a".repeat(41))).contains("invalide");
        assertThat(SlugEcole.erreur("a--b")).contains("invalide");
        assertThat(SlugEcole.erreur("-abc")).contains("invalide");
        assertThat(SlugEcole.erreur("xn--abc")).contains("xn--");
        for (String r : new String[]{"www", "api", "admin", "administration", "login", "paiements", "orange-money", "wave",
                "staging", "demo", "mta-sts", "autodiscover", "ecole221", "ecole-221", "par-hote", "ecole-221-dakar",
                "ecole221-paiements"}) {
            assertThat(SlugEcole.erreur(r)).as(r).contains("réservé");
        }
        assertThat(SlugEcole.reserveInfrastructure("ecole-221")).as("l'école existante reste résolue").isFalse();
    }
}
