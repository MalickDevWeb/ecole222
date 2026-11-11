package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.parent.application.service.MessagerieParentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaEntity.ADMINISTRATION;

/**
 * Messagerie parent ↔ école. Les droits d'accès par préfixe sont ceux de SecurityConfig :
 * /parent/** (PARENT), /professor/** (professeurs), /admin/** (administration).
 */
@RestController
@Tag(name = "Messagerie parents", description = "Échanges entre les parents, les professeurs et l'administration")
public class MessagerieParentsController {

    private final MessagerieParentsService service;

    public MessagerieParentsController(MessagerieParentsService service) {
        this.service = service;
    }

    // ==================== Parent ====================

    @GetMapping("/parent/messages")
    @Operation(summary = "Mes échanges", description = "Messages envoyés et reçus par le parent, les plus récents d'abord")
    public ResponseEntity<List<Map<String, Object>>> filDuParent() {
        return ResponseEntity.ok(service.filDuParent(utilisateur()));
    }

    @GetMapping("/parent/messages/destinataires")
    @Operation(summary = "Destinataires possibles", description = "L'administration et les professeurs actifs de l'école")
    public ResponseEntity<List<Map<String, Object>>> destinataires() {
        utilisateur();
        return ResponseEntity.ok(service.destinataires());
    }

    @PostMapping("/parent/messages")
    @Operation(summary = "Écrire à l'école", description = "Corps {destinataireId (professeur ou ADMINISTRATION), sujet, message}")
    public ResponseEntity<Map<String, Object>> envoyer(@RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> c = corps == null ? Map.of() : corps;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.envoyerParParent(utilisateur(), texte(c, "destinataireId"), texte(c, "sujet"), texte(c, "message")));
    }

    @PatchMapping("/parent/messages/{id}/lu")
    @Operation(summary = "Marquer comme lu", description = "Réponse de l'école lue par le parent")
    public ResponseEntity<Map<String, Object>> luParParent(@PathVariable String id) {
        return ResponseEntity.ok(service.marquerLuParParent(utilisateur(), id));
    }

    // ==================== Professeur ====================

    @GetMapping("/professor/messages-parents")
    @Operation(summary = "Messages des parents", description = "Échanges avec les parents qui ont écrit à ce professeur")
    public ResponseEntity<List<Map<String, Object>>> boiteProfesseur() {
        return ResponseEntity.ok(service.boiteEcole(utilisateur()));
    }

    @PostMapping("/professor/messages-parents/{id}/repondre")
    @Operation(summary = "Répondre à un parent", description = "Corps {message}")
    public ResponseEntity<Map<String, Object>> repondreProfesseur(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        String moi = utilisateur();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.repondre(moi, moi, id, texte(corps, "message")));
    }

    @PatchMapping("/professor/messages-parents/{id}/lu")
    @Operation(summary = "Marquer comme lu", description = "Message d'un parent lu par le professeur")
    public ResponseEntity<Map<String, Object>> luProfesseur(@PathVariable String id) {
        return ResponseEntity.ok(service.marquerLuParEcole(utilisateur(), id));
    }

    // ==================== Administration ====================

    @GetMapping("/admin/messages-parents")
    @Operation(summary = "Messages des parents à l'administration", description = "Boîte partagée par les administrateurs de l'école")
    public ResponseEntity<List<Map<String, Object>>> boiteAdministration() {
        utilisateur();
        return ResponseEntity.ok(service.boiteEcole(ADMINISTRATION));
    }

    @PostMapping("/admin/messages-parents/{id}/repondre")
    @Operation(summary = "Répondre à un parent au nom de l'administration", description = "Corps {message}")
    public ResponseEntity<Map<String, Object>> repondreAdministration(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.repondre(ADMINISTRATION, utilisateur(), id, texte(corps, "message")));
    }

    @PatchMapping("/admin/messages-parents/{id}/lu")
    @Operation(summary = "Marquer comme lu", description = "Message d'un parent lu par l'administration")
    public ResponseEntity<Map<String, Object>> luAdministration(@PathVariable String id) {
        utilisateur();
        return ResponseEntity.ok(service.marquerLuParEcole(ADMINISTRATION, id));
    }

    private static String texte(Map<String, Object> corps, String cle) {
        Object v = corps == null ? null : corps.get(cle);
        return v == null ? null : String.valueOf(v);
    }

    private static String utilisateur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return u.getId();
    }
}
