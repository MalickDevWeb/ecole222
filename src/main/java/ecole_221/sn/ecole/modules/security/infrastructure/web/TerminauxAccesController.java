package ecole_221.sn.ecole.modules.security.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.security.application.service.TerminauxAccesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Terminaux d'accès de l'école : réservé à l'administration (APP-BE-21). */
@RestController
@RequestMapping("/admin/terminaux")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRATOR','SUPER_ADMIN','DIRECTEUR','SECRETAIRE','COMPTABLE','SURVEILLANT','RH')") // personnel : filtré par la matrice RBAC
@Tag(name = "Terminaux d'accès", description = "Bornes et lecteurs de portique de l'école")
public class TerminauxAccesController {

    private final TerminauxAccesService service;
    private final ActionLogger journal;

    public TerminauxAccesController(TerminauxAccesService service, ActionLogger journal) {
        this.service = service;
        this.journal = journal;
    }

    @GetMapping
    @Operation(summary = "Terminaux de l'école")
    public List<Map<String, Object>> lister() {
        return service.lister();
    }

    @PostMapping
    @Operation(summary = "Enregistrer un terminal", description = "name obligatoire (100 car.), location facultatif ; actif à la création.")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> t = service.creer(corps);
        journal.logAdminAction("CREATE_TERMINAL", "ADMIN", String.valueOf(t.get("id")), "Terminal créé : " + t.get("name"));
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Activer / désactiver", description = "{\"status\": \"Actif\" | \"Inactif\"} ; 409 si le terminal est révoqué.")
    public Map<String, Object> changerStatut(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> t = service.changerStatut(id, corps);
        journal.logAdminAction("TERMINAL_STATUS", "ADMIN", id, "Terminal " + t.get("name") + " : " + t.get("status"));
        return t;
    }

    @PostMapping("/{id}/revocation")
    @Operation(summary = "Révoquer (définitif)", description = "Le vigile affecté ne peut plus scanner depuis ce terminal.")
    public Map<String, Object> revoquer(@PathVariable String id) {
        Map<String, Object> t = service.revoquer(id);
        journal.logAdminAction("REVOKE_TERMINAL", "ADMIN", id, "Terminal révoqué : " + t.get("name"));
        return t;
    }

    @PutMapping("/{id}/vigile")
    @Operation(summary = "Affecter un vigile", description = "{\"vigilId\": \"…\"} (vide pour retirer) ; le vigile doit appartenir à l'école.")
    public Map<String, Object> affecterVigile(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> t = service.affecterVigile(id, corps);
        journal.logAdminAction("ASSIGN_TERMINAL", "ADMIN", id, "Terminal " + t.get("name") + " → vigile " + t.get("vigilName"));
        return t;
    }
}
