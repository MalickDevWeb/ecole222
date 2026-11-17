package ecole_221.sn.ecole.modules.pointage.infrastructure.web;

import ecole_221.sn.ecole.modules.pointage.application.PointagesService;
import ecole_221.sn.ecole.modules.security.application.service.TerminauxAccesService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static ecole_221.sn.ecole.partage.ChampsRequete.corpsRequis;

/** Pointage d'arrivée et de départ par le QR d'entrée du vigile. */
@RestController
public class PointagesController {

    private final PointagesService service;
    /** Terminal affecté au vigile : s'il est désactivé ou révoqué, le QR d'entrée n'est plus délivré (APP-BE-21). */
    @Autowired(required = false)
    private TerminauxAccesService terminauxAcces;

    public PointagesController(PointagesService service) {
        this.service = service;
    }

    @Operation(summary = "QR d'entrée de l'école", description = "Vigile : QR signé par le serveur, valable une minute ; à renouveler après renouvelerDansSecondes. 403 si le terminal du vigile est désactivé ou révoqué.")
    @GetMapping("/vigil/qr-entree")
    public ResponseEntity<Map<String, Object>> qrEntree() {
        if (terminauxAcces != null) {
            terminauxAcces.exigerTerminalUtilisable(PointagesService.compteConnecte().getId());
        }
        return ResponseEntity.ok(service.qrEntree());
    }

    @Operation(summary = "Pointer son arrivée ou son départ", description = "Élève ou professeur : corps {qr} = contenu du QR d'entrée scanné. Le serveur décide arrivée ou départ. 400 QR étranger, expiré ou d'une autre école ; 409 second pointage en moins de 2 minutes.")
    @PostMapping("/pointages")
    public ResponseEntity<Map<String, Object>> pointer(@RequestBody(required = false) Map<String, Object> corps) {
        corpsRequis(corps);
        Object qr = corps.get("qr");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.pointer(qr instanceof String s ? s : null));
    }

    @Operation(summary = "Pointages du jour (administration)", description = "Pointages de l'école regroupés par personne : arrivée, départ, présence actuelle. date=AAAA-MM-JJ (aujourd'hui par défaut), role=ETUDIANT|PROFESSEUR, de / a = plage horaire HH:MM (personnes ayant un passage dans la plage). Module « Sécurité et audit » de la matrice des droits.")
    @GetMapping("/admin/pointages")
    public ResponseEntity<Map<String, Object>> pointagesDuJour(@RequestParam(required = false) String date,
                                                               @RequestParam(required = false) String role,
                                                               @RequestParam(required = false) String de,
                                                               @RequestParam(required = false) String a) {
        return ResponseEntity.ok(service.pointagesDuJour(date, role, de, a));
    }

    @Operation(summary = "Mes pointages", description = "60 derniers pointages du compte connecté, nombre du jour et sens du prochain pointage.")
    @GetMapping("/pointages/moi")
    public ResponseEntity<Map<String, Object>> mesPointages() {
        return ResponseEntity.ok(service.mesPointages());
    }
}
