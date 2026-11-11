package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.student.application.service.PerformanceEleveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Courbe de performance de l'élève connecté (APP-BE-13) ; règle d'accès /student/** de SecurityConfig. */
@RestController
@Tag(name = "Étudiant — performance", description = "Moyenne, moyenne de la classe et assiduité, mois par mois")
public class PerformanceEleveController {

    private final PerformanceEleveService service;

    public PerformanceEleveController(PerformanceEleveService service) {
        this.service = service;
    }

    @GetMapping("/student/performance")
    @Operation(summary = "Ma performance mois par mois",
            description = "[{mois AAAA-MM, note /20|null, moyenneClasse|null, presence %|null, seances, evaluations}] — "
                    + "uniquement l'élève connecté ; mois sans donnée = null, jamais de valeur inventée.")
    public ResponseEntity<List<Map<String, Object>>> performance() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return ResponseEntity.ok(service.performance(u.getId()));
    }
}
