package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.IdentiteVisuelleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Administration", description = "Identité visuelle de l'école")
public class IdentiteVisuelleController {

    private final IdentiteVisuelleService service;

    public IdentiteVisuelleController(IdentiteVisuelleService service) {
        this.service = service;
    }

    @GetMapping("/admin/identite-visuelle")
    @Operation(summary = "Identité visuelle de l'école", description = "Logo, couleur principale et fond de l'écran de connexion.")
    public ResponseEntity<Map<String, Object>> lire() {
        return ResponseEntity.ok(service.lire());
    }

    @PutMapping("/admin/identite-visuelle")
    @Operation(summary = "Enregistrer l'identité visuelle",
            description = "Corps {logoUrl?, primaryColor?, backgroundUrl?} : liens http(s) (images envoyées via /media/upload), couleur #RRGGBB.")
    public ResponseEntity<Map<String, Object>> enregistrer(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(service.enregistrer(corps));
    }
}
