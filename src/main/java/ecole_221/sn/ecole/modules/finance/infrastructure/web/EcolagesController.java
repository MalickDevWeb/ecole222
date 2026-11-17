package ecole_221.sn.ecole.modules.finance.infrastructure.web;

import ecole_221.sn.ecole.modules.finance.application.service.EcolagesService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Écolages : ouverture du dossier financier d'un élève et encaissement des versements. Droits :
 * module « finances » de la matrice de l'école (écriture), comme le reste de /finance.
 */
@RestController
@RequestMapping("/finance/ecolages")
public class EcolagesController {

    private final EcolagesService ecolages;

    public EcolagesController(EcolagesService ecolages) {
        this.ecolages = ecolages;
    }

    @Operation(summary = "Ouvrir le dossier financier d'un élève",
            description = "Corps : {eleveId, montantDu, classe?}. Un seul dossier par élève ; élève de l'école uniquement.")
    @PostMapping("/dossiers")
    public ResponseEntity<Map<String, Object>> ouvrir(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ecolages.ouvrirDossier(corps));
    }

    @Operation(summary = "Encaisser un versement",
            description = "Corps : {eleveId, montant, moyen?, reference?, date?}. Payé, reste dû et statut recalculés.")
    @PostMapping("/versements")
    public ResponseEntity<Map<String, Object>> encaisser(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ecolages.encaisser(corps));
    }
}
