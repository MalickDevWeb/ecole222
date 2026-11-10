package ecole_221.sn.ecole.modules.professor.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.LessonJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.LessonJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.QuizJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.QuizJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Contenus pédagogiques d'un cours : modules, supports de cours (« leçons ») et quiz QCM.
 *
 * <p>Test du parcours professeur du 2026-11-10 : l'écran envoyait {title, description} pour un module,
 * {title, description, pièce jointe, module} pour un support et {title, description, questions} pour un quiz,
 * alors que le serveur exigeait {name}, {title, duration, date} et {title, duration} → chaque création était
 * REFUSÉE (400) ; et la salle de classe plantait car les listes revenaient en page au lieu d'un tableau.
 * Ce service enregistre réellement ce que l'écran saisit, avec des validations lisibles.</p>
 *
 * <p>Le contrôle « cours du professeur / école » est fait en amont par AccesCoursInterceptor.</p>
 */
@Service
public class ContenuPedagogiqueService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ModuleJpaRepository modules;
    private final LessonJpaRepository supports;
    private final QuizJpaRepository quiz;

    public ContenuPedagogiqueService(ModuleJpaRepository modules, LessonJpaRepository supports, QuizJpaRepository quiz) {
        this.modules = modules;
        this.supports = supports;
        this.quiz = quiz;
    }

    // ---------------------------------------------------------------- modules

    @Transactional(readOnly = true)
    public List<Map<String, Object>> modules(String courseId) {
        return modules.findByCourseIdOrderByNameAsc(courseId).stream().map(ContenuPedagogiqueService::vue).toList();
    }

    @Transactional
    public Map<String, Object> creerModule(String courseId, Map<String, Object> corps) {
        String titre = obligatoire(premier(corps, "title", "name", "titre"), "Le titre du module est obligatoire.", 150);
        ModuleJpaEntity m = new ModuleJpaEntity();
        m.setId("module-" + UUID.randomUUID());
        m.setCourseId(courseId);
        m.setName(titre);
        m.setDescription(facultatif(premier(corps, "description"), 2000));
        return vue(modules.save(m));
    }

    // ---------------------------------------------------------------- supports de cours

    @Transactional(readOnly = true)
    public List<Map<String, Object>> supports(String courseId) {
        return supports.findByCourseId(courseId).stream().map(ContenuPedagogiqueService::vue).toList();
    }

    @Transactional
    public Map<String, Object> creerSupport(String courseId, Map<String, Object> corps) {
        String titre = obligatoire(premier(corps, "title", "titre"), "Le titre du support est obligatoire.", 200);
        String moduleId = facultatif(premier(corps, "moduleId"), 100);
        if (moduleId != null) moduleDuCours(moduleId, courseId);
        String url = facultatif(premier(corps, "attachmentUrl"), 2000);
        if (url != null && !url.matches("(?i)^https?://\\S+$")) {
            throw erreur("Le lien de la pièce jointe doit commencer par http:// ou https://.");
        }
        String date = facultatif(premier(corps, "date", "dateStr"), 10);
        if (date != null) {
            try {
                LocalDate.parse(date);
            } catch (RuntimeException e) {
                throw erreur("Date invalide (format attendu AAAA-MM-JJ) : " + date);
            }
        }
        Object duree = corps == null ? null : corps.get("duration");
        int minutes = 0;
        if (duree != null && !String.valueOf(duree).isBlank()) {
            try {
                minutes = Integer.parseInt(String.valueOf(duree).trim());
            } catch (NumberFormatException e) {
                throw erreur("La durée doit être un nombre de minutes.");
            }
            if (minutes <= 0) throw erreur("La durée doit être positive.");
        }
        LessonJpaEntity l = new LessonJpaEntity();
        l.setId("lesson-" + UUID.randomUUID());
        l.setCourseId(courseId);
        l.setTitle(titre);
        l.setDescription(facultatif(premier(corps, "description"), 4000));
        l.setModuleId(moduleId);
        l.setAttachmentName(facultatif(premier(corps, "attachmentName"), 255));
        l.setAttachmentUrl(url);
        l.setDuration(minutes);
        l.setDate(date != null ? date : LocalDate.now().toString());   // date réelle de publication
        return vue(supports.save(l));
    }

    // ---------------------------------------------------------------- quiz

    @Transactional(readOnly = true)
    public List<Map<String, Object>> quizDuCours(String courseId) {
        return quiz.findByCourseId(courseId).stream().map(ContenuPedagogiqueService::vue).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> quizDuModule(String moduleId) {
        return quiz.findByModuleId(moduleId).stream().map(ContenuPedagogiqueService::vue).toList();
    }

    @Transactional
    public Map<String, Object> creerQuiz(String moduleId, Map<String, Object> corps) {
        ModuleJpaEntity module = modules.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module introuvable."));
        String titre = obligatoire(premier(corps, "title", "titre"), "Le titre du quiz est obligatoire.", 200);
        List<Map<String, Object>> questions = questionsValides(corps == null ? null : corps.get("questions"));
        Object duree = corps.get("duration");
        int minutes = 0;
        if (duree != null) {
            try {
                minutes = Integer.parseInt(String.valueOf(duree).trim());
            } catch (NumberFormatException e) {
                throw erreur("La durée du quiz doit être un nombre de minutes.");
            }
            if (minutes <= 0) throw erreur("La durée du quiz doit être positive.");
        }
        QuizJpaEntity q = new QuizJpaEntity();
        q.setId("quiz-" + UUID.randomUUID());
        q.setCourseId(module.getCourseId());   // avant : l'identifiant du MODULE était rangé ici
        q.setModuleId(moduleId);
        q.setTitle(titre);
        q.setDescription(facultatif(premier(corps, "description"), 2000));
        q.setDuration(minutes);
        q.setMaxTentatives(ReglesQuiz.maxTentativesSaisi(corps.get("maxTentatives")));   // lot D (null = 1)
        try {
            q.setQuestionsJson(JSON.writeValueAsString(questions));
        } catch (Exception e) {
            throw erreur("Questions illisibles.");
        }
        return vue(quiz.save(q));
    }

    /** Quiz créé depuis un cours : le module doit appartenir à CE cours (vérifié AVANT tout enregistrement). */
    @Transactional
    public Map<String, Object> creerQuizDansCours(String courseId, String moduleId, Map<String, Object> corps) {
        moduleDuCours(moduleId, courseId);
        return creerQuiz(moduleId, corps);
    }

    /** Chaque question : un énoncé, au moins 2 choix non vides, une bonne réponse parmi les choix. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> questionsValides(Object brut) {
        if (!(brut instanceof List<?> liste) || liste.isEmpty()) {
            throw erreur("Le quiz doit contenir au moins une question.");
        }
        List<Map<String, Object>> res = new ArrayList<>();
        int n = 0;
        for (Object o : liste) {
            n++;
            if (!(o instanceof Map<?, ?> q)) throw erreur("Question " + n + " illisible.");
            Object texte = ((Map<String, Object>) q).getOrDefault("questionText", ((Map<String, Object>) q).get("question"));
            if (texte == null || texte.toString().isBlank()) throw erreur("La question " + n + " n'a pas d'énoncé.");
            if (!(q.get("options") instanceof List<?> options) || options.size() < 2
                    || options.stream().anyMatch(x -> x == null || x.toString().isBlank())) {
                throw erreur("La question " + n + " doit proposer au moins deux choix non vides.");
            }
            Object bonne = q.get("correctAnswerIndex");
            int index;
            try {
                index = Integer.parseInt(String.valueOf(bonne));
            } catch (NumberFormatException e) {
                throw erreur("La question " + n + " n'indique pas la bonne réponse.");
            }
            if (index < 0 || index >= options.size()) {
                throw erreur("La bonne réponse de la question " + n + " ne correspond à aucun choix.");
            }
            Map<String, Object> propre = new LinkedHashMap<>();
            propre.put("id", q.get("id") == null ? "q-" + n : String.valueOf(q.get("id")));
            propre.put("questionText", texte.toString().trim());
            propre.put("options", options.stream().map(x -> x.toString().trim()).toList());
            propre.put("correctAnswerIndex", index);
            res.add(propre);
        }
        return res;
    }

    // ---------------------------------------------------------------- vues (forme attendue par l'écran)

    static Map<String, Object> vue(ModuleJpaEntity m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.getId());
        v.put("courseId", m.getCourseId());
        v.put("title", m.getName());
        v.put("description", m.getDescription() == null ? "" : m.getDescription());
        return v;
    }

    static Map<String, Object> vue(LessonJpaEntity l) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", l.getId());
        v.put("courseId", l.getCourseId());
        v.put("moduleId", l.getModuleId());
        v.put("title", l.getTitle());
        v.put("description", l.getDescription() == null ? "" : l.getDescription());
        v.put("dateStr", l.getDate());
        v.put("attachmentName", l.getAttachmentName());
        v.put("attachmentUrl", l.getAttachmentUrl());
        return v;
    }

    static Map<String, Object> vue(QuizJpaEntity q) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", q.getId());
        v.put("moduleId", q.getModuleId());
        v.put("courseId", q.getCourseId());
        v.put("title", q.getTitle());
        v.put("description", q.getDescription() == null ? "" : q.getDescription());
        v.put("duration", q.getDuration() > 0 ? q.getDuration() : null);
        List<Object> questions = List.of();
        if (q.getQuestionsJson() != null) {
            try {
                questions = JSON.readValue(q.getQuestionsJson(), new TypeReference<List<Object>>() { });
            } catch (Exception ignore) {
                questions = List.of();
            }
        }
        v.put("questions", questions);
        v.put("maxTentatives", ReglesQuiz.maxTentatives(q.getMaxTentatives()));   // lot D
        return v;
    }

    // ---------------------------------------------------------------- outils

    private void moduleDuCours(String moduleId, String courseId) {
        ModuleJpaEntity m = modules.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module introuvable."));
        if (!courseId.equals(m.getCourseId())) throw erreur("Ce module n'appartient pas à ce cours.");
    }

    private static String premier(Map<String, Object> corps, String... cles) {
        if (corps == null) return null;
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
}
