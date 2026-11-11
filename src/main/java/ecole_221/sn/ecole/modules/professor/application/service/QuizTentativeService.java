package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.QuizJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.QuizJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.TentativeQuizJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.TentativeQuizJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Quiz passés par les élèves (lot D) et résultats pour le professeur.
 *
 * <ul>
 *   <li>un élève ne voit et ne passe que les quiz des cours de SA classe (404 sinon, même message qu'un quiz
 *       inexistant : on ne révèle pas l'existence des quiz des autres classes) ;</li>
 *   <li>les bonnes réponses ne sont jamais envoyées avant la tentative ; la correction n'est montrée que
 *       lorsque l'élève a épuisé ses tentatives (sinon il pourrait recopier la correction à l'essai suivant) ;</li>
 *   <li>la correction et le score sont calculés par le serveur à partir des questions enregistrées ;</li>
 *   <li>nombre de tentatives limité (quiz.maxTentatives, 1 par défaut) : 409 au-delà.</li>
 * </ul>
 */
@Service
public class QuizTentativeService {

    private final QuizJpaRepository quiz;
    private final TentativeQuizJpaRepository tentatives;
    private final CourseJpaRepository cours;
    private final ModuleJpaRepository modules;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ComptesEcoleService comptesEcole;

    public QuizTentativeService(QuizJpaRepository quiz, TentativeQuizJpaRepository tentatives, CourseJpaRepository cours,
                                ModuleJpaRepository modules, InscriptionClasseJpaRepository inscriptions,
                                ComptesEcoleService comptesEcole) {
        this.quiz = quiz;
        this.tentatives = tentatives;
        this.cours = cours;
        this.modules = modules;
        this.inscriptions = inscriptions;
        this.comptesEcole = comptesEcole;
    }

    // ================================================================ côté élève

