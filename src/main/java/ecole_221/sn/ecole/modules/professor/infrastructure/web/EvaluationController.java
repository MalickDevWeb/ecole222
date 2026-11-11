package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Évaluations multiples d'un cours, saisie des notes en lot, moyennes et journal (espace professeur).
 * AccesCoursInterceptor vérifie {courseId} (cours de l'école, attribué au professeur sauf administration) ;
 * l'appartenance de {evaluationId} au cours est vérifiée ici.
 */
@RestController
@Tag(name = "Professor — évaluations", description = "Évaluations, coefficients, notes en lot, moyennes, journal")
public class EvaluationController {

    private final EvaluationService service;

    @Autowired(required = false)
    private AccesCoursInterceptor accesCours;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    /** L'évaluation appartient au cours de l'URL et ce cours est autorisé pour l'utilisateur connecté. */
    private void verifier(String courseId, String evaluationId) {
        var e = service.evaluation(courseId, evaluationId);
        if (accesCours != null) accesCours.verifierCours(e.getCourseId());
    }

    @GetMapping("/professor/courses/{courseId}/evaluations")
    @Operation(summary = "Évaluations d'un cours", description = "Liste des évaluations (date croissante) avec le nombre de notes saisies.")
    public ResponseEntity<List<Map<String, Object>>> lister(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(service.evaluations(courseId));
    }

    @PostMapping("/professor/courses/{courseId}/evaluations")
    @Operation(summary = "Créer une évaluation",
            description = "{titre, type (DEVOIR_SURVEILLE|EXAMEN|TP|ORAL|AUTRE), date AAAA-MM-JJ, coefficient ]0;10], noteSur (20 par défaut), "
                    + "session (NORMALE|RATTRAPAGE), evaluationRattrapeeId (obligatoire pour un rattrapage)}")
    public ResponseEntity<Map<String, Object>> creer(@PathVariable("courseId") String courseId,
                                                     @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(courseId, corps));
    }

    @PutMapping("/professor/courses/{courseId}/evaluations/{evaluationId}")
    @Operation(summary = "Modifier une évaluation", description = "Champs envoyés seulement. 409 si verrouillée.")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable("courseId") String courseId,
                                                        @PathVariable("evaluationId") String evaluationId,
                                                        @RequestBody(required = false) Map<String, Object> corps) {
        verifier(courseId, evaluationId);
        return ResponseEntity.ok(service.modifier(courseId, evaluationId, corps));
    }

    @DeleteMapping("/professor/courses/{courseId}/evaluations/{evaluationId}")
    @Operation(summary = "Supprimer une évaluation", description = "409 si verrouillée, si des notes sont saisies ou si un rattrapage la vise.")
    public ResponseEntity<Void> supprimer(@PathVariable("courseId") String courseId,
                                          @PathVariable("evaluationId") String evaluationId) {
        verifier(courseId, evaluationId);
        service.supprimer(courseId, evaluationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/professor/courses/{courseId}/evaluations/{evaluationId}/notes")
    @Operation(summary = "Notes d'une évaluation", description = "{evaluation, notes:[{studentId, matricule, nom, prenom, valeur|null, absence|null, commentaire}]} pour TOUS les élèves de la classe.")
    public ResponseEntity<Map<String, Object>> notes(@PathVariable("courseId") String courseId,
                                                     @PathVariable("evaluationId") String evaluationId) {
        verifier(courseId, evaluationId);
        return ResponseEntity.ok(service.notes(courseId, evaluationId));
    }

    @PutMapping("/professor/courses/{courseId}/evaluations/{evaluationId}/notes")
    @Operation(summary = "Enregistrer des notes en lot",
            description = "[{studentId, valeur|null, absence (ABS|EXCUSE)?, commentaire?}] — transaction : tout est refusé (400) si une ligne est invalide ; "
                    + "valeur et absence vides = note effacée ; 409 si l'évaluation est verrouillée.")
    public ResponseEntity<Map<String, Object>> enregistrer(@PathVariable("courseId") String courseId,
                                                           @PathVariable("evaluationId") String evaluationId,
                                                           @RequestBody(required = false) Object corps) {
        verifier(courseId, evaluationId);
        return ResponseEntity.ok(service.enregistrerNotes(courseId, evaluationId, corps));
    }

    @GetMapping("/professor/courses/{courseId}/moyennes")
    @Operation(summary = "Moyennes du cours", description = "Par élève : moyenne pondérée sur 20 (null si aucune note), mention, rang ; règle de calcul incluse.")
    public ResponseEntity<Map<String, Object>> moyennes(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(service.moyennes(courseId));
    }

    @GetMapping("/professor/courses/{courseId}/evaluations/{evaluationId}/journal")
    @Operation(summary = "Journal des notes d'une évaluation", description = "Créations, modifications, suppressions de notes et verrouillages (plus récent d'abord).")
    public ResponseEntity<List<Map<String, Object>>> journal(@PathVariable("courseId") String courseId,
                                                             @PathVariable("evaluationId") String evaluationId) {
        verifier(courseId, evaluationId);
        return ResponseEntity.ok(service.journal(courseId, evaluationId));
    }
}
