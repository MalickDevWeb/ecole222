package ecole_221.sn.ecole.modules.professor.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calcul (pur, sans base de données) de la moyenne d'un élève dans un cours à partir de ses évaluations.
 *
 * <h2>Règle appliquée (documentée et testée dans CalculMoyenneEvaluationsTest)</h2>
 * <ol>
 *   <li>Chaque note est ramenée sur 20 : {@code valeur / noteSur × 20}.</li>
 *   <li>Seules les évaluations de la session NORMALE portent un coefficient dans la moyenne.</li>
 *   <li>Une évaluation de RATTRAPAGE désigne l'évaluation normale qu'elle rattrape
 *       ({@code evaluationRattrapeeId}). Sa note (sur 20) REMPLACE celle de l'évaluation normale
 *       uniquement si elle est MEILLEURE ; elle remplace aussi une note absente, une absence (ABS)
 *       ou une absence excusée (EXCUSE). Si plusieurs rattrapages visent la même évaluation, le meilleur compte.
 *       Une absence au rattrapage ne change rien.</li>
 *   <li>Absence non justifiée (ABS) = 0 ; absence justifiée (EXCUSE) = évaluation neutralisée
 *       (ni la note, ni le coefficient ne comptent) ; évaluation sans note saisie = non comptée.</li>
 *   <li>Moyenne = Σ(note sur 20 × coefficient) / Σ(coefficients comptés), arrondie au centième.</li>
 *   <li>Aucune évaluation comptée → moyenne {@code null} (affichée « — »). Rien n'est inventé.</li>
 * </ol>
 */
public final class CalculMoyenneEvaluations {

    public static final String ABS = "ABS";
    public static final String EXCUSE = "EXCUSE";

    private CalculMoyenneEvaluations() {}

    /** Évaluation vue par le calcul. */
    public record Evaluation(String id, double coefficient, double noteSur, boolean rattrapage, String evaluationRattrapeeId) {}

    /** Note d'un élève : valeur (sur noteSur) OU absence (ABS / EXCUSE). */
    public record Note(Double valeur, String absence) {}

    /** Résultat : moyenne sur 20 (null si rien à compter) et nombre d'évaluations normales comptées. */
    public record Resultat(Double moyenne, int evaluationsComptees, double coefficientsComptes) {}

    /**
     * @param evaluations toutes les évaluations du cours
     * @param notes notes de l'élève, indexées par identifiant d'évaluation
     */
    public static Resultat moyenne(Collection<Evaluation> evaluations, Map<String, Note> notes) {
        Map<String, List<Evaluation>> rattrapages = new HashMap<>();
        for (Evaluation e : evaluations) {
            if (e.rattrapage() && e.evaluationRattrapeeId() != null) {
                rattrapages.computeIfAbsent(e.evaluationRattrapeeId(), k -> new ArrayList<>()).add(e);
            }
        }
        double somme = 0;
        double coefficients = 0;
        int comptees = 0;
        for (Evaluation e : evaluations) {
            if (e.rattrapage() || e.coefficient() <= 0 || e.noteSur() <= 0) continue;
            Note n = notes.get(e.id());
            Double retenue = null;          // note sur 20 retenue pour cette évaluation
            if (n != null) {
                if (n.valeur() != null) retenue = surVingt(n.valeur(), e.noteSur());
                else if (ABS.equals(n.absence())) retenue = 0.0;
                // EXCUSE ou rien : neutralisée tant qu'aucun rattrapage n'est noté
            }
            for (Evaluation r : rattrapages.getOrDefault(e.id(), List.of())) {
                Note nr = notes.get(r.id());
                if (nr == null || nr.valeur() == null || r.noteSur() <= 0) continue;
                double vr = surVingt(nr.valeur(), r.noteSur());
                if (retenue == null || vr > retenue) retenue = vr;
            }
            if (retenue == null) continue;
            somme += retenue * e.coefficient();
            coefficients += e.coefficient();
            comptees++;
        }
        if (coefficients <= 0) return new Resultat(null, 0, 0);
        return new Resultat(arrondi(somme / coefficients), comptees, coefficients);
    }

    /** Mention d'une moyenne sur 20 ; null si pas de moyenne. */
    public static String mention(Double moyenne) {
        if (moyenne == null) return null;
        if (moyenne >= 16) return "Très bien";
        if (moyenne >= 14) return "Bien";
        if (moyenne >= 12) return "Assez bien";
        if (moyenne >= 10) return "Passable";
        return "Insuffisant";
    }

    /**
     * Rang de chaque élève ayant une moyenne (1 = meilleure) ; ex æquo au même rang, le suivant saute
     * (16, 14, 14, 12 → 1, 2, 2, 4). Les élèves sans moyenne n'ont pas de rang.
     */
    public static Map<String, Integer> rangs(Map<String, Double> moyennes) {
        List<Map.Entry<String, Double>> classees = moyennes.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();
        Map<String, Integer> rangs = new HashMap<>();
        Double precedente = null;
        int rang = 0;
        for (int i = 0; i < classees.size(); i++) {
            double m = classees.get(i).getValue();
            if (precedente == null || Double.compare(m, precedente) != 0) rang = i + 1;
            rangs.put(classees.get(i).getKey(), rang);
            precedente = m;
        }
        return rangs;
    }

    static double surVingt(double valeur, double noteSur) {
        return valeur / noteSur * 20.0;
    }

    public static double arrondi(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
