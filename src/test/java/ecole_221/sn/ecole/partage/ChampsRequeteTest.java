package ecole_221.sn.ecole.partage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChampsRequeteTest {

    private static Map<String, Object> corps(String cle, Object valeur) {
        Map<String, Object> m = new HashMap<>();
        m.put(cle, valeur);
        return m;
    }

    @Test
    void texteObligatoireRefuseAbsentVideMauvaisTypeEtTropLong() {
        assertThat(ChampsRequete.texte(corps("t", "  Exposé  "), "t", "Le titre", 10)).isEqualTo("Exposé");
        assertThatThrownBy(() -> ChampsRequete.texte(corps("t", null), "t", "Le titre", 10)).hasMessageContaining("Le titre est obligatoire.");
        assertThatThrownBy(() -> ChampsRequete.texte(corps("t", "   "), "t", "Le titre", 10)).hasMessageContaining("obligatoire");
        for (Object absurde : List.of(-999999999999L, true, List.of("a"), Map.of("$ne", "x"))) {
            assertThatThrownBy(() -> ChampsRequete.texte(corps("t", absurde), "t", "Le titre", 10)).hasMessageContaining("doit être un texte");
        }
        assertThatThrownBy(() -> ChampsRequete.texte(corps("t", "A".repeat(11)), "t", "Le titre", 10)).hasMessageContaining("10 caractères");
        assertThatThrownBy(() -> ChampsRequete.texte(null, "t", "Le titre", 10)).hasMessageContaining("obligatoire");
    }

    @Test
    void nombresBornesEtEntiers() {
        assertThat(ChampsRequete.entierOptionnel(corps("n", 12), "n", "La durée", 0, 1440)).isEqualTo(12);
        assertThat(ChampsRequete.entierOptionnel(corps("n", "12"), "n", "La durée", 0, 1440)).isEqualTo(12);
        assertThat(ChampsRequete.entierOptionnel(corps("n", null), "n", "La durée", 0, 1440)).isNull();
        assertThatThrownBy(() -> ChampsRequete.entierOptionnel(corps("n", -999999999999L), "n", "La durée", 0, 1440))
                .hasMessageContaining("doit être entre 0 et 1440");
        assertThatThrownBy(() -> ChampsRequete.entierOptionnel(corps("n", 1.5), "n", "La durée", 0, 1440)).hasMessageContaining("entier");
        assertThatThrownBy(() -> ChampsRequete.entierOptionnel(corps("n", "douze"), "n", "La durée", 0, 1440)).hasMessageContaining("nombre");
        assertThatThrownBy(() -> ChampsRequete.entierOptionnel(corps("n", true), "n", "La durée", 0, 1440)).hasMessageContaining("nombre");
        assertThat(ChampsRequete.nombreOptionnel(corps("n", "12,5"), "n", "La note", 0, 20)).isEqualTo(12.5);
    }

    @Test
    void booleenChoixEtDates() {
        assertThat(ChampsRequete.booleenOptionnel(corps("b", true), "b", "Privé")).isTrue();
        assertThatThrownBy(() -> ChampsRequete.booleenOptionnel(corps("b", "oui"), "b", "Privé")).hasMessageContaining("true / false");
        assertThat(ChampsRequete.choix(corps("s", null), "s", "Le statut", List.of("A faire", "Terminé"), "A faire")).isEqualTo("A faire");
        assertThatThrownBy(() -> ChampsRequete.choix(corps("s", "Piraté"), "s", "Le statut", List.of("A faire", "Terminé"), null))
                .hasMessageContaining("A faire, Terminé");
        assertThat(ChampsRequete.dateOptionnelle(corps("d", "2026-11-13"), "d", "La date")).isEqualTo("2026-11-13");
        assertThat(ChampsRequete.dateOptionnelle(corps("d", "2026-11-13T10:00:00Z"), "d", "La date")).isNotNull();
        assertThatThrownBy(() -> ChampsRequete.dateOptionnelle(corps("d", "2026-13-45"), "d", "La date")).hasMessageContaining("date valide");
        assertThatThrownBy(() -> ChampsRequete.corpsRequis(Map.of())).hasMessageContaining("Aucune donnée");
    }
}
