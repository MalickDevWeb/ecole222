package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.service.QuizTentativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Lot D — quiz passés par l'élève connecté. Un élève n'accède qu'aux quiz des cours de SA classe (404 sinon).
 * Les bonnes réponses ne sont jamais envoyées avant la tentative.
 */
@RestController
@RequestMapping("/student")
@Tag(name = "Student", description = "Espace élève — quiz")
public class QuizEleveController {

    private final QuizTentativeService service;

    public QuizEleveController(QuizTentativeService service) {
        this.service = service;
    }

    @GetMapping("/courses/{courseId}/quizzes")
    @Operation(summary = "Quiz d'un cours de ma classe", description = "Questions et choix SANS les bonnes réponses, avec l'état de mes tentatives.")
    public ResponseEntity<List<Map<String, Object>>> quizDuCours(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(service.quizDuCoursPourEleve(courseId, eleve()));
    }

    @PostMapping("/quizzes/{quizId}/tentatives")
    @Operation(summary = "Passer un quiz", description = "Corps : {\"reponses\": [index du choix pour chaque question, dans l'ordre]}. "
            + "Correction par le serveur ; 409 si les tentatives sont épuisées ; 400 si une réponse manque ou ne correspond à aucun choix.")
    public ResponseEntity<Map<String, Object>> passer(@PathVariable("quizId") String quizId,
                                                      @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.passer(quizId, eleve(), corps));
    }

    @GetMapping("/quizzes/{quizId}/tentatives/moi")
    @Operation(summary = "Mon résultat à un quiz", description = "Mes tentatives et, une fois mes tentatives épuisées, la correction. 404 si je ne l'ai pas encore passé.")
    public ResponseEntity<Map<String, Object>> monResultat(@PathVariable("quizId") String quizId) {
        return ResponseEntity.ok(service.mesResultats(quizId, eleve()));
    }

    private static String eleve() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User u) return u.getId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
    }
}
