package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.service.FeuilleAppelService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feuille d'appel par séance d'un cours. Le contrôle « cours de l'école attribué au professeur connecté »
 * est fait en amont par AccesCoursInterceptor ({courseId} dans le chemin) ; il est rejoué ici pour obtenir
 * le cours. L'ancien chemin POST /prof/attendance/submit reste disponible (ProfExtraController).
 */
@RestController
@Tag(name = "Professor", description = "Espace professeur — feuille d'appel par séance")
public class FeuilleAppelController {

    private final FeuilleAppelService feuilles;
    private final AccesCoursInterceptor accesCours;

    public FeuilleAppelController(FeuilleAppelService feuilles, AccesCoursInterceptor accesCours) {
        this.feuilles = feuilles;
        this.accesCours = accesCours;
    }

    @GetMapping("/professor/courses/{courseId}/sessions")
    @Operation(summary = "Séances du cours", description = "Séances planifiées du cours (jour, horaire, salle, date si connue) et dates d'appel déjà enregistrées.")
    public ResponseEntity<List<Map<String, Object>>> seances(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(feuilles.seancesDuCours(cours(courseId)));
    }

    @GetMapping("/professor/courses/{courseId}/attendance")
    @Operation(summary = "Feuille d'appel", description = "Élèves de la classe du cours et statut enregistré pour la séance et la date (null = non noté).")
    public ResponseEntity<Map<String, Object>> feuille(@PathVariable("courseId") String courseId,
                                                       @RequestParam(value = "sessionId", required = false) String sessionId,
                                                       @RequestParam(value = "date", required = false) String date) {
        return ResponseEntity.ok(feuilles.feuille(cours(courseId), sessionId, date));
    }

    @PostMapping("/professor/courses/{courseId}/attendance")
    @Operation(summary = "Enregistrer l'appel", description = "{ sessionId?, date, entries:[{studentId, status: PRESENT|ABSENT|RETARD|EXCUSE, note?}] } — tout ou rien ; remplace la saisie précédente.")
    public ResponseEntity<Map<String, Object>> enregistrer(@PathVariable("courseId") String courseId,
                                                           @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(feuilles.enregistrer(cours(courseId), corps, ProfExtraController.utilisateurConnecte()));
    }

    @GetMapping("/professor/courses/{courseId}/attendance/stats")
    @Operation(summary = "Assiduité du cours", description = "Par élève : présences, retards, absences, excuses et taux d'assiduité (null si aucune séance notée).")
    public ResponseEntity<Map<String, Object>> statistiques(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(feuilles.statistiques(cours(courseId)));
    }

    private CourseJpaEntity cours(String courseId) {
        return accesCours.verifierCours(courseId);
    }
}
