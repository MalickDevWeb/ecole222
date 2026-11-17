package ecole_221.sn.ecole.modules.notifications.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.notifications.application.AnnoncesEcoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Annonces et alertes de l'école (décision du Tech Lead du 15/11).
 *
 * <ul>
 *   <li>Administration (module « communication » de la matrice : lecture pour GET, écriture pour POST) :
 *       {@code GET /v1/notifications/annonces/apercu?destinataires=}, {@code POST /v1/notifications/annonces},
 *       {@code GET /v1/notifications/annonces} (historique) ;</li>
 *   <li>Destinataire, TOUS les rôles (SecurityConfig, routes communes de CataloguePermissions) :
 *       {@code GET /v1/notifications/mes}, {@code GET /v1/notifications/mes/non-lues},
 *       {@code PATCH /v1/notifications/{id}/lue}.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/v1/notifications", "/api/v1/notifications"})
@Tag(name = "Notifications", description = "Annonces de l'école et notifications du compte")
public class AnnoncesController {

    private final AnnoncesEcoleService service;

    public AnnoncesController(AnnoncesEcoleService service) {
        this.service = service;
    }

    private static User compte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User u ? u : null;
    }

    private static String compteId() {
        User u = compte();
        return u == null ? null : u.getId();
    }

    @GetMapping("/annonces/apercu")
    @Operation(summary = "Nombre de destinataires d'une annonce",
            description = "destinataires = TOUTE_ECOLE | ELEVES | PARENTS | PROFESSEURS | PERSONNEL → {destinataires, nombre}")
    public ResponseEntity<Map<String, Object>> apercu(@RequestParam(value = "destinataires", required = false) String destinataires) {
        return ResponseEntity.ok(service.apercu(destinataires));
    }

    @PostMapping("/annonces")
    @Operation(summary = "Envoyer une annonce",
            description = "Corps {titre (1-200), message (1-5000), destinataires, nature: ANNONCE|URGENCE}. Liste des comptes "
                    + "calculée par le serveur. 201 {id, destinatairesTouches, envoyeeLe} ; 409 aucun destinataire actif.")
    public ResponseEntity<Map<String, Object>> envoyer(@RequestBody(required = false) Map<String, Object> corps) {
        User u = compte();
        String nom = u == null ? null : ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.envoyer(corps, u == null ? null : u.getId(),
                nom == null || nom.isBlank() ? null : nom));
    }

    @GetMapping("/annonces")
    @Operation(summary = "Historique des annonces de l'école",
            description = "[{id, titre, nature, destinataires, destinatairesTouches, lues, envoyeeLe, auteur}]")
    public ResponseEntity<List<Map<String, Object>>> historique() {
        return ResponseEntity.ok(service.historique());
    }

    @GetMapping("/mes")
    @Operation(summary = "Mes notifications", description = "50 au plus, récentes d'abord : [{id, titre, message, nature, lue, recueLe}]")
    public ResponseEntity<List<Map<String, Object>>> mes() {
        return ResponseEntity.ok(service.mes(compteId()));
    }

    @GetMapping("/mes/non-lues")
    @Operation(summary = "Nombre de mes notifications non lues", description = "{nombre}")
    public ResponseEntity<Map<String, Object>> nonLues() {
        return ResponseEntity.ok(service.nonLues(compteId()));
    }

    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer MA notification comme lue", description = "204 ; 404 si elle n'est pas la mienne")
    public ResponseEntity<Void> marquerLue(@PathVariable("id") String id) {
        service.marquerLue(id, compteId());
        return ResponseEntity.noContent().build();
    }
}
