package ecole_221.sn.ecole.modules.admin.transferts;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Transferts d'élèves entre écoles (2026-11-13). Accès : administration de l'école et personnel ayant la permission
 * « transferts » dans la matrice des droits (PermissionsInterceptor).
 */
@RestController
@RequestMapping("/admin/transferts")
public class TransfertsController {

    private final TransfertsElevesService transferts;
    private final DossierTransfertService dossiers;

    public TransfertsController(TransfertsElevesService transferts, DossierTransfertService dossiers) {
        this.transferts = transferts;
        this.dossiers = dossiers;
    }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
    }

    private static String champ(Map<String, Object> corps, String cle) {
        Object v = corps == null ? null : corps.get(cle);
        if (v != null && !(v instanceof String)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ « " + cle + " » invalide.");
        }
        return (String) v;
    }

    @Operation(summary = "Écoles d'accueil possibles", description = "Écoles actives dont le nom ou l'identifiant contient le texte (2 caractères au moins), hors la vôtre.")
    @GetMapping("/ecoles")
    public ResponseEntity<List<Map<String, Object>>> ecoles(@RequestParam(value = "recherche", required = false) String recherche) {
        return ResponseEntity.ok(transferts.ecolesDestinataires(recherche));
    }

    @Operation(summary = "Demandes de transfert", description = "Demandes envoyées (sortants) et reçues (entrants) par l'école.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> liste() {
        return ResponseEntity.ok(transferts.liste());
    }

    @Operation(summary = "Demander le transfert d'un élève", description = "Corps {eleveId, ecoleDestinationId, motif?}. L'école d'accueil devra accepter.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> demander(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferts.demander(champ(corps, "eleveId"),
                champ(corps, "ecoleDestinationId"), champ(corps, "motif"), auteur()));
    }

    @Operation(summary = "Accepter un transfert reçu", description = "L'élève rejoint votre école ; une copie de son dossier vous est remise. L'école d'origine garde ses archives.")
    @PostMapping("/{id}/accepter")
    public ResponseEntity<Map<String, Object>> accepter(@PathVariable("id") String id) {
        DemandeTransfertJpaEntity d = transferts.demandeRecueEnAttente(id);
        Organization source = transferts.ecole(d.getEcoleSourceId());
        // Dossier lu DANS l'école d'origine (transaction séparée), puis contexte rétabli
        Organization ici = TenantContext.getCurrentOrganization().orElse(null);
        Map<String, Object> dossier;
        try {
            TenantContext.setCurrentOrganization(source);
            dossier = dossiers.construire(d.getEleveId(), source.getName());
        } finally {
            TenantContext.setCurrentOrganization(ici);
        }
        return ResponseEntity.ok(transferts.accepter(id, dossier, auteur()));
    }

    @Operation(summary = "Refuser un transfert reçu", description = "Corps {motif} obligatoire : il est transmis à l'école d'origine.")
    @PostMapping("/{id}/refuser")
    public ResponseEntity<Map<String, Object>> refuser(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(transferts.refuser(id, champ(corps, "motif"), auteur()));
    }

    @Operation(summary = "Annuler une demande envoyée", description = "Seulement tant qu'elle est en attente.")
    @PostMapping("/{id}/annuler")
    public ResponseEntity<Map<String, Object>> annuler(@PathVariable("id") String id) {
        return ResponseEntity.ok(transferts.annuler(id, auteur()));
    }

    @Operation(summary = "Dossier transféré", description = "Copie figée du dossier remise à l'école d'accueil après acceptation.")
    @GetMapping("/{id}/dossier")
    public ResponseEntity<Map<String, Object>> dossier(@PathVariable("id") String id) {
        return ResponseEntity.ok(transferts.dossier(id));
    }
}
