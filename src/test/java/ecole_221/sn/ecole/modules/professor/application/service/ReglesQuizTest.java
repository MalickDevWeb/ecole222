package ecole_221.sn.ecole.modules.professor.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Lot D : correction serveur, validation des réponses et masquage des bonnes réponses. */
class ReglesQuizTest {

    static final List<Map<String, Object>> QUESTIONS = List.of(
            Map.of("id", "q1", "questionText", "2 + 2 ?", "options", List.of("3", "4", "5"), "correctAnswerIndex", 1),
            Map.of("id", "q2", "questionText", "Capitale du Sénégal ?", "options", List.of("Dakar", "Thiès"), "correctAnswerIndex", 0),
            Map.of("id", "q3", "questionText", "Tri en O(n log n) ?", "options", List.of("bulles", "fusion"), "correctAnswerIndex", 1));

    @Test
    void lesQuestionsEnvoyeesALEleveNeContiennentPasLaBonneReponse() {
        List<Map<String, Object>> vues = ReglesQuiz.sansReponses(QUESTIONS);
        assertEquals(3, vues.size());
        vues.forEach(v -> assertFalse(v.containsKey("correctAnswerIndex")));
        assertEquals(List.of("3", "4", "5"), vues.get(0).get("options"));
        assertEquals("2 + 2 ?", vues.get(0).get("questionText"));
    }

    @Test
    void scoreCalculeParLeServeur() {
        ReglesQuiz.Correction c = ReglesQuiz.corriger(QUESTIONS, List.of(1, 1, 1));
        assertEquals(2, c.score());
        assertEquals(3, c.total());
        assertEquals(66.7, c.pourcentage());
        assertEquals(Boolean.TRUE, c.details().get(0).get("correcte"));
        assertEquals(Boolean.FALSE, c.details().get(1).get("correcte"));
        assertEquals(0, c.details().get(1).get("bonneReponse"));
    }

    @Test
    void toutJuste() {
        assertEquals(3, ReglesQuiz.corriger(QUESTIONS, List.of(1, 0, 1)).score());
    }

    @Test
    void reponsesIncompletesRefusees() {
        var e = assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, List.of(1, 0)));
        assertEquals(400, e.getStatusCode().value());
        assertTrue(e.getReason().contains("3 réponse(s) attendue(s)"));
    }

    @Test
    void indexHorsDesChoixRefuse() {
        var e = assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, List.of(1, 2, 1)));
        assertEquals(400, e.getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, List.of(-1, 0, 1)));
    }

    @Test
    void reponsesIllisiblesRefusees() {
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, null));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, "1,0,1"));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, List.of("a", 0, 1)));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, List.of(1.5, 0, 1)));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.corriger(QUESTIONS, java.util.Arrays.asList(1, null, 1)));
    }

    @Test
    void nombreDeTentatives() {
        assertEquals(1, ReglesQuiz.maxTentatives(null));
        assertEquals(1, ReglesQuiz.maxTentatives(0));
        assertEquals(3, ReglesQuiz.maxTentatives(3));
        assertNull(ReglesQuiz.maxTentativesSaisi(null));
        assertEquals(2, ReglesQuiz.maxTentativesSaisi("2"));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.maxTentativesSaisi(0));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.maxTentativesSaisi("beaucoup"));
        assertThrows(ResponseStatusException.class, () -> ReglesQuiz.maxTentativesSaisi(99));
    }
}
