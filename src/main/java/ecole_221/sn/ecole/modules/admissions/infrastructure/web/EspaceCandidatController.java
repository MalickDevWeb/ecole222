package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.application.service.EspaceCandidatService;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;
import ecole_221.sn.ecole.modules.media.infrastructure.web.RemiseFichier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Espace du candidat et échanges avec l'école.
 * <ul>
 *   <li>Candidat (portail public) : routes /candidatures/suivi/… avec l'en-tête {@code X-Jeton-Suivi} reçu au suivi ;
 *       uniquement SON dossier.</li>
 *   <li>Administration : /admissions/candidats/{id}/messages (droit « admissions »).</li>
 * </ul>
 */
@RestController
@Tag(name = "Admissions")
public class EspaceCandidatController {

    static final String ENTETE = "X-Jeton-Suivi";
    private final EspaceCandidatService espace;
    private final MediaStoragePort stockage;

    public EspaceCandidatController(EspaceCandidatService espace, MediaStoragePort stockage) {
        this.espace = espace;
        this.stockage = stockage;
    }

    // ───────── Candidat ─────────

    @GetMapping("/candidatures/suivi/dossier")
    @Operation(summary = "Mon dossier (candidat)", description = "En-tête X-Jeton-Suivi ; 401 si la session de suivi a expiré.")
    public Map<String, Object> dossier(@RequestHeader(value = ENTETE, required = false) String jeton) {
        return espace.dossier(espace.dossierDuJeton(jeton));
    }

    @GetMapping("/candidatures/suivi/messages")
    @Operation(summary = "Mes échanges avec l'école (candidat)")
    public List<Map<String, Object>> mesMessages(@RequestHeader(value = ENTETE, required = false) String jeton) {
        return espace.lireMessages(espace.dossierDuJeton(jeton).getId(), "CANDIDAT");
    }

    @PostMapping("/candidatures/suivi/messages")
    @Operation(summary = "Écrire à l'école (candidat)", description = "Corps {texte} (2000 caractères au maximum).")
    public ResponseEntity<Map<String, Object>> ecrire(@RequestHeader(value = ENTETE, required = false) String jeton,
                                                      @RequestBody(required = false) Map<String, Object> corps) {
        CandidateJpaEntity c = espace.dossierDuJeton(jeton);
        return ResponseEntity.status(HttpStatus.CREATED).body(espace.messageCandidat(c, texte(corps, "texte")));
    }

    @GetMapping("/candidatures/suivi/piece")
    @Operation(summary = "Une de mes pièces (candidat)", description = "?code=pieceIdentite|diplome|releves|photo|<pièce exigée>")
    public ResponseEntity<byte[]> maPiece(@RequestHeader(value = ENTETE, required = false) String jeton, @RequestParam("code") String code) {
        CandidateJpaEntity c = espace.dossierDuJeton(jeton);
        return RemiseFichier.remettre(stockage, espace.lienPiece(c, code), code);
    }

    @PostMapping("/candidatures/suivi/pieces")
    @Operation(summary = "Ajouter ou remplacer une pièce (candidat)", description = "Corps {code, lien} ; 409 si la décision est rendue.")
    public List<Map<String, Object>> deposerPiece(@RequestHeader(value = ENTETE, required = false) String jeton,
                                                  @RequestBody(required = false) Map<String, Object> corps) {
        CandidateJpaEntity c = espace.dossierDuJeton(jeton);
        return espace.deposerPiece(c, texte(corps, "code"), texte(corps, "lien"));
    }

    // ───────── Administration ─────────

    @GetMapping({"/admissions/candidats/{id}/messages", "/v1/admissions/candidats/{id}/messages"})
    @Operation(summary = "Échanges avec un candidat")
    public List<Map<String, Object>> messages(@PathVariable String id) {
        return espace.lireMessages(id, "ECOLE");
    }

    @PostMapping({"/admissions/candidats/{id}/messages", "/v1/admissions/candidats/{id}/messages"})
    @Operation(summary = "Écrire à un candidat", description = "Corps {texte} ; le candidat est prévenu par e-mail.")
    public ResponseEntity<Map<String, Object>> ecrireAuCandidat(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espace.messageEcole(id, auteur(), texte(corps, "texte")));
    }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            String nom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
            return nom.isEmpty() ? "L'école" : nom;
        }
        return "L'école";
    }

    private static String texte(Map<String, Object> corps, String cle) {
        Object v = corps == null ? null : corps.get(cle);
        return v == null ? null : String.valueOf(v);
    }
}
