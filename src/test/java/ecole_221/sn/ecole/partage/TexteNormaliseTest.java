package ecole_221.sn.ecole.partage;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TexteNormaliseTest {

    @Test
    void invisiblesEtFormesUnicodeRamenesAuMemeTexte() {
        String nfd = Normalizer.normalize("Préau", Normalizer.Form.NFD);
        assertThat(nfd).isNotEqualTo("Préau");
        assertThat(TexteNormalise.normaliser(nfd)).isEqualTo("Préau");
        assertThat(TexteNormalise.normaliser("Entree ​Sud")).isEqualTo("Entree Sud");
        assertThat(TexteNormalise.normaliser("﻿Salle‮ 1 ")).isEqualTo("Salle 1");
        assertThat(TexteNormalise.normaliser("15 000")).isEqualTo("15 000");
        assertThat(TexteNormalise.normaliser("ligne 1\nligne 2")).isEqualTo("ligne 1\nligne 2");   // retours conservés
        assertThat(TexteNormalise.normaliser("​​")).isEmpty();
    }

    @Test
    void cleDeComparaisonIgnoreCasseEspacesEtInvisibles() {
        assertThat(TexteNormalise.cle("  L1  INFO ")).isEqualTo(TexteNormalise.cle("l1 info"));
        assertThat(TexteNormalise.cle("Entree Sud")).isEqualTo(TexteNormalise.cle("entree sud"));
    }

    @Test
    void champsRequeteRefuseUnNomFaitDeCaracteresInvisibles() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> ChampsRequete.texte(Map.of("name", "​⁠"), "name", "Le nom", 100))
                .hasMessageContaining("obligatoire");
    }
}
