package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.professor.application.service.FeuilleAppelService;
import ecole_221.sn.ecole.modules.professor.domain.model.CalculMoyenneEvaluations;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.EvaluationJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.EvaluationJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.NoteEvaluationJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.NoteEvaluationJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Courbe de performance d'un élève, mois par mois (APP-BE-13), calculée sur les données réelles :
 * <ul>
 *   <li><b>note</b> : sa moyenne sur 20 des évaluations du mois (date de l'évaluation), avec la règle du professeur
 *       ({@link CalculMoyenneEvaluations} : coefficients, ABS = 0, EXCUSE neutralisée, meilleur rattrapage) ;</li>
 *   <li><b>moyenneClasse</b> : moyenne des moyennes des élèves notés à ces MÊMES évaluations ;</li>
 *   <li><b>presence</b> : % de séances du mois où il était présent ou en retard (absences excusées comptées absentes).</li>
 * </ul>
 * Un mois sans évaluation ou sans appel donne {@code null} pour la valeur concernée : rien n'est inventé.
 */
@Service
public class PerformanceEleveService {

    private final NoteEvaluationJpaRepository notes;
    private final EvaluationJpaRepository evaluations;
    private final AttendanceJpaRepository presences;

    public PerformanceEleveService(NoteEvaluationJpaRepository notes, EvaluationJpaRepository evaluations,
                                   AttendanceJpaRepository presences) {
        this.notes = notes;
        this.evaluations = evaluations;
        this.presences = presences;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> performance(String eleveId) {
        // Évaluations des cours où l'élève a au moins une note (rattrapages compris)
        List<NoteEvaluationJpaEntity> sesNotes = notes.findByStudentId(eleveId);
        Set<String> cours = sesNotes.stream().map(NoteEvaluationJpaEntity::getCourseId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        Map<String, EvaluationJpaEntity> parId = new HashMap<>();
        for (String c : cours) {
            for (EvaluationJpaEntity e : evaluations.findByCourseIdOrderByDateEvaluationAscCreeLeAsc(c)) parId.put(e.getId(), e);
        }
        Map<YearMonth, List<EvaluationJpaEntity>> normalesParMois = new TreeMap<>();
        Map<String, List<EvaluationJpaEntity>> rattrapages = new HashMap<>();
        for (EvaluationJpaEntity e : parId.values()) {
            if (estRattrapage(e)) {
                if (e.getEvaluationRattrapeeId() != null) rattrapages.computeIfAbsent(e.getEvaluationRattrapeeId(), k -> new ArrayList<>()).add(e);
            } else if (e.getDateEvaluation() != null) {
                normalesParMois.computeIfAbsent(YearMonth.from(e.getDateEvaluation()), k -> new ArrayList<>()).add(e);
            }
        }
        Map<String, Map<String, CalculMoyenneEvaluations.Note>> notesParEleve = new HashMap<>();   // élève → (évaluation → note)
        Set<String> chargees = new HashSet<>();

        Map<YearMonth, Double> note = new TreeMap<>(), moyenneClasse = new TreeMap<>();
        for (Map.Entry<YearMonth, List<EvaluationJpaEntity>> m : normalesParMois.entrySet()) {
            List<EvaluationJpaEntity> concernees = new ArrayList<>(m.getValue());
            for (EvaluationJpaEntity e : m.getValue()) concernees.addAll(rattrapages.getOrDefault(e.getId(), List.of()));
            for (EvaluationJpaEntity e : concernees) {
                if (chargees.add(e.getId())) {
                    for (NoteEvaluationJpaEntity n : notes.findByEvaluationId(e.getId())) {
                        notesParEleve.computeIfAbsent(n.getStudentId(), k -> new HashMap<>())
                                .put(n.getEvaluationId(), new CalculMoyenneEvaluations.Note(n.getValeur(), n.getAbsence()));
                    }
                }
            }
            List<CalculMoyenneEvaluations.Evaluation> calc = concernees.stream().map(PerformanceEleveService::versCalcul).toList();
            Double sienne = CalculMoyenneEvaluations.moyenne(calc, notesParEleve.getOrDefault(eleveId, Map.of())).moyenne();
            if (sienne == null) continue;   // aucune note de l'élève ce mois-ci
            note.put(m.getKey(), sienne);
            List<Double> toutes = new ArrayList<>();
            for (Map<String, CalculMoyenneEvaluations.Note> n : notesParEleve.values()) {
                Double moy = CalculMoyenneEvaluations.moyenne(calc, n).moyenne();
                if (moy != null) toutes.add(moy);
            }
            moyenneClasse.put(m.getKey(), arrondi(toutes.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN)));
        }

        Map<YearMonth, int[]> appel = new TreeMap<>();   // [présent ou retard, total]
        for (AttendanceJpaEntity a : presences.findByStudentId(eleveId)) {
            String statut = FeuilleAppelService.statutAffiche(a.getStatus());
            YearMonth mois = mois(a.getDate());
            if (statut == null || mois == null) continue;
            int[] c = appel.computeIfAbsent(mois, k -> new int[2]);
            if ("PRESENT".equals(statut) || "RETARD".equals(statut)) c[0]++;
            c[1]++;
        }

        TreeSet<YearMonth> moisConnus = new TreeSet<>(note.keySet());
        moisConnus.addAll(appel.keySet());
        List<Map<String, Object>> res = new ArrayList<>();
        for (YearMonth m : moisConnus) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("mois", m.toString());
            v.put("note", note.get(m));
            v.put("moyenneClasse", moyenneClasse.get(m));
            int[] c = appel.get(m);
            v.put("presence", c == null || c[1] == 0 ? null : Math.round(c[0] * 100.0f / c[1]));
            v.put("seances", c == null ? 0 : c[1]);
            v.put("evaluations", normalesParMois.getOrDefault(m, List.of()).size());
            res.add(v);
        }
        return res;
    }

    private static boolean estRattrapage(EvaluationJpaEntity e) {
        return "RATTRAPAGE".equals(e.getSession());
    }

    private static CalculMoyenneEvaluations.Evaluation versCalcul(EvaluationJpaEntity e) {
        return new CalculMoyenneEvaluations.Evaluation(e.getId(), e.getCoefficient(), e.getNoteSur(), estRattrapage(e), e.getEvaluationRattrapeeId());
    }

    private static YearMonth mois(String date) {
        if (date == null || date.length() < 10) return null;
        try {
            return YearMonth.from(LocalDate.parse(date.substring(0, 10)));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Double arrondi(double v) {
        return Double.isNaN(v) ? null : Math.round(v * 100.0) / 100.0;
    }
}
