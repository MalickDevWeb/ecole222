package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.LienAccesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Liens d'accès émis par l'école (15/11) : activation d'un compte ou nouveau mot de passe, sans jamais écrire de mot de
 * passe dans un message. Réservé à l'administration de l'école (routes hors matrice des droits du personnel).
 */
@RestController
@Tag(name = "Admin — liens d'accès", description = "Lien d'activation ou de réinitialisation envoyé par WhatsApp, SMS ou e-mail")
public class LienAccesController {

    private final LienAccesService service;

    public LienAccesController(LienAccesService service) {
        this.service = service;
    }

    @PostMapping("/admin/comptes/{id}/lien-acces")
    @Operation(summary = "Émettre un lien d'accès",
            description = "Corps {canal : WHATSAPP | SMS | EMAIL}. Annule les liens précédents. WhatsApp / SMS : le serveur "
                    + "n'envoie rien, il renvoie {message, adresseEnvoi} (wa.me ou sms:) à ouvrir. Compte administrateur ou "
                    + "directeur, ou numéro modifié depuis moins de 24 h : {accordRequis : true, lienId} et rien n'est délivré "
                    + "avant l'accord d'un second administrateur ; compte à privilèges : {codeOral} à dire de vive voix.")
    public ResponseEntity<Map<String, Object>> emettre(@PathVariable("id") String userId,
                                                       @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(service.emettre(userId, corps));
    }

    @PostMapping("/admin/liens-acces/{lienId}/accord")
    @Operation(summary = "Accord d'un second administrateur",
            description = "Par un administrateur autre que le demandeur et le titulaire ; renvoie le lien à transmettre.")
    public ResponseEntity<Map<String, Object>> accorder(@PathVariable("lienId") String lienId) {
        return ResponseEntity.ok(service.accorder(lienId));
    }

    @GetMapping("/admin/liens-acces/en-attente")
    @Operation(summary = "Liens en attente d'accord", description = "Demandes de l'école qui attendent un second administrateur.")
    public ResponseEntity<List<Map<String, Object>>> enAttente() {
        return ResponseEntity.ok(service.enAttente());
    }
}