    /** Quiz d'un cours de la classe de l'élève, sans les bonnes réponses, avec l'état de ses tentatives. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> quizDuCoursPourEleve(String courseId, String eleveId) {
        coursDeSaClasse(courseId, eleveId, "Cours introuvable parmi les cours de votre classe.");
        Map<String, String> titresModules = new HashMap<>();
        for (ModuleJpaEntity m : modules.findByCourseIdOrderByNameAsc(courseId)) titresModules.put(m.getId(), m.getName());
        return quiz.findByCourseId(courseId).stream()
                .sorted(Comparator.comparing(q -> String.valueOf(q.getTitle()), String.CASE_INSENSITIVE_ORDER))
                .map(q -> vueEleve(q, titresModules.get(q.getModuleId()),
                        tentatives.findByQuizIdAndStudentIdOrderByDateTentativeAsc(q.getId(), eleveId)))
                .toList();
    }

    /** Enregistre une tentative : réponses validées, correction et score calculés ici. */
    @Transactional
    public Map<String, Object> passer(String quizId, String eleveId, Map<String, Object> corps) {
        QuizJpaEntity q = quizDeSaClasse(quizId, eleveId);
        int max = ReglesQuiz.maxTentatives(q.getMaxTentatives());
        long dejaFaites = tentatives.countByQuizIdAndStudentId(quizId, eleveId);
        if (dejaFaites >= max) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, max == 1
                    ? "Vous avez déjà passé ce quiz : une seule tentative est autorisée."
                    : "Vous avez déjà utilisé vos " + max + " tentatives pour ce quiz.");
        }
        List<Map<String, Object>> questions = ReglesQuiz.questions(q.getQuestionsJson());
        Object reponses = corps == null ? null : corps.getOrDefault("reponses", corps.get("answers"));
        ReglesQuiz.Correction c = ReglesQuiz.corriger(questions, reponses);

        TentativeQuizJpaEntity t = new TentativeQuizJpaEntity();
        t.setId("tentative-" + UUID.randomUUID());
        t.setQuizId(quizId);
        t.setCourseId(q.getCourseId());
        t.setStudentId(eleveId);
        t.setReponsesJson(ReglesQuiz.ecrireReponses(c.reponses()));
        t.setScore(c.score());
        t.setTotal(c.total());
        t.setDateTentative(LocalDateTime.now());
        tentatives.save(t);

        int restantes = (int) Math.max(0, max - dejaFaites - 1);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tentativeId", t.getId());
        res.put("quizId", quizId);
        res.put("score", c.score());
        res.put("total", c.total());
        res.put("pourcentage", c.pourcentage());
        res.put("date", t.getDateTentative().toString());
        res.put("numeroTentative", dejaFaites + 1);
        res.put("maxTentatives", max);
        res.put("tentativesRestantes", restantes);
        res.put("correction", restantes == 0 ? c.details() : null);
        res.put("message", "Quiz corrigé : " + c.score() + " bonne(s) réponse(s) sur " + c.total() + ".");
        return res;
    }

    /** Résultat(s) de l'élève à ce quiz ; 404 tant qu'il ne l'a pas passé (aucune correction avant la tentative). */
    @Transactional(readOnly = true)
    public Map<String, Object> mesResultats(String quizId, String eleveId) {
        QuizJpaEntity q = quizDeSaClasse(quizId, eleveId);
        List<TentativeQuizJpaEntity> miennes = tentatives.findByQuizIdAndStudentIdOrderByDateTentativeAsc(quizId, eleveId);
        if (miennes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vous n'avez pas encore passé ce quiz : aucun résultat à afficher.");
        }
        int max = ReglesQuiz.maxTentatives(q.getMaxTentatives());
        int restantes = Math.max(0, max - miennes.size());
        List<Map<String, Object>> liste = new ArrayList<>();
        int n = 0;
        for (TentativeQuizJpaEntity t : miennes) {
            Map<String, Object> v = resume(t);
            v.put("numero", ++n);
            liste.add(v);
        }
        TentativeQuizJpaEntity derniere = miennes.get(miennes.size() - 1);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("quizId", quizId);
        res.put("title", q.getTitle());
        res.put("maxTentatives", max);
        res.put("tentativesUtilisees", miennes.size());
        res.put("tentativesRestantes", restantes);
        res.put("tentatives", liste);
        res.put("derniere", resume(derniere));
        res.put("correction", restantes == 0
                ? ReglesQuiz.details(ReglesQuiz.questions(q.getQuestionsJson()), ReglesQuiz.lireReponses(derniere.getReponsesJson()))
                : null);
        return res;
    }

    // ================================================================ côté professeur

    /**
     * Résultats d'un quiz pour le professeur (le cours est contrôlé en amont par AccesCoursInterceptor) :
     * chaque élève de la classe avec sa meilleure tentative ou « non fait », taux de réussite par question, moyenne.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resultats(String courseId, String quizId) {
        QuizJpaEntity q = quiz.findById(quizId)
                .filter(x -> courseId.equals(x.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable dans ce cours."));
        CourseJpaEntity c = cours.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        List<Map<String, Object>> questions = ReglesQuiz.questions(q.getQuestionsJson());

        // meilleure tentative de chaque élève (score le plus haut, puis la plus ancienne)
        Map<String, List<TentativeQuizJpaEntity>> parEleve = new LinkedHashMap<>();
        for (TentativeQuizJpaEntity t : tentatives.findByQuizId(quizId)) {
            parEleve.computeIfAbsent(t.getStudentId(), k -> new ArrayList<>()).add(t);
        }
        Map<String, TentativeQuizJpaEntity> retenues = new LinkedHashMap<>();
        parEleve.forEach((eleve, liste) -> retenues.put(eleve, liste.stream()
                .min(Comparator.comparingInt(TentativeQuizJpaEntity::getScore).reversed()
                        .thenComparing(TentativeQuizJpaEntity::getDateTentative))
                .orElseThrow()));

        Set<String> classe = new LinkedHashSet<>();
        if (c.getClasse() != null) {
            inscriptions.findByClasseId(c.getClasse()).stream().map(InscriptionClasseJpaEntity::getUserId).forEach(classe::add);
        }
        Map<String, UserJpaEntity> comptes = new HashMap<>();
        for (var e : comptesEcole.membres("ETUDIANT")) comptes.put(e.getKey().getId(), e.getKey());

        List<Map<String, Object>> eleves = new ArrayList<>();
        Set<String> ordre = new LinkedHashSet<>(classe);
        ordre.addAll(retenues.keySet());   // élève qui a passé le quiz puis changé de classe : gardé, signalé
        for (String id : ordre) {
            UserJpaEntity u = comptes.get(id);
            if (u == null && !retenues.containsKey(id)) continue;   // inscription orpheline
            TentativeQuizJpaEntity t = retenues.get(id);
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("studentId", id);
            v.put("nom", u == null ? null : u.getNom());
            v.put("prenom", u == null ? null : u.getPrenom());
            v.put("nomComplet", u == null ? "Compte introuvable" : ComptesEcoleService.nomComplet(u));
            v.put("matricule", u == null ? null : u.getMatricule());
            v.put("horsClasse", !classe.contains(id));
            v.put("statut", t == null ? "NON_FAIT" : "FAIT");
            v.put("libelleStatut", t == null ? "Non fait" : "Fait");
            v.put("score", t == null ? null : t.getScore());
            v.put("total", t == null ? null : t.getTotal());
            v.put("pourcentage", t == null ? null : pourcentage(t.getScore(), t.getTotal()));
            v.put("nombreTentatives", parEleve.getOrDefault(id, List.of()).size());
            v.put("date", t == null ? null : t.getDateTentative().toString());
            eleves.add(v);
        }
        eleves.sort(Comparator.comparing(v -> String.valueOf(v.get("nomComplet")), String.CASE_INSENSITIVE_ORDER));

        // taux de réussite par question (sur les tentatives retenues)
        List<Map<String, Object>> parQuestion = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> qu = questions.get(i);
            List<String> options = ReglesQuiz.options(qu);
            int bonne = ReglesQuiz.bonneReponse(qu);
            int[] repartition = new int[options.size()];
            int reponses = 0, justes = 0;
            for (TentativeQuizJpaEntity t : retenues.values()) {
                List<Integer> r = ReglesQuiz.lireReponses(t.getReponsesJson());
                if (i >= r.size() || r.get(i) == null) continue;
                reponses++;
                int choix = r.get(i);
                if (choix >= 0 && choix < repartition.length) repartition[choix]++;
                if (choix == bonne) justes++;
            }
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("questionId", qu.get("id") == null ? "q-" + (i + 1) : String.valueOf(qu.get("id")));
            v.put("questionText", qu.get("questionText"));
            v.put("options", options);
            v.put("bonneReponse", bonne);
            v.put("nombreReponses", reponses);
            v.put("nombreBonnesReponses", justes);
            v.put("tauxReussite", reponses == 0 ? null : pourcentage(justes, reponses));
            v.put("repartition", Arrays.stream(repartition).boxed().toList());
            parQuestion.add(v);
        }

        long faits = eleves.stream().filter(v -> "FAIT".equals(v.get("statut"))).count();
        OptionalDouble moyenne = retenues.values().stream().mapToInt(TentativeQuizJpaEntity::getScore).average();
        OptionalDouble moyennePct = retenues.values().stream().mapToDouble(t -> pourcentage(t.getScore(), t.getTotal())).average();
        Map<String, Object> synthese = new LinkedHashMap<>();
        synthese.put("effectifClasse", eleves.stream().filter(v -> !Boolean.TRUE.equals(v.get("horsClasse"))).count());
        synthese.put("nombreFaits", faits);
        synthese.put("nombreNonFaits", eleves.size() - faits);
        synthese.put("moyenneScore", moyenne.isPresent() ? Math.round(moyenne.getAsDouble() * 100) / 100.0 : null);
        synthese.put("moyennePourcentage", moyennePct.isPresent() ? Math.round(moyennePct.getAsDouble() * 10) / 10.0 : null);
        synthese.put("nombreQuestions", questions.size());

        Map<String, Object> qv = new LinkedHashMap<>();
        qv.put("id", q.getId());
        qv.put("title", q.getTitle());
        qv.put("moduleId", q.getModuleId());
        qv.put("courseId", q.getCourseId());
        qv.put("maxTentatives", ReglesQuiz.maxTentatives(q.getMaxTentatives()));
        qv.put("nombreQuestions", questions.size());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("quiz", qv);
        res.put("synthese", synthese);
        res.put("eleves", eleves);
        res.put("questions", parQuestion);
        return res;
    }

    // ================================================================ outils

    static Map<String, Object> vueEleve(QuizJpaEntity q, String titreModule, List<TentativeQuizJpaEntity> miennes) {
        List<Map<String, Object>> questions = ReglesQuiz.questions(q.getQuestionsJson());
        int max = ReglesQuiz.maxTentatives(q.getMaxTentatives());
        int restantes = Math.max(0, max - miennes.size());
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", q.getId());
        v.put("courseId", q.getCourseId());
        v.put("moduleId", q.getModuleId());
        v.put("moduleTitle", titreModule);
        v.put("title", q.getTitle());
        v.put("description", q.getDescription() == null ? "" : q.getDescription());
        v.put("duration", q.getDuration() > 0 ? q.getDuration() : null);
        v.put("nombreQuestions", questions.size());
        v.put("questions", ReglesQuiz.sansReponses(questions));
        v.put("maxTentatives", max);
        v.put("tentativesUtilisees", miennes.size());
        v.put("tentativesRestantes", restantes);
        v.put("dejaFait", !miennes.isEmpty());
        v.put("peutPasser", restantes > 0 && !questions.isEmpty());
        v.put("dernierResultat", miennes.isEmpty() ? null : resume(miennes.get(miennes.size() - 1)));
        return v;
    }

    private static Map<String, Object> resume(TentativeQuizJpaEntity t) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", t.getId());
        v.put("score", t.getScore());
        v.put("total", t.getTotal());
        v.put("pourcentage", pourcentage(t.getScore(), t.getTotal()));
        v.put("date", t.getDateTentative() == null ? null : t.getDateTentative().toString());
        return v;
    }

    private static double pourcentage(int score, int total) {
        return total == 0 ? 0 : Math.round(score * 1000.0 / total) / 10.0;
    }

    private CourseJpaEntity coursDeSaClasse(String courseId, String eleveId, String messageIntrouvable) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        String classe = inscriptions.findFirstByUserId(eleveId).map(InscriptionClasseJpaEntity::getClasseId).orElse(null);
        return cours.findById(courseId)
                .filter(c -> ecole == null || c.getOrganizationId() == null || ecole.equals(c.getOrganizationId()))
                .filter(c -> classe != null && classe.equals(c.getClasse()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, messageIntrouvable));
    }

    private QuizJpaEntity quizDeSaClasse(String quizId, String eleveId) {
        String message = "Quiz introuvable parmi les cours de votre classe.";
        QuizJpaEntity q = quiz.findById(quizId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
        if (q.getCourseId() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        coursDeSaClasse(q.getCourseId(), eleveId, message);
        return q;
    }
}
