package ecole_221.sn.ecole.modules.corbeille.application;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Textes de la corbeille selon la charte d'écriture : accord au genre du type, pluriels réels, jamais « élément(s) ». */
class TextesCorbeilleTest {

    @Test
    void accordAuGenreDuType() {
        assertThat(TypeCorbeille.SALLE.messageMiseALaCorbeille("Salle “B12”")).isEqualTo("Salle “B12” placée dans la corbeille.");
        assertThat(TypeCorbeille.CRENEAU_HORAIRE.messageMiseALaCorbeille("Créneau horaire « Lundi 08:00-10:00 »"))
                .isEqualTo("Créneau horaire « Lundi 08:00-10:00 » placé dans la corbeille.");
        assertThat(TypeCorbeille.EVALUATION.accorde("restauré")).isEqualTo("restaurée");
        assertThat(TypeCorbeille.MESSAGE.accorde("restauré")).isEqualTo("restauré");
        assertThat(TypeCorbeille.REFERENTIEL.demonstratif()).isEqualTo("cette valeur de référentiel");
        assertThat(TypeCorbeille.DEVOIR.demonstratif()).isEqualTo("ce devoir");
        assertThat(TypeCorbeille.SEANCE_EN_DIRECT.pronom()).isEqualTo("elle");
        for (TypeCorbeille t : TypeCorbeille.values()) {
            assertThat(t.messageMiseALaCorbeille("X")).doesNotContain("(e)").doesNotContain("élément");
            assertThat(t.pluriel()).isNotBlank();
        }
    }

    @Test
    void plusieursMisesALaCorbeilleAuPluriel() {
        assertThat(TypeCorbeille.CLASSE.messageMiseALaCorbeille(List.of("Classe “L1”")))
                .isEqualTo("Classe “L1” placée dans la corbeille.");
        assertThat(TypeCorbeille.CLASSE.messageMiseALaCorbeille(List.of("Classe “L1”", "Classe “L2”")))
                .isEqualTo("2 classes placées dans la corbeille : Classe “L1”, Classe “L2”.");
        assertThat(TypeCorbeille.CRENEAU.messageMiseALaCorbeille(List.of("a", "b", "c", "d", "e", "f", "g")))
                .isEqualTo("7 créneaux d'emploi du temps placés dans la corbeille : a, b, c, d, e et 2 autres.");
    }

    @Test
    void elementsLiesAuPlurielReel() {
        assertThat(CorbeilleService.lies(TypeCorbeille.QUIZ, 1)).isEqualTo("1 tentative d'élève");
        assertThat(CorbeilleService.lies(TypeCorbeille.QUIZ, 3)).isEqualTo("3 tentatives d'élèves");
        assertThat(CorbeilleService.lies(TypeCorbeille.DEVOIR, 2)).isEqualTo("2 rendus d'élèves");
        assertThat(CorbeilleService.lies(TypeCorbeille.SALLE, 2)).isEqualTo("2 données liées");
    }

    @Test
    void reponseDeSuppressionCommune() {
        Map<String, Object> r = CorbeilleService.reponseSuppression(TypeCorbeille.FILIERE, "f-1", "Filière “Informatique”");
        assertThat(r).containsEntry("id", "f-1").containsEntry("type", "filiere").containsEntry("libelle", "Filière “Informatique”")
                .containsEntry("supprime", true).containsEntry("corbeille", true)
                .containsEntry("message", "Filière “Informatique” placée dans la corbeille.");
    }
}
