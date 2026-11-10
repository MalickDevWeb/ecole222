package ecole_221.sn.ecole.modules.professor.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modifier / supprimer les contenus d'un cours (lot D) : modules, supports, quiz, devoirs, rappels.
 *
 * <p>Chaque méthode reçoit {@code verifierCours} : le contrôle « cours du professeur dans cette école »
 * (AccesCoursInterceptor.verifierCours) appliqué au cours de l'objet APRÈS l'avoir trouvé (404 d'abord,
 * puis 403 pour le cours d'un collègue, puis 400 / 409).</p>
 *
 * <p>Règles de suppression (aucune donnée d'élève supprimée en silence) :</p>
 * <ul>
 *   <li>module contenant des supports ou des quiz : REFUS (409) — le professeur supprime ou déplace d'abord
 *       son contenu ; on ne supprime jamais un module « en cascade » ;</li>
 *   <li>quiz déjà passé par des élèves : modification REFUSÉE (409) ; suppression refusée (409) sauf demande
 *       explicite {@code supprimerResultats=true}, qui supprime aussi les tentatives (et le dit dans la réponse) ;</li>
 *   <li>support, devoir, rappel : suppression directe (confirmée à l'écran).</li>
 * </ul>
 * <p>Pour une modification, un champ absent du corps est conservé ; un champ présent est validé.</p>
 */
@Service
public class GestionContenusService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ModuleJpaRepository modules;
    private final LessonJpaRepository supports;
    private final QuizJpaRepository quiz;
    private final TentativeQuizJpaRepository tentatives;
    private final HomeworkJpaRepository devoirs;
    private final ReminderJpaRepository rappels;

    public GestionContenusService(ModuleJpaRepository modules, LessonJpaRepository supports, QuizJpaRepository quiz,
                                  TentativeQuizJpaRepository tentatives, HomeworkJpaRepository devoirs,
                                  ReminderJpaRepository rappels) {
        this.modules = modules;
        this.supports = supports;
        this.quiz = quiz;
        this.tentatives = tentatives;
        this.devoirs = devoirs;
        this.rappels = rappels;
    }

    // ---------------------------------------------------------------- modules

    @Transactional
    public Map<String, Object> modifierModule(String moduleId, Map<String, Object> corps, Consumer<String> verifierCours) {
        ModuleJpaEntity m = module(moduleId, verifierCours);
        corps = corpsObligatoire(corps);
        if (present(corps, "title", "name", "titre")) {
            m.setName(obligatoire(texte(corps, "title", "name", "titre"), "Le titre du module est obligatoire.", 150));
        }
        if (corps.containsKey("description")) m.setDescription(facultatif(texte(corps, "description"), 2000));
        return ContenuPedagogiqueService.vue(modules.save(m));
    }

    @Transactional
    public Map<String, Object> supprimerModule(String moduleId, Consumer<String> verifierCours) {
        ModuleJpaEntity m = module(moduleId, verifierCours);
        long nbSupports = supports.findByCourseId(m.getCourseId()).stream().filter(l -> moduleId.equals(l.getModuleId())).count();
        long nbQuiz = quiz.findByModuleId(moduleId).size();
        if (nbSupports > 0 || nbQuiz > 0) {
            throw conflit("Ce module contient " + nbSupports + " support(s) et " + nbQuiz + " quiz : supprimez-les "
                    + "ou déplacez les supports vers un autre module avant de supprimer le module.");
        }
        modules.delete(m);
        return supprime(moduleId, "Module supprimé.");
    }

    // ---------------------------------------------------------------- supports de cours

    @Transactional
    public Map<String, Object> modifierSupport(String lessonId, Map<String, Object> corps, Consumer<String> verifierCours) {
        LessonJpaEntity l = supports.findById(lessonId)
                .orElseThrow(() -> introuvable("Support de cours introuvable."));
        verifier(l.getCourseId(), verifierCours, "Support de cours introuvable.");
        corps = corpsObligatoire(corps);
        if (present(corps, "title", "titre")) {
            l.setTitle(obligatoire(texte(corps, "title", "titre"), "Le titre du support est obligatoire.", 200));
        }
        if (corps.containsKey("description")) l.setDescription(facultatif(texte(corps, "description"), 4000));
        if (corps.containsKey("attachmentName")) l.setAttachmentName(facultatif(texte(corps, "attachmentName"), 255));
        if (corps.containsKey("attachmentUrl")) {
            String url = facultatif(texte(corps, "attachmentUrl"), 2000);
            if (url != null && !url.matches("(?i)^https?://\\S+$")) {
                throw erreur("Le lien de la pièce jointe doit commencer par http:// ou https://.");
            }
            l.setAttachmentUrl(url);
            if (url == null && !corps.containsKey("attachmentName")) l.setAttachmentName(null);
        }
        if (corps.containsKey("moduleId")) {
            String moduleId = texte(corps, "moduleId");
            if (moduleId != null) {
                ModuleJpaEntity m = modules.findById(moduleId).orElseThrow(() -> introuvable("Module introuvable."));
                if (!l.getCourseId().equals(m.getCourseId())) throw erreur("Ce module n'appartient pas à ce cours.");
            }
            l.setModuleId(moduleId);
        }
        return ContenuPedagogiqueService.vue(supports.save(l));
    }

    @Transactional
    public Map<String, Object> supprimerSupport(String lessonId, Consumer<String> verifierCours) {
        LessonJpaEntity l = supports.findById(lessonId).orElseThrow(() -> introuvable("Support de cours introuvable."));
        verifier(l.getCourseId(), verifierCours, "Support de cours introuvable.");
        supports.delete(l);
        return supprime(lessonId, "Support de cours supprimé.");
    }

    // ---------------------------------------------------------------- quiz

    @Transactional
    public Map<String, Object> modifierQuiz(String quizId, Map<String, Object> corps, Consumer<String> verifierCours) {
        QuizJpaEntity q = quiz.findById(quizId).orElseThrow(() -> introuvable("Quiz introuvable."));
        verifier(q.getCourseId(), verifierCours, "Quiz introuvable.");
        long faites = tentatives.countByQuizId(quizId);
        if (faites > 0) {
            throw conflit("Ce quiz a déjà été passé (" + faites + " tentative(s) d'élèves) : il ne peut plus être modifié, "
                    + "sinon les copies déjà corrigées ne correspondraient plus aux questions. Créez un nouveau quiz si besoin.");
        }
        corps = corpsObligatoire(corps);
        if (present(corps, "title", "titre")) {
            q.setTitle(obligatoire(texte(corps, "title", "titre"), "Le titre du quiz est obligatoire.", 200));
        }
        if (corps.containsKey("description")) q.setDescription(facultatif(texte(corps, "description"), 2000));
        if (corps.containsKey("duration")) {
            Object d = corps.get("duration");
            if (d == null || String.valueOf(d).isBlank()) {
                q.setDuration(0);
            } else {
                int minutes;
                try {
                    minutes = Integer.parseInt(String.valueOf(d).trim());
                } catch (NumberFormatException e) {
                    throw erreur("La durée du quiz doit être un nombre de minutes.");
                }
                if (minutes <= 0) throw erreur("La durée du quiz doit être positive.");
                q.setDuration(minutes);
            }
        }
        if (corps.containsKey("maxTentatives")) q.setMaxTentatives(ReglesQuiz.maxTentativesSaisi(corps.get("maxTentatives")));
        if (corps.containsKey("questions")) {
            List<Map<String, Object>> questions = ContenuPedagogiqueService.questionsValides(corps.get("questions"));
            try {
                q.setQuestionsJson(JSON.writeValueAsString(questions));
            } catch (Exception e) {
                throw erreur("Questions illisibles.");
            }
        }
        if (present(corps, "moduleId")) {
            String moduleId = texte(corps, "moduleId");
            if (moduleId == null) throw erreur("Un quiz appartient toujours à un module : indiquez le module.");
            ModuleJpaEntity m = modules.findById(moduleId).orElseThrow(() -> introuvable("Module introuvable."));
            if (!q.getCourseId().equals(m.getCourseId())) throw erreur("Ce module n'appartient pas à ce cours.");
            q.setModuleId(moduleId);
        }
        Map<String, Object> v = ContenuPedagogiqueService.vue(quiz.save(q));
        v.put("maxTentatives", ReglesQuiz.maxTentatives(q.getMaxTentatives()));
        return v;
    }

    @Transactional
    public Map<String, Object> supprimerQuiz(String quizId, boolean supprimerResultats, Consumer<String> verifierCours) {
        QuizJpaEntity q = quiz.findById(quizId).orElseThrow(() -> introuvable("Quiz introuvable."));
        verifier(q.getCourseId(), verifierCours, "Quiz introuvable.");
        List<TentativeQuizJpaEntity> faites = tentatives.findByQuizId(quizId);
        if (!faites.isEmpty() && !supprimerResultats) {
            throw conflit("Ce quiz a déjà été passé (" + faites.size() + " tentative(s) d'élèves). Le supprimer effacerait "
                    + "aussi ces résultats : confirmez explicitement la suppression des résultats.");
        }
        tentatives.deleteAll(faites);
        quiz.delete(q);
        Map<String, Object> res = supprime(quizId, faites.isEmpty() ? "Quiz supprimé."
                : "Quiz supprimé avec ses " + faites.size() + " tentative(s) d'élèves.");
        res.put("tentativesSupprimees", faites.size());
        return res;
    }

    // ---------------------------------------------------------------- devoirs

    @Transactional
    public Map<String, Object> modifierDevoir(String homeworkId, Map<String, Object> corps, Consumer<String> verifierCours) {
        HomeworkJpaEntity h = devoirs.findById(homeworkId).orElseThrow(() -> introuvable("Devoir introuvable."));
        verifier(h.getCourseId(), verifierCours, "Devoir introuvable.");
        corps = corpsObligatoire(corps);
        if (present(corps, "title", "titre")) {
            h.setTitle(obligatoire(texte(corps, "title", "titre"), "Le titre du devoir est obligatoire.", 200));
        }
        if (corps.containsKey("description") || corps.containsKey("desc") || corps.containsKey("consigne")) {
            h.setDescription(facultatif(texte(corps, "description", "desc", "consigne"), 4000));
        }
        if (present(corps, "priority", "prio", "priorite")) {
            String p = texte(corps, "priority", "prio", "priorite");
            if (p == null) throw erreur("La priorité du devoir est obligatoire.");
            try {
                h.setPriority(Priority.valueOf(p).name());
            } catch (IllegalArgumentException e) {
                throw erreur("Priorité inconnue : " + p + " (attendu : LOW, MEDIUM, HIGH ou URGENT).");
            }
        }
        if (present(corps, "deadline", "deadlineStr", "echeance")) {
            String s = texte(corps, "deadline", "deadlineStr", "echeance");
            if (s == null) throw erreur("La date limite du devoir est obligatoire.");
            LocalDate jour;
            try {
                jour = LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
            } catch (RuntimeException e) {
                throw erreur("Date limite invalide (format attendu AAAA-MM-JJ) : " + s);
            }
            if (jour.isBefore(LocalDate.now())) throw erreur("La date limite du devoir est déjà passée.");
            h.setDeadline(s);
        }
        return vue(devoirs.save(h));
    }

    @Transactional
    public Map<String, Object> supprimerDevoir(String homeworkId, Consumer<String> verifierCours) {
        HomeworkJpaEntity h = devoirs.findById(homeworkId).orElseThrow(() -> introuvable("Devoir introuvable."));
        verifier(h.getCourseId(), verifierCours, "Devoir introuvable.");
        devoirs.delete(h);
        return supprime(homeworkId, "Devoir supprimé.");
    }

    // ---------------------------------------------------------------- rappels

    @Transactional
    public Map<String, Object> modifierRappel(String reminderId, Map<String, Object> corps, Consumer<String> verifierCours) {
        ReminderJpaEntity r = rappels.findById(reminderId).orElseThrow(() -> introuvable("Rappel introuvable."));
        verifier(r.getCourseId(), verifierCours, "Rappel introuvable.");
        corps = corpsObligatoire(corps);
        if (corps.containsKey("content")) {
            r.setContent(obligatoire(texte(corps, "content"), "Le texte du rappel est obligatoire.", 1000));
        }
        if (corps.containsKey("isUrgent")) {
            if (!(corps.get("isUrgent") instanceof Boolean b)) throw erreur("« isUrgent » doit valoir true ou false.");
            r.setIsUrgent(b);
        }
        if (corps.containsKey("isCompleted")) {
            if (!(corps.get("isCompleted") instanceof Boolean b)) throw erreur("« isCompleted » doit valoir true ou false.");
            r.setIsCompleted(b);
        }
        r.setUpdatedAt(LocalDateTime.now());
        return vue(rappels.save(r));
    }

    @Transactional
    public Map<String, Object> supprimerRappel(String reminderId, Consumer<String> verifierCours) {
        ReminderJpaEntity r = rappels.findById(reminderId).orElseThrow(() -> introuvable("Rappel introuvable."));
        verifier(r.getCourseId(), verifierCours, "Rappel introuvable.");
        rappels.delete(r);
        return supprime(reminderId, "Rappel supprimé.");
    }

    // ---------------------------------------------------------------- vues et outils

    static Map<String, Object> vue(HomeworkJpaEntity h) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", h.getId());
        v.put("courseId", h.getCourseId());
        v.put("title", h.getTitle());
        v.put("description", h.getDescription() == null ? "" : h.getDescription());
        v.put("priority", h.getPriority());
        v.put("deadline", h.getDeadline());
        return v;
    }

    static Map<String, Object> vue(ReminderJpaEntity r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", r.getId());
        v.put("courseId", r.getCourseId());
        v.put("content", r.getContent());
        v.put("isUrgent", Boolean.TRUE.equals(r.getIsUrgent()));
        v.put("isCompleted", Boolean.TRUE.equals(r.getIsCompleted()));
        v.put("dueDate", r.getDueDate() == null ? null : r.getDueDate().toString());
        return v;
    }

    private ModuleJpaEntity module(String moduleId, Consumer<String> verifierCours) {
        ModuleJpaEntity m = modules.findById(moduleId).orElseThrow(() -> introuvable("Module introuvable."));
        verifier(m.getCourseId(), verifierCours, "Module introuvable.");
        return m;
    }

    private static void verifier(String courseId, Consumer<String> verifierCours, String messageIntrouvable) {
        if (courseId == null || courseId.isBlank()) throw introuvable(messageIntrouvable);
        if (verifierCours != null) verifierCours.accept(courseId);
    }

    private static Map<String, Object> supprime(String id, String message) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", id);
        v.put("supprime", true);
        v.put("message", message);
        return v;
    }

    private static Map<String, Object> corpsObligatoire(Map<String, Object> corps) {
        if (corps == null || corps.isEmpty()) throw erreur("Aucune modification envoyée.");
        return corps;
    }

    private static boolean present(Map<String, Object> corps, String... cles) {
        for (String c : cles) if (corps.containsKey(c)) return true;
        return false;
    }

    /** Première valeur non vide parmi les clés (texte rogné), sinon null. */
    private static String texte(Map<String, Object> corps, String... cles) {
        for (String c : cles) {
            Object v = corps.get(c);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }

    private static String obligatoire(String v, String message, int max) {
        if (v == null) throw erreur(message);
        if (v.length() > max) throw erreur("Texte trop long (" + max + " caractères au plus).");
        return v;
    }

    private static String facultatif(String v, int max) {
        if (v != null && v.length() > max) throw erreur("Texte trop long (" + max + " caractères au plus).");
        return v;
    }

    private static ResponseStatusException erreur(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflit(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException introuvable(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
