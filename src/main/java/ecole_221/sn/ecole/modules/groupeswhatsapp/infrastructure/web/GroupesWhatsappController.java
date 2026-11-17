package ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.web;

import ecole_221.sn.ecole.modules.groupeswhatsapp.application.GroupesWhatsappService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Groupes WhatsApp de l'école et partage des annonces, sans API WhatsApp payante (FEAT-007, 16/11). */
@RestController
public class GroupesWhatsappController {

    private final GroupesWhatsappService service;

    public GroupesWhatsappController(GroupesWhatsappService service) {
        this.service = service;
    }

    @Operation(summary = "Groupes WhatsApp de l'école", description = "Administration (droit Communication) : groupes déclarés, avec leur public, leur classe et leur lien d'invitation.")
    @GetMapping("/admin/groupes-whatsapp")
    public ResponseEntity<List<Map<String, Object>>> lister() {
        return ResponseEntity.ok(service.lister());
    }

    @Operation(summary = "Déclarer un groupe WhatsApp", description = "Corps {nom, public: TOUTE_ECOLE|ELEVES|PARENTS|PROFESSEURS|PERSONNEL, classeId? (groupes Élèves ou Parents), lien: https://chat.whatsapp.com/…}. 400 lien ou public invalide ; 409 lien déjà enregistré.")
    @PostMapping("/admin/groupes-whatsapp")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(corps));
    }

    @Operation(summary = "Modifier un groupe WhatsApp", description = "Mêmes champs que la création, tous facultatifs, plus actif: true|false.")
    @PutMapping("/admin/groupes-whatsapp/{id}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(service.modifier(id, corps));
    }

    @Operation(summary = "Supprimer un groupe WhatsApp", description = "Retire le groupe de l'application (le groupe WhatsApp lui-même n'est pas touché) ; l'historique des partages est conservé.")
    @DeleteMapping("/admin/groupes-whatsapp/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Noter un partage dans un groupe", description = "Corps {titre} : l'annonce a été ouverte dans WhatsApp pour ce groupe (l'application ne sait pas si elle a été envoyée).")
    @PostMapping("/admin/groupes-whatsapp/{id}/partages")
    public ResponseEntity<Map<String, Object>> noterPartage(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.noterPartage(id, corps));
    }

    @Operation(summary = "Historique des partages WhatsApp", description = "100 derniers partages : groupe, titre de l'annonce, date et heure, auteur.")
    @GetMapping("/admin/groupes-whatsapp/partages")
    public ResponseEntity<List<Map<String, Object>>> historique() {
        return ResponseEntity.ok(service.historique());
    }

    @Operation(summary = "Mes groupes WhatsApp à rejoindre", description = "Élève : groupes « Toute l'école » et « Élèves » (de sa classe ou sans classe). Parent : « Toute l'école » et « Parents » (classes de ses enfants ou sans classe).")
    @GetMapping("/groupes-whatsapp/mes")
    public ResponseEntity<List<Map<String, Object>>> mesGroupes() {
        return ResponseEntity.ok(service.mesGroupes());
    }
}
