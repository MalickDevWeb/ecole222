package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Verrouillage des évaluations par l'administration de l'école. /admin/** est réservé aux rôles
 * ADMIN / ADMINISTRATOR (SecurityConfig) : un professeur reçoit 403. Une évaluation verrouillée refuse
 * toute modification de ses notes (409).
 */
@RestController
@Tag(name = "Admin — évaluations", description = "Verrouillage / déverrouillage des évaluations")
public class EvaluationVerrouillageController {

    private final EvaluationService service;

    public EvaluationVerrouillageController(EvaluationService service) {
        this.service = service;
    }

    @PostMapping("/admin/courses/{courseId}/evaluations/{evaluationId}/verrouiller")
    @Operation(summary = "Verrouiller une évaluation", description = "Plus aucune note ne peut être saisie, modifiée ou effacée.")
    public ResponseEntity<Map<String, Object>> verrouiller(@PathVariable("courseId") String courseId,
                                                           @PathVariable("evaluationId") String evaluationId) {
        return ResponseEntity.ok(service.verrouiller(courseId, evaluationId, true));
    }

    @PostMapping("/admin/courses/{courseId}/evaluations/{evaluationId}/deverrouiller")
    @Operation(summary = "Déverrouiller une évaluation", description = "Le professeur peut à nouveau modifier les notes.")
    public ResponseEntity<Map<String, Object>> deverrouiller(@PathVariable("courseId") String courseId,
                                                             @PathVariable("evaluationId") String evaluationId) {
        return ResponseEntity.ok(service.verrouiller(courseId, evaluationId, false));
    }
}
