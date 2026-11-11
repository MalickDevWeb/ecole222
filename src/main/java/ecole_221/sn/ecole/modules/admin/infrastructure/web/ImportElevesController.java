package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ImportElevesService;
import ecole_221.sn.ecole.modules.admin.application.service.ImportElevesService.LigneImport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Import groupé d'élèves (accès administration : règle /admin/** de SecurityConfig). */
@RestController
@Tag(name = "Admin — import d'élèves", description = "Import groupé d'élèves depuis un fichier CSV")
public class ImportElevesController {

    private final ImportElevesService service;

    public ImportElevesController(ImportElevesService service) {
        this.service = service;
    }

    public record DemandeImport(List<LigneImport> eleves) {}

    @PostMapping("/admin/students/import")
    @Operation(summary = "Importer des élèves",
            description = "Corps {eleves:[{ligne, nom, email, matricule?, telephone?, classe?}]}. "
                    + "?simulation=true vérifie sans rien écrire ; sinon tout ou rien : une ligne en erreur et aucun compte n'est créé. "
                    + "Chaque élève créé reçoit un mot de passe provisoire (renvoyé une seule fois).")
    public ResponseEntity<Map<String, Object>> importer(@RequestBody(required = false) DemandeImport demande,
                                                        @RequestParam(value = "simulation", defaultValue = "false") boolean simulation) {
        return ResponseEntity.ok(service.importer(demande == null ? null : demande.eleves(), simulation));
    }
}
