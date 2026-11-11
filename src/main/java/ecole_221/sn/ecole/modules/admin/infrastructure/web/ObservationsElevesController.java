package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ObservationsElevesService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Journal d'observations d'un élève (accès administration : règle /admin/** de SecurityConfig). */
@RestController
@Tag(name = "Admin — observations élèves", description = "Suivi individuel des élèves")
public class ObservationsElevesController {

    private final ObservationsElevesService service;

    public ObservationsElevesController(ObservationsElevesService service) {
        this.service = service;
    }

    @GetMapping("/admin/students/{id}/observations")
    @Operation(summary = "Observations d'un élève", description = "Les plus récentes d'abord ; 404 si l'élève n'est pas de l'école")
    public ResponseEntity<List<Map<String, Object>>> liste(@PathVariable String id) {
        return ResponseEntity.ok(service.liste(id));
    }

    @PostMapping("/admin/students/{id}/observations")
    @Operation(summary = "Ajouter une observation", description = "Corps {type, text} ; type parmi Comportement, Notes, Retards/Absences, Frais Scolaires, Général")
    public ResponseEntity<Map<String, Object>> ajouter(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        Object type = corps == null ? null : corps.get("type");
        Object text = corps == null ? null : corps.get("text");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(id, auteur(),
                type == null ? null : String.valueOf(type), text == null ? null : String.valueOf(text)));
    }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return u.getId();
    }
}
