package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lot B — rendus des élèves et correction d'un devoir de classe.
 * {courseId} et {studentId} sont contrôlés par AccesCoursInterceptor (cours du professeur, élève de l'école) ;
 * le service vérifie en plus que le devoir appartient à ce cours et que l'élève est dans la classe.
 */
@RestController
@Tag(name = "Professor — rendus de devoirs", description = "Rendus des élèves, correction (note /20 + commentaire), notifications")
public class RendusDevoirsController {

    private final RendusDevoirsService service;

    public RendusDevoirsController(RendusDevoirsService service) {
        this.service = service;
    }

    @GetMapping("/professor/courses/{courseId}/homeworks/{homeworkId}/submissions")
    @Operation(summary = "Rendus d'un devoir", description = "Pour chaque élève de la classe : rendu (date, fichier, retard) ou non rendu, correction ; statistiques rendus / attendus.")
    public ResponseEntity<Map<String, Object>> rendus(@PathVariable("courseId") String courseId,
                                                      @PathVariable("homeworkId") String homeworkId) {
        return ResponseEntity.ok(service.rendus(courseId, homeworkId));
    }

    @PutMapping("/professor/courses/{courseId}/homeworks/{homeworkId}/submissions/{studentId}/grade")
    @Operation(summary = "Corriger le devoir d'un élève", description = "Corps : {note: 0–20, commentaire: ≤ 2000 caractères}. L'élève est notifié ; un échec de notification n'annule pas la correction.")
    public ResponseEntity<Map<String, Object>> corriger(@PathVariable("courseId") String courseId,
                                                        @PathVariable("homeworkId") String homeworkId,
                                                        @PathVariable("studentId") String studentId,
                                                        @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(service.corriger(courseId, homeworkId, studentId, corps, ProfExtraController.utilisateurConnecte()));
    }
}
