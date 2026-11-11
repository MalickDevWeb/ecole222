package ecole_221.sn.ecole.modules.professor.domain.model;

import ecole_221.sn.ecole.modules.professor.domain.model.CalculMoyenneEvaluations.Evaluation;
import ecole_221.sn.ecole.modules.professor.domain.model.CalculMoyenneEvaluations.Note;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Règle de moyenne pondérée, rattrapage, absences, mention et rang (lot C — évaluations). */
class CalculMoyenneEvaluationsTest {

    private static final Evaluation DS = new Evaluation("ds", 1, 20, false, null);
    private static final Evaluation EXAM = new Evaluation("exam", 2, 20, false, null);
    private static final Evaluation TP_SUR_10 = new Evaluation("tp", 1, 10, false, null);
    private static final Evaluation RATT_EXAM = new Evaluation("ratt", 1, 20, true, "exam");

    private static Map<String, Note> notes(Object... couples) {
        Map<String, Note> m = new HashMap<>();
        for (int i = 0; i < couples.length; i += 2) {
            Object v = couples[i + 1];
            m.put((String) couples[i], v instanceof String s ? new Note(null, s) : new Note(((Number) v).doubleValue(), null));
        }
        return m;
    }

    @Test
    void moyennePondereeParLesCoefficients() {
        // (10×1 + 16×2) / 3 = 14
        var r = CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), notes("ds", 10, "exam", 16));
        assertEquals(14.0, r.moyenne());
        assertEquals(2, r.evaluationsComptees());
    }

    @Test
    void lesNotesSontRameneesSurVingt() {
        // TP 7/10 = 14/20 ; (14 + 8) / 2 = 11
        var r = CalculMoyenneEvaluations.moyenne(List.of(DS, TP_SUR_10), notes("ds", 8, "tp", 7));
        assertEquals(11.0, r.moyenne());
    }

    @Test
    void arrondiAuCentieme() {
        // (13×1 + 14×2) / 3 = 13.666… → 13.67
        assertEquals(13.67, CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), notes("ds", 13, "exam", 14)).moyenne());
    }

    @Test
    void aucuneNoteDonneUneMoyenneNulle() {
        var r = CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), Map.of());
        assertNull(r.moyenne());
        assertEquals(0, r.evaluationsComptees());
    }

    @Test
    void uneEvaluationNonNoteeNestPasComptee() {
        assertEquals(12.0, CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), notes("ds", 12)).moyenne());
    }

    @Test
    void absenceNonJustifieeVautZeroEtExcuseeEstNeutralisee() {
        assertEquals(4.0, CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), notes("ds", 12, "exam", "ABS")).moyenne());
        assertEquals(12.0, CalculMoyenneEvaluations.moyenne(List.of(DS, EXAM), notes("ds", 12, "exam", "EXCUSE")).moyenne());
        assertNull(CalculMoyenneEvaluations.moyenne(List.of(DS), notes("ds", "EXCUSE")).moyenne());
    }

    @Test
    void leRattrapageRemplaceLaNoteSeulementSIlEstMeilleur() {
        List<Evaluation> evals = List.of(DS, EXAM, RATT_EXAM);
        // examen 6, rattrapage 12 → examen retenu 12 : (10 + 24) / 3 = 11.33
        assertEquals(11.33, CalculMoyenneEvaluations.moyenne(evals, notes("ds", 10, "exam", 6, "ratt", 12)).moyenne());
        // examen 14, rattrapage 9 → on garde 14 : (10 + 28) / 3 = 12.67
        assertEquals(12.67, CalculMoyenneEvaluations.moyenne(evals, notes("ds", 10, "exam", 14, "ratt", 9)).moyenne());
        // le rattrapage n'ajoute PAS de coefficient
        assertEquals(2, CalculMoyenneEvaluations.moyenne(evals, notes("ds", 10, "exam", 6, "ratt", 12)).evaluationsComptees());
    }

    @Test
    void leRattrapageRemplaceUneAbsenceEtUneAbsenceAuRattrapageNeChangeRien() {
        List<Evaluation> evals = List.of(EXAM, RATT_EXAM);
        assertEquals(11.0, CalculMoyenneEvaluations.moyenne(evals, notes("exam", "ABS", "ratt", 11)).moyenne());
        assertEquals(11.0, CalculMoyenneEvaluations.moyenne(evals, notes("exam", "EXCUSE", "ratt", 11)).moyenne());
        assertEquals(11.0, CalculMoyenneEvaluations.moyenne(evals, notes("ratt", 11)).moyenne());
        assertEquals(7.0, CalculMoyenneEvaluations.moyenne(evals, notes("exam", 7, "ratt", "ABS")).moyenne());
    }

    @Test
    void unRattrapageSeulSansEvaluationViseeNeCompteRien() {
        var orphelin = new Evaluation("r2", 1, 20, true, "inexistante");
        assertNull(CalculMoyenneEvaluations.moyenne(List.of(orphelin), notes("r2", 15)).moyenne());
    }

    @Test
    void mentions() {
        assertNull(CalculMoyenneEvaluations.mention(null));
        assertEquals("Très bien", CalculMoyenneEvaluations.mention(16.0));
        assertEquals("Bien", CalculMoyenneEvaluations.mention(14.0));
        assertEquals("Assez bien", CalculMoyenneEvaluations.mention(12.5));
        assertEquals("Passable", CalculMoyenneEvaluations.mention(10.0));
        assertEquals("Insuffisant", CalculMoyenneEvaluations.mention(9.99));
    }

    @Test
    void rangsAvecExAequoEtSansMoyenne() {
        Map<String, Double> m = new HashMap<>();
        m.put("a", 16.0);
        m.put("b", 14.0);
        m.put("c", 14.0);
        m.put("d", 12.0);
        m.put("e", null);
        Map<String, Integer> r = CalculMoyenneEvaluations.rangs(m);
        assertEquals(1, r.get("a"));
        assertEquals(2, r.get("b"));
        assertEquals(2, r.get("c"));
        assertEquals(4, r.get("d"));
        assertFalse(r.containsKey("e"));
    }
}
