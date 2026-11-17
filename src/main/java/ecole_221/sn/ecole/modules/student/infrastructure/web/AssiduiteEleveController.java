package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.student.application.service.AssiduiteEleveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** « Mes absences » — l'élève relit l'appel de ses professeurs (lot A). */
@RestController
@Tag(name = "Student — assiduité", description = "Taux d'assiduité et séances manquées de l'élève connecté")
public class AssiduiteEleveController {

    private final AssiduiteEleveService service;

    public AssiduiteEleveController(AssiduiteEleveService service) {
        this.service = service;
    }

    @GetMapping("/student/assiduite")
    @Operation(summary = "Mon assiduité",
            description = "Taux global et par cours (même règle que la feuille d'appel du professeur) et liste des séances "
                    + "où l'élève était absent, en retard ou excusé, avec l'observation du professeur.")
    public ResponseEntity<Map<String, Object>> monAssiduite() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return ResponseEntity.ok(service.assiduite(u.getId()));
    }
}
