package ecole_221.sn.ecole.modules.student.application.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Contenu d'un cours envoyé à l'élève : aucune bonne réponse, le reste intact (2026-11-16). */
class ContenuCoursEleveTest {

    @Test
    void retireLesBonnesReponsesATouteProfondeurSansToucherAuReste() {
        Map<String, Object> choixJuste = new HashMap<>(Map.of("texte", "3", "correct", true));
        Map<String, Object> choixFaux = new HashMap<>(Map.of("texte", "2", "isCorrect", false));
        Map<String, Object> question = new HashMap<>();
        question.put("question", "Combien ?");
        question.put("options", List.of("1", "2", "3"));
        question.put("reponseCorrecte", 2);
        question.put("correct_answer_index", 2);
        question.put("Bonne-Réponse", "3");
        question.put("explication", "Corrigé détaillé");
        question.put("choix", List.of(choixJuste, choixFaux));
        Map<String, Object> module = new HashMap<>();
        module.put("titre", "Chapitre 1");
        module.put("explication", "Présentation du chapitre");   // pas une question : gardée
        module.put("quiz", List.of(question));
        Map<String, Object> contenu = new HashMap<>();
        contenu.put("title", "Algorithmique");
        contenu.put("modules", List.of(module));
        contenu.put("answers", List.of("a", "b"));   // ambigu (liste de choix) : gardé

        Map<String, Object> vue = ContenuCoursEleve.sansBonnesReponses(contenu);

        assertThat(vue).containsEntry("title", "Algorithmique").containsEntry("answers", List.of("a", "b"));
        @SuppressWarnings("unchecked")
        Map<String, Object> m = ((List<Map<String, Object>>) vue.get("modules")).get(0);
        assertThat(m).containsEntry("titre", "Chapitre 1").containsEntry("explication", "Présentation du chapitre");
        @SuppressWarnings("unchecked")
        Map<String, Object> q = ((List<Map<String, Object>>) m.get("quiz")).get(0);
        assertThat(q).containsOnlyKeys("question", "options", "choix");
        assertThat(q.get("options")).isEqualTo(List.of("1", "2", "3"));
        assertThat(q.get("choix")).isEqualTo(List.of(Map.of("texte", "3"), Map.of("texte", "2")));
        // le contenu enregistré n'est pas modifié
        assertThat(question).containsKey("reponseCorrecte");
        assertThat(choixJuste).containsKey("correct");
    }

    @Test
    void contenuAbsent() {
        assertThat(ContenuCoursEleve.sansBonnesReponses(null)).isNull();
        assertThat(ContenuCoursEleve.sansBonnesReponses(Map.of())).isEmpty();
    }
}
