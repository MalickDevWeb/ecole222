package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.parent.application.service.JustificatifAbsenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Le parent justifie l'absence ou le retard de son enfant ; l'administration valide (JustificatifsAbsenceAdminController). */
@RestController
@Tag(name = "Parent — justificatifs d'absence", description = "Dépôt et suivi des justificatifs d'absence")
public class JustificatifsAbsenceParentController {

    private final JustificatifAbsenceService service;

    public JustificatifsAbsenceParentController(JustificatifAbsenceService service) {
        this.service = service;
    }

    @PostMapping("/parent/presences/{presenceId}/justifier")
    @Operation(summary = "Justifier une absence",
            description = "Corps JSON {motif, fichierUrl?} (paramètres motif / fileUrl acceptés pour compatibilité). "
                    + "La présence doit concerner l'un de ses enfants et être une absence ou un retard. "
                    + "Le justificatif reste EN_ATTENTE jusqu'à la décision de l'administration.")
    public ResponseEntity<Map<String, Object>> justifier(@PathVariable String presenceId,
                                                         @RequestBody(required = false) Map<String, Object> corps,
                                                         @RequestParam(value = "motif", required = false) String motif,
                                                         @RequestParam(value = "fileUrl", required = false) String fileUrl) {
        String m = corps != null && corps.get("motif") != null ? String.valueOf(corps.get("motif")) : motif;
        String f = corps != null && corps.get("fichierUrl") != null ? String.valueOf(corps.get("fichierUrl")) : fileUrl;
        return ResponseEntity.status(HttpStatus.CREATED).body(service.deposer(parent(), presenceId, m, f));
    }

    @GetMapping("/parent/justificatifs")
    @Operation(summary = "Mes justificatifs", description = "Justificatifs déposés par le parent connecté, les plus récents d'abord, avec la décision de l'administration.")
    public ResponseEntity<List<Map<String, Object>>> mesJustificatifs() {
        return ResponseEntity.ok(service.duParent(parent()));
    }

    private static String parent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return u.getId();
    }
}
