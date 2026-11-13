package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.CoordonneesEcoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Administration", description = "Coordonnées officielles de l'école")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRATOR','SUPER_ADMIN','PLATFORM_ADMIN','DIRECTEUR','SECRETAIRE','COMPTABLE','SURVEILLANT','RH')") // personnel : filtré par la matrice RBAC
public class CoordonneesEcoleController {

    private final CoordonneesEcoleService service;

    public CoordonneesEcoleController(CoordonneesEcoleService service) {
        this.service = service;
    }

    @GetMapping("/admin/coordonnees")
    @Operation(summary = "Coordonnées officielles de l'école", description = "Nom, email, téléphone, adresse, ville et site web de l'école courante.")
    public ResponseEntity<Map<String, Object>> lire() {
        return ResponseEntity.ok(service.lire());
    }

    @PutMapping("/admin/coordonnees")
    @Operation(summary = "Enregistrer les coordonnées de l'école",
            description = "Corps {name?, email?, phone?, address?, city?, website?} : seuls les champs envoyés changent ; « » efface un champ facultatif.")
    public ResponseEntity<Map<String, Object>> enregistrer(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(service.enregistrer(corps));
    }
}
