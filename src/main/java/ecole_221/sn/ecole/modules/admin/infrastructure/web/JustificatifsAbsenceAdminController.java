package ecole_221.sn.ecole.modules.admin.infrastructure.web;

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

/** L'administration valide ou refuse les justificatifs d'absence déposés par les parents. */
@RestController
@Tag(name = "Admin — justificatifs d'absence", description = "Validation des justificatifs d'absence")
public class JustificatifsAbsenceAdminController {

    private final JustificatifAbsenceService service;

    public JustificatifsAbsenceAdminController(JustificatifAbsenceService service) {
        this.service = service;
    }

    @GetMapping("/admin/justificatifs-absence")
    @Operation(summary = "Justificatifs de l'école", description = "Filtre facultatif ?statut=EN_ATTENTE|ACCEPTE|REFUSE ; les plus récents d'abord.")
    public ResponseEntity<List<Map<String, Object>>> liste(@RequestParam(value = "statut", required = false) String statut) {
        return ResponseEntity.ok(service.liste(statut));
    }

    @PostMapping("/admin/justificatifs-absence/{id}/accepter")
    @Operation(summary = "Accepter un justificatif", description = "L'absence devient « excusée » dans l'appel du professeur.")
    public ResponseEntity<Map<String, Object>> accepter(@PathVariable String id) {
        return ResponseEntity.ok(service.accepter(admin(), id));
    }

    @PostMapping("/admin/justificatifs-absence/{id}/refuser")
    @Operation(summary = "Refuser un justificatif", description = "Corps {motif} obligatoire : il est communiqué au parent.")
    public ResponseEntity<Map<String, Object>> refuser(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        Object motif = corps == null ? null : corps.get("motif");
        return ResponseEntity.ok(service.refuser(admin(), id, motif == null ? null : String.valueOf(motif)));
    }

    private static String admin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return u.getId();
    }
}
