package ecole_221.sn.ecole.modules.professor.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Règles PURES des quiz passés par les élèves (sans base de données, testables seules) :
 * masquage des bonnes réponses, validation des réponses, correction et score, nombre maximal de tentatives.
 */
public final class ReglesQuiz {

    /** Nombre de tentatives par élève quand le professeur n'a rien précisé. */
    public static final int TENTATIVES_PAR_DEFAUT = 1;
    public static final int TENTATIVES_MAX_AUTORISEES = 10;

    private static final ObjectMapper JSON = new ObjectMapper();

    private ReglesQuiz() {}

    /** Résultat de la correction d'une tentative. */
    public record Correction(int score, int total, List<Integer> reponses, List<Map<String, Object>> details) {
        public double pourcentage() {
            return total == 0 ? 0 : Math.round(score * 1000.0 / total) / 10.0;
        }
    }

    /** Questions enregistrées du quiz (liste vide si le JSON est absent ou illisible). */
    public static List<Map<String, Object>> questions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) return List.of();
        try {
            return JSON.readValue(questionsJson, new TypeReference<List<Map<String, Object>>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Questions présentées à l'élève : énoncé et choix, SANS la bonne réponse. */
    public static List<Map<String, Object>> sansReponses(List<Map<String, Object>> questions) {
        List<Map<String, Object>> res = new ArrayList<>();
        int n = 0;
        for (Map<String, Object> q : questions) {
            n++;
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", q.get("id") == null ? "q-" + n : String.valueOf(q.get("id")));
            v.put("questionText", q.get("questionText"));
            v.put("options", options(q));
            res.add(v);
        }
        return res;
    }

    /** Nombre maximal de tentatives effectif (null ou ≤ 0 → 1). */
    public static int maxTentatives(Integer configure) {
        return configure == null || configure <= 0 ? TENTATIVES_PAR_DEFAUT : configure;
    }

    /** Valeur saisie par le professeur pour « maxTentatives » (null = défaut) ; 400 si illisible ou hors bornes. */
    public static Integer maxTentativesSaisi(Object brut) {
        if (brut == null || String.valueOf(brut).isBlank()) return null;
        int n;
        try {
            n = brut instanceof Number num && num.doubleValue() == Math.floor(num.doubleValue())
                    ? num.intValue() : Integer.parseInt(String.valueOf(brut).trim());
        } catch (NumberFormatException e) {
            throw erreur("Le nombre de tentatives doit être un nombre entier.");
        }
        if (n < 1 || n > TENTATIVES_MAX_AUTORISEES) {
            throw erreur("Le nombre de tentatives doit être compris entre 1 et " + TENTATIVES_MAX_AUTORISEES + ".");
        }
        return n;
    }

    /**
     * Valide les réponses de l'élève puis corrige : une réponse (index de choix) par question, dans l'ordre.
     * 400 si la liste est absente, incomplète, trop longue, ou contient un index hors des choix.
     */
    public static Correction corriger(List<Map<String, Object>> questions, Object brut) {
        if (questions.isEmpty()) throw erreur("Ce quiz ne contient aucune question.");
        if (!(brut instanceof List<?> liste)) {
            throw erreur("Envoyez vos réponses dans « reponses » : un numéro de choix par question.");
        }
        if (liste.size() != questions.size()) {
            throw erreur("Répondez à toutes les questions : " + questions.size() + " réponse(s) attendue(s), "
                    + liste.size() + " reçue(s).");
        }
        List<Integer> reponses = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            List<String> options = options(q);
            Object r = liste.get(i);
            if (r == null || String.valueOf(r).isBlank()) throw erreur("La question " + (i + 1) + " est sans réponse.");
            int index;
            if (r instanceof Number num) {
                if (num.doubleValue() != Math.floor(num.doubleValue())) {
                    throw erreur("Réponse illisible à la question " + (i + 1) + ".");
                }
                index = num.intValue();
            } else {
                try {
                    index = Integer.parseInt(String.valueOf(r).trim());
                } catch (NumberFormatException e) {
                    throw erreur("Réponse illisible à la question " + (i + 1) + ".");
                }
            }
            if (index < 0 || index >= options.size()) {
                throw erreur("La réponse à la question " + (i + 1) + " ne correspond à aucun choix proposé.");
            }
            int bonne = bonneReponse(q);
            boolean juste = index == bonne;
            if (juste) score++;
            reponses.add(index);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("questionId", q.get("id") == null ? "q-" + (i + 1) : String.valueOf(q.get("id")));
            d.put("questionText", q.get("questionText"));
            d.put("options", options);
            d.put("reponseDonnee", index);
            d.put("bonneReponse", bonne);
            d.put("correcte", juste);
            details.add(d);
        }
        return new Correction(score, questions.size(), reponses, details);
    }

    /** Correction d'une tentative déjà enregistrée (relecture du résultat). */
    public static List<Map<String, Object>> details(List<Map<String, Object>> questions, List<Integer> reponses) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            Integer donnee = i < reponses.size() ? reponses.get(i) : null;
            int bonne = bonneReponse(q);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("questionId", q.get("id") == null ? "q-" + (i + 1) : String.valueOf(q.get("id")));
            d.put("questionText", q.get("questionText"));
            d.put("options", options(q));
            d.put("reponseDonnee", donnee);
            d.put("bonneReponse", bonne);
            d.put("correcte", donnee != null && donnee == bonne);
            details.add(d);
        }
        return details;
    }

    public static List<Integer> lireReponses(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return JSON.readValue(json, new TypeReference<List<Integer>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String ecrireReponses(List<Integer> reponses) {
        try {
            return JSON.writeValueAsString(reponses);
        } catch (Exception e) {
            throw erreur("Réponses illisibles.");
        }
    }

    static int bonneReponse(Map<String, Object> q) {
        Object b = q.get("correctAnswerIndex");
        try {
            return b instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(b));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static List<String> options(Map<String, Object> q) {
        return q.get("options") instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
    }

    private static ResponseStatusException erreur(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
