package ecole_221.sn.ecole.modules.archivage.infrastructure.web;

import ecole_221.sn.ecole.modules.archivage.application.ArchivageEcolesService;
import ecole_221.sn.ecole.modules.archivage.application.ReglagesArchivageService;
import ecole_221.sn.ecole.modules.archivage.application.VueArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.partage.ChampsRequete;
import ecole_221.sn.ecole.partage.CompteCourant;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Archivage des écoles (décisions du Tech Lead du 16/11) — SUPER_ADMIN seul (PLATFORM_ADMIN refusé, 403).
 * Les écritures sont aussi inscrites au journal d'audit par JournalAuditInterceptor.
 */
@RestController
@RequestMapping("/v1/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ArchivesEcolesController {

    private final ArchivageEcolesService archivage;
    private final ReglagesArchivageService reglages;

    public ArchivesEcolesController(ArchivageEcolesService archivage, ReglagesArchivageService reglages) {
        this.archivage = archivage;
        this.reglages = reglages;
    }

    @Operation(summary = "Archives des écoles", description = "Archivages et leur état ; filtres facultatifs ?etat=ARCHIVEE et ?ecole=<id>.")
    @GetMapping("/archives")
    public ResponseEntity<List<Map<String, Object>>> lister(@RequestParam(value = "etat", required = false) String etat,
                                                            @RequestParam(value = "ecole", required = false) String ecole) {
        return ResponseEntity.ok(archivage.lister(etat, ecole).stream().map(a -> VueArchivage.de(a, false)).toList());
    }

    @Operation(summary = "Détail d'une archive", description = "État, comptes par table, purge, conflits et historique des étapes.")
    @GetMapping("/archives/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") String id) {
        Map<String, Object> vue = VueArchivage.de(archivage.detail(id), true);
        vue.put("journal", archivage.journal(id));
        return ResponseEntity.ok(vue);
    }

    @Operation(summary = "Archiver maintenant", description = "Corps {confirmationSlug, motif}. École suspendue seulement ; "
            + "202 : archivage lancé en arrière-plan (suivi par GET /archives/{id}).")
    @PostMapping("/tenants/{id}/archivage")
    public ResponseEntity<Map<String, Object>> archiver(@PathVariable("id") String ecoleId,
                                                        @RequestBody(required = false) Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        String confirmation = ChampsRequete.texte(corps, "confirmationSlug", "La confirmation", 255);
        String motif = ChampsRequete.texte(corps, "motif", "Le motif", 1000);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VueArchivage.de(
                archivage.archiverMaintenant(ecoleId, confirmation, motif, CompteCourant.id(), CompteCourant.nomComplet()), false));
    }

    @Operation(summary = "Restaurer une école archivée", description = "Corps {confirmationSlug}. 202 : restauration lancée ; "
            + "en cas de conflit, l'archive reste intacte et la liste des conflits est rangée avec elle.")
    @PostMapping("/tenants/{id}/restauration")
    public ResponseEntity<Map<String, Object>> restaurer(@PathVariable("id") String ecoleId,
                                                         @RequestBody(required = false) Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        String confirmation = ChampsRequete.texte(corps, "confirmationSlug", "La confirmation", 255);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VueArchivage.de(
                archivage.restaurer(ecoleId, confirmation, CompteCourant.id(), CompteCourant.nomComplet()), false));
    }

    @Operation(summary = "Supprimer définitivement une archive", description = "Corps {\"confirmer\": true}. Seulement après "
            + "la fin de conservation (état SUPPRESSION_A_CONFIRMER). Jamais automatique.")
    @PostMapping("/tenants/{id}/suppression-definitive")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable("id") String ecoleId,
                                                         @RequestBody(required = false) Map<String, Object> corps) {
        Boolean confirmer = ChampsRequete.booleenOptionnel(corps, "confirmer", "La confirmation");
        if (!Boolean.TRUE.equals(confirmer)) {
            throw ArchivageException.invalide("Confirmez la suppression définitive : {\"confirmer\": true}.");
        }
        return ResponseEntity.ok(VueArchivage.de(
                archivage.supprimerDefinitivement(ecoleId, true, CompteCourant.id(), CompteCourant.nomComplet()), false));
    }

    @Operation(summary = "Réglages de l'archivage", description = "Seuil de suspension avant archivage et durée de conservation "
            + "(jours), bornes, dernière modification, stockage utilisé.")
    @GetMapping("/parametres/archivage")
    public ResponseEntity<Map<String, Object>> lireReglages() {
        return ResponseEntity.ok(vueReglages(reglages.lire()));
    }

    @Operation(summary = "Modifier les réglages de l'archivage", description = "Corps {seuilJours?, conservationJours?} : seuil "
            + "7 à 365 jours, conservation 15 à 365 jours ; sans effet sur les archives déjà créées.")
    @PutMapping("/parametres/archivage")
    public ResponseEntity<Map<String, Object>> modifierReglages(@RequestBody(required = false) Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        Integer seuil = ChampsRequete.entierOptionnel(corps, "seuilJours", "Le seuil d'archivage", Integer.MIN_VALUE, Integer.MAX_VALUE);
        Integer conservation = ChampsRequete.entierOptionnel(corps, "conservationJours", "La durée de conservation",
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        return ResponseEntity.ok(vueReglages(reglages.modifier(seuil, conservation, CompteCourant.id(), CompteCourant.nomComplet())));
    }

    private Map<String, Object> vueReglages(ReglagesArchivageService.Valeurs v) {
        Map<String, Object> m = new LinkedHashMap<>(ReglagesArchivageService.vue(v));
        m.put("stockage", archivage.nomStockage());
        String pret = null;
        try {
            archivage.verifierPret();
        } catch (ArchivageException e) {
            pret = e.getMessage();
        }
        m.put("stockagePret", pret == null);
        m.put("messageStockage", pret);
        m.put("ecolesAuDelaDuSeuil", archivage.ecolesAuDelaDuSeuil(v.reglages().seuilJours()));
        return m;
    }
}
