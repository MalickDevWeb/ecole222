package ecole_221.sn.ecole.modules.student.application.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Contenu d'un cours tel qu'il peut être envoyé au navigateur d'un élève (2026-11-16).
 *
 * <p>Avant : GET /student/courses/{id}/details renvoyait le contenu brut de la table course_details, quiz compris
 * AVEC leur bonne réponse (« reponseCorrecte ») : l'élève la lisait dans le navigateur avant de répondre. Les quiz
 * se passent désormais par GET /student/courses/{id}/quizzes et POST /student/quizzes/{id}/tentatives (corrigés par
 * le serveur) ; ici, toute clé qui désigne une bonne réponse ou un corrigé est retirée, à toute profondeur, sans
 * toucher au reste du contenu (titre, modules, ressources, questions et choix).</p>
 */
public final class ContenuCoursEleve {

    /** Clés retirées, comparées sans accents, sans casse, sans « _ » ni « - ». */
    private static final Set<String> CLES_REPONSE = Set.of(
            "reponsecorrecte", "reponsescorrectes", "bonnereponse", "bonnesreponses",
            "indexreponsecorrecte", "indicereponsecorrecte",
            "correctanswer", "correctanswers", "correctanswerindex", "correctindex", "correctoption", "correctoptions",
            // « answers » n'est pas retiré : dans certains contenus, c'est la liste des choix proposés
            "answerindex", "reponseattendue",
            "iscorrect", "estcorrecte", "estcorrect", "correct", "correcte",
            "solution", "solutions", "corrige", "correction");

    /** Explication du corrigé : retirée seulement dans un objet qui portait une bonne réponse (une question). */
    private static final Set<String> CLES_EXPLICATION = Set.of("explication", "explanation", "justification");

    private ContenuCoursEleve() {
    }

    /** Copie du contenu sans aucune bonne réponse ; le contenu enregistré n'est pas modifié. */
    public static Map<String, Object> sansBonnesReponses(Map<String, Object> contenu) {
        if (contenu == null) return null;
        return nettoyerObjet(contenu);
    }

    private static Map<String, Object> nettoyerObjet(Map<?, ?> objet) {
        boolean portaitUneReponse = objet.keySet().stream().anyMatch(k -> CLES_REPONSE.contains(cle(k)));
        Map<String, Object> copie = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : objet.entrySet()) {
            String k = cle(e.getKey());
            if (CLES_REPONSE.contains(k)) continue;
            if (portaitUneReponse && CLES_EXPLICATION.contains(k)) continue;
            copie.put(String.valueOf(e.getKey()), nettoyer(e.getValue()));
        }
        return copie;
    }

    private static Object nettoyer(Object valeur) {
        if (valeur instanceof Map<?, ?> m) return nettoyerObjet(m);
        if (valeur instanceof List<?> l) {
            List<Object> copie = new ArrayList<>(l.size());
            for (Object o : l) copie.add(nettoyer(o));
            return copie;
        }
        return valeur;
    }

    private static String cle(Object k) {
        String s = Normalizer.normalize(String.valueOf(k), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}
