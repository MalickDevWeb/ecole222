package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** APP-QA-04 : une salle ou une classe enregistrée doit rester relisible (capacité ≥ 1), et un nom reste un texte. */
class PlanningPayloadsChampsTest {

    private static Map<String, Object> ligne(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void salleSansCapaciteOuDeCapaciteNulleRefusee() {
        assertThatThrownBy(() -> PlanningPayloads.salles(List.of(ligne("name", "Salle B")))).hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> PlanningPayloads.salles(List.of(ligne("name", "Salle B", "capacity", 0)))).hasMessageContaining("au moins 1");
        assertThat(PlanningPayloads.salles(List.of(ligne("name", "Salle B", "capacity", 30)))).hasSize(1);
    }

    @Test
    void classeSansCapaciteRefusee() {
        assertThatThrownBy(() -> PlanningPayloads.classes(List.of(ligne("name", "Terminale S", "filiereId", "f1"))))
                .hasMessageContaining("capacité de la classe « Terminale S » est obligatoire");
    }

    @Test
    void nomQuiNEstPasUnTexteRefuse() {
        assertThatThrownBy(() -> PlanningPayloads.salles(List.of(ligne("name", List.of("a", 1), "capacity", 10))))
                .hasMessageContaining("doit être un texte");
        assertThatThrownBy(() -> PlanningPayloads.salles(List.of(ligne("name", true, "capacity", 10))))
                .hasMessageContaining("doit être un texte");
        assertThatThrownBy(() -> PlanningPayloads.filieres(List.of(ligne("name", -999999999999L))))
                .hasMessageContaining("pas un nombre");
    }
}
