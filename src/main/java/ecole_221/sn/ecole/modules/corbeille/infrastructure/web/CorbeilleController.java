package ecole_221.sn.ecole.modules.corbeille.infrastructure.web;

import ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Corbeille de l'école courante (V21). Accès : tout rôle disposant d'un espace (SecurityConfig, anyRequest) ; le
 * personnel passe la matrice des droits par une route commune (CataloguePermissions.routeCommune), et le service
 * contrôle chaque élément (auteur, administration, droit d'écriture du module, enseignant du cours).
 */
@RestController
@RequestMapping("/corbeille")
@Tag(name = "Corbeille", description = "Éléments supprimés : restauration pendant 30 jours, puis suppression définitive")
public class CorbeilleController {

    private final CorbeilleService corbeille;

    public CorbeilleController(CorbeilleService corbeille) {
        this.corbeille = corbeille;
    }

    @GetMapping
    @Operation(summary = "Éléments supprimés", description = "L'administration voit toute la corbeille de l'école ; "
            + "les autres comptes voient ce qu'ils ont supprimé. Filtre facultatif ?type=devoir, salle…")
    public Map<String, Object> lister(@RequestParam(required = false) String type) {
        return corbeille.lister(type);
    }

    @PostMapping("/{type}/{id}/restaurer")
    @Operation(summary = "Restaurer un élément", description = "Auteur de la suppression ou administration de l'école ; "
            + "409 si l'élément dépend d'un élément supprimé ou d'une période clôturée.")
    public Map<String, Object> restaurer(@PathVariable String type, @PathVariable String id) {
        return corbeille.restaurer(type, id);
    }

    @PostMapping("/{type}/{id}/purger")
    @Operation(summary = "Supprimer définitivement", description = "Administration de l'école seulement ; corps {\"confirmer\": true} obligatoire.")
    public Map<String, Object> purger(@PathVariable String type, @PathVariable String id,
                                      @RequestBody(required = false) Map<String, Object> corps) {
        boolean confirme = corps != null && Boolean.TRUE.equals(corps.get("confirmer"));
        return corbeille.purger(type, id, confirme);
    }
}
