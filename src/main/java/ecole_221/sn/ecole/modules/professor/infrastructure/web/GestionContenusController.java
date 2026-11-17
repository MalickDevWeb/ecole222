package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.professor.application.service.GestionContenusService;
import ecole_221.sn.ecole.modules.professor.application.service.QuizTentativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Lot D — modifier / supprimer les contenus d'un cours et lire les résultats d'un quiz.
 *
 * <p>Les routes {moduleId} et {courseId} sont contrôlées par AccesCoursInterceptor ; pour {lessonId}, {quizId},
 * {homeworkId} et {reminderId}, le service retrouve l'objet puis applique {@link AccesCoursInterceptor#verifierCours}
 * au cours de l'objet (404 objet inexistant, 403 cours d'un collègue).</p>
 */
@RestController
@Tag(name = "Professor", description = "Espace professeur — modification et suppression des contenus, résultats des quiz")
public class GestionContenusController {

    private final GestionContenusService gestion;
    private final QuizTentativeService quizTentatives;

    @Autowired(required = false)
    private AccesCoursInterceptor accesCours;
    @Autowired(required = false)
    private ActionLogger actionLogger;

    public GestionContenusController(GestionContenusService gestion, QuizTentativeService quizTentatives) {
        this.gestion = gestion;
        this.quizTentatives = quizTentatives;
    }

    private Consumer<String> verif() {
        return courseId -> {
            if (accesCours != null) accesCours.verifierCours(courseId);
        };
    }

    private ResponseEntity<Map<String, Object>> journal(String action, Map<String, Object> res) {
        if (actionLogger != null) {
            String moi = ProfExtraController.utilisateurConnecte();
            Object cible = res.getOrDefault("courseId", res.get("id"));
            actionLogger.logProfessorAction(action, moi != null ? moi : "anonyme", "Professeur",
                    cible == null ? null : cible.toString(), String.valueOf(res.getOrDefault("title", res.getOrDefault("message", ""))));
        }
        return ResponseEntity.ok(res);
    }

    // ---------------------------------------------------------------- modules

    @PutMapping("/professor/modules/{moduleId}")
    @Operation(summary = "Modifier un module", description = "Champs modifiables : title (ou name), description. Un champ absent est conservé.")
    public ResponseEntity<Map<String, Object>> modifierModule(@PathVariable("moduleId") String moduleId,
                                                              @RequestBody(required = false) Map<String, Object> corps) {
        return journal("UPDATE_MODULE", gestion.modifierModule(moduleId, corps, verif()));
    }

    @DeleteMapping("/professor/modules/{moduleId}")
    @Operation(summary = "Supprimer un module", description = "Refusé (409) si le module contient encore des supports ou des quiz.")
    public ResponseEntity<Map<String, Object>> supprimerModule(@PathVariable("moduleId") String moduleId) {
        return journal("DELETE_MODULE", gestion.supprimerModule(moduleId, verif()));
    }

    // ---------------------------------------------------------------- supports

    @PutMapping("/professor/lessons/{lessonId}")
    @Operation(summary = "Modifier un support de cours", description = "title, description, attachmentName, attachmentUrl (http/https), moduleId (null = aucun module).")
    public ResponseEntity<Map<String, Object>> modifierSupport(@PathVariable("lessonId") String lessonId,
                                                               @RequestBody(required = false) Map<String, Object> corps) {
        return journal("UPDATE_LESSON", gestion.modifierSupport(lessonId, corps, verif()));
    }

    @DeleteMapping("/professor/lessons/{lessonId}")
    @Operation(summary = "Supprimer un support de cours")
    public ResponseEntity<Map<String, Object>> supprimerSupport(@PathVariable("lessonId") String lessonId) {
        return journal("DELETE_LESSON", gestion.supprimerSupport(lessonId, verif()));
    }

    // ---------------------------------------------------------------- quiz

    @PutMapping("/professor/quizzes/{quizId}")
    @Operation(summary = "Modifier un quiz", description = "Uniquement si aucun élève ne l'a passé (409 sinon). title, description, duration, maxTentatives, questions, moduleId.")
    public ResponseEntity<Map<String, Object>> modifierQuiz(@PathVariable("quizId") String quizId,
                                                            @RequestBody(required = false) Map<String, Object> corps) {
        return journal("UPDATE_QUIZ", gestion.modifierQuiz(quizId, corps, verif()));
    }

    @DeleteMapping("/professor/quizzes/{quizId}")
    @Operation(summary = "Supprimer un quiz", description = "Refusé (409) si des élèves l'ont passé, sauf avec supprimerResultats=true (les tentatives sont alors supprimées aussi).")
    public ResponseEntity<Map<String, Object>> supprimerQuiz(@PathVariable("quizId") String quizId,
                                                             @RequestParam(value = "supprimerResultats", defaultValue = "false") boolean supprimerResultats) {
        return journal("DELETE_QUIZ", gestion.supprimerQuiz(quizId, supprimerResultats, verif()));
    }

    @GetMapping("/professor/courses/{courseId}/quizzes/{quizId}/resultats")
    @Operation(summary = "Résultats d'un quiz", description = "Chaque élève de la classe : meilleure tentative ou « non fait » ; taux de réussite par question ; moyenne.")
    public ResponseEntity<Map<String, Object>> resultats(@PathVariable("courseId") String courseId,
                                                         @PathVariable("quizId") String quizId) {
        return ResponseEntity.ok(quizTentatives.resultats(courseId, quizId));
    }

    // ---------------------------------------------------------------- devoirs

    @PutMapping("/professor/homeworks/{homeworkId}")
    @Operation(summary = "Modifier un devoir", description = "title (ou titre), description (ou desc / consigne), priority (ou prio), deadline (ou deadlineStr, AAAA-MM-JJ, non passée).")
    public ResponseEntity<Map<String, Object>> modifierDevoir(@PathVariable("homeworkId") String homeworkId,
                                                              @RequestBody(required = false) Map<String, Object> corps) {
        return journal("UPDATE_HOMEWORK", gestion.modifierDevoir(homeworkId, corps, verif()));
    }

    @DeleteMapping("/professor/homeworks/{homeworkId}")
    @Operation(summary = "Supprimer un devoir")
    public ResponseEntity<Map<String, Object>> supprimerDevoir(@PathVariable("homeworkId") String homeworkId) {
        return journal("DELETE_HOMEWORK", gestion.supprimerDevoir(homeworkId, verif()));
    }

    // ---------------------------------------------------------------- rappels

    @PutMapping("/professor/reminders/{reminderId}")
    @Operation(summary = "Modifier un rappel", description = "content, isUrgent (booléen), isCompleted (booléen).")
    public ResponseEntity<Map<String, Object>> modifierRappel(@PathVariable("reminderId") String reminderId,
                                                              @RequestBody(required = false) Map<String, Object> corps) {
        return journal("UPDATE_REMINDER", gestion.modifierRappel(reminderId, corps, verif()));
    }

    @DeleteMapping("/professor/reminders/{reminderId}")
    @Operation(summary = "Supprimer un rappel")
    public ResponseEntity<Map<String, Object>> supprimerRappel(@PathVariable("reminderId") String reminderId) {
        return journal("DELETE_REMINDER", gestion.supprimerRappel(reminderId, verif()));
    }
}
